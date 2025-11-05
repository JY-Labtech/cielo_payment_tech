import 'package:cielo_payment_tech/cielo_payment_tech.dart';

class PaymentHandler implements ICieloHandler {
  final Function(Map<String, dynamic>)? onSuccessCallback;
  final Function(String)? onErrorCallback;
  final Function(String)? onMessageCallback;

  PaymentHandler({
    this.onSuccessCallback,
    this.onErrorCallback,
    this.onMessageCallback,
  });

  @override
  Future<void> onTransactionSuccess(Map<String, dynamic> response) async {
    print('✅ Pagamento aprovado: $response');
    onSuccessCallback?.call(response);
  }

  @override
  Future<void> onError(String message) async {
    print('❌ Erro no pagamento: $message');
    onErrorCallback?.call(message);
  }

  @override
  Future<void> onMessage(String message) async {
    print('ℹ️ Mensagem: $message');
    onMessageCallback?.call(message);
  }
}
