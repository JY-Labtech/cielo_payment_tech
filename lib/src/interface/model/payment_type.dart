enum PaymentType {
  creditoAvista,
  debitoAvista,
}

extension PaymentTypeExtension on PaymentType {
  String get code {
    switch (this) {
      case PaymentType.creditoAvista:
        return 'CREDITO_AVISTA';
      case PaymentType.debitoAvista:
        return 'DEBITO_AVISTA';
    }
  }
}
