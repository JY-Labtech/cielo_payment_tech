import 'package:flutter/material.dart';
import 'package:cielo_payment_tech/cielo_payment_tech.dart';
import 'payment_handler.dart';

/// Credenciais da aplicação Cielo, informadas no build:
/// `flutter run --dart-define=CIELO_ACCESS_TOKEN=... --dart-define=CIELO_CLIENT_ID=...`
const _cieloAccessToken = String.fromEnvironment('CIELO_ACCESS_TOKEN');
const _cieloClientId = String.fromEnvironment('CIELO_CLIENT_ID');

/// Opcional: `--dart-define=CIELO_MERCHANT_CODE=...`.
const _cieloMerchantCodeValue = String.fromEnvironment('CIELO_MERCHANT_CODE');
const String? _cieloMerchantCode = _cieloMerchantCodeValue == '' ? null : _cieloMerchantCodeValue;

const _hasCieloCredentials = _cieloAccessToken != '' && _cieloClientId != '';

const _missingCredentialsMessage =
    'Credenciais Cielo não informadas. Rode o exemplo com '
    '--dart-define=CIELO_ACCESS_TOKEN=... --dart-define=CIELO_CLIENT_ID=...';

class PaymentPage extends StatefulWidget {
  const PaymentPage({super.key});

  @override
  State<PaymentPage> createState() => _PaymentPageState();
}

class _PaymentPageState extends State<PaymentPage> {
  final TextEditingController _amountController = TextEditingController(text: '10000');
  final TextEditingController _emailController = TextEditingController(text: 'email@exemplo.com');

  // Campos para cancelamento
  final TextEditingController _orderIdController = TextEditingController();
  final TextEditingController _cieloCodeController = TextEditingController();
  final TextEditingController _authCodeController = TextEditingController();
  final TextEditingController _cancelValueController = TextEditingController();

  // Campos para impressão
  final TextEditingController _printTextController = TextEditingController(
    text:
        'TEXTO PARA IMPRIMIR NA PRIMEIRA LINHA\nTEXTO PARA IMPRIMIR NA SEGUNDA LINHA\nTEXTO PARA IMPRIMIR NA TERCEIRA LINHA\n\n',
  );
  final TextEditingController _imagePathController = TextEditingController(
    text: '/storage/emulated/0/saved_images/Image-5005.jpg',
  );

  String _statusMessage = '';
  bool _isLoading = false;
  Payment? _payment;

  @override
  void initState() {
    super.initState();
    _initializePayment();
  }

  void _initializePayment() {
    final handler = PaymentHandler(
      onSuccessCallback: (response) {
        setState(() {
          _isLoading = false;
          _statusMessage = '✅ Pagamento aprovado!\n${response.toString()}';

          // Preencher campos de cancelamento automaticamente
          if (response['id'] != null) {
            _orderIdController.text = response['id'].toString();
          }
          if (response['payments'] != null && (response['payments'] as List).isNotEmpty) {
            final payment = (response['payments'] as List)[0] as Map<String, dynamic>;
            if (payment['cieloCode'] != null) {
              _cieloCodeController.text = payment['cieloCode'].toString();
            }
            if (payment['authCode'] != null) {
              _authCodeController.text = payment['authCode'].toString();
            }
            if (payment['amount'] != null) {
              _cancelValueController.text = payment['amount'].toString();
            }
          }
        });
        _showDialog('Sucesso', 'Pagamento aprovado com sucesso!');
      },
      onCancelSuccessCallback: (response) {
        setState(() {
          _isLoading = false;
          _statusMessage = '✅ Cancelamento aprovado!\n${response.toString()}';
        });
        _showDialog('Sucesso', 'Cancelamento realizado com sucesso!');
      },
      onPrintSuccessCallback: (response) {
        setState(() {
          _isLoading = false;
          _statusMessage = '✅ Impressão concluída!\n${response.toString()}';
        });
        _showDialog('Sucesso', 'Impressão realizada com sucesso!');
      },
      onErrorCallback: (message) {
        setState(() {
          _isLoading = false;
          _statusMessage = '❌ Erro: $message';
        });
        _showDialog('Erro', message);
      },
      onMessageCallback: (message) {
        setState(() {
          _statusMessage = 'ℹ️ $message';
        });
      },
    );

    _payment = CieloPaymentTech.I.initPayment(handler: handler);
  }

  void _showDialog(String title, String message) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(message),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  Future<void> _processPayment(PaymentType paymentType) async {
    if (!_hasCieloCredentials) {
      _showDialog('Credenciais ausentes', _missingCredentialsMessage);
      return;
    }
    if (_amountController.text.isEmpty) {
      _showDialog('Erro', 'Por favor, informe o valor do pagamento');
      return;
    }

    final amount = int.tryParse(_amountController.text);
    if (amount == null || amount <= 0) {
      _showDialog('Erro', 'Valor inválido. Informe um valor em centavos (ex: 10000 para R\$ 100,00)');
      return;
    }

    setState(() {
      _isLoading = true;
      _statusMessage = 'Processando pagamento...';
    });

    try {
      final items = [
        PaymentItem(
          name: 'Produto Exemplo',
          quantity: 1,
          sku: 'SKU001',
          unitOfMeasure: 'UN',
          unitPrice: amount,
        ),
      ];

      bool success = false;
      if (paymentType == PaymentType.creditoAvista) {
        success = await _payment!.creditPayment(
          amount: amount,
          accessToken: _cieloAccessToken,
          clientId: _cieloClientId,
          merchantCode: _cieloMerchantCode,
          email: _emailController.text.isEmpty ? null : _emailController.text,
          installments: 0,
          items: items,
        );
      } else if (paymentType == PaymentType.debitoAvista) {
        success = await _payment!.debitPayment(
          amount: amount,
          accessToken: _cieloAccessToken,
          clientId: _cieloClientId,
          merchantCode: _cieloMerchantCode,
          email: _emailController.text.isEmpty ? null : _emailController.text,
          items: items,
        );
      }

      if (!success && mounted) {
        setState(() {
          _isLoading = false;
          _statusMessage = 'Não foi possível iniciar o pagamento. Verifique se o LIO está instalado.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _statusMessage = 'Erro: $e';
        });
        _showDialog('Erro', 'Erro ao processar pagamento: $e');
      }
    }
  }

  Future<void> _processCancel() async {
    if (!_hasCieloCredentials) {
      _showDialog('Credenciais ausentes', _missingCredentialsMessage);
      return;
    }
    if (_orderIdController.text.isEmpty) {
      _showDialog('Erro', 'Por favor, informe o ID da ordem');
      return;
    }

    if (_cieloCodeController.text.isEmpty) {
      _showDialog('Erro', 'Por favor, informe o código Cielo');
      return;
    }

    if (_authCodeController.text.isEmpty) {
      _showDialog('Erro', 'Por favor, informe o código de autorização');
      return;
    }

    final cancelValue = int.tryParse(_cancelValueController.text);
    if (cancelValue == null || cancelValue <= 0) {
      _showDialog('Erro', 'Valor inválido. Informe um valor em centavos');
      return;
    }

    setState(() {
      _isLoading = true;
      _statusMessage = 'Processando cancelamento...';
    });

    try {
      final cancelRequest = CancelRequest(
        id: _orderIdController.text,
        clientId: _cieloClientId,
        accessToken: _cieloAccessToken,
        cieloCode: _cieloCodeController.text,
        authCode: _authCodeController.text,
        value: cancelValue,
      );

      final success = await _payment!.cancelPayment(cancelRequest: cancelRequest);

      if (!success && mounted) {
        setState(() {
          _isLoading = false;
          _statusMessage = 'Não foi possível iniciar o cancelamento. Verifique se o LIO está instalado.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _statusMessage = 'Erro: $e';
        });
        _showDialog('Erro', 'Erro ao processar cancelamento: $e');
      }
    }
  }

  Future<void> _processPrintText() async {
    if (_printTextController.text.isEmpty) {
      _showDialog('Erro', 'Por favor, informe o texto para impressão');
      return;
    }

    setState(() {
      _isLoading = true;
      _statusMessage = 'Processando impressão...';
    });

    try {
      final success = await _payment!.printText(
        text: _printTextController.text,
      );

      if (!success && mounted) {
        setState(() {
          _isLoading = false;
          _statusMessage = 'Não foi possível iniciar a impressão. Verifique se o LIO está instalado.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _statusMessage = 'Erro: $e';
        });
        _showDialog('Erro', 'Erro ao processar impressão: $e');
      }
    }
  }

  Future<void> _processPrintImage() async {
    if (_imagePathController.text.isEmpty) {
      _showDialog('Erro', 'Por favor, informe o caminho da imagem');
      return;
    }

    setState(() {
      _isLoading = true;
      _statusMessage = 'Processando impressão de imagem...';
    });

    try {
      final success = await _payment!.printImage(
        imagePath: _imagePathController.text,
      );

      if (!success && mounted) {
        setState(() {
          _isLoading = false;
          _statusMessage = 'Não foi possível iniciar a impressão. Verifique se o LIO está instalado.';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _statusMessage = 'Erro: $e';
        });
        _showDialog('Erro', 'Erro ao processar impressão: $e');
      }
    }
  }

  @override
  void dispose() {
    _amountController.dispose();
    _emailController.dispose();
    _orderIdController.dispose();
    _cieloCodeController.dispose();
    _authCodeController.dispose();
    _cancelValueController.dispose();
    _printTextController.dispose();
    _imagePathController.dispose();
    _payment?.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Cielo Payment Tech - Exemplo'),
        backgroundColor: Colors.blue,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (!_hasCieloCredentials)
              Container(
                margin: const EdgeInsets.only(bottom: 16),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.orange.shade100,
                  border: Border.all(color: Colors.orange),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text(
                  '⚠️ $_missingCredentialsMessage\nPagamento e cancelamento ficam desativados.',
                ),
              ),
            const Text(
              'Configuração do Pagamento',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),

            const SizedBox(height: 12),
            TextField(
              controller: _amountController,
              decoration: const InputDecoration(
                labelText: 'Valor (em centavos)',
                hintText: 'Ex: 10000 (R\$ 100,00)',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
            ),

            const SizedBox(height: 12),
            TextField(
              controller: _emailController,
              decoration: const InputDecoration(
                labelText: 'Email (opcional)',
                hintText: 'email@exemplo.com',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.emailAddress,
            ),
            const SizedBox(height: 24),
            const Text(
              'Tipo de Pagamento',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _isLoading ? null : () => _processPayment(PaymentType.creditoAvista),
              icon: const Icon(Icons.credit_card),
              label: const Text('Crédito à Vista'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                backgroundColor: Colors.green,
                foregroundColor: Colors.white,
              ),
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _isLoading ? null : () => _processPayment(PaymentType.debitoAvista),
              icon: const Icon(Icons.account_balance_wallet),
              label: const Text('Débito à Vista'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                backgroundColor: Colors.blue,
                foregroundColor: Colors.white,
              ),
            ),
            const SizedBox(height: 32),
            const Divider(),
            const SizedBox(height: 16),
            const Text(
              'Cancelamento de Pagamento',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _orderIdController,
              decoration: const InputDecoration(
                labelText: 'ID da Ordem',
                hintText: 'ID retornado após o pagamento',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _cieloCodeController,
                    decoration: const InputDecoration(
                      labelText: 'Código Cielo',
                      hintText: 'Ex: 123',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextField(
                    controller: _authCodeController,
                    decoration: const InputDecoration(
                      labelText: 'Código de Autorização',
                      hintText: 'Ex: 123',
                      border: OutlineInputBorder(),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            TextField(
              controller: _cancelValueController,
              decoration: const InputDecoration(
                labelText: 'Valor do Cancelamento (centavos)',
                hintText: 'Ex: 1000',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _isLoading ? null : _processCancel,
              icon: const Icon(Icons.cancel),
              label: const Text('Cancelar Pagamento'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                backgroundColor: Colors.red,
                foregroundColor: Colors.white,
              ),
            ),
            const SizedBox(height: 32),
            const Divider(),
            const SizedBox(height: 16),
            const Text(
              'Impressão',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            const Text(
              'Imprimir Texto',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _printTextController,
              decoration: const InputDecoration(
                labelText: 'Texto para Impressão',
                hintText: 'Use \\n para quebras de linha',
                border: OutlineInputBorder(),
              ),
              maxLines: 5,
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _isLoading ? null : _processPrintText,
              icon: const Icon(Icons.print),
              label: const Text('Imprimir Texto'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                backgroundColor: Colors.orange,
                foregroundColor: Colors.white,
              ),
            ),
            const SizedBox(height: 24),
            const Text(
              'Imprimir Imagem',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            TextField(
              controller: _imagePathController,
              decoration: const InputDecoration(
                labelText: 'Caminho da Imagem',
                hintText: '/storage/emulated/0/saved_images/Image.jpg',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            ElevatedButton.icon(
              onPressed: _isLoading ? null : _processPrintImage,
              icon: const Icon(Icons.image),
              label: const Text('Imprimir Imagem'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
                backgroundColor: Colors.purple,
                foregroundColor: Colors.white,
              ),
            ),
            const SizedBox(height: 24),
            if (_isLoading)
              const Center(
                child: CircularProgressIndicator(),
              ),
            if (_statusMessage.isNotEmpty)
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey[200],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  _statusMessage,
                  style: const TextStyle(fontSize: 14),
                ),
              ),
            const SizedBox(height: 24),
            const Text(
              'Observações:',
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 8),
            const Text(
              '• O valor deve ser informado em centavos (ex: 10000 = R\$ 100,00)\n'
              '• É necessário ter o app LIO instalado no dispositivo\n'
              '• Access Token e Client ID são obrigatórios\n'
              '• Após iniciar o pagamento, o app LIO será aberto automaticamente\n'
              '• Os campos de cancelamento são preenchidos automaticamente após um pagamento\n'
              '• Para impressão de texto, use \\n para quebras de linha\n'
              '• Para impressão de imagem, informe o caminho completo do arquivo',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }
}
