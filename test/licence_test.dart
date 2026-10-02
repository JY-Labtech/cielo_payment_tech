import 'dart:convert';

import 'package:cielo_payment_tech/cielo_payment_tech.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeHandler implements ICieloHandler {
  final errors = <String>[];
  final successes = <Map<String, dynamic>>[];

  @override
  Future<void> onError(String message) async => errors.add(message);

  @override
  Future<void> onMessage(String message) async {}

  @override
  Future<void> onTransactionSuccess(Map<String, dynamic> response) async => successes.add(response);

  @override
  Future<void> onCancelSuccess(Map<String, dynamic> response) async => successes.add(response);

  @override
  Future<void> onPrintSuccess(Map<String, dynamic> response) async => successes.add(response);
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel(CieloPaymentTech.channelName);
  const fakeKey = 'TEST-LICENCE-KEY-0000';
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  late List<MethodCall> calls;
  late _FakeHandler handler;
  late CieloPaymentTech plugin;
  Payment? payment;

  void mockNative(Future<Object?> Function(MethodCall call) answer) {
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return answer(call);
    });
  }

  Never licenceError() =>
      throw PlatformException(code: 'LICENCE_ERROR', message: 'Licença vencida');

  setUp(() {
    calls = [];
    handler = _FakeHandler();
    plugin = CieloPaymentTech(channel);
  });

  tearDown(() {
    payment?.dispose();
    payment = null;
    messenger.setMockMethodCallHandler(channel, null);
  });

  group('licenceCheck', () {
    test('decodifica para a URL do servidor oficial da JY Labtech', () {
      final url = Uri.parse(utf8.decode(base64.decode(CieloPaymentTech.licenceCheck)));
      expect(url.scheme, 'https');
      expect(url.host, 'portal-api.jylabtech.com.br');
      expect(url.path, startsWith('/pos-payments/'));
      expect(url.toString(), 'https://portal-api.jylabtech.com.br/pos-payments/licence/check');
    });
  });

  group('initPayment', () {
    test('envia licenceKey e o licenceInternalKey padrão quando omitido', () async {
      mockNative((call) async => true);

      payment = plugin.initPayment(handler: handler, licenceKey: fakeKey);
      await pumpEventQueue();

      final init = calls.singleWhere((c) => c.method == 'initPayment');
      final args = Map<String, dynamic>.from(init.arguments as Map);
      expect(args['licenceKey'], fakeKey);
      expect(args['licenceInternalKey'], CieloPaymentTech.licenceCheck);
    });

    test('envia o licenceInternalKey informado', () async {
      mockNative((call) async => true);
      const custom = 'aHR0cHM6Ly9wb3J0YWwtYXBpLmp5bGFidGVjaC5jb20uYnIvcG9zLXBheW1lbnRzL2xpY2VuY2UvY2hlY2svaW52b2ljZQ==';

      payment = plugin.initPayment(handler: handler, licenceKey: fakeKey, licenceInternalKey: custom);
      await pumpEventQueue();

      final args = Map<String, dynamic>.from(calls.single.arguments as Map);
      expect(args['licenceInternalKey'], custom);
    });

    test('erro do nativo ao configurar a licença chega em onError', () async {
      mockNative((call) async => throw PlatformException(code: 'INVALID_ARGUMENT', message: 'licenceKey é obrigatório'));

      payment = plugin.initPayment(handler: handler, licenceKey: '');
      await pumpEventQueue();

      expect(handler.errors, hasLength(1));
      expect(handler.errors.single, contains('INVALID_ARGUMENT'));
    });
  });

  group('operações com licença inválida (LICENCE_ERROR)', () {
    setUp(() {
      mockNative((call) async {
        if (call.method == 'initPayment') return true;
        licenceError();
      });
      payment = plugin.initPayment(handler: handler, licenceKey: fakeKey);
    });

    test('checkout retorna false e chama onError', () async {
      final ok = await payment!.creditPayment(amount: 1000, accessToken: 'token', clientId: 'client');

      expect(ok, isFalse);
      expect(handler.errors.single, contains('LICENCE_ERROR'));
      expect(handler.errors.single, contains('Licença vencida'));
      expect(handler.successes, isEmpty);
      expect(calls.map((c) => c.method), contains('paymentCheckout'));
    });

    test('débito retorna false e chama onError', () async {
      final ok = await payment!.debitPayment(amount: 1000, accessToken: 'token', clientId: 'client');

      expect(ok, isFalse);
      expect(handler.errors.single, contains('LICENCE_ERROR'));
    });

    test('cancelamento retorna false e chama onError', () async {
      final ok = await payment!.cancelPayment(
        cancelRequest: CancelRequest(
          id: 'order-id',
          clientId: 'client',
          accessToken: 'token',
          cieloCode: '123',
          authCode: '456',
          value: 1000,
        ),
      );

      expect(ok, isFalse);
      expect(handler.errors.single, contains('LICENCE_ERROR'));
      expect(calls.map((c) => c.method), contains('paymentCancel'));
    });

    test('impressão retorna false e chama onError', () async {
      final ok = await payment!.printText(text: 'teste');

      expect(ok, isFalse);
      expect(handler.errors.single, contains('LICENCE_ERROR'));
      expect(calls.map((c) => c.method), contains('print'));
    });
  });

  group('operações com licença válida', () {
    test('checkout retorna true quando o nativo aceita', () async {
      mockNative((call) async => true);
      payment = plugin.initPayment(handler: handler, licenceKey: fakeKey);

      final ok = await payment!.creditPayment(amount: 1000, accessToken: 'token', clientId: 'client');

      expect(ok, isTrue);
      expect(handler.errors, isEmpty);
    });
  });

  group('sem configurar a licença', () {
    test('acessar payment antes de initPayment lança exceção', () {
      expect(() => plugin.payment, throwsException);
    });
  });
}
