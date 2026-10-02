import 'package:flutter/services.dart';
import 'package:cielo_payment_tech/src/interface/handler/icielo_handler.dart';
import 'package:cielo_payment_tech/src/payments/payment.dart';

class CieloPaymentTech {
  static const channelName = "cielo_payment_tech";

  /// URL de validação de licença do servidor oficial da JY Labtech, em base64
  /// (`https://licence-server.invalid/pos-payments/licence/check`).
  ///
  /// É o valor padrão de `licenceInternalKey`. O plugin nativo sempre valida
  /// nesse servidor: qualquer outro valor de `licenceInternalKey` é ignorado.
  static const licenceCheck =
      '***REMOVIDO***';
  final MethodChannel _channel;
  Payment? _payment;

  static CieloPaymentTech? _instance;

  CieloPaymentTech(this._channel);

  static CieloPaymentTech get I {
    _instance ??= CieloPaymentTech(const MethodChannel(channelName));
    return _instance!;
  }

  /// Inicializa o pagamento e envia as chaves de licença para o módulo nativo.
  ///
  /// `licenceInternalKey` é opcional (padrão: [licenceCheck]). Toda operação
  /// (pagamento, cancelamento, impressão) valida a licença no nativo antes de
  /// executar; com licença inválida a operação falha com `LICENCE_ERROR`,
  /// entregue em `handler.onError`.
  Payment initPayment({
    required ICieloHandler handler,
    required String licenceKey,
    String licenceInternalKey = licenceCheck,
    String? urlCallback,
  }) {
    final payment = Payment.create(
      channel: _channel,
      paymentHandler: handler,
      urlCallback: urlCallback ?? "order://payment",
      licenceKey: licenceKey,
      licenceInternalKey: licenceInternalKey,
    );
    _payment = payment;
    return payment;
  }

  /// Get the Payment object. Note: It needs to be initialized.
  Payment get payment {
    if (_payment == null) {
      throw Exception(
        "PAYMENT NEEDS TO BE INITIALIZED! \n TRY: CieloPaymentTech.I.initPayment(handler: handler, licenceKey: '...')",
      );
    }
    return _payment!;
  }
}
