import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_client.dart';
import '../../../core/api/api_exceptions.dart';
import '../../summarize/domain/summary.dart';

final readingListRepositoryProvider = Provider<ReadingListRepository>((ref) {
  return ReadingListRepository(ref.read(apiClientProvider));
});

class ReadingListRepository {
  final ApiClient _client;

  ReadingListRepository(this._client);

  Future<PaginatedSummaries> getSummaries({
    int page = 0,
    int size = 20,
    String filter = 'all',
    String? search,
    String? tag,
  }) async {
    final params = <String, dynamic>{
      'page': page,
      'size': size,
      'filter': filter,
    };
    if (search != null && search.isNotEmpty) params['search'] = search;
    if (tag != null && tag.isNotEmpty) params['tag'] = tag;

    final response = await _client.get(
      '/api/summaries',
      queryParameters: params,
    );
    return PaginatedSummaries.fromJson(response.data as Map<String, dynamic>);
  }

  Future<Summary?> getSummaryById(String id) async {
    try {
      final response = await _client.get('/api/summaries/$id');
      return Summary.fromJson(response.data as Map<String, dynamic>);
    } on NotFoundException {
      return null;
    }
  }

  Future<Summary> toggleReadStatus(String id, bool isRead) async {
    final response = await _client.patch(
      '/api/summaries/$id/read-status',
      data: {'isRead': isRead},
    );
    return Summary.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> deleteSummary(String id) async {
    await _client.delete('/api/summaries/$id');
  }

  Future<Summary> updateNotes(String id, String notes) async {
    final response = await _client.patch(
      '/api/summaries/$id/notes',
      data: {'notes': notes},
    );
    return Summary.fromJson(response.data as Map<String, dynamic>);
  }

  Future<Summary> updateTags(String id, List<String> tags) async {
    final response = await _client.patch(
      '/api/summaries/$id/tags',
      data: {'tags': tags},
    );
    return Summary.fromJson(response.data as Map<String, dynamic>);
  }

  Future<String?> getArticleText(String id) async {
    final response = await _client.get('/api/summaries/$id/article-text');
    final data = response.data as Map<String, dynamic>;
    return data['articleText'] as String?;
  }

  Future<void> markAllRead() async {
    await _client.patch('/api/summaries/read-status/bulk');
  }

  Future<void> markAllUnread() async {
    await _client.patch('/api/summaries/unread-status/bulk');
  }

  Future<String> exportMarkdown({String filter = 'all'}) async {
    final response = await _client.get(
      '/api/summaries/export',
      queryParameters: {'format': 'md', 'filter': filter},
    );
    return response.data as String;
  }
}
