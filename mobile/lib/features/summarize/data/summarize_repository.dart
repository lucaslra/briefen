import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_client.dart';
import '../domain/summary.dart';

final summarizeRepositoryProvider = Provider<SummarizeRepository>((ref) {
  return SummarizeRepository(ref.read(apiClientProvider));
});

class SummarizeRepository {
  final ApiClient _client;

  SummarizeRepository(this._client);

  Future<Summary> summarize({
    String? url,
    String? text,
    String? title,
    String? lengthHint,
    String? model,
    bool refresh = false,
    Duration timeout = const Duration(seconds: 310),
  }) async {
    final body = <String, dynamic>{};
    if (url != null) body['url'] = url;
    if (text != null) body['text'] = text;
    if (title != null) body['title'] = title;
    if (lengthHint != null) body['lengthHint'] = lengthHint;
    if (model != null) body['model'] = model;

    final response = await _client.post(
      '/api/summarize',
      data: body,
      queryParameters: refresh ? {'refresh': 'true'} : null,
      timeout: timeout,
    );
    return Summary.fromJson(response.data as Map<String, dynamic>);
  }

  Future<PaginatedSummaries> getSummaries({
    int page = 0,
    int size = 10,
    String? search,
  }) async {
    final params = <String, dynamic>{'page': page, 'size': size};
    if (search != null && search.isNotEmpty) params['search'] = search;

    final response = await _client.get(
      '/api/summaries',
      queryParameters: params,
    );
    return PaginatedSummaries.fromJson(response.data as Map<String, dynamic>);
  }

  Future<int> getUnreadCount() async {
    final response = await _client.get('/api/summaries/unread-count');
    final data = response.data as Map<String, dynamic>;
    return data['count'] as int? ?? 0;
  }
}
