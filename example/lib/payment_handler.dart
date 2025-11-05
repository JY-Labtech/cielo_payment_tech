import 'package:cielo_payment_tech/cielo_payment_tech.dart';

class PaymentHandler implements ICieloHandler {
  final Function(Map<String, dynamic>)? onSuccessCallback;
  final Function(Map<String, dynamic>)? onCancelSuccessCallback;
  final Function(Map<String, dynamic>)? onPrintSuccessCallback;
  final Function(String)? onErrorCallback;
  final Function(String)? onMessageCallback;

  PaymentHandler({
    this.onSuccessCallback,
    this.onCancelSuccessCallback,
    this.onPrintSuccessCallback,
    this.onErrorCallback,
    this.onMessageCallback,
  });

  @override
  Future<void> onTransactionSuccess(Map<String, dynamic> response) async {
    print('✅ Pagamento aprovado: $response');
    onSuccessCallback?.call(response);
  }

  @override
  Future<void> onCancelSuccess(Map<String, dynamic> response) async {
    print('✅ Cancelamento aprovado: $response');
    onCancelSuccessCallback?.call(response);
  }

  @override
  Future<void> onPrintSuccess(Map<String, dynamic> response) async {
    print('✅ Impressão concluída: $response');
    onPrintSuccessCallback?.call(response);
  }

  @override
  Future<void> onError(String message) async {
    print('❌ Erro: $message');
    onErrorCallback?.call(message);
  }

  @override
  Future<void> onMessage(String message) async {
    print('ℹ️ Mensagem: $message');
    onMessageCallback?.call(message);
  }
}
