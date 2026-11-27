# Exemplo `cielo_payment_tech`

Aplicativo de exemplo do plugin [`cielo_payment_tech`](../README.md): crédito, débito, cancelamento e impressão de texto e imagem na Cielo LIO.

## Como rodar

1. Use um terminal Cielo LIO com o app LIO instalado, conectado por ADB.
2. Rode informando as credenciais da sua aplicação Cielo por `--dart-define` (nada fica gravado no código):

```bash
flutter pub get
flutter run \
  --dart-define=CIELO_ACCESS_TOKEN=seu_access_token \
  --dart-define=CIELO_CLIENT_ID=seu_client_id \
  --dart-define=CIELO_MERCHANT_CODE=seu_merchant_code   # opcional
```

Sem `CIELO_ACCESS_TOKEN` ou `CIELO_CLIENT_ID`, o app mostra um aviso na tela e não abre o LIO para pagamento e cancelamento (a impressão continua disponível, pois não usa credenciais). O mesmo vale para `flutter build apk`.

## Inicialização do plugin

```dart
final payment = CieloPaymentTech.I.initPayment(
  handler: MyPaymentHandler(),
  urlCallback: 'order://payment', // opcional
);
```

Os intent filters `order://payment` e `order://response` já estão no `android/app/src/main/AndroidManifest.xml` do exemplo.

Confira `lib/payment_page.dart` para o fluxo completo de pagamento, cancelamento e impressão.
