# Exemplo `cielo_payment_tech`

Aplicativo de exemplo que demonstra como integrar o plugin `cielo_payment_tech`.

## Como testar

1. Confirme que os binários nativos (`android/libs/cielo_payment_tech-release.aar` e `cielo_payment_tech-native.jar`) estão atualizados.
2. Conecte um Smart POS compatível com o SDK da Cielo.
3. Instale e abra o app de exemplo informando a sua chave de licença:

```bash
flutter run --dart-define=CIELO_LICENCE_KEY=sua_chave_aqui
```

O `CIELO_LICENCE_INTERNAL_KEY` é opcional (o padrão já aponta para o servidor de licenças da JY Labtech).

## Inicialização do plugin

As chaves de licença agora devem ser passadas diretamente pelo código Flutter:

```dart
final payment = CieloPaymentTech.I.initPayment(
  handler: MyPaymentHandler(),
  licenceKey: 'SUA_LICENCE_KEY',
  licenceInternalKey:
      '***REMOVIDO***',
  urlCallback: 'order://payment',
);
```

A `licenceKey` é obtida em https://portal.jylabtech.com.br (o cadastro inclui um plano debug de cortesia para até 2 aparelhos). O `licenceInternalKey` acima aponta para `https://licence-server.invalid/pos-payments/licence/check`.

> **Importante:** Não armazene `licenceKey` e `licenceInternalKey` em `local.properties`. Busque-as de forma segura no seu backend ou outro serviço protegido e injete-as em tempo de execução.

Confira `lib/payment_page.dart` para um fluxo completo de pagamento, cancelamento e impressão.
