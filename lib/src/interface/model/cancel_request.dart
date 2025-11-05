import 'dart:convert';

class CancelRequest {
  String id;
  String clientId;
  String accessToken;
  String cieloCode;
  String authCode;
  int value;

  CancelRequest({
    required this.id,
    required this.clientId,
    required this.accessToken,
    required this.cieloCode,
    required this.authCode,
    required this.value,
  });

  factory CancelRequest.fromJson(String str) => CancelRequest.fromMap(json.decode(str));

  String toJson() => json.encode(toMap());

  factory CancelRequest.fromMap(Map<String, dynamic> json) => CancelRequest(
    id: json["id"] ?? '',
    clientId: json["clientID"] ?? '',
    accessToken: json["accessToken"] ?? '',
    cieloCode: json["cieloCode"] ?? '',
    authCode: json["authCode"] ?? '',
    value: json["value"] ?? 0,
  );

  Map<String, dynamic> toMap() => {
    "id": id,
    "clientID": clientId,
    "accessToken": accessToken,
    "cieloCode": cieloCode,
    "authCode": authCode,
    "value": value,
  };

  String toBase64Encode() {
    final encodedText = utf8.encode(toJson());
    return base64.encode(encodedText);
  }
}

