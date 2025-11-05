import 'dart:convert';

class PaymentRequest {
  String? accessToken;
  String? clientId;
  dynamic email;
  int? installments;
  List<PaymentItem> items;
  String? merchantCode;
  String paymentCode;
  int value;

  PaymentRequest({
    this.accessToken,
    this.clientId,
    this.email,
    this.installments,
    required this.items,
    this.merchantCode,
    required this.paymentCode,
    required this.value,
  });

  factory PaymentRequest.fromJson(String str) => PaymentRequest.fromMap(json.decode(str));

  String toJson() => json.encode(toMap());

  factory PaymentRequest.fromMap(Map<String, dynamic> json) => PaymentRequest(
    accessToken: json["accessToken"] == null ? null : json["accessToken"],
    clientId: json["clientID"] == null ? null : json["clientID"],
    email: json["email"],
    installments: json["installments"] == null ? null : json["installments"],
    items: json["items"] == null ? [] : List<PaymentItem>.from(json["items"].map((x) => PaymentItem.fromMap(x))),
    merchantCode: json["merchantCode"] == null ? null : json["merchantCode"],
    paymentCode: json["paymentCode"] ?? '',
    value: json["value"] ?? 0,
  );

  Map<String, dynamic> toMap() => {
    "accessToken": accessToken == null ? null : accessToken,
    "clientID": clientId == null ? null : clientId,
    "email": email,
    "installments": installments == null ? null : installments,
    "items": List<dynamic>.from(items.map((x) => x.toMap())),
    "merchantCode": merchantCode == null ? null : merchantCode,
    "paymentCode": paymentCode,
    "value": value,
  };

  String toBase64Encode() {
    final encodedText = utf8.encode(toJson());
    return base64.encode(encodedText);
  }
}

class PaymentItem {
  String name;
  int quantity;
  String sku;
  String unitOfMeasure;
  int unitPrice;

  PaymentItem({
    required this.name,
    required this.quantity,
    required this.sku,
    required this.unitOfMeasure,
    required this.unitPrice,
  });

  factory PaymentItem.fromJson(String str) => PaymentItem.fromMap(json.decode(str));

  String toJson() => json.encode(toMap());

  factory PaymentItem.fromMap(Map<String, dynamic> json) => PaymentItem(
    name: json["name"] ?? '',
    quantity: json["quantity"] ?? 0,
    sku: json["sku"] ?? '',
    unitOfMeasure: json["unitOfMeasure"] ?? '',
    unitPrice: json["unitPrice"] ?? 0,
  );

  Map<String, dynamic> toMap() => {
    "name": name,
    "quantity": quantity,
    "sku": sku,
    "unitOfMeasure": unitOfMeasure,
    "unitPrice": unitPrice,
  };
}
