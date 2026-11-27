import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:cielo_payment_tech_example/main.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      const MethodChannel('cielo_payment_tech'),
      (MethodCall call) async => true,
    );
  });

  testWidgets('Renderiza a tela de pagamento', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.text('Configuração do Pagamento'), findsOneWidget);
  });

  testWidgets('Sem --dart-define das credenciais mostra aviso', (WidgetTester tester) async {
    await tester.pumpWidget(const MyApp());

    expect(find.textContaining('Credenciais Cielo não informadas'), findsOneWidget);
  });
}
