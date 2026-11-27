# cielo_payment_tech

> Open source Flutter plugin for Cielo LIO smart POS terminals: credit, debit, PIX and voucher payments, cancellation and receipt printing via the LIO deep link API.

[![pub package](https://img.shields.io/pub/v/cielo_payment_tech.svg)](https://pub.dev/packages/cielo_payment_tech)
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

<div align="center">
  <img src="assets/cielo_payment_tech.png" alt="Cielo Payment Tech" height="300" />
</div>

Plugin Flutter para integrar apps com os terminais **Cielo LIO** (Smart POS Android). O app conversa só com Dart: o plugin abre o app LIO por deep link (`lio://payment`, `lio://payment-reversal`, `lio://print`), recebe o retorno pelo deep link do seu app (`order://...`) e entrega o resultado num handler.

> **Aviso:** este plugin **não é oficial** da Cielo. Ele foi desenvolvido de forma independente pela [JY Labtech](https://jylabtech.com.br) e é distribuído como open source sob a licença MIT.

## Sumário

- [Funcionalidades](#funcionalidades)
- [Compatibilidade e requisitos](#compatibilidade-e-requisitos)
- [Instalação](#instalação)
- [Configuração Android](#configuração-android)
- [Uso rápido](#uso-rápido)
- [Referência da API](#referência-da-api)
- [Exemplos por operação](#exemplos-por-operação)
- [Tratamento de erros](#tratamento-de-erros)
- [App de exemplo](#app-de-exemplo)
- [Testes](#testes)
- [Migração da 2.x para a 3.0.0](#migração-da-2x-para-a-300)
- [Contribuindo](#contribuindo)
- [Licença](#licença)
- [Créditos](#créditos)

## Funcionalidades

| Operação | Método | Deep link do LIO | Retorno no handler |
|---|---|---|---|
| Pagamento genérico (qualquer `PaymentType`) | `checkout` | `lio://payment` | `onTransactionSuccess` |
| Crédito à vista ou parcelado | `creditPayment` | `lio://payment` | `onTransactionSuccess` |
| Débito à vista | `debitPayment` | `lio://payment` | `onTransactionSuccess` |
| PIX, vouchers, cartão loja, pré-autorização, crediário | `checkout` + `PaymentType` | `lio://payment` | `onTransactionSuccess` |
| Cancelamento (estorno) | `cancelPayment` | `lio://payment-reversal` | `onCancelSuccess` |
| Impressão (texto ou imagem) | `print` | `lio://print` | `onPrintSuccess` |
| Impressão de texto | `printText` | `lio://print` | `onPrintSuccess` |
| Impressão de imagem | `printImage` | `lio://print` | `onPrintSuccess` |

Mensagens de andamento chegam em `onMessage` e erros em `onError`.

Tipos de pagamento (`PaymentType`): débito à vista; crédito à vista, parcelado loja, administradora, banco, cliente e crediário; vouchers alimentação, refeição, automotivo, cultura, benefícios e auto; cartão loja à vista, parcelado loja, parcelado, parcelado banco, pagamento de fatura (cheque e dinheiro); PIX; pré-autorização; crediário venda. A lista completa de códigos está em [Enums](#enums).

## Compatibilidade e requisitos

| Item | Requisito |
|---|---|
| Terminal | Cielo LIO (Smart POS Android) com o app LIO instalado e a integração local por deep link habilitada |
| Plataforma | Somente Android |
| `minSdk` | 24 |
| `compileSdk` do plugin | 36 |
| Dart | `^3.9.2` |
| Flutter | `>= 3.3.0` (testado com 3.47) |
| Credenciais Cielo | `clientId` e `accessToken` da sua aplicação no portal de desenvolvedores da Cielo (enviados em cada pagamento e cancelamento) |
| Permissões | Nenhuma permissão extra. O plugin não acessa a internet: a comunicação é toda por deep link com o app LIO |

## Instalação

```bash
flutter pub add cielo_payment_tech
```

Ou no `pubspec.yaml`:

```yaml
dependencies:
  cielo_payment_tech: ^3.0.0
```

O código nativo Android já vem pré-compilado em `android/libs` (gerado a partir de `android/native`). Não é preciso configurar repositório Maven nem credenciais extras.

## Configuração Android

### 1. Intent filters de retorno

O LIO devolve o resultado abrindo o deep link de retorno do seu app. Declare os dois hosts na `MainActivity` do **seu app** (`android/app/src/main/AndroidManifest.xml`):

- `order://payment`: retorno dos pagamentos (padrão de `initPayment`).
- `order://response`: retorno de cancelamentos e impressões (padrão de `cancelPayment` e `print`).

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTop"
    ...>

    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Retorno de pagamento: order://payment -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="order" android:host="payment" />
    </intent-filter>

    <!-- Retorno de cancelamento e impressão: order://response -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="order" android:host="response" />
    </intent-filter>
</activity>
```

Observações:

- `launchMode="singleTop"` faz o retorno chegar na Activity que já está aberta (via `onNewIntent`), sem recriar o app.
- `android:exported="true"` é obrigatório para o LIO conseguir abrir o deep link.
- O plugin trata no nativo apenas o scheme `order` com os hosts `payment` e `response`. Se você passar outro `urlCallback`, ele precisa continuar sendo `order://payment` ou `order://response`; caso contrário o retorno não é processado.
- Nenhum código Dart extra é necessário para receber o deep link: o plugin o lê e chama o handler.

### 2. Gradle

Nada a configurar além do padrão do Flutter: o plugin usa só `google()` e `mavenCentral()` e o binário nativo vem no pacote. Confirme que o `minSdk` do app é pelo menos 24.

## Uso rápido

```dart
import 'package:cielo_payment_tech/cielo_payment_tech.dart';
import 'package:flutter/material.dart';

class MyCieloHandler implements ICieloHandler {
  @override
  Future<void> onTransactionSuccess(Map<String, dynamic> response) async {
    debugPrint('Pagamento aprovado: ${response['id']} (${response['status']})');
  }

  @override
  Future<void> onCancelSuccess(Map<String, dynamic> response) async {
    debugPrint('Cancelamento concluído: $response');
  }

  @override
  Future<void> onPrintSuccess(Map<String, dynamic> response) async {
    debugPrint('Impressão concluída: $response');
  }

  @override
  Future<void> onError(String message) async => debugPrint('Erro: $message');

  @override
  Future<void> onMessage(String message) async => debugPrint('Info: $message');
}

void main() => runApp(const MaterialApp(home: PayPage()));

class PayPage extends StatefulWidget {
  const PayPage({super.key});

  @override
  State<PayPage> createState() => _PayPageState();
}

class _PayPageState extends State<PayPage> {
  late final Payment _payment;

  @override
  void initState() {
    super.initState();
    _payment = CieloPaymentTech.I.initPayment(handler: MyCieloHandler());
  }

  Future<void> _pay() async {
    final started = await _payment.creditPayment(
      amount: 1000, // R$ 10,00 em centavos
      accessToken: 'SEU_ACCESS_TOKEN',
      clientId: 'SEU_CLIENT_ID',
    );
    // started == true: o LIO foi aberto. O resultado chega no handler.
    // started == false: não foi possível iniciar; o motivo chegou em onError.
    debugPrint('Pagamento iniciado: $started');
  }

  @override
  void dispose() {
    _payment.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Cielo LIO')),
      body: Center(
        child: ElevatedButton(onPressed: _pay, child: const Text('Pagar R\$ 10,00')),
      ),
    );
  }
}
```

## Referência da API

### `CieloPaymentTech`

Ponto de entrada do plugin.

| Membro | Descrição |
|---|---|
| `static CieloPaymentTech get I` | Instância única, usando o canal `cielo_payment_tech`. |
| `CieloPaymentTech(MethodChannel channel)` | Construtor com canal próprio (útil em testes). |
| `static const String channelName` | `"cielo_payment_tech"`. |
| `Payment initPayment({required ICieloHandler handler, String? urlCallback})` | Cria o `Payment` e registra o handler. `urlCallback` é o deep link de retorno dos pagamentos (padrão `order://payment`). Não chama o nativo. |
| `Payment get payment` | O `Payment` criado por `initPayment`. Lança `Exception` se `initPayment` ainda não foi chamado. |

```dart
final payment = CieloPaymentTech.I.initPayment(
  handler: MyCieloHandler(),
  urlCallback: 'order://payment', // opcional
);

// Em outro ponto do app:
final same = CieloPaymentTech.I.payment;
```

> `Payment` é criado uma vez por processo. Chamar `initPayment` de novo devolve a mesma instância, com o handler e o `urlCallback` da primeira chamada. Para trocar o handler, chame `payment.dispose()` antes de `initPayment`.

### `Payment`

Todos os métodos retornam `Future<bool>`: `true` quando o LIO foi acionado (o resultado chega depois no handler) e `false` quando a operação não pôde ser iniciada (o motivo chega em `onError`). Eles não lançam exceção.

#### `checkout`

```dart
Future<bool> checkout({required PaymentRequest paymentRequest})
```

Envia um `PaymentRequest` (codificado em base64) para `lio://payment`, com o `urlCallback` de `initPayment`. Use para qualquer `PaymentType`.

```dart
await payment.checkout(
  paymentRequest: PaymentRequest(
    accessToken: 'SEU_ACCESS_TOKEN',
    clientId: 'SEU_CLIENT_ID',
    items: [],
    paymentCode: PaymentType.pix.code,
    value: 1500,
  ),
);
```

#### `creditPayment`

```dart
Future<bool> creditPayment({
  required int amount,
  required String accessToken,
  required String clientId,
  String? merchantCode,
  String? email,
  int installments = 0,
  List<PaymentItem>? items,
})
```

Atalho para `checkout` com `PaymentType.creditoAvista` (`CREDITO_AVISTA`). `amount` em centavos.

#### `debitPayment`

```dart
Future<bool> debitPayment({
  required int amount,
  required String accessToken,
  required String clientId,
  String? merchantCode,
  String? email,
  List<PaymentItem>? items,
})
```

Atalho para `checkout` com `PaymentType.debitoAvista` (`DEBITO_AVISTA`) e `installments: 0`.

#### `cancelPayment`

```dart
Future<bool> cancelPayment({required CancelRequest cancelRequest, String? urlCallback})
```

Abre `lio://payment-reversal`. `urlCallback` padrão: `order://response`. O retorno chega em `onCancelSuccess` ou `onError`.

#### `print`

```dart
Future<bool> print({required PrintRequest printRequest, String? urlCallback})
```

Abre `lio://print`. `urlCallback` padrão: `order://response`. O retorno chega em `onPrintSuccess` ou `onError`.

#### `printText` e `printImage`

```dart
Future<bool> printText({required String text, List<Map<String, dynamic>>? styles, String? urlCallback})
Future<bool> printImage({required String imagePath, List<Map<String, dynamic>>? styles, String? urlCallback})
```

Atalhos para `print` com `PrintRequest.text` / `PrintRequest.image`.

#### `dispose`

```dart
void dispose()
```

Remove o handler do canal e libera a instância. Depois disso, chame `initPayment` de novo para voltar a usar o plugin.

#### Propriedades

| Propriedade | Tipo | Descrição |
|---|---|---|
| `channel` | `MethodChannel` | Canal usado com o nativo. |
| `paymentHandler` | `ICieloHandler` | Handler registrado. |
| `urlCallback` | `String` | Deep link de retorno dos pagamentos. |

### `ICieloHandler`

Interface que o app implementa para receber os retornos.

| Método | Quando é chamado |
|---|---|
| `Future<void> onTransactionSuccess(Map<String, dynamic> response)` | Retorno de pagamento em `order://payment` com `responsecode`. `response` é o JSON do pedido devolvido pelo LIO. |
| `Future<void> onCancelSuccess(Map<String, dynamic> response)` | Retorno em `order://response` com `code` ausente ou `0` e com `order` ou `id` no JSON. |
| `Future<void> onPrintSuccess(Map<String, dynamic> response)` | Retorno em `order://response` com `code` ausente ou `0` e sem `order`/`id`. |
| `Future<void> onError(String message)` | Falha ao iniciar a operação, pagamento recusado/cancelado no LIO (`reason`), app LIO ausente ou retorno ilegível. |
| `Future<void> onMessage(String message)` | Andamento: `Pagamento iniciado. Aguardando resposta do LIO...`, `Cancelamento iniciado...`, `Impressão iniciada...`. |

### Modelos de requisição

#### `PaymentRequest`

| Campo | Tipo | JSON | Descrição |
|---|---|---|---|
| `accessToken` | `String?` | `accessToken` | Token da aplicação Cielo. |
| `clientId` | `String?` | `clientID` | Client ID da aplicação Cielo. |
| `email` | `dynamic` | `email` | E-mail do cliente (opcional). |
| `installments` | `int?` | `installments` | Parcelas (`0` à vista). |
| `items` | `List<PaymentItem>` | `items` | Itens do pedido. |
| `merchantCode` | `String?` | `merchantCode` | Código do estabelecimento (opcional). |
| `paymentCode` | `String` | `paymentCode` | Código do tipo de pagamento (`PaymentType.x.code`). |
| `value` | `int` | `value` | Valor total em centavos. |

Métodos: `toMap()`, `toJson()`, `toBase64Encode()`, `PaymentRequest.fromMap`, `PaymentRequest.fromJson`.

#### `PaymentItem`

`name` (`String`), `quantity` (`int`), `sku` (`String`), `unitOfMeasure` (`String`), `unitPrice` (`int`, centavos). Todos obrigatórios.

#### `CancelRequest`

| Campo | Tipo | JSON | Descrição |
|---|---|---|---|
| `id` | `String` | `id` | ID do pedido (campo `id` do retorno do pagamento). |
| `clientId` | `String` | `clientID` | Client ID Cielo. |
| `accessToken` | `String` | `accessToken` | Access token Cielo. |
| `cieloCode` | `String` | `cieloCode` | `payments[].cieloCode` do pagamento. |
| `authCode` | `String` | `authCode` | `payments[].authCode` do pagamento. |
| `value` | `int` | `value` | Valor a cancelar, em centavos. |

#### `PrintRequest`

| Campo | Tipo | Descrição |
|---|---|---|
| `operation` | `String` | `PRINT_TEXT` ou `PRINT_IMAGE` (`PrintOperation.x.value`). |
| `styles` | `List<Map<String, dynamic>>` | Estilos do LIO, um mapa por valor. Padrão `[{}]` nas factories. |
| `value` | `List<String>` | Textos ou caminhos de imagem. |

Factories: `PrintRequest.text({required String text, styles})` e `PrintRequest.image({required String imagePath, styles})`.

### Modelos de resposta

O handler recebe `Map<String, dynamic>`. Para tipar, use `PaymentResponseSuccess.fromMap(response)` (tolera `items`/`payments` ausentes) ou `Order.fromMap(response)` (exige `items` e `payments`):

| Classe | Campos principais |
|---|---|
| `Order` / `PaymentResponseSuccess` | `id`, `number`, `reference`, `status`, `type`, `price`, `paidAmount`, `pendingAmount`, `notes`, `createdAt`, `updatedAt`, `items` (`List<OrderItem>`), `payments` |
| `OrderItem` | `id`, `name`, `description`, `details`, `sku`, `quantity`, `unitPrice`, `unitOfMeasure`, `reference` |
| Pagamento (`payments[]`) | `id`, `amount`, `brand`, `mask`, `authCode`, `cieloCode`, `installments`, `terminal`, `merchantCode`, `primaryCode`, `secondaryCode`, `externalId`, `requestDate`, `paymentFields` |
| `PaymentFields` | Campos extras do LIO como `statusCode`, `pan`, `numberOfQuotas`, `merchantName`, `paymentTransactionId`, `cardCaptureType`, `hasSignature` etc. |
| `PaymentResponse` | `code`, `reason`, `order` (envelope com erro/pedido) |

> A classe de pagamento de `payments[]` não é exportada com o nome `Payment` (para não conflitar com a classe `Payment` do plugin). Leia os campos pelo mapa ou por `PaymentResponseSuccess.fromMap(response).payments`.

```dart
@override
Future<void> onTransactionSuccess(Map<String, dynamic> response) async {
  final order = PaymentResponseSuccess.fromMap(response);
  final first = order.payments.isNotEmpty ? order.payments.first : null;
  debugPrint('Pedido ${order.id} ${order.status}: ${first?.brand} ${first?.mask} '
      'auth=${first?.authCode} cieloCode=${first?.cieloCode}');
}
```

### Enums

#### `PaymentType`

| Enum | Código | Enum | Código |
|---|---|---|---|
| `debitoAvista` | `DEBITO_AVISTA` | `voucherBeneficios` | `VOUCHER_BENEFICIOS` |
| `creditoAvista` | `CREDITO_AVISTA` | `voucherAuto` | `VOUCHER_AUTO` |
| `creditoParceladoLoja` | `CREDITO_PARCELADO_LOJA` | `cartaoLojaAvista` | `CARTAO_LOJA_AVISTA` |
| `creditoParceladoAdm` | `CREDITO_PARCELADO_ADM` | `cartaoLojaParceladoLoja` | `CARTAO_LOJA_PARCELADO_LOJA` |
| `creditoParceladoBanco` | `CREDITO_PARCELADO_BNCO` | `cartaoLojaParcelado` | `CARTAO_LOJA_PARCELADO` |
| `creditoParceladoCliente` | `CREDITO_PARCELADO_CLIENTE` | `cartaoLojaParceladoBanco` | `CARTAO_LOJA_PARCELADO_BANCO` |
| `creditoCrediarioCredito` | `CREDITO_CREDIARIO_CREDITO` | `cartaoLojaPagtoFaturaCheque` | `CARTAO_LOJA_PAGTO_FATURA_CHEQUE` |
| `voucherAlimentacao` | `VOUCHER_ALIMENTACAO` | `cartaoLojaPagtoFaturaDinheiro` | `CARTAO_LOJA_PAGTO_FATURA_DINHEIRO` |
| `voucherRefeicao` | `VOUCHER_REFEICAO` | `preAutorizacao` | `PRE_AUTORIZACAO` |
| `voucherAutomotivo` | `VOUCHER_AUTOMOTIVO` | `crediarioVenda` | `CREDIARIO_VENDA` |
| `voucherCultura` | `VOUCHER_CULTURA` | `pix` | `PIX` |

Auxiliares na extensão `PaymentTypeExtension`:

```dart
PaymentType.pix.code;                              // 'PIX'
PaymentTypeExtension.fromCode('VOUCHER_REFEICAO'); // PaymentType.voucherRefeicao (ou null)
PaymentTypeExtension.allCodes;                     // ['DEBITO_AVISTA', 'CREDITO_AVISTA', ...]
```

#### `PrintOperation`

`printText` (`PRINT_TEXT`) e `printImage` (`PRINT_IMAGE`), via `PrintOperation.x.value`.

## Exemplos por operação

Os exemplos usam `payment` retornado por `initPayment`. Valores sempre em centavos.

### Crédito à vista

```dart
await payment.creditPayment(
  amount: 10000,
  accessToken: 'SEU_ACCESS_TOKEN',
  clientId: 'SEU_CLIENT_ID',
  email: 'cliente@exemplo.com',
  items: [
    PaymentItem(name: 'Produto', quantity: 1, sku: 'SKU123', unitOfMeasure: 'UN', unitPrice: 10000),
  ],
);
```

### Crédito parcelado

`creditPayment` sempre envia `CREDITO_AVISTA`. Para parcelar, use `checkout` com o tipo de parcelamento desejado:

```dart
await payment.checkout(
  paymentRequest: PaymentRequest(
    accessToken: 'SEU_ACCESS_TOKEN',
    clientId: 'SEU_CLIENT_ID',
    installments: 3,
    items: [
      PaymentItem(name: 'Produto', quantity: 1, sku: 'SKU123', unitOfMeasure: 'UN', unitPrice: 30000),
    ],
    paymentCode: PaymentType.creditoParceladoLoja.code, // sem juros (loja)
    // ou PaymentType.creditoParceladoAdm.code (com juros, administradora)
    value: 30000,
  ),
);
```

### Débito

```dart
await payment.debitPayment(
  amount: 2500,
  accessToken: 'SEU_ACCESS_TOKEN',
  clientId: 'SEU_CLIENT_ID',
);
```

### PIX

```dart
await payment.checkout(
  paymentRequest: PaymentRequest(
    accessToken: 'SEU_ACCESS_TOKEN',
    clientId: 'SEU_CLIENT_ID',
    items: [],
    paymentCode: PaymentType.pix.code,
    value: 1500,
  ),
);
```

### Voucher

```dart
await payment.checkout(
  paymentRequest: PaymentRequest(
    accessToken: 'SEU_ACCESS_TOKEN',
    clientId: 'SEU_CLIENT_ID',
    items: [],
    paymentCode: PaymentType.voucherRefeicao.code, // ou voucherAlimentacao, voucherCultura...
    value: 3200,
  ),
);
```

### Cancelamento (estorno)

Use os dados do retorno do pagamento (`id`, `payments[0].cieloCode`, `payments[0].authCode`, `payments[0].amount`):

```dart
Future<void> cancel(Map<String, dynamic> paidOrder) async {
  final paid = (paidOrder['payments'] as List).first as Map<String, dynamic>;
  await payment.cancelPayment(
    cancelRequest: CancelRequest(
      id: paidOrder['id'] as String,
      clientId: 'SEU_CLIENT_ID',
      accessToken: 'SEU_ACCESS_TOKEN',
      cieloCode: paid['cieloCode'] as String,
      authCode: paid['authCode'] as String,
      value: paid['amount'] as int,
    ),
  );
  // Resultado em onCancelSuccess / onError.
}
```

### Impressão de texto

```dart
await payment.printText(text: 'COMPROVANTE\nObrigado pela preferência!\n\n');

// Com estilos do LIO (chaves conforme a documentação de impressão da Cielo LIO):
await payment.printText(
  text: 'TOTAL R\$ 10,00',
  styles: [
    {'key_attributes_align': 0, 'key_attributes_textsize': 30},
  ],
);
```

### Impressão de imagem

```dart
await payment.printImage(imagePath: '/storage/emulated/0/Pictures/logo.png');
```

### Reimpressão

O plugin não tem método de reimpressão de comprovante. Para reimprimir, monte o comprovante com os dados do retorno do pagamento e use `printText`/`printImage`.

## Tratamento de erros

Os métodos de `Payment` não lançam exceção: em caso de falha retornam `false` e chamam `onError` com o texto `Erro ao iniciar pagamento: <erro>` (ou `Erro ao iniciar cancelamento` / `Erro ao iniciar impressão`), em que `<erro>` é a `PlatformException` do nativo.

Códigos de `PlatformException` do nativo:

| Código | Significado |
|---|---|
| `INVALID_ARGUMENT` | Faltou `paymentRequest`, `cancelRequest`, `printRequest` ou `urlCallback` na chamada ao nativo. |
| `ERROR` | Exceção ao montar ou abrir o deep link do LIO (mensagem da exceção em `message`). |
| `notImplemented` (`MissingPluginException` no Dart) | Método desconhecido no canal (por exemplo, `paymentCredit`/`paymentDebit`, que não existem no nativo, ou o antigo `initPayment`). |

Mensagens em `onError` vindas do retorno do LIO:

| Mensagem | Causa |
|---|---|
| `Erro: App LIO não instalado.` | Nenhum app responde a `lio://...`: o aparelho não é uma LIO ou o app LIO não está instalado. Nesse caso o método ainda retorna `true` (a chamada ao nativo deu certo), mas o LIO não abre. |
| `reason` do LIO (ou `Erro desconhecido no pagamento`) | Pagamento recusado ou cancelado pelo operador (retorno em `order://payment` sem `responsecode`). |
| `reason` do LIO (ou `Erro desconhecido na operação`) | Cancelamento ou impressão com `code` diferente de `0`. |
| `Erro ao processar resposta: ...` | O retorno do LIO não pôde ser decodificado (base64/JSON). |
| `Erro ao processar deep link: ...` | Falha inesperada ao ler o deep link. |
| `Erro ao processar callback: ...` | O JSON recebido no Dart é inválido. |

## App de exemplo

O app em [`example/`](example) demonstra crédito, débito, cancelamento (com os campos preenchidos a partir do último pagamento) e impressão de texto e imagem.

```bash
cd example
flutter pub get
flutter run \
  --dart-define=CIELO_ACCESS_TOKEN=seu_access_token \
  --dart-define=CIELO_CLIENT_ID=seu_client_id \
  --dart-define=CIELO_MERCHANT_CODE=seu_merchant_code   # opcional
```

Rode com uma LIO conectada por USB/ADB. As credenciais da sua aplicação Cielo entram só por `--dart-define` e não ficam no código. Sem `CIELO_ACCESS_TOKEN` ou `CIELO_CLIENT_ID`, o exemplo mostra um aviso e não abre o LIO para pagamento e cancelamento. A impressão continua disponível, porque não usa credenciais.

## Testes

Testes Dart (MethodChannel mockado, cobrindo todos os métodos de `Payment`, os callbacks e os modelos):

```bash
flutter test
```

Testes JVM do nativo (`android/native/src/test`: roteamento do `MethodChannel` e abertura dos deep links do LIO), com JDK 17 e o wrapper do example:

```bash
export JAVA_HOME=<JDK 17>
export FLUTTER_SDK=<caminho do Flutter SDK>   # precisa de bin/cache/artifacts/engine/**/flutter.jar (flutter precache --android)
example/android/gradlew -p android/native --no-daemon -Dorg.gradle.jvmargs=-Xmx2g testDebugUnitTest
```

Depois de alterar `android/native`, gere de novo o binário distribuído em `android/libs`:

```bash
JAVA_TOOL_OPTIONS=-Xmx2g sh scripts/build_android_aar.sh   # ou: make build-native
```

## Migração da 2.x para a 3.0.0

A 3.0.0 torna o plugin open source (MIT) e **remove toda a validação de licença**. Não existe mais chave, cadastro ou consulta a servidor da JY Labtech.

O que mudou:

- `CieloPaymentTech.I.initPayment` não recebe mais `licenceKey` nem `licenceInternalKey`.
- A constante `CieloPaymentTech.licenceCheck` foi removida.
- O erro `LICENCE_ERROR` e as mensagens de licença em `onMessage`/`onError` deixaram de existir.
- `initPayment` não chama mais o nativo; o método `initPayment` do canal foi removido.
- Pagamento, cancelamento e impressão abrem o LIO direto, sem consulta de rede antes.

Antes (2.x):

```dart
final payment = CieloPaymentTech.I.initPayment(
  handler: handler,
  licenceKey: 'SUA_LICENCE_KEY',
  licenceInternalKey: CieloPaymentTech.licenceCheck,
  urlCallback: 'order://payment',
);
```

Depois (3.0.0):

```dart
final payment = CieloPaymentTech.I.initPayment(
  handler: handler,
  urlCallback: 'order://payment', // opcional
);
```

Remova também do seu projeto qualquer `--dart-define` ou configuração que servia só para a chave de licença (por exemplo, `CIELO_LICENCE_KEY`). Os demais métodos (`checkout`, `creditPayment`, `debitPayment`, `cancelPayment`, `print`, `printText`, `printImage`) não mudaram.

## Contribuindo

Contribuições são bem-vindas:

1. Abra uma [issue](https://github.com/JY-Labtech/cielo_payment_tech/issues) descrevendo o bug ou a melhoria.
2. Faça um fork, crie uma branch e envie um pull request com testes (`flutter test` e os testes nativos).
3. Se mudar `android/native`, regenere `android/libs` com `scripts/build_android_aar.sh` no mesmo PR.

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).

## Créditos

Desenvolvido e mantido por **JY Labtech**: <https://jylabtech.com.br> · [GitHub](https://github.com/JY-Labtech).

Cielo e LIO são marcas de seus respectivos donos. Para dúvidas sobre o terminal e o SDK, consulte a documentação oficial da Cielo.
