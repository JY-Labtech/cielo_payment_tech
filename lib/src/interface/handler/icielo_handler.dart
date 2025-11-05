//Contract payment handler
abstract class ICieloHandler {
  Future<void> onTransactionSuccess(Map<String, dynamic> response);

  Future<void> onCancelSuccess(Map<String, dynamic> response);

  Future<void> onPrintSuccess(Map<String, dynamic> response);

  Future<void> onError(String message);

  Future<void> onMessage(String message);
}
