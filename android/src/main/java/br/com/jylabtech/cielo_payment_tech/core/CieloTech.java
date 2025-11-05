package br.com.jylabtech.cielo_payment_tech.core;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import io.flutter.plugin.common.MethodChannel;

import org.json.JSONObject;

public class CieloTech {
    private static final String TAG = "CieloTech";
    private MethodChannel channel;
    private Context context;
    private String urlCallback;

    public CieloTech(MethodChannel channel, Context context) {
        this.channel = channel;
        this.context = context;
    }

    public void setUrlCallback(String urlCallback) {
        this.urlCallback = urlCallback;
    }

    public void handleDeepLink(Uri uri) {
        try {
            Log.i(TAG, "Processando deep link: " + uri.toString());
            
            // Verifica se é o callback de pagamento
            if (uri.getScheme() != null && uri.getScheme().equals("order") 
                && uri.getHost() != null && uri.getHost().equals("payment")) {
                
                String response = uri.getQueryParameter("response");
                String responseCode = uri.getQueryParameter("responsecode");

                if (response != null && !response.isEmpty()) {
                    try {
                        // Decodifica a resposta base64
                        byte[] decodedBytes = Base64.decode(response.replaceAll("\n", ""), Base64.DEFAULT);
                        String decodedResponse = new String(decodedBytes, "UTF-8");
                        
                        Log.i(TAG, "Resposta decodificada: " + decodedResponse);

                        if (responseCode != null) {
                            // Sucesso
                            JSONObject responseJson = new JSONObject(decodedResponse);
                            channel.invokeMethod("onTransactionSuccess", responseJson.toString());
                        } else {
                            // Erro
                            JSONObject responseJson = new JSONObject(decodedResponse);
                            String reason = responseJson.optString("reason", "Erro desconhecido no pagamento");
                            channel.invokeMethod("onError", reason);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Erro ao processar resposta", e);
                        channel.invokeMethod("onError", "Erro ao processar resposta: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao processar deep link", e);
            channel.invokeMethod("onError", "Erro ao processar deep link: " + e.getMessage());
        }
    }

    public void dispose() {
        // Limpar recursos se necessário
    }
}

