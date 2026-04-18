class LlmModel {
  final String id;
  final String name;
  final String description;

  const LlmModel({
    required this.id,
    required this.name,
    required this.description,
  });

  factory LlmModel.fromJson(Map<String, dynamic> json) {
    return LlmModel(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? '',
      description: json['description'] as String? ?? '',
    );
  }
}

class LlmProvider {
  final String id;
  final String name;
  final bool configured;
  final List<LlmModel> models;

  const LlmProvider({
    required this.id,
    required this.name,
    required this.configured,
    required this.models,
  });

  factory LlmProvider.fromJson(Map<String, dynamic> json) {
    final rawModels = json['models'] as List<dynamic>? ?? [];
    return LlmProvider(
      id: json['id'] as String? ?? '',
      name: json['name'] as String? ?? '',
      configured: json['configured'] as bool? ?? false,
      models: rawModels
          .map((m) => LlmModel.fromJson(m as Map<String, dynamic>))
          .toList(),
    );
  }
}
