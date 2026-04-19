class Summary {
  final String id;
  final String? url;
  final String title;
  final String summary;
  final String modelUsed;
  final DateTime createdAt;
  final bool isRead;
  final DateTime savedAt;
  final String? notes;
  final List<String> tags;
  final bool hasArticleText;

  const Summary({
    required this.id,
    this.url,
    required this.title,
    required this.summary,
    required this.modelUsed,
    required this.createdAt,
    required this.isRead,
    required this.savedAt,
    this.notes,
    this.tags = const [],
    this.hasArticleText = false,
  });

  factory Summary.fromJson(Map<String, dynamic> json) {
    return Summary(
      id: json['id'] as String,
      url: json['url'] as String?,
      title: json['title'] as String? ?? '',
      summary: json['summary'] as String? ?? '',
      modelUsed: json['modelUsed'] as String? ?? '',
      createdAt: DateTime.parse(json['createdAt'] as String),
      isRead: json['isRead'] as bool? ?? false,
      savedAt: DateTime.parse(
        (json['savedAt'] as String?) ?? json['createdAt'] as String,
      ),
      notes: json['notes'] as String?,
      tags:
          (json['tags'] as List<dynamic>?)?.map((e) => e as String).toList() ??
          const [],
      hasArticleText: json['hasArticleText'] as bool? ?? false,
    );
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'url': url,
    'title': title,
    'summary': summary,
    'modelUsed': modelUsed,
    'createdAt': createdAt.toIso8601String(),
    'isRead': isRead,
    'savedAt': savedAt.toIso8601String(),
    'notes': notes,
    'tags': tags,
    'hasArticleText': hasArticleText,
  };

  Summary copyWith({bool? isRead, String? notes, List<String>? tags}) {
    return Summary(
      id: id,
      url: url,
      title: title,
      summary: summary,
      modelUsed: modelUsed,
      createdAt: createdAt,
      isRead: isRead ?? this.isRead,
      savedAt: savedAt,
      notes: notes ?? this.notes,
      tags: tags ?? this.tags,
      hasArticleText: hasArticleText,
    );
  }

  String get domain {
    if (url == null) return '';
    final uri = Uri.tryParse(url!);
    return uri?.host ?? '';
  }
}

class PaginatedSummaries {
  final List<Summary> content;
  final int totalElements;
  final int totalPages;
  final bool first;
  final bool last;

  /// Whether this data was served from local cache (no network).
  final bool isOffline;

  const PaginatedSummaries({
    required this.content,
    required this.totalElements,
    required this.totalPages,
    required this.first,
    required this.last,
    this.isOffline = false,
  });

  factory PaginatedSummaries.fromJson(
    Map<String, dynamic> json, {
    bool isOffline = false,
  }) {
    return PaginatedSummaries(
      content: (json['content'] as List<dynamic>)
          .map((e) => Summary.fromJson(e as Map<String, dynamic>))
          .toList(),
      totalElements: json['totalElements'] as int? ?? 0,
      totalPages: json['totalPages'] as int? ?? 0,
      first: json['first'] as bool? ?? true,
      last: json['last'] as bool? ?? true,
      isOffline: isOffline,
    );
  }

  Map<String, dynamic> toJson() => {
    'content': content.map((s) => s.toJson()).toList(),
    'totalElements': totalElements,
    'totalPages': totalPages,
    'first': first,
    'last': last,
  };
}
