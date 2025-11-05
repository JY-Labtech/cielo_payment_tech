import 'dart:convert';

enum PrintOperation {
  printText,
  printImage,
}

extension PrintOperationExtension on PrintOperation {
  String get value {
    switch (this) {
      case PrintOperation.printText:
        return 'PRINT_TEXT';
      case PrintOperation.printImage:
        return 'PRINT_IMAGE';
    }
  }
}

class PrintRequest {
  String operation;
  List<Map<String, dynamic>> styles;
  List<String> value;

  PrintRequest({
    required this.operation,
    this.styles = const [],
    required this.value,
  });

  /// Factory para criar um PrintRequest de texto
  factory PrintRequest.text({
    required String text,
    List<Map<String, dynamic>>? styles,
  }) {
    return PrintRequest(
      operation: PrintOperation.printText.value,
      styles: styles ?? [{}],
      value: [text],
    );
  }

  /// Factory para criar um PrintRequest de imagem
  factory PrintRequest.image({
    required String imagePath,
    List<Map<String, dynamic>>? styles,
  }) {
    return PrintRequest(
      operation: PrintOperation.printImage.value,
      styles: styles ?? [{}],
      value: [imagePath],
    );
  }

  factory PrintRequest.fromJson(String str) => PrintRequest.fromMap(json.decode(str));

  String toJson() => json.encode(toMap());

  factory PrintRequest.fromMap(Map<String, dynamic> json) => PrintRequest(
    operation: json["operation"] ?? '',
    styles: json["styles"] == null 
        ? [] 
        : List<Map<String, dynamic>>.from(json["styles"].map((x) => x as Map<String, dynamic>)),
    value: json["value"] == null 
        ? [] 
        : List<String>.from(json["value"].map((x) => x.toString())),
  );

  Map<String, dynamic> toMap() => {
    "operation": operation,
    "styles": List<dynamic>.from(styles.map((x) => x)),
    "value": List<dynamic>.from(value.map((x) => x)),
  };

  String toBase64Encode() {
    final encodedText = utf8.encode(toJson());
    return base64.encode(encodedText);
  }
}

