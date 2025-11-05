import 'package:flutter/material.dart';
import 'payment_page.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Cielo Payment Tech Example',
      theme: ThemeData(primarySwatch: Colors.blue, useMaterial3: true),
      home: const PaymentPage(),
    );
  }
}
