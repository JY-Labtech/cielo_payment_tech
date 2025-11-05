enum PaymentType {
  // Débito
  debitoAvista,

  // Crédito
  creditoAvista,
  creditoParceladoLoja,
  creditoParceladoAdm,
  creditoParceladoBanco,
  creditoParceladoCliente,
  creditoCrediarioCredito,

  // Vouchers
  voucherAlimentacao,
  voucherRefeicao,
  voucherAutomotivo,
  voucherCultura,
  voucherBeneficios,
  voucherAuto,

  // Cartão Loja
  cartaoLojaAvista,
  cartaoLojaParceladoLoja,
  cartaoLojaParcelado,
  cartaoLojaParceladoBanco,
  cartaoLojaPagtoFaturaCheque,
  cartaoLojaPagtoFaturaDinheiro,

  // Outros
  preAutorizacao,
  crediarioVenda,
  pix,
}

extension PaymentTypeExtension on PaymentType {
  String get code {
    switch (this) {
      // Débito
      case PaymentType.debitoAvista:
        return 'DEBITO_AVISTA';

      // Crédito
      case PaymentType.creditoAvista:
        return 'CREDITO_AVISTA';
      case PaymentType.creditoParceladoLoja:
        return 'CREDITO_PARCELADO_LOJA';
      case PaymentType.creditoParceladoAdm:
        return 'CREDITO_PARCELADO_ADM';
      case PaymentType.creditoParceladoBanco:
        return 'CREDITO_PARCELADO_BNCO';
      case PaymentType.creditoParceladoCliente:
        return 'CREDITO_PARCELADO_CLIENTE';
      case PaymentType.creditoCrediarioCredito:
        return 'CREDITO_CREDIARIO_CREDITO';

      // Vouchers
      case PaymentType.voucherAlimentacao:
        return 'VOUCHER_ALIMENTACAO';
      case PaymentType.voucherRefeicao:
        return 'VOUCHER_REFEICAO';
      case PaymentType.voucherAutomotivo:
        return 'VOUCHER_AUTOMOTIVO';
      case PaymentType.voucherCultura:
        return 'VOUCHER_CULTURA';
      case PaymentType.voucherBeneficios:
        return 'VOUCHER_BENEFICIOS';
      case PaymentType.voucherAuto:
        return 'VOUCHER_AUTO';

      // Cartão Loja
      case PaymentType.cartaoLojaAvista:
        return 'CARTAO_LOJA_AVISTA';
      case PaymentType.cartaoLojaParceladoLoja:
        return 'CARTAO_LOJA_PARCELADO_LOJA';
      case PaymentType.cartaoLojaParcelado:
        return 'CARTAO_LOJA_PARCELADO';
      case PaymentType.cartaoLojaParceladoBanco:
        return 'CARTAO_LOJA_PARCELADO_BANCO';
      case PaymentType.cartaoLojaPagtoFaturaCheque:
        return 'CARTAO_LOJA_PAGTO_FATURA_CHEQUE';
      case PaymentType.cartaoLojaPagtoFaturaDinheiro:
        return 'CARTAO_LOJA_PAGTO_FATURA_DINHEIRO';

      // Outros
      case PaymentType.preAutorizacao:
        return 'PRE_AUTORIZACAO';
      case PaymentType.crediarioVenda:
        return 'CREDIARIO_VENDA';
      case PaymentType.pix:
        return 'PIX';
    }
  }

  /// Retorna o PaymentType a partir de uma string de código
  static PaymentType? fromCode(String code) {
    for (var type in PaymentType.values) {
      if (type.code == code) {
        return type;
      }
    }
    return null;
  }

  /// Retorna uma lista de todos os códigos disponíveis
  static List<String> get allCodes => PaymentType.values.map((e) => e.code).toList();
}
