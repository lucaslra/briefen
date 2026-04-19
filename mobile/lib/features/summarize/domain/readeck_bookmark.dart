class ReadeckBookmark {
  final String id;
  final String title;
  final String? url;
  final DateTime? createdAt;
  final bool isArchived;
  final int? wordCount;

  const ReadeckBookmark({
    required this.id,
    required this.title,
    this.url,
    this.createdAt,
    this.isArchived = false,
    this.wordCount,
  });

  factory ReadeckBookmark.fromJson(Map<String, dynamic> json) {
    DateTime? createdAt;
    final created = json['created'] ?? json['created_at'];
    if (created != null) {
      createdAt = DateTime.tryParse(created.toString());
    }

    return ReadeckBookmark(
      id: json['id']?.toString() ?? '',
      title: json['title']?.toString() ?? '(Untitled)',
      url: json['url']?.toString(),
      createdAt: createdAt,
      isArchived: json['is_archived'] as bool? ?? false,
      wordCount: json['word_count'] as int?,
    );
  }
}

class ReadeckArticle {
  final String title;
  final String text;
  final String? error;

  const ReadeckArticle({required this.title, required this.text, this.error});

  factory ReadeckArticle.fromJson(Map<String, dynamic> json) {
    return ReadeckArticle(
      title: json['title']?.toString() ?? '',
      text: json['text']?.toString() ?? '',
      error: json['error']?.toString(),
    );
  }

  bool get hasError => error != null && error!.isNotEmpty;
}
