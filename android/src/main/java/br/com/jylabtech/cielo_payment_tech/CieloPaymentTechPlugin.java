package br.com.jylabtech.cielo_payment_tech;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import br.com.jylabtech.cielo_payment_tech.core.PaymentMethod;
import br.com.jylabtech.cielo_payment_tech.payments.CieloPaymentPresenter;

/** CieloPaymentTechPlugin */
public class CieloPaymentTechPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  private MethodChannel channel;
  private Context context;
  private CieloPaymentPresenter paymentPresenter;
  private ActivityPluginBinding activityBinding;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "cielo_payment_tech");
    context = flutterPluginBinding.getApplicationContext();
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    try {
      PaymentMethod paymentMethod = PaymentMethod.fromString(call.method);
      
      if (paymentMethod == null) {
        if (call.method.equals("getPlatformVersion")) {
          result.success("Android " + android.os.Build.VERSION.RELEASE);
        } else {
          result.notImplemented();
        }
        return;
      }

      if (paymentPresenter == null) {
        paymentPresenter = new CieloPaymentPresenter(channel, context);
      }

      switch (paymentMethod) {
        case INIT_PAYMENT:
          // Inicialização do payment - licença é lida automaticamente do BuildConfig
          result.success(true);
          break;

        case PAYMENT_CHECKOUT:
          String paymentRequestJson = call.argument("paymentRequest");
          String urlCallback = call.argument("urlCallback");
          if (paymentRequestJson == null || urlCallback == null) {
            result.error("INVALID_ARGUMENT", "paymentRequest e urlCallback são obrigatórios", null);
            return;
          }
          try {
            paymentPresenter.checkout(paymentRequestJson, urlCallback);
            result.success(true);
          } catch (Exception e) {
            result.error("LICENCE_ERROR", e.getMessage(), null);
          }
          break;

        case PAYMENT_CANCEL:
          String cancelRequestJson = call.argument("cancelRequest");
          String cancelUrlCallback = call.argument("urlCallback");
          if (cancelRequestJson == null || cancelUrlCallback == null) {
            result.error("INVALID_ARGUMENT", "cancelRequest e urlCallback são obrigatórios", null);
            return;
          }
          try {
            paymentPresenter.cancel(cancelRequestJson, cancelUrlCallback);
            result.success(true);
          } catch (Exception e) {
            result.error("LICENCE_ERROR", e.getMessage(), null);
          }
          break;

        case PRINT:
          String printRequestJson = call.argument("printRequest");
          String printUrlCallback = call.argument("urlCallback");
          if (printRequestJson == null || printUrlCallback == null) {
            result.error("INVALID_ARGUMENT", "printRequest e urlCallback são obrigatórios", null);
            return;
          }
          try {
            paymentPresenter.print(printRequestJson, printUrlCallback);
            result.success(true);
          } catch (Exception e) {
            result.error("LICENCE_ERROR", e.getMessage(), null);
          }
          break;

        case PAYMENT_CREDIT:
        case PAYMENT_DEBIT:
          // Estes métodos podem ser implementados se necessário, ou usar PAYMENT_CHECKOUT
          result.notImplemented();
          break;

        default:
          result.notImplemented();
          break;
      }
    } catch (Exception e) {
      result.error("ERROR", e.getMessage(), null);
    }
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    if (paymentPresenter != null) {
      paymentPresenter.dispose();
      paymentPresenter = null;
    }
    channel.setMethodCallHandler(null);
    channel = null;
    context = null;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    this.activityBinding = binding;
    // Registrar listener para deep links
    binding.addOnNewIntentListener(new io.flutter.plugin.common.PluginRegistry.NewIntentListener() {
      @Override
      public boolean onNewIntent(Intent intent) {
        return handleIntent(intent);
      }
    });
    // Processar intent inicial se houver
    handleIntent(binding.getActivity().getIntent());
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.activityBinding = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    this.activityBinding = binding;
    binding.addOnNewIntentListener(new io.flutter.plugin.common.PluginRegistry.NewIntentListener() {
      @Override
      public boolean onNewIntent(Intent intent) {
        return handleIntent(intent);
      }
    });
    handleIntent(binding.getActivity().getIntent());
  }

  @Override
  public void onDetachedFromActivity() {
    this.activityBinding = null;
  }

  private boolean handleIntent(Intent intent) {
    if (intent != null && intent.getData() != null) {
      Uri uri = intent.getData();
      // Processa tanto order://payment quanto order://response
      if (uri.getScheme() != null && uri.getScheme().equals("order") 
          && (uri.getHost() != null && (uri.getHost().equals("payment") || uri.getHost().equals("response")))) {
        if (paymentPresenter == null) {
          paymentPresenter = new CieloPaymentPresenter(channel, context);
        }
        paymentPresenter.handleDeepLink(uri);
        return true;
      }
    }
    return false;
  }
}
