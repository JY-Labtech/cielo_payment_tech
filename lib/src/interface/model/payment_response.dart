import 'dart:convert';

class PaymentResponse {
  int? code;
  Order? order;
  String? reason;

  PaymentResponse({
    this.code,
    this.order,
    this.reason,
  });

  factory PaymentResponse.fromJson(String str) => PaymentResponse.fromMap(json.decode(str));

  String toJson() => json.encode(toMap());

  factory PaymentResponse.fromMap(Map<String, dynamic> json) => PaymentResponse(
    code: json["code"] == null ? null : json["code"],
    order: json["order"] == null ? null : Order.fromMap(json["order"]),
    reason: json["reason"] == null ? null : json["reason"],
  );

  Map<String, dynamic> toMap() => {
    "code": code == null ? null : code,
    "order": order == null ? null : order!.toMap(),
    "reason": reason == null ? null : reason,
  };
}

class PaymentResponseSuccess {
  String? createdAt;
  String? id;
  List<OrderItem> items;
  String? notes;
  String? number;
  int? paidAmount;
  List<Payment> payments;
  int? pendingAmount;
  int? price;
  String? reference;
  String? status;
  String? type;
  String? updatedAt;

  PaymentResponseSuccess({
    this.createdAt,
    this.id,
    required this.items,
    this.notes,
    this.number,
    this.paidAmount,
    required this.payments,
    this.pendingAmount,
    this.price,
    this.reference,
    this.status,
    this.type,
    this.updatedAt,
  });

  factory PaymentResponseSuccess.fromJson(String str) => PaymentResponseSuccess.fromMap(json.decode(str));

  String toJson() => json.encode(toMap());

  factory PaymentResponseSuccess.fromMap(Map<String, dynamic> json) => PaymentResponseSuccess(
    createdAt: json["createdAt"] == null ? null : json["createdAt"],
    id: json["id"] == null ? null : json["id"],
    items: json["items"] == null ? [] : List<OrderItem>.from(json["items"].map((x) => OrderItem.fromMap(x))),
    notes: json["notes"] == null ? null : json["notes"],
    number: json["number"] == null ? null : json["number"],
    paidAmount: json["paidAmount"] == null ? null : json["paidAmount"],
    payments: json["payments"] == null ? [] : List<Payment>.from(json["payments"].map((x) => Payment.fromMap(x))),
    pendingAmount: json["pendingAmount"] == null ? null : json["pendingAmount"],
    price: json["price"] == null ? null : json["price"],
    reference: json["reference"] == null ? null : json["reference"],
    status: json["status"] == null ? null : json["status"],
    type: json["type"] == null ? null : json["type"],
    updatedAt: json["updatedAt"] == null ? null : json["updatedAt"],
  );

  Map<String, dynamic> toMap() => {
    "createdAt": createdAt == null ? null : createdAt,
    "id": id == null ? null : id,
    "items": List<dynamic>.from(items.map((x) => x.toMap())),
    "notes": notes == null ? null : notes,
    "number": number == null ? null : number,
    "paidAmount": paidAmount == null ? null : paidAmount,
    "payments": List<dynamic>.from(payments.map((x) => x.toMap())),
    "pendingAmount": pendingAmount == null ? null : pendingAmount,
    "price": price == null ? null : price,
    "reference": reference == null ? null : reference,
    "status": status == null ? null : status,
    "type": type == null ? null : type,
    "updatedAt": updatedAt == null ? null : updatedAt,
  };
}

class Order {
  String? createdAt;
  String? id;
  List<OrderItem> items;
  String? notes;
  String? number;
  int? paidAmount;
  List<Payment> payments;
  int? pendingAmount;
  int? price;
  String? reference;
  String? status;
  String? type;
  String? updatedAt;

  Order({
    this.createdAt,
    this.id,
    required this.items,
    this.notes,
    this.number,
    this.paidAmount,
    required this.payments,
    this.pendingAmount,
    this.price,
    this.reference,
    this.status,
    this.type,
    this.updatedAt,
  });

  factory Order.fromMap(Map<String, dynamic> json) => Order(
    createdAt: json["createdAt"],
    id: json["id"],
    items: List<OrderItem>.from(json["items"].map((x) => OrderItem.fromMap(x))),
    notes: json["notes"],
    number: json["number"],
    paidAmount: json["paidAmount"],
    payments: List<Payment>.from(json["payments"].map((x) => Payment.fromMap(x))),
    pendingAmount: json["pendingAmount"],
    price: json["price"],
    reference: json["reference"],
    status: json["status"],
    type: json["type"],
    updatedAt: json["updatedAt"],
  );

  Map<String, dynamic> toMap() => {
    "createdAt": createdAt,
    "id": id,
    "items": List<dynamic>.from(items.map((x) => x.toMap())),
    "notes": notes,
    "number": number,
    "paidAmount": paidAmount,
    "payments": List<dynamic>.from(payments.map((x) => x.toMap())),
    "pendingAmount": pendingAmount,
    "price": price,
    "reference": reference,
    "status": status,
    "type": type,
    "updatedAt": updatedAt,
  };
}

class OrderItem {
  String? description;
  String? details;
  String? id;
  String? name;
  int? quantity;
  String? reference;
  String? sku;
  String? unitOfMeasure;
  int? unitPrice;

  OrderItem({
    this.description,
    this.details,
    this.id,
    this.name,
    this.quantity,
    this.reference,
    this.sku,
    this.unitOfMeasure,
    this.unitPrice,
  });

  factory OrderItem.fromMap(Map<String, dynamic> json) => OrderItem(
    description: json["description"] == null ? null : json["description"],
    details: json["details"] == null ? null : json["details"],
    id: json["id"] == null ? null : json["id"],
    name: json["name"] == null ? null : json["name"],
    quantity: json["quantity"] == null ? null : json["quantity"],
    reference: json["reference"] == null ? null : json["reference"],
    sku: json["sku"] == null ? null : json["sku"],
    unitOfMeasure: json["unitOfMeasure"] == null ? null : json["unitOfMeasure"],
    unitPrice: json["unitPrice"] == null ? null : json["unitPrice"],
  );

  Map<String, dynamic> toMap() => {
    "description": description == null ? null : description,
    "details": details == null ? null : details,
    "id": id == null ? null : id,
    "name": name == null ? null : name,
    "quantity": quantity == null ? null : quantity,
    "reference": reference == null ? null : reference,
    "sku": sku == null ? null : sku,
    "unitOfMeasure": unitOfMeasure == null ? null : unitOfMeasure,
    "unitPrice": unitPrice == null ? null : unitPrice,
  };
}

class Payment {
  String? accessKey;
  int? amount;
  String? applicationName;
  String? authCode;
  String? brand;
  String? cieloCode;
  String? description;
  int? discountedAmount;
  String? externalId;
  String? id;
  int? installments;
  String? mask;
  String? merchantCode;
  PaymentFields? paymentFields;
  String? primaryCode;
  String? requestDate;
  String? secondaryCode;
  String? terminal;

  Payment({
    this.accessKey,
    this.amount,
    this.applicationName,
    this.authCode,
    this.brand,
    this.cieloCode,
    this.description,
    this.discountedAmount,
    this.externalId,
    this.id,
    this.installments,
    this.mask,
    this.merchantCode,
    this.paymentFields,
    this.primaryCode,
    this.requestDate,
    this.secondaryCode,
    this.terminal,
  });

  factory Payment.fromJson(String str) => Payment.fromMap(json.decode(str));

  String toJson() => json.encode(toMap());

  factory Payment.fromMap(Map<String, dynamic> json) => Payment(
    accessKey: json["accessKey"],
    amount: json["amount"],
    applicationName: json["applicationName"],
    authCode: json["authCode"],
    brand: json["brand"],
    cieloCode: json["cieloCode"],
    description: json["description"],
    discountedAmount: json["discountedAmount"],
    externalId: json["externalId"],
    id: json["id"],
    installments: json["installments"],
    mask: json["mask"],
    merchantCode: json["merchantCode"],
    paymentFields: json["paymentFields"] == null ? null : PaymentFields.fromMap(json["paymentFields"]),
    primaryCode: json["primaryCode"],
    requestDate: json["requestDate"],
    secondaryCode: json["secondaryCode"],
    terminal: json["terminal"],
  );

  Map<String, dynamic> toMap() => {
    "accessKey": accessKey,
    "amount": amount,
    "applicationName": applicationName,
    "authCode": authCode,
    "brand": brand,
    "cieloCode": cieloCode,
    "description": description,
    "discountedAmount": discountedAmount,
    "externalId": externalId,
    "id": id,
    "installments": installments,
    "mask": mask,
    "merchantCode": merchantCode,
    "paymentFields": paymentFields == null ? null : paymentFields!.toMap(),
    "primaryCode": primaryCode,
    "requestDate": requestDate,
    "secondaryCode": secondaryCode,
    "terminal": terminal,
  };
}

class PaymentFields {
  String? cityState;
  String? serviceTax;
  String? documentType;
  String? hasConnectivity;
  String? document;
  String? entranceMode;
  String? paymentTypeCode;
  String? hasPassword;
  String? changeAmount;
  String? productName;
  String? hasWarranty;
  String? merchantName;
  String? hasSentMerchantCode;
  String? isOnlyIntegrationCancelable;
  String? primaryProductCode;
  String? originalTransactionId;
  String? requestDate;
  String? firstQuotaAmount;
  String? secondaryProductCode;
  String? pan;
  String? applicationName;
  String? isFinancialProduct;
  String? v40Code;
  String? merchantCode;
  String? totalizerCode;
  String? isDoubleFontPrintAllowed;
  String? hasSentReference;
  String? isExternalCall;
  String? originalTransactionDate;
  String? interestAmount;
  String? receiptPrintPermission;
  String? cardCaptureType;
  String? upFrontAmount;
  String? hasPrintedClientReceipt;
  String? creditAdminTax;
  String? primaryProductName;
  String? avaiableBalance;
  String? merchantAddress;
  String? applicationId;
  String? numberOfQuotas;
  String? boardingTax;
  String? paymentTransactionId;
  String? firstQuotaDate;
  String? hasSignature;
  String? secondaryProductName;
  String? statusCode;

  PaymentFields({
    this.cityState,
    this.serviceTax,
    this.documentType,
    this.hasConnectivity,
    this.document,
    this.entranceMode,
    this.paymentTypeCode,
    this.hasPassword,
    this.changeAmount,
    this.productName,
    this.hasWarranty,
    this.merchantName,
    this.hasSentMerchantCode,
    this.isOnlyIntegrationCancelable,
    this.primaryProductCode,
    this.originalTransactionId,
    this.requestDate,
    this.firstQuotaAmount,
    this.secondaryProductCode,
    this.pan,
    this.applicationName,
    this.isFinancialProduct,
    this.v40Code,
    this.merchantCode,
    this.totalizerCode,
    this.isDoubleFontPrintAllowed,
    this.hasSentReference,
    this.isExternalCall,
    this.originalTransactionDate,
    this.interestAmount,
    this.receiptPrintPermission,
    this.cardCaptureType,
    this.upFrontAmount,
    this.hasPrintedClientReceipt,
    this.creditAdminTax,
    this.primaryProductName,
    this.avaiableBalance,
    this.merchantAddress,
    this.applicationId,
    this.numberOfQuotas,
    this.boardingTax,
    this.paymentTransactionId,
    this.firstQuotaDate,
    this.hasSignature,
    this.secondaryProductName,
    this.statusCode,
  });

  factory PaymentFields.fromMap(Map<String, dynamic> json) => PaymentFields(
    cityState: json["cityState"],
    serviceTax: json["serviceTax"],
    documentType: json["documentType"],
    hasConnectivity: json["hasConnectivity"],
    document: json["document"],
    entranceMode: json["entranceMode"],
    paymentTypeCode: json["paymentTypeCode"],
    hasPassword: json["hasPassword"],
    changeAmount: json["changeAmount"],
    productName: json["productName"],
    hasWarranty: json["hasWarranty"],
    merchantName: json["merchantName"],
    hasSentMerchantCode: json["hasSentMerchantCode"],
    isOnlyIntegrationCancelable: json["isOnlyIntegrationCancelable"],
    primaryProductCode: json["primaryProductCode"],
    originalTransactionId: json["originalTransactionId"],
    requestDate: json["requestDate"],
    firstQuotaAmount: json["firstQuotaAmount"],
    secondaryProductCode: json["secondaryProductCode"],
    pan: json["pan"],
    applicationName: json["applicationName"],
    isFinancialProduct: json["isFinancialProduct"],
    v40Code: json["v40Code"],
    merchantCode: json["merchantCode"],
    totalizerCode: json["totalizerCode"],
    isDoubleFontPrintAllowed: json["isDoubleFontPrintAllowed"],
    hasSentReference: json["hasSentReference"],
    isExternalCall: json["isExternalCall"],
    originalTransactionDate: json["originalTransactionDate"],
    interestAmount: json["interestAmount"],
    receiptPrintPermission: json["receiptPrintPermission"],
    cardCaptureType: json["cardCaptureType"],
    upFrontAmount: json["upFrontAmount"],
    hasPrintedClientReceipt: json["hasPrintedClientReceipt"],
    creditAdminTax: json["creditAdminTax"],
    primaryProductName: json["primaryProductName"],
    avaiableBalance: json["avaiableBalance"],
    merchantAddress: json["merchantAddress"],
    applicationId: json["applicationId"],
    numberOfQuotas: json["numberOfQuotas"],
    boardingTax: json["boardingTax"],
    paymentTransactionId: json["paymentTransactionId"],
    firstQuotaDate: json["firstQuotaDate"],
    hasSignature: json["hasSignature"],
    secondaryProductName: json["secondaryProductName"],
    statusCode: json["statusCode"],
  );

  Map<String, dynamic> toMap() => {
    "cityState": cityState,
    "serviceTax": serviceTax,
    "documentType": documentType,
    "hasConnectivity": hasConnectivity,
    "document": document,
    "entranceMode": entranceMode,
    "paymentTypeCode": paymentTypeCode,
    "hasPassword": hasPassword,
    "changeAmount": changeAmount,
    "productName": productName,
    "hasWarranty": hasWarranty,
    "merchantName": merchantName,
    "hasSentMerchantCode": hasSentMerchantCode,
    "isOnlyIntegrationCancelable": isOnlyIntegrationCancelable,
    "primaryProductCode": primaryProductCode,
    "originalTransactionId": originalTransactionId,
    "requestDate": requestDate,
    "firstQuotaAmount": firstQuotaAmount,
    "secondaryProductCode": secondaryProductCode,
    "pan": pan,
    "applicationName": applicationName,
    "isFinancialProduct": isFinancialProduct,
    "v40Code": v40Code,
    "merchantCode": merchantCode,
    "totalizerCode": totalizerCode,
    "isDoubleFontPrintAllowed": isDoubleFontPrintAllowed,
    "hasSentReference": hasSentReference,
    "isExternalCall": isExternalCall,
    "originalTransactionDate": originalTransactionDate,
    "interestAmount": interestAmount,
    "receiptPrintPermission": receiptPrintPermission,
    "cardCaptureType": cardCaptureType,
    "upFrontAmount": upFrontAmount,
    "hasPrintedClientReceipt": hasPrintedClientReceipt,
    "creditAdminTax": creditAdminTax,
    "primaryProductName": primaryProductName,
    "avaiableBalance": avaiableBalance,
    "merchantAddress": merchantAddress,
    "applicationId": applicationId,
    "numberOfQuotas": numberOfQuotas,
    "boardingTax": boardingTax,
    "paymentTransactionId": paymentTransactionId,
    "firstQuotaDate": firstQuotaDate,
    "hasSignature": hasSignature,
    "secondaryProductName": secondaryProductName,
    "statusCode": statusCode,
  };
}
