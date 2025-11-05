//Contract payment handler
abstract class ICieloHandler {
  Future<void> onTransactionSuccess(Map<String, dynamic> response);

  Future<void> onError(String message);

  Future<void> onMessage(String message);
}
