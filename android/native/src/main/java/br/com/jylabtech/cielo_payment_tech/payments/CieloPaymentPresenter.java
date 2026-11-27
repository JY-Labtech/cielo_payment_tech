package br.com.jylabtech.cielo_payment_tech.payments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import br.com.jylabtech.cielo_payment_tech.core.CieloTech;
import io.flutter.plugin.common.MethodChannel;

/**
 * Abre o app LIO (Cielo) por deep link para pagamento, cancelamento e impressão.
 * A resposta do LIO chega depois pelo deep link de retorno ({@link #handleDeepLink}).
 */
public class CieloPaymentPresenter {
    private static final String TAG = "CieloPaymentPresenter";

    static final String LIO_PAYMENT = "lio://payment";
    static final String LIO_PAYMENT_REVERSAL = "lio://payment-reversal";
    static final String LIO_PRINT = "lio://print";

    /** Abre uma URL do LIO. Retorna false se o app LIO não estiver instalado. */
    interface LioLauncher {
        boolean launch(String url) throws Exception;
    }

    private final MethodChannel channel;
    private final CieloTech cieloTech;
    private final LioLauncher launcher;

    public CieloPaymentPresenter(MethodChannel channel, Context context) {
        this(channel, new CieloTech(channel, context), new IntentLioLauncher(context));
    }

    CieloPaymentPresenter(MethodChannel channel, CieloTech cieloTech, LioLauncher launcher) {
        this.channel = channel;
        this.cieloTech = cieloTech;
        this.launcher = launcher;
    }

    /** Monta a URL do LIO: {@code <base>?request=<base64>&urlCallback=<callback>}. */
    static String buildLioUrl(String base, String requestBase64, String urlCallback) {
        return base + "?request=" + requestBase64 + "&urlCallback=" + urlCallback;
    }

    public void checkout(final String paymentRequestJson, final String urlCallback, final MethodChannel.Result result) {
        start(result, "checkout", LIO_PAYMENT, paymentRequestJson, urlCallback,
                "Pagamento iniciado. Aguardando resposta do LIO...");
    }

    public void cancel(final String cancelRequestJson, final String urlCallback, final MethodChannel.Result result) {
        start(result, "cancelamento", LIO_PAYMENT_REVERSAL, cancelRequestJson, urlCallback,
                "Cancelamento iniciado. Aguardando resposta do LIO...");
    }

    public void print(final String printRequestJson, final String urlCallback, final MethodChannel.Result result) {
        start(result, "impressão", LIO_PRINT, printRequestJson, urlCallback,
                "Impressão iniciada. Aguardando resposta do LIO...");
    }

    /** Abre o LIO e responde o MethodChannel.Result uma única vez. */
    private void start(MethodChannel.Result result, String operation, String base, String request,
                       String urlCallback, String startedMessage) {
        try {
            Log.i(TAG, "Iniciando " + operation + " com URL callback: " + urlCallback);
            cieloTech.setUrlCallback(urlCallback);

            if (launcher.launch(buildLioUrl(base, request, urlCallback))) {
                channel.invokeMethod("onMessage", startedMessage);
            } else {
                channel.invokeMethod("onError", "Erro: App LIO não instalado.");
            }
            result.success(true);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao iniciar " + operation, e);
            result.error("ERROR", e.getMessage(), null);
        }
    }

    /** Resposta do LIO (deep link) de uma operação já iniciada: sempre repassada ao app. */
    public void handleDeepLink(Uri uri) {
        cieloTech.handleDeepLink(uri);
    }

    public void dispose() {
        cieloTech.dispose();
    }

    /** Implementação real: dispara um Intent ACTION_VIEW para o LIO. */
    static final class IntentLioLauncher implements LioLauncher {
        private final Context context;

        IntentLioLauncher(Context context) {
            this.context = context;
        }

        @Override
        public boolean launch(String url) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (intent.resolveActivity(context.getPackageManager()) == null) {
                return false;
            }
            context.startActivity(intent);
            return true;
        }
    }
}
