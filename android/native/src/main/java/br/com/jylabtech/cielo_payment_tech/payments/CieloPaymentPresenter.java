package br.com.jylabtech.cielo_payment_tech.payments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import br.com.jylabtech.cielo_payment_tech.core.CieloTech;
import br.com.jylabtech.cielo_payment_tech.licence.CheckLicenceTech;
import br.com.jylabtech.cielo_payment_tech.licence.LicenceResult;
import br.com.jylabtech.cielo_payment_tech.licence.LicenceRules;
import io.flutter.plugin.common.MethodChannel;

public class CieloPaymentPresenter {
    private static final String TAG = "CieloPaymentPresenter";
    private final MethodChannel channel;
    private final Context context;
    private final CieloTech cieloTech;
    private final CheckLicenceTech checkLicenceTech;
    private final ExecutorService licenceExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String licenceKey;
    private String licenceInternalKey;

    public CieloPaymentPresenter(MethodChannel channel, Context context) {
        this.channel = channel;
        this.context = context;
        this.cieloTech = new CieloTech(channel, context);
        this.checkLicenceTech = new CheckLicenceTech(context);
    }

    public void updateLicenceKeys(String licenceKey, String licenceInternalKey) {
        this.licenceKey = licenceKey;
        this.licenceInternalKey = licenceInternalKey;
        channel.invokeMethod("onMessage", "Licença recebida. Validando...");
    }

    /** Resultado da checagem de licença, entregue na main thread. */
    public interface LicenceCallback {
        void onValid();

        void onInvalid(String message);
    }

    /**
     * Gate de licença: valida (cache válido ou online) numa thread de background e
     * responde na main thread. Toda operação de negócio passa por aqui antes de
     * chamar o LIO; se a licença for inválida a operação não é executada.
     */
    public void ensureLicence(final LicenceCallback callback) {
        final String key = licenceKey;
        final String internalKey = licenceInternalKey;
        licenceExecutor.execute(() -> {
            LicenceResult resultLicence;
            try {
                resultLicence = checkLicenceTech.validateLicence(key, internalKey);
            } catch (Exception e) {
                resultLicence = LicenceRules.error(e.getMessage());
            }
            final LicenceResult finalResult = resultLicence;
            mainHandler.post(() -> {
                if (finalResult != null && finalResult.isSuccess()) {
                    callback.onValid();
                    return;
                }
                String errorMessage = finalResult != null ? finalResult.getErrorMessage() : null;
                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Sua licença não foi ativada.";
                }
                callback.onInvalid(errorMessage);
            });
        });
    }

    /** Executa a operação só com licença válida e responde o MethodChannel.Result. */
    private void runWithLicence(final MethodChannel.Result result, final String operation, final Operation action) {
        ensureLicence(new LicenceCallback() {
            @Override
            public void onValid() {
                try {
                    action.run();
                    result.success(true);
                } catch (Exception e) {
                    Log.e(TAG, "Erro ao iniciar " + operation, e);
                    result.error("ERROR", e.getMessage(), null);
                }
            }

            @Override
            public void onInvalid(String message) {
                result.error("LICENCE_ERROR", message, null);
            }
        });
    }

    private interface Operation {
        void run() throws Exception;
    }

    public void checkout(final String paymentRequestJson, final String urlCallback, final MethodChannel.Result result) {
        runWithLicence(result, "checkout", () -> startCheckout(paymentRequestJson, urlCallback));
    }

    public void cancel(final String cancelRequestJson, final String urlCallback, final MethodChannel.Result result) {
        runWithLicence(result, "cancelamento", () -> startCancel(cancelRequestJson, urlCallback));
    }

    public void print(final String printRequestJson, final String urlCallback, final MethodChannel.Result result) {
        runWithLicence(result, "impressão", () -> startPrint(printRequestJson, urlCallback));
    }

    private void startCheckout(String paymentRequestJson, String urlCallback) throws Exception {
        try {
            Log.i(TAG, "Iniciando checkout com URL callback: " + urlCallback);

            cieloTech.setUrlCallback(urlCallback);

            String url = "lio://payment?request=" + paymentRequestJson + "&urlCallback=" + urlCallback;
            Uri uri = Uri.parse(url);

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                channel.invokeMethod("onMessage", "Pagamento iniciado. Aguardando resposta do LIO...");
            } else {
                channel.invokeMethod("onError", "Erro: App LIO não instalado.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar checkout", e);
            throw e;
        }
    }

    private void startCancel(String cancelRequestJson, String urlCallback) throws Exception {
        try {
            Log.i(TAG, "Iniciando cancelamento com URL callback: " + urlCallback);

            cieloTech.setUrlCallback(urlCallback);

            String url = "lio://payment-reversal?request=" + cancelRequestJson + "&urlCallback=" + urlCallback;
            Uri uri = Uri.parse(url);

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                channel.invokeMethod("onMessage", "Cancelamento iniciado. Aguardando resposta do LIO...");
            } else {
                channel.invokeMethod("onError", "Erro: App LIO não instalado.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar cancelamento", e);
            throw e;
        }
    }

    private void startPrint(String printRequestJson, String urlCallback) throws Exception {
        try {
            Log.i(TAG, "Iniciando impressão com URL callback: " + urlCallback);

            cieloTech.setUrlCallback(urlCallback);

            String url = "lio://print?request=" + printRequestJson + "&urlCallback=" + urlCallback;
            Uri uri = Uri.parse(url);

            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
                channel.invokeMethod("onMessage", "Impressão iniciada. Aguardando resposta do LIO...");
            } else {
                channel.invokeMethod("onError", "Erro: App LIO não instalado.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar impressão", e);
            throw e;
        }
    }

    /**
     * Resposta do LIO (deep link) de uma operação já iniciada: sempre repassada ao app.
     * A licença é exigida para iniciar a operação, não para receber o resultado
     * (senão uma transação aprovada no LIO viraria erro no app).
     */
    public void handleDeepLink(Uri uri) {
        cieloTech.handleDeepLink(uri);
    }

    public void dispose() {
        cieloTech.dispose();
        licenceExecutor.shutdown();
    }
}

