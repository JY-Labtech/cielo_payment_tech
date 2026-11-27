import 'dart:convert';

import 'package:cielo_payment_tech/cielo_payment_tech.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

class _FakeHandler implements ICieloHandler {
  final errors = <String>[];
  final messages = <String>[];
  final transactions = <Map<String, dynamic>>[];
  final cancels = <Map<String, dynamic>>[];
  final prints = <Map<String, dynamic>>[];

  @override
  Future<void> onError(String message) async => errors.add(message);

  @override
  Future<void> onMessage(String message) async => messages.add(message);

  @override
  Future<void> onTransactionSuccess(Map<String, dynamic> response) async => transactions.add(response);

  @override
  Future<void> onCancelSuccess(Map<String, dynamic> response) async => cancels.add(response);

  @override
  Future<void> onPrintSuccess(Map<String, dynamic> response) async => prints.add(response);
}

Map<String, dynamic> _decodeBase64(String value) =>
    json.decode(utf8.decode(base64.decode(value))) as Map<String, dynamic>;

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel(CieloPaymentTech.channelName);
  final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

  late List<MethodCall> calls;
  late _FakeHandler handler;
  late CieloPaymentTech plugin;
  late Payment payment;

  void mockNative(Future<Object?> Function(MethodCall call) answer) {
    messenger.setMockMethodCallHandler(channel, (call) async {
      calls.add(call);
      return answer(call);
    });
  }

  /// Simula o nativo chamando o Dart (retorno do LIO por deep link).
  Future<void> nativeCalls(String method, Object? arguments) async {
    await messenger.handlePlatformMessage(
      CieloPaymentTech.channelName,
      const StandardMethodCodec().encodeMethodCall(MethodCall(method, arguments)),
      (_) {},
    );
  }

  Map<String, dynamic> argsOf(String method) =>
      Map<String, dynamic>.from(calls.singleWhere((c) => c.method == method).arguments as Map);

  setUp(() {
    calls = [];
    handler = _FakeHandler();
    plugin = CieloPaymentTech(channel);
    mockNative((call) async => true);
    payment = plugin.initPayment(handler: handler);
  });

  tearDown(() {
    payment.dispose();
    messenger.setMockMethodCallHandler(channel, null);
  });

  group('initPayment', () {
    test('não chama o nativo (sem configuração prévia)', () async {
      await pumpEventQueue();
      expect(calls, isEmpty);
    });

    test('usa order://payment como callback padrão', () {
      expect(payment.urlCallback, 'order://payment');
    });

    test('aceita urlCallback customizado', () {
      payment.dispose();
      payment = plugin.initPayment(handler: handler, urlCallback: 'meuapp://payment');
      expect(payment.urlCallback, 'meuapp://payment');
    });

    test('payment retorna a instância criada', () {
      expect(plugin.payment, same(payment));
    });

    test('acessar payment antes de initPayment lança exceção', () {
      expect(() => CieloPaymentTech(channel).payment, throwsException);
    });

    test('CieloPaymentTech.I é singleton', () {
      expect(CieloPaymentTech.I, same(CieloPaymentTech.I));
    });
  });

  group('checkout', () {
    test('envia paymentRequest em base64 e urlCallback', () async {
      final request = PaymentRequest(
        accessToken: 'token',
        clientId: 'client',
        installments: 3,
        items: [PaymentItem(name: 'Produto', quantity: 1, sku: '1', unitOfMeasure: 'UN', unitPrice: 1500)],
        paymentCode: PaymentType.creditoParceladoLoja.code,
        value: 1500,
      );

      final ok = await payment.checkout(paymentRequest: request);

      expect(ok, isTrue);
      final args = argsOf('paymentCheckout');
      expect(args['urlCallback'], 'order://payment');
      final decoded = _decodeBase64(args['paymentRequest'] as String);
      expect(decoded['paymentCode'], 'CREDITO_PARCELADO_LOJA');
      expect(decoded['installments'], 3);
      expect(decoded['value'], 1500);
      expect(decoded['clientID'], 'client');
      expect((decoded['items'] as List).single['name'], 'Produto');
    });

    test('erro do nativo retorna false e chama onError', () async {
      mockNative((call) async => throw PlatformException(code: 'ERROR', message: 'falhou'));

      final ok = await payment.checkout(
        paymentRequest: PaymentRequest(items: [], paymentCode: 'PIX', value: 100),
      );

      expect(ok, isFalse);
      expect(handler.errors.single, contains('Erro ao iniciar pagamento'));
      expect(handler.errors.single, contains('falhou'));
    });
  });

  group('creditPayment', () {
    test('monta CREDITO_AVISTA com os dados informados', () async {
      final ok = await payment.creditPayment(
        amount: 1000,
        accessToken: 'token',
        clientId: 'client',
        merchantCode: 'mc',
        email: 'a@b.com',
        installments: 2,
      );

      expect(ok, isTrue);
      final decoded = _decodeBase64(argsOf('paymentCheckout')['paymentRequest'] as String);
      expect(decoded['paymentCode'], 'CREDITO_AVISTA');
      expect(decoded['value'], 1000);
      expect(decoded['accessToken'], 'token');
      expect(decoded['clientID'], 'client');
      expect(decoded['merchantCode'], 'mc');
      expect(decoded['email'], 'a@b.com');
      expect(decoded['installments'], 2);
      expect(decoded['items'], isEmpty);
    });

    test('installments padrão é 0', () async {
      await payment.creditPayment(amount: 1000, accessToken: 't', clientId: 'c');
      final decoded = _decodeBase64(argsOf('paymentCheckout')['paymentRequest'] as String);
      expect(decoded['installments'], 0);
    });
  });

  group('debitPayment', () {
    test('monta DEBITO_AVISTA sem parcelas', () async {
      final ok = await payment.debitPayment(amount: 2500, accessToken: 'token', clientId: 'client');

      expect(ok, isTrue);
      final decoded = _decodeBase64(argsOf('paymentCheckout')['paymentRequest'] as String);
      expect(decoded['paymentCode'], 'DEBITO_AVISTA');
      expect(decoded['installments'], 0);
      expect(decoded['value'], 2500);
    });

    test('erro do nativo retorna false', () async {
      mockNative((call) async => throw PlatformException(code: 'ERROR'));
      final ok = await payment.debitPayment(amount: 1, accessToken: 't', clientId: 'c');
      expect(ok, isFalse);
      expect(handler.errors, hasLength(1));
    });
  });

  group('cancelPayment', () {
    final cancelRequest = CancelRequest(
      id: 'order-id',
      clientId: 'client',
      accessToken: 'token',
      cieloCode: '123',
      authCode: '456',
      value: 1000,
    );

    test('envia cancelRequest em base64 e order://response por padrão', () async {
      final ok = await payment.cancelPayment(cancelRequest: cancelRequest);

      expect(ok, isTrue);
      final args = argsOf('paymentCancel');
      expect(args['urlCallback'], 'order://response');
      final decoded = _decodeBase64(args['cancelRequest'] as String);
      expect(decoded, {
        'id': 'order-id',
        'clientID': 'client',
        'accessToken': 'token',
        'cieloCode': '123',
        'authCode': '456',
        'value': 1000,
      });
    });

    test('aceita urlCallback customizado', () async {
      await payment.cancelPayment(cancelRequest: cancelRequest, urlCallback: 'meuapp://response');
      expect(argsOf('paymentCancel')['urlCallback'], 'meuapp://response');
    });

    test('erro do nativo retorna false e chama onError', () async {
      mockNative((call) async => throw PlatformException(code: 'ERROR', message: 'x'));
      final ok = await payment.cancelPayment(cancelRequest: cancelRequest);
      expect(ok, isFalse);
      expect(handler.errors.single, contains('Erro ao iniciar cancelamento'));
    });
  });

  group('impressão', () {
    test('print envia printRequest e order://response', () async {
      final ok = await payment.print(printRequest: PrintRequest.text(text: 'oi'));

      expect(ok, isTrue);
      final args = argsOf('print');
      expect(args['urlCallback'], 'order://response');
      final decoded = _decodeBase64(args['printRequest'] as String);
      expect(decoded['operation'], 'PRINT_TEXT');
      expect(decoded['value'], ['oi']);
    });

    test('printText envia PRINT_TEXT com estilos', () async {
      final ok = await payment.printText(
        text: 'linha',
        styles: [
          {'key_attributes_align': 1},
        ],
        urlCallback: 'meuapp://response',
      );

      expect(ok, isTrue);
      final args = argsOf('print');
      expect(args['urlCallback'], 'meuapp://response');
      final decoded = _decodeBase64(args['printRequest'] as String);
      expect(decoded['operation'], 'PRINT_TEXT');
      expect(decoded['styles'], [
        {'key_attributes_align': 1},
      ]);
    });

    test('printImage envia PRINT_IMAGE com o caminho', () async {
      final ok = await payment.printImage(imagePath: '/sdcard/img.jpg');

      expect(ok, isTrue);
      final decoded = _decodeBase64(argsOf('print')['printRequest'] as String);
      expect(decoded['operation'], 'PRINT_IMAGE');
      expect(decoded['value'], ['/sdcard/img.jpg']);
      expect(decoded['styles'], [{}]);
    });

    test('erro do nativo retorna false e chama onError', () async {
      mockNative((call) async => throw PlatformException(code: 'ERROR'));
      final ok = await payment.printText(text: 'x');
      expect(ok, isFalse);
      expect(handler.errors.single, contains('Erro ao iniciar impressão'));
    });
  });

  group('callbacks do nativo', () {
    test('onTransactionSuccess decodifica o JSON', () async {
      await nativeCalls('onTransactionSuccess', '{"id":"1","status":"PAID"}');
      expect(handler.transactions.single, {'id': '1', 'status': 'PAID'});
    });

    test('onCancelSuccess decodifica o JSON', () async {
      await nativeCalls('onCancelSuccess', '{"id":"1"}');
      expect(handler.cancels.single, {'id': '1'});
    });

    test('onPrintSuccess decodifica o JSON', () async {
      await nativeCalls('onPrintSuccess', '{"code":0}');
      expect(handler.prints.single, {'code': 0});
    });

    test('onError e onMessage repassam o texto', () async {
      await nativeCalls('onError', 'falhou');
      await nativeCalls('onMessage', 'aguarde');
      expect(handler.errors.single, 'falhou');
      expect(handler.messages.single, 'aguarde');
    });

    test('JSON inválido vira onError', () async {
      await nativeCalls('onTransactionSuccess', 'nao-json');
      expect(handler.errors.single, contains('Erro ao processar callback'));
    });

    test('método desconhecido é ignorado', () async {
      await nativeCalls('outro', null);
      expect(handler.errors, isEmpty);
    });
  });

  group('modelos', () {
    test('PaymentType.code e fromCode', () {
      expect(PaymentType.pix.code, 'PIX');
      expect(PaymentType.creditoParceladoBanco.code, 'CREDITO_PARCELADO_BNCO');
      expect(PaymentTypeExtension.fromCode('VOUCHER_REFEICAO'), PaymentType.voucherRefeicao);
      expect(PaymentTypeExtension.fromCode('NAO_EXISTE'), isNull);
      expect(PaymentTypeExtension.allCodes, hasLength(PaymentType.values.length));
    });

    test('PaymentRequest ida e volta em JSON', () {
      final request = PaymentRequest(
        accessToken: 't',
        clientId: 'c',
        items: [PaymentItem(name: 'n', quantity: 2, sku: 's', unitOfMeasure: 'UN', unitPrice: 10)],
        paymentCode: 'PIX',
        value: 20,
      );
      final copy = PaymentRequest.fromJson(request.toJson());
      expect(copy.toMap(), request.toMap());
    });

    test('PrintRequest.fromMap', () {
      final request = PrintRequest.fromMap({
        'operation': 'PRINT_TEXT',
        'styles': [<String, dynamic>{}],
        'value': ['a'],
      });
      expect(request.operation, PrintOperation.printText.value);
      expect(request.value, ['a']);
    });

    test('PaymentResponse.fromMap lê order e payments', () {
      final response = PaymentResponse.fromMap({
        'code': 0,
        'order': {
          'id': 'order-1',
          'items': [
            {'name': 'Produto', 'quantity': 1},
          ],
          'payments': [
            {
              'cieloCode': '123',
              'authCode': '456',
              'amount': 1000,
              'paymentFields': {'statusCode': '1'},
            },
          ],
        },
      });
      expect(response.code, 0);
      expect(response.order!.id, 'order-1');
      expect(response.order!.items.single.name, 'Produto');
      expect(response.order!.payments.single.cieloCode, '123');
      expect(response.order!.payments.single.paymentFields!.statusCode, '1');
    });
  });
}
