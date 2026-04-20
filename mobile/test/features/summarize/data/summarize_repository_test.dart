import 'package:briefen/core/api/api_client.dart';
import 'package:briefen/core/api/api_exceptions.dart';
import 'package:briefen/features/summarize/data/summarize_repository.dart';
import 'package:briefen/features/summarize/domain/summary.dart';
import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:mocktail/mocktail.dart';

import '../../../helpers/fixtures.dart';

class MockApiClient extends Mock implements ApiClient {}

Response<dynamic> _ok(String path, Map<String, dynamic> data) => Response(
  requestOptions: RequestOptions(path: path),
  statusCode: 200,
  data: data,
);

Response<dynamic> _okRaw(String path, dynamic data) => Response(
  requestOptions: RequestOptions(path: path),
  statusCode: 200,
  data: data,
);

void main() {
  late MockApiClient mockClient;
  late SummarizeRepository repository;

  setUp(() {
    mockClient = MockApiClient();
    repository = SummarizeRepository(mockClient);
  });

  group('summarize', () {
    test('should send url in POST body and return Summary', () async {
      when(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: any(named: 'data'),
          queryParameters: any(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _ok('/api/summarize', summaryFixture));

      final result = await repository.summarize(
        url: 'https://example.com/article',
      );

      expect(result, isA<Summary>());
      expect(result.id, summaryFixture['id']);
      expect(result.title, summaryFixture['title']);

      final captured = verify(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: captureAny(named: 'data'),
          queryParameters: any(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).captured;
      final body = captured.first as Map<String, dynamic>;
      expect(body['url'], 'https://example.com/article');
      expect(body.containsKey('text'), isFalse);
    });

    test('should send text and title in POST body', () async {
      when(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: any(named: 'data'),
          queryParameters: any(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _ok('/api/summarize', summaryFixture));

      await repository.summarize(
        text: 'Article body text here.',
        title: 'My Title',
      );

      final captured = verify(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: captureAny(named: 'data'),
          queryParameters: any(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).captured;
      final body = captured.first as Map<String, dynamic>;
      expect(body['text'], 'Article body text here.');
      expect(body['title'], 'My Title');
      expect(body.containsKey('url'), isFalse);
    });

    test(
      'should include refresh=true query param when refresh is set',
      () async {
        when(
          () => mockClient.post<dynamic>(
            '/api/summarize',
            data: any(named: 'data'),
            queryParameters: any(named: 'queryParameters'),
            timeout: any(named: 'timeout'),
          ),
        ).thenAnswer((_) async => _ok('/api/summarize', summaryFixture));

        await repository.summarize(url: 'https://example.com', refresh: true);

        final captured = verify(
          () => mockClient.post<dynamic>(
            '/api/summarize',
            data: any(named: 'data'),
            queryParameters: captureAny(named: 'queryParameters'),
            timeout: any(named: 'timeout'),
          ),
        ).captured;
        final params = captured.first as Map<String, dynamic>?;
        expect(params, {'refresh': 'true'});
      },
    );

    test('should not include refresh param when refresh is false', () async {
      when(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: any(named: 'data'),
          queryParameters: any(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _ok('/api/summarize', summaryFixture));

      await repository.summarize(url: 'https://example.com');

      final captured = verify(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: any(named: 'data'),
          queryParameters: captureAny(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).captured;
      expect(captured.first, isNull);
    });

    test('should propagate ApiTimeoutException from client', () async {
      when(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: any(named: 'data'),
          queryParameters: any(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).thenThrow(const ApiTimeoutException());

      expect(
        () => repository.summarize(url: 'https://example.com'),
        throwsA(isA<ApiTimeoutException>()),
      );
    });

    test('should propagate NetworkException from client', () async {
      when(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: any(named: 'data'),
          queryParameters: any(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).thenThrow(const NetworkException());

      expect(
        () => repository.summarize(url: 'https://example.com'),
        throwsA(isA<NetworkException>()),
      );
    });

    test('should include optional lengthHint and model in body', () async {
      when(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: any(named: 'data'),
          queryParameters: any(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).thenAnswer((_) async => _ok('/api/summarize', summaryFixture));

      await repository.summarize(
        url: 'https://example.com',
        lengthHint: 'shorter',
        model: 'gpt-4o',
      );

      final captured = verify(
        () => mockClient.post<dynamic>(
          '/api/summarize',
          data: captureAny(named: 'data'),
          queryParameters: any(named: 'queryParameters'),
          timeout: any(named: 'timeout'),
        ),
      ).captured;
      final body = captured.first as Map<String, dynamic>;
      expect(body['lengthHint'], 'shorter');
      expect(body['model'], 'gpt-4o');
    });
  });

  group('getSummaries', () {
    test('should send page and size as query params', () async {
      when(
        () => mockClient.get<dynamic>(
          '/api/summaries',
          queryParameters: any(named: 'queryParameters'),
        ),
      ).thenAnswer(
        (_) async => _ok('/api/summaries', paginatedSummariesFixture),
      );

      await repository.getSummaries(page: 2, size: 15);

      final captured = verify(
        () => mockClient.get<dynamic>(
          '/api/summaries',
          queryParameters: captureAny(named: 'queryParameters'),
        ),
      ).captured;
      final params = captured.first as Map<String, dynamic>;
      expect(params['page'], 2);
      expect(params['size'], 15);
    });

    test('should include search param when provided', () async {
      when(
        () => mockClient.get<dynamic>(
          '/api/summaries',
          queryParameters: any(named: 'queryParameters'),
        ),
      ).thenAnswer(
        (_) async => _ok('/api/summaries', paginatedSummariesFixture),
      );

      await repository.getSummaries(search: 'flutter');

      final captured = verify(
        () => mockClient.get<dynamic>(
          '/api/summaries',
          queryParameters: captureAny(named: 'queryParameters'),
        ),
      ).captured;
      expect((captured.first as Map)['search'], 'flutter');
    });

    test('should not include search when empty string', () async {
      when(
        () => mockClient.get<dynamic>(
          '/api/summaries',
          queryParameters: any(named: 'queryParameters'),
        ),
      ).thenAnswer(
        (_) async => _ok('/api/summaries', paginatedSummariesFixture),
      );

      await repository.getSummaries(search: '');

      final captured = verify(
        () => mockClient.get<dynamic>(
          '/api/summaries',
          queryParameters: captureAny(named: 'queryParameters'),
        ),
      ).captured;
      expect((captured.first as Map).containsKey('search'), isFalse);
    });

    test('should return PaginatedSummaries from response', () async {
      when(
        () => mockClient.get<dynamic>(
          '/api/summaries',
          queryParameters: any(named: 'queryParameters'),
        ),
      ).thenAnswer(
        (_) async => _ok('/api/summaries', paginatedSummariesFixture),
      );

      final result = await repository.getSummaries();

      expect(result.content, hasLength(1));
      expect(result.totalElements, 1);
      expect(result.last, true);
    });
  });

  group('getUnreadCount', () {
    test('should return count from response', () async {
      when(
        () => mockClient.get<dynamic>('/api/summaries/unread-count'),
      ).thenAnswer(
        (_) async => _ok('/api/summaries/unread-count', unreadCountFixture),
      );

      final count = await repository.getUnreadCount();
      expect(count, 7);
    });

    test('should return 0 when count key is missing', () async {
      when(
        () => mockClient.get<dynamic>('/api/summaries/unread-count'),
      ).thenAnswer(
        (_) async => _okRaw('/api/summaries/unread-count', <String, dynamic>{}),
      );

      final count = await repository.getUnreadCount();
      expect(count, 0);
    });
  });
}
