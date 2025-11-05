package br.com.jylabtech.cielo_payment_tech_example;

import android.content.Intent;
import android.os.Bundle;

import io.flutter.embedding.android.FlutterActivity;

public class MainActivity extends FlutterActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Processa o intent inicial se houver deep link
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        // Processa o novo intent (deep link)
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        // O plugin CieloPaymentTechPlugin vai processar o deep link
        // através do ActivityAware. Esta função apenas garante que o intent
        // está disponível para o plugin.
    }
}
