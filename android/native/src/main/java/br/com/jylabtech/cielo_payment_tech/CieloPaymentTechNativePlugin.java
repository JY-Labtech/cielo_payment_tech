package br.com.jylabtech.cielo_payment_tech;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;

import br.com.jylabtech.cielo_payment_tech.core.PaymentMethod;
import br.com.jylabtech.cielo_payment_tech.licence.LicenceConfig;
import br.com.jylabtech.cielo_payment_tech.payments.CieloPaymentPresenter;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

public class CieloPaymentTechNativePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  private MethodChannel channel;
  private Context context;
  private CieloPaymentPresenter paymentPresenter;
  private ActivityPluginBinding activityBinding;
  private String cachedLicenceKey;
  private String cachedLicenceInternalKey;

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

      ensurePresenter();

      switch (paymentMethod) {
        case INIT_PAYMENT:
          String licenceKey = call.argument("licenceKey");
          String licenceInternalKey = call.argument("licenceInternalKey");

          if (licenceKey == null || licenceKey.trim().isEmpty()) {
            result.error("INVALID_ARGUMENT", "licenceKey é obrigatório", null);
            return;
          }

          // Opcional: a URL efetiva é sempre resolvida para o servidor oficial
          // (LicenceConfig.resolveLicenceUrl); valores fora dele são ignorados.
          if (licenceInternalKey == null || licenceInternalKey.trim().isEmpty()) {
            licenceInternalKey = LicenceConfig.LICENCE_CHECK_URL;
          }

          licenceKey = licenceKey.trim();
          cachedLicenceKey = licenceKey;
          cachedLicenceInternalKey = licenceInternalKey;
          paymentPresenter.updateLicenceKeys(licenceKey, licenceInternalKey);
          result.success(true);
          break;

        case PAYMENT_CHECKOUT:
          String paymentRequestJson = call.argument("paymentRequest");
          String urlCallback = call.argument("urlCallback");
          if (paymentRequestJson == null || urlCallback == null) {
            result.error("INVALID_ARGUMENT", "paymentRequest e urlCallback são obrigatórios", null);
            return;
          }
          // Gate de licença: valida antes de abrir o LIO e responde o result.
          paymentPresenter.checkout(paymentRequestJson, urlCallback, result);
          break;

        case PAYMENT_CANCEL:
          String cancelRequestJson = call.argument("cancelRequest");
          String cancelUrlCallback = call.argument("urlCallback");
          if (cancelRequestJson == null || cancelUrlCallback == null) {
            result.error("INVALID_ARGUMENT", "cancelRequest e urlCallback são obrigatórios", null);
            return;
          }
          // Gate de licença: valida antes de abrir o LIO e responde o result.
          paymentPresenter.cancel(cancelRequestJson, cancelUrlCallback, result);
          break;

        case PRINT:
          String printRequestJson = call.argument("printRequest");
          String printUrlCallback = call.argument("urlCallback");
          if (printRequestJson == null || printUrlCallback == null) {
            result.error("INVALID_ARGUMENT", "printRequest e urlCallback são obrigatórios", null);
            return;
          }
          // Gate de licença: valida antes de abrir o LIO e responde o result.
          paymentPresenter.print(printRequestJson, printUrlCallback, result);
          break;

        case PAYMENT_CREDIT:
        case PAYMENT_DEBIT:
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

  private void ensurePresenter() {
    if (paymentPresenter == null) {
      paymentPresenter = new CieloPaymentPresenter(channel, context);
      if (cachedLicenceKey != null && cachedLicenceInternalKey != null) {
        paymentPresenter.updateLicenceKeys(cachedLicenceKey, cachedLicenceInternalKey);
      }
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
    binding.addOnNewIntentListener(intent -> handleIntent(intent));
    handleIntent(binding.getActivity().getIntent());
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    this.activityBinding = null;
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    this.activityBinding = binding;
    binding.addOnNewIntentListener(intent -> handleIntent(intent));
    handleIntent(binding.getActivity().getIntent());
  }

  @Override
  public void onDetachedFromActivity() {
    this.activityBinding = null;
  }

  private boolean handleIntent(Intent intent) {
    if (intent != null && intent.getData() != null) {
      Uri uri = intent.getData();
      if (uri.getScheme() != null && uri.getScheme().equals("order")
          && (uri.getHost() != null && (uri.getHost().equals("payment") || uri.getHost().equals("response")))) {
        ensurePresenter();
        paymentPresenter.handleDeepLink(uri);
        return true;
      }
    }
    return false;
  }
}

