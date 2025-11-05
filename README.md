# cielo_payment_tech

<div align="center">
  <img src="assets/cielo_payment_tech.png" alt="Stone" height="300" />
</div>

---

## 🚨 Aviso

> **Este plugin é _não oficial_** e foi desenvolvido de forma independente para facilitar a integração de pagamentos via SDK da Cielo em dispositivos Android Smart POS.

---

Plugin Flutter para facilitar a integração com o SDK de pagamentos da Cielo através do LIO.

**Package:** `br.com.jylabtech.cielo_payment_tech`

## Descrição

Este plugin simplifica a integração com o SDK de pagamentos da Cielo, permitindo que desenvolvedores trabalhem apenas com código Flutter, enquanto o plugin gerencia toda a comunicação nativa com o LIO através de deep linking.

**Características principais:**

- ✅ Integração simplificada com LIO via deep linking
- ✅ Sistema de licenciamento integrado e seguro
- ✅ Validação de licença automática no nativo
- ✅ Callbacks assíncronos para sucesso e erro
- ✅ Suporte a pagamentos de crédito e débito
- ✅ Interface simples e intuitiva

## Instalação

Adicione a dependência no seu `pubspec.yaml`:

```yaml
dependencies:
  cielo_payment_tech:
    path: ../cielo_payment_tech # ou use a versão do pub.dev quando disponível
```

## Configuração Android

### 1. Configurar Chaves de Licença

O plugin possui um sistema de licenciamento integrado que valida automaticamente a licença antes de permitir o uso.

**Importante:** Adicione as chaves de licença no arquivo `android/local.properties` do **plugin** (não do app que usa o plugin):

```properties
licenceKey=SUA_LICENCE_KEY
licenceInternalKey=aHR0cHM6Ly9wb3MtcGF5bWVudHMtYXBpLTU3NzQ2NDIzNTQwOC5zb3V0aGFtZXJpY2EtZWFzdDEucnVuLmFwcC9wb3MtcGF5bWVudHMvbGljZW5jZS9jaGVjay9pbnZvaWNl
```

**Onde configurar:**

- Se você está desenvolvendo o plugin: `cielo_payment_tech/android/local.properties`
- Se você está usando o plugin: as chaves devem estar no plugin, não no seu app

**Importante:**

- O arquivo `local.properties` é local e não deve ser versionado no Git (já está no `.gitignore`)
- Se não configurar as chaves, o build falhará com uma mensagem de erro clara
- As chaves também podem ser definidas via variáveis de ambiente: `LICENCE_KEY` e `LICENCE_INTERNAL_KEY`
- A validação da licença acontece automaticamente no nativo antes de cada pagamento
- A licença é armazenada de forma criptografada no dispositivo usando `EncryptedSharedPreferences`
- A licença válida é armazenada em cache por 7 dias para melhor performance

### 2. Configurar Intent Filter

Adicione o intent filter no `AndroidManifest.xml` do seu **app** (na `MainActivity`) para receber os callbacks de pagamento:

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTop"
    android:exported="true"
    ...>

    <!-- Intent filter para deep linking -->
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:host="payment"
            android:scheme="order" />
    </intent-filter>
</activity>
```

**Nota:**

- O `launchMode="singleTop"` é importante para garantir que o deep link seja processado corretamente
- O `android:exported="true"` é necessário para que o deep link funcione
- O plugin processa automaticamente os deep links no nativo, então não é necessário configurar nada adicional no código Dart

## Uso

### 1. Implementar o Handler

Crie uma classe que implementa a interface `ICieloHandler` para receber os callbacks do plugin:

```dart
import 'package:cielo_payment_tech/cielo_payment_tech.dart';

class MyPaymentHandler implements ICieloHandler {
  @override
  Future<void> onTransactionSuccess(Map<String, dynamic> response) async {
    // response contém os dados completos do pagamento aprovado
    // Exemplo: id, status, payments, items, etc.
    print('Pagamento aprovado: $response');

    // Você pode acessar os campos específicos:
    // final orderId = response['id'];
    // final status = response['status'];
    // final payments = response['payments'];

    // Atualizar UI, salvar no banco de dados, etc.
  }

  @override
  Future<void> onError(String message) async {
    // message contém a descrição do erro
    print('Erro no pagamento: $message');

    // Mostrar mensagem de erro ao usuário, fazer log, etc.
  }

  @override
  Future<void> onMessage(String message) async {
    // message contém mensagens informativas durante o processo
    // Exemplo: "Verificando licença...", "Pagamento iniciado...", etc.
    print('Mensagem: $message');

    // Atualizar status na UI, mostrar loading, etc.
  }
}
```

### 2. Inicializar o Plugin

```dart
import 'package:cielo_payment_tech/cielo_payment_tech.dart';

// Criar o handler
final handler = MyPaymentHandler();

// Inicializar o plugin
// As chaves de licença são lidas automaticamente do local.properties
final payment = CieloPaymentTech.I.initPayment(
  handler: handler,
  urlCallback: "order://payment", // Opcional, padrão é "order://payment"
);

// Agora você pode usar o objeto payment para realizar pagamentos
```

**Nota:**

- As chaves de licença são lidas automaticamente do arquivo `local.properties`. Não é necessário passá-las via código Dart.
- A inicialização deve ser feita uma vez, geralmente no `initState()` ou `main()`
- O plugin valida a licença automaticamente antes de permitir qualquer pagamento

### 3. Realizar Pagamento

#### Pagamento de Crédito à Vista

```dart
try {
  final success = await payment.creditPayment(
    amount: 10000, // Valor em centavos (R$ 100,00)
    accessToken: "seu_access_token",
    clientId: "seu_client_id",
    merchantCode: "seu_merchant_code", // Opcional
    email: "email@exemplo.com", // Opcional
    installments: 0, // Para crédito à vista, use 0
    items: [
      PaymentItem(
        name: "Produto",
        quantity: 1,
        sku: "SKU123",
        unitOfMeasure: "UN",
        unitPrice: 10000,
      ),
    ],
  );

  if (success) {
    // O pagamento foi iniciado
    // O resultado será retornado via onTransactionSuccess ou onError no handler
  }
} catch (e) {
  // Tratar erros de licença ou outros erros
  print('Erro: $e');
}
```

#### Pagamento de Débito à Vista

```dart
try {
  final success = await payment.debitPayment(
    amount: 10000, // Valor em centavos (R$ 100,00)
    accessToken: "seu_access_token",
    clientId: "seu_client_id",
    merchantCode: "seu_merchant_code", // Opcional
    email: "email@exemplo.com", // Opcional
    items: [
      PaymentItem(
        name: "Produto",
        quantity: 1,
        sku: "SKU123",
        unitOfMeasure: "UN",
        unitPrice: 10000,
      ),
    ],
  );

  if (success) {
    // O pagamento foi iniciado
    // O resultado será retornado via onTransactionSuccess ou onError no handler
  }
} catch (e) {
  // Tratar erros de licença ou outros erros
  print('Erro: $e');
}
```

#### Pagamento Customizado

Para ter mais controle sobre o pagamento, você pode criar um `PaymentRequest` customizado com qualquer tipo de pagamento suportado:

```dart
import 'package:cielo_payment_tech/cielo_payment_tech.dart';

// Exemplo: Pagamento PIX
final paymentRequestPix = PaymentRequest(
  accessToken: "seu_access_token",
  clientId: "seu_client_id",
  email: "email@exemplo.com",
  installments: 0,
  items: [
    PaymentItem(
      name: "Produto",
      quantity: 1,
      sku: "SKU123",
      unitOfMeasure: "UN",
      unitPrice: 10000,
    ),
  ],
  merchantCode: "seu_merchant_code",
  paymentCode: PaymentType.pix.code,
  value: 10000,
);

// Exemplo: Crédito Parcelado Loja (3x)
final paymentRequestParcelado = PaymentRequest(
  accessToken: "seu_access_token",
  clientId: "seu_client_id",
  email: "email@exemplo.com",
  installments: 3, // Número de parcelas
  items: [
    PaymentItem(
      name: "Produto",
      quantity: 1,
      sku: "SKU123",
      unitOfMeasure: "UN",
      unitPrice: 10000,
    ),
  ],
  merchantCode: "seu_merchant_code",
  paymentCode: PaymentType.creditoParceladoLoja.code,
  value: 10000,
);

// Exemplo: Voucher Alimentação
final paymentRequestVoucher = PaymentRequest(
  accessToken: "seu_access_token",
  clientId: "seu_client_id",
  email: "email@exemplo.com",
  installments: 0,
  items: [
    PaymentItem(
      name: "Produto",
      quantity: 1,
      sku: "SKU123",
      unitOfMeasure: "UN",
      unitPrice: 10000,
    ),
  ],
  merchantCode: "seu_merchant_code",
  paymentCode: PaymentType.voucherAlimentacao.code,
  value: 10000,
);

try {
  final success = await payment.checkout(paymentRequest: paymentRequestPix);
  if (success) {
    // Pagamento iniciado
  }
} catch (e) {
  print('Erro: $e');
}
```

**Todos os tipos de pagamento disponíveis:**

```dart
// Listar todos os códigos disponíveis
final allCodes = PaymentType.allCodes;
print(allCodes); // ['DEBITO_AVISTA', 'CREDITO_AVISTA', 'PIX', ...]

// Converter código string para PaymentType
final type = PaymentType.fromCode('PIX');
if (type != null) {
  print(type.code); // 'PIX'
}
```

**Exemplos de uso por categoria:**

- **Débito:** `PaymentType.debitoAvista.code`
- **Crédito:** `PaymentType.creditoAvista.code`, `PaymentType.creditoParceladoLoja.code`, etc.
- **Vouchers:** `PaymentType.voucherAlimentacao.code`, `PaymentType.voucherRefeicao.code`, etc.
- **Cartão Loja:** `PaymentType.cartaoLojaAvista.code`, `PaymentType.cartaoLojaParceladoLoja.code`, etc.
- **Outros:** `PaymentType.pix.code`, `PaymentType.preAutorizacao.code`, etc.

### 4. Estrutura de Resposta

Quando o pagamento é processado com sucesso, o método `onTransactionSuccess` recebe um `Map<String, dynamic>` com a seguinte estrutura:

```dart
{
  "id": "order_id",
  "status": "PAID",  // ou "PENDING", "CANCELLED", etc.
  "number": "order_number",
  "reference": "order_reference",
  "price": 10000,
  "paidAmount": 10000,
  "pendingAmount": 0,
  "createdAt": "2024-01-01T00:00:00",
  "updatedAt": "2024-01-01T00:00:00",
  "items": [
    {
      "id": "item_id",
      "name": "Produto",
      "quantity": 1,
      "unitPrice": 10000,
      ...
    }
  ],
  "payments": [
    {
      "id": "payment_id",
      "amount": 10000,
      "brand": "VISA",
      "mask": "1234",
      "authCode": "123456",
      "cieloCode": "00",
      "terminal": "terminal_info",
      "merchantCode": "merchant_code",
      ...
    }
  ]
}
```

**Exemplo de uso da resposta:**

```dart
@override
Future<void> onTransactionSuccess(Map<String, dynamic> response) async {
  final orderId = response['id'] as String?;
  final status = response['status'] as String?;
  final paidAmount = response['paidAmount'] as int?;

  if (status == 'PAID') {
    // Pagamento aprovado
    final payments = response['payments'] as List?;
    if (payments != null && payments.isNotEmpty) {
      final payment = payments[0] as Map<String, dynamic>;
      final authCode = payment['authCode'];
      final brand = payment['brand'];
      // Processar dados do pagamento
    }
  }
}
```

### 5. Tratamento de Erros

O plugin pode retornar erros em diferentes situações:

- **Erro de Licença**: Quando a licença não é válida ou não está configurada
- **Erro de LIO**: Quando o app LIO não está instalado
- **Erro de Pagamento**: Quando o pagamento é recusado ou cancelado
- **Erro de Rede**: Quando há problemas de comunicação

Todos os erros são retornados via `onError` no handler:

```dart
@override
Future<void> onError(String message) async {
  // message contém a descrição do erro
  // Exemplos:
  // - "Sua licença não foi ativada."
  // - "Erro: App LIO não instalado."
  // - "Erro desconhecido no pagamento"

  // Tratar o erro apropriadamente
}
```

## Recursos

### Tipos de Pagamento Suportados

**Débito:**
- ✅ Débito à vista

**Crédito:**
- ✅ Crédito à vista
- ✅ Crédito parcelado loja
- ✅ Crédito parcelado administradora
- ✅ Crédito parcelado banco
- ✅ Crédito parcelado cliente
- ✅ Crédito crediário

**Vouchers:**
- ✅ Voucher alimentação
- ✅ Voucher refeição
- ✅ Voucher automotivo
- ✅ Voucher cultura
- ✅ Voucher benefícios
- ✅ Voucher auto

**Cartão Loja:**
- ✅ Cartão loja à vista
- ✅ Cartão loja parcelado loja
- ✅ Cartão loja parcelado
- ✅ Cartão loja parcelado banco
- ✅ Cartão loja pagamento fatura cheque
- ✅ Cartão loja pagamento fatura dinheiro

**Outros:**
- ✅ PIX
- ✅ Pré-autorização
- ✅ Crediário venda

### Outros Recursos

- ✅ Deep linking automático com o LIO
- ✅ Sistema de licenciamento integrado e seguro
- ✅ Validação de licença automática no nativo
- ✅ Callbacks assíncronos para sucesso e erro
- ✅ Suporte a itens de pagamento
- ✅ Armazenamento criptografado de licença
- ✅ Interface simples e intuitiva
- ✅ Processamento de deep links no nativo

## Estrutura do Projeto

O plugin segue a mesma estrutura do projeto Stone Payment Tech:

- `lib/src/interface/` - Interfaces e handlers
  - `handler/` - Interface ICieloHandler
  - `model/` - Modelos de dados (PaymentRequest, PaymentResponse, PaymentType, etc.)
- `lib/src/payments/` - Lógica de pagamento
  - `payment.dart` - Classe Payment com métodos de pagamento
- `lib/src/cielo_payment_tech.dart` - Classe principal do plugin
- `android/src/main/java/br/com/jylabtech/cielo_payment_tech/` - Código nativo Android
  - `licence/` - Sistema de licenciamento
  - `payments/` - Presenter de pagamentos
  - `core/` - Classes core do plugin

## Requisitos

- Flutter >= 3.3.0
- Android SDK 24+ (minSdk 24)
- LIO instalado no dispositivo
- Chaves de licença configuradas no `local.properties`

## Exemplo Completo

Aqui está um exemplo completo de uso do plugin:

```dart
import 'package:flutter/material.dart';
import 'package:cielo_payment_tech/cielo_payment_tech.dart';

class PaymentScreen extends StatefulWidget {
  @override
  _PaymentScreenState createState() => _PaymentScreenState();
}

class _PaymentScreenState extends State<PaymentScreen> {
  Payment? _payment;

  @override
  void initState() {
    super.initState();
    _initializePayment();
  }

  void _initializePayment() {
    final handler = MyPaymentHandler();
    _payment = CieloPaymentTech.I.initPayment(handler: handler);
  }

  Future<void> _processPayment() async {
    try {
      await _payment?.creditPayment(
        amount: 10000,
        accessToken: "seu_access_token",
        clientId: "seu_client_id",
      );
    } catch (e) {
      print('Erro: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Pagamento')),
      body: Center(
        child: ElevatedButton(
          onPressed: _processPayment,
          child: Text('Pagar'),
        ),
      ),
    );
  }

  @override
  void dispose() {
    _payment?.dispose();
    super.dispose();
  }
}

class MyPaymentHandler implements ICieloHandler {
  @override
  Future<void> onTransactionSuccess(Map<String, dynamic> response) async {
    print('Pagamento aprovado: $response');
  }

  @override
  Future<void> onError(String message) async {
    print('Erro: $message');
  }

  @override
  Future<void> onMessage(String message) async {
    print('Mensagem: $message');
  }
}
```

Veja também o exemplo completo em `example/lib/payment_page.dart` para uma implementação funcional com interface de usuário completa.

## Troubleshooting

### Erro: "LicenceKey não encontrado"

- Verifique se o arquivo `android/local.properties` existe
- Confirme que as chaves `licenceKey` e `licenceInternalKey` estão configuradas
- Certifique-se de que não há espaços extras nas chaves

### Erro: "App LIO não instalado"

- Instale o app LIO no dispositivo
- Verifique se o app LIO está atualizado

### Deep link não está funcionando

- Verifique se o intent filter está configurado no `AndroidManifest.xml`
- Confirme que o `launchMode="singleTop"` está configurado na Activity
- Verifique se o `urlCallback` está correto

### Licença não é validada

- Verifique se as chaves estão corretas no `local.properties`
- Confirme que há conexão com a internet para validação inicial
- Verifique os logs para mais detalhes sobre o erro

## Contribuindo

Contribuições são bem-vindas! Por favor, abra uma issue ou pull request.

## Licença

Este projeto é proprietário e está protegido por direitos autorais.

**ATENÇÃO:** Este software é propriedade de JYLabtech e está protegido por
lei de direitos autorais. O uso deste software é restrito e está sujeito aos
termos e condições da licença proprietária.

**Você NÃO tem permissão para:**

- Copiar, reproduzir ou duplicar o software
- Modificar, adaptar ou criar trabalhos derivados
- Vender, alugar, sublicenciar ou distribuir o software
- Fazer engenharia reversa ou descompilar o software
- Usar o software para fins não autorizados

Para mais informações sobre licenciamento, consulte o arquivo `LICENSE` ou
entre em contato com JYLabtech.

**Copyright (c) 2024 JYLabtech. All Rights Reserved.**

## 📞 Suporte

Para questões relacionadas ao SDK Zoop, consulte a documentação oficial da Zoop.

## Para questões específicas sobre este plugin, verifique os issues do repositório ou entre em contato com a equipe de desenvolvimento.

---

<div align="left">
  <table>
  <tr>
    <td align="center"><a href="https://github.com/JY-Labtech"><img src="https://avatars.githubusercontent.com/u/185066406?s=200&v=4" width="100px;" alt=""/><br /><sub><b>JY Labtech</b></sub></a><br /> <a href="https://github.com/JY-Labtech" title="Autor"></a></td>
  </tr>

</table>

</div>

---
