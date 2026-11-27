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

  /// Inicializa o plugin e registra o [handler] que recebe os retornos do LIO.
  ///
  /// [urlCallback] é o deep link de retorno do pagamento (padrão:
  /// `order://payment`). Ele precisa estar declarado no `AndroidManifest.xml`
  /// do app. Retorna o [Payment] usado para pagar, cancelar e imprimir.
  Payment initPayment({
    required ICieloHandler handler,
    String? urlCallback,
  }) {
    final payment = Payment.create(
      channel: _channel,
      paymentHandler: handler,
      urlCallback: urlCallback ?? "order://payment",
    );
    _payment = payment;
    return payment;
  }

  /// Get the Payment object. Note: It needs to be initialized.
  Payment get payment {
    if (_payment == null) {
      throw Exception(
        "PAYMENT NEEDS TO BE INITIALIZED! \n TRY: CieloPaymentTech.I.initPayment(handler: handler)",
      );
    }
    return _payment!;
  }
}
