import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:cielo_payment_tech/src/interface/handler/icielo_handler.dart';
import 'package:cielo_payment_tech/src/interface/model/payment_request.dart';
import 'package:cielo_payment_tech/src/interface/model/payment_type.dart';
import 'package:cielo_payment_tech/src/interface/model/cancel_request.dart';
import 'package:cielo_payment_tech/src/interface/model/print_request.dart';

final class Payment {
  final MethodChannel channel;
  final ICieloHandler paymentHandler;
  final String urlCallback;

  // Cache da instância criada
  static Payment? _instance;

  Payment._({
    required this.channel,
    required this.paymentHandler,
    required this.urlCallback,
  }) {
    // Configura o handler de métodos do channel
    channel.setMethodCallHandler(_handleMethodCall);
  }

  /// Método factory assíncrono para criação controlada
  /// As chaves de licença são lidas automaticamente do BuildConfig (local.properties)
  static Payment create({
    required MethodChannel channel,
    required ICieloHandler paymentHandler,
    required String urlCallback,
  }) {
    // Se já existe instância, retorna ela diretamente
    if (_instance != null) {
      return _instance!;
    }

    // Cria nova instância
    final payment = Payment._(
      channel: channel,
      paymentHandler: paymentHandler,
      urlCallback: urlCallback,
    );

    // Salva instância
    _instance = payment;

    // Inicializa o payment no nativo
    // As chaves de licença são lidas automaticamente do BuildConfig
    channel.invokeMethod("initPayment");

    return payment;
  }

  Future<void> _handleMethodCall(MethodCall call) async {
    try {
      switch (call.method) {
        case "onTransactionSuccess":
          final responseJson = call.arguments as String;
          final responseMap = json.decode(responseJson) as Map<String, dynamic>;
          await paymentHandler.onTransactionSuccess(responseMap);
          break;

        case "onCancelSuccess":
          final responseJson = call.arguments as String;
          final responseMap = json.decode(responseJson) as Map<String, dynamic>;
          await paymentHandler.onCancelSuccess(responseMap);
          break;

        case "onPrintSuccess":
          final responseJson = call.arguments as String;
          final responseMap = json.decode(responseJson) as Map<String, dynamic>;
          await paymentHandler.onPrintSuccess(responseMap);
          break;

        case "onError":
          final errorMessage = call.arguments as String;
          await paymentHandler.onError(errorMessage);
          break;

        case "onMessage":
          final message = call.arguments as String;
          await paymentHandler.onMessage(message);
          break;

        default:
          break;
      }
    } catch (e) {
      await paymentHandler.onError('Erro ao processar callback: $e');
    }
  }

  /// Function to checkout payment
  Future<bool> checkout({
    required PaymentRequest paymentRequest,
  }) async {
    try {
      final paymentRequestJson = paymentRequest.toBase64Encode();
      await channel.invokeMethod("paymentCheckout", {
        "paymentRequest": paymentRequestJson,
        "urlCallback": urlCallback,
      });
      return true;
    } catch (e) {
      await paymentHandler.onError('Erro ao iniciar pagamento: $e');
      return false;
    }
  }

  /// Function to credit payment
  Future<bool> creditPayment({
    required int amount,
    required String accessToken,
    required String clientId,
    String? merchantCode,
    String? email,
    int installments = 0,
    List<PaymentItem>? items,
  }) async {
    final paymentRequest = PaymentRequest(
      accessToken: accessToken,
      clientId: clientId,
      email: email,
      installments: installments,
      items: items ?? [],
      merchantCode: merchantCode,
      paymentCode: PaymentType.creditoAvista.code,
      value: amount,
    );

    return await checkout(paymentRequest: paymentRequest);
  }

  /// Function to debit payment
  Future<bool> debitPayment({
    required int amount,
    required String accessToken,
    required String clientId,
    String? merchantCode,
    String? email,
    List<PaymentItem>? items,
  }) async {
    final paymentRequest = PaymentRequest(
      accessToken: accessToken,
      clientId: clientId,
      email: email,
      installments: 0,
      items: items ?? [],
      merchantCode: merchantCode,
      paymentCode: PaymentType.debitoAvista.code,
      value: amount,
    );

    return await checkout(paymentRequest: paymentRequest);
  }

  /// Function to cancel payment
  Future<bool> cancelPayment({
    required CancelRequest cancelRequest,
    String? urlCallback,
  }) async {
    try {
      final cancelRequestJson = cancelRequest.toBase64Encode();
      // Para cancelamento, usa order://response como callback padrão
      final callback = urlCallback ?? "order://response";

      await channel.invokeMethod("paymentCancel", {
        "cancelRequest": cancelRequestJson,
        "urlCallback": callback,
      });
      return true;
    } catch (e) {
      await paymentHandler.onError('Erro ao iniciar cancelamento: $e');
      return false;
    }
  }

  /// Function to print text or image
  Future<bool> print({
    required PrintRequest printRequest,
    String? urlCallback,
  }) async {
    try {
      final printRequestJson = printRequest.toBase64Encode();
      // Para impressão, usa order://response como callback padrão
      final callback = urlCallback ?? "order://response";

      await channel.invokeMethod("print", {
        "printRequest": printRequestJson,
        "urlCallback": callback,
      });
      return true;
    } catch (e) {
      await paymentHandler.onError('Erro ao iniciar impressão: $e');
      return false;
    }
  }

  /// Convenience method to print text
  Future<bool> printText({
    required String text,
    List<Map<String, dynamic>>? styles,
    String? urlCallback,
  }) async {
    final printRequest = PrintRequest.text(
      text: text,
      styles: styles,
    );
    return await print(
      printRequest: printRequest,
      urlCallback: urlCallback,
    );
  }

  /// Convenience method to print image
  Future<bool> printImage({
    required String imagePath,
    List<Map<String, dynamic>>? styles,
    String? urlCallback,
  }) async {
    final printRequest = PrintRequest.image(
      imagePath: imagePath,
      styles: styles,
    );
    return await print(
      printRequest: printRequest,
      urlCallback: urlCallback,
    );
  }

  /// Dispose resources
  void dispose() {
    channel.setMethodCallHandler(null);
    _instance = null;
  }
}
