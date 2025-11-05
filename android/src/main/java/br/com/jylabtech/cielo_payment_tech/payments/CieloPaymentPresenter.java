package br.com.jylabtech.cielo_payment_tech.payments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import io.flutter.plugin.common.MethodChannel;

import br.com.jylabtech.cielo_payment_tech.BuildConfig;
import br.com.jylabtech.cielo_payment_tech.core.CieloTech;
import br.com.jylabtech.cielo_payment_tech.licence.CheckLicenceTech;
import br.com.jylabtech.cielo_payment_tech.licence.LicenceResult;

public class CieloPaymentPresenter {
    private static final String TAG = "CieloPaymentPresenter";
    private MethodChannel channel;
    private Context context;
    private CieloTech cieloTech;
    private CheckLicenceTech checkLicenceTech;

    public CieloPaymentPresenter(MethodChannel channel, Context context) {
        this.channel = channel;
        this.context = context;
        this.cieloTech = new CieloTech(channel, context);
        this.checkLicenceTech = new CheckLicenceTech(context);
    }

    public boolean checkValidLicence() throws Exception {
        String licenceKey = BuildConfig.LICENCE_KEY;
        String licenceInternalKey = BuildConfig.LICENCE_INTERNAL_KEY;

        if (licenceKey == null || licenceKey.isEmpty() || licenceInternalKey == null || licenceInternalKey.isEmpty()) {
            throw new Exception("Licença não configurada. Configure a licença no arquivo local.properties do projeto Android.");
        }

        Log.i(TAG, "**Licença local: " + licenceKey);
        channel.invokeMethod("onMessage", "Verificando licença...");
        
        LicenceResult resultLicence = checkLicenceTech.validateLicence(licenceKey, licenceInternalKey);

        if (resultLicence.getResult() == 0) {
            Log.i(TAG, "*resultLicence: true");
            channel.invokeMethod("onMessage", "Licença ativada");
            return true;
        }

        Log.i(TAG, "*resultLicence: false");
        String errorMessage = resultLicence.getErrorMessage();
        if (errorMessage == null || errorMessage.isEmpty()) {
            errorMessage = "Sua licença não foi ativada.";
        }
        throw new Exception(errorMessage);
    }

    public void checkout(String paymentRequestJson, String urlCallback) throws Exception {
        // Valida licença antes de permitir o checkout
        if (!checkValidLicence()) {
            throw new Exception("Licença inválida. Não é possível realizar o pagamento.");
        }

        try {
            Log.i(TAG, "Iniciando checkout com URL callback: " + urlCallback);
            
            // Salva o urlCallback para usar quando receber o deep link
            cieloTech.setUrlCallback(urlCallback);
            
            // Cria a URL do LIO
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
            throw e; // Re-throw para o plugin tratar
        }
    }

    public void handleDeepLink(Uri uri) {
        cieloTech.handleDeepLink(uri);
    }

    public void dispose() {
        if (cieloTech != null) {
            cieloTech.dispose();
        }
    }
}
