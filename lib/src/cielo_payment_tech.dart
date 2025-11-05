import 'package:flutter/services.dart';
import 'package:cielo_payment_tech/src/interface/handler/icielo_handler.dart';
import 'package:cielo_payment_tech/src/payments/payment.dart';

class CieloPaymentTech {
  static const channelName = "cielo_payment_tech";
  final MethodChannel _channel;
  Payment? _payment;

  static CieloPaymentTech? _instance;

  CieloPaymentTech(this._channel);

  static CieloPaymentTech get I {
    _instance ??= CieloPaymentTech(const MethodChannel(channelName));
    return _instance!;
  }

  /// Function to initialize payment and register the notification handler
  /// As chaves de licença são lidas automaticamente do arquivo local.properties do Android
  Payment initPayment({
    required ICieloHandler handler,
    String? urlCallback,
  }) {
    if (_payment != null) {
      return _payment!;
    }
    _payment = Payment.create(
      channel: _channel,
      paymentHandler: handler,
      urlCallback: urlCallback ?? "order://payment",
    );
    return _payment!;
  }

  /// Get the Payment object. Note: It needs to be initialized.
  Payment get payment {
    if (_payment == null) {
      throw Exception("PAYMENT NEEDS TO BE INITIALIZED! \n TRY: CieloPaymentTech.I.initPayment(handler)");
    }
    return _payment!;
  }
}
