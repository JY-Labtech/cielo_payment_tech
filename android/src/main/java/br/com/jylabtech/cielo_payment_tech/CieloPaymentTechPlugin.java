package br.com.jylabtech.cielo_payment_tech;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * Ponte que expõe a implementação nativa empacotada no AAR.
 * Mantemos esta classe no diretório padrão para que o Flutter tooling
 * registre automaticamente o plugin, delegando toda a lógica para o
 * {@link CieloPaymentTechNativePlugin}.
 */
public final class CieloPaymentTechPlugin implements FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private final CieloPaymentTechNativePlugin delegate = new CieloPaymentTechNativePlugin();

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        delegate.onAttachedToEngine(binding);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        delegate.onDetachedFromEngine(binding);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        delegate.onMethodCall(call, result);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        delegate.onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        delegate.onDetachedFromActivityForConfigChanges();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        delegate.onReattachedToActivityForConfigChanges(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        delegate.onDetachedFromActivity();
    }
}

