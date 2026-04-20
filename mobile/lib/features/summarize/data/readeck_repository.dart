import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_client.dart';
import '../domain/readeck_bookmark.dart';

final readeckRepositoryProvider = Provider<ReadeckRepository>((ref) {
  return ReadeckRepository(ref.read(apiClientProvider));
});

class ReadeckRepository {
  final ApiClient _client;

  ReadeckRepository(this._client);

  Future<bool> isConfigured() async {
    final response = await _client.get('/api/readeck/status');
    final data = response.data as Map<String, dynamic>;
    return data['configured'] as bool? ?? false;
  }

  Future<List<ReadeckBookmark>> getBookmarks({
    int page = 1,
    int limit = 20,
    String? search,
  }) async {
    final params = <String, dynamic>{'page': page, 'limit': limit};
    if (search != null && search.isNotEmpty) params['search'] = search;

    final response = await _client.get(
      '/api/readeck/bookmarks',
      queryParameters: params,
    );

    // Backend proxies Readeck's raw JSON string — parse it here.
    final raw = response.data;
    List<dynamic> items;
    if (raw is String) {
      final decoded = jsonDecode(raw);
      items = decoded is List
          ? decoded
          : (decoded['items'] ?? decoded['results'] ?? []) as List;
    } else if (raw is List) {
      items = raw;
    } else {
      items = (raw['items'] ?? raw['results'] ?? []) as List;
    }

    return items
        .map((e) => ReadeckBookmark.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<ReadeckArticle> getArticle(String bookmarkId) async {
    final response = await _client.get(
      '/api/readeck/bookmarks/$bookmarkId/article',
    );
    return ReadeckArticle.fromJson(response.data as Map<String, dynamic>);
  }
}
