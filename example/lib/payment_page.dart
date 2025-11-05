import 'package:flutter/material.dart';
import 'package:cielo_payment_tech/cielo_payment_tech.dart';
import 'payment_handler.dart';

class PaymentPage extends StatefulWidget {
  const PaymentPage({super.key});

  @override
  State<PaymentPage> createState() => _PaymentPageState();
}

class _PaymentPageState extends State<PaymentPage> {
  final TextEditingController _amountController = TextEditingController(text: '10000');
  final TextEditingController _emailController = TextEditingController(text: 'email@exemplo.com');

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
        });
        _showDialog('Sucesso', 'Pagamento aprovado com sucesso!');
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
          accessToken: 'cWVMl4iZWx4o5IYBK0C6sTbkdsKfDTbIuarp0xt9NBeDHMb43a',
          clientId: 'hO75XFfWlyhvQIf6ae8QSLnsRfY363XgNcspdXpUQsU4LPoEG3',
          merchantCode: '3c8efefc-e655-4c40-a521-2a188967ef44',
          email: _emailController.text.isEmpty ? null : _emailController.text,
          installments: 0,
          items: items,
        );
      } else if (paymentType == PaymentType.debitoAvista) {
        success = await _payment!.debitPayment(
          amount: amount,
          accessToken: 'cWVMl4iZWx4o5IYBK0C6sTbkdsKfDTbIuarp0xt9NBeDHMb43a',
          clientId: 'hO75XFfWlyhvQIf6ae8QSLnsRfY363XgNcspdXpUQsU4LPoEG3',
          merchantCode: '3c8efefc-e655-4c40-a521-2a188967ef44',
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

  @override
  void dispose() {
    _amountController.dispose();

    _emailController.dispose();
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
            const Text(
              'Configuração do Pagamento',
              style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 16),
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
              '• Após iniciar o pagamento, o app LIO será aberto automaticamente',
              style: TextStyle(fontSize: 12, color: Colors.grey),
            ),
          ],
        ),
      ),
    );
  }
}
