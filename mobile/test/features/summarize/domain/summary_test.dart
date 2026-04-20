import 'package:briefen/features/summarize/domain/summary.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../../helpers/fixtures.dart';

void main() {
  group('Summary', () {
    group('fromJson', () {
      test('should parse all required fields', () {
        final s = Summary.fromJson(summaryFixture);

        expect(s.id, '550e8400-e29b-41d4-a716-446655440000');
        expect(s.url, 'https://example.com/article');
        expect(s.title, 'Example Article');
        expect(s.summary, '# Summary\n\nThis is the summary content.');
        expect(s.modelUsed, 'gemma3:4b');
        expect(s.isRead, false);
        expect(s.hasArticleText, true);
      });

      test('should parse createdAt and savedAt as DateTime', () {
        final s = Summary.fromJson(summaryFixture);

        expect(s.createdAt, DateTime.utc(2026, 4, 17, 10, 30, 0));
        expect(s.savedAt, DateTime.utc(2026, 4, 17, 10, 30, 0));
      });

      test('should parse tags list', () {
        final s = Summary.fromJson(summaryFixture);
        expect(s.tags, ['tech', 'ai']);
      });

      test('should default tags to empty list when missing', () {
        final json = Map<String, dynamic>.from(summaryFixture)..remove('tags');
        final s = Summary.fromJson(json);
        expect(s.tags, isEmpty);
      });

      test('should default hasArticleText to false when key absent', () {
        final json = Map<String, dynamic>.from(summaryFixture)
          ..remove('hasArticleText');
        final s = Summary.fromJson(json);
        expect(s.hasArticleText, false);
      });

      test('should default title to empty string when null', () {
        final json = Map<String, dynamic>.from(summaryFixture)
          ..['title'] = null;
        final s = Summary.fromJson(json);
        expect(s.title, '');
      });

      test('should use createdAt as savedAt when savedAt is absent', () {
        final json = Map<String, dynamic>.from(summaryFixture)
          ..remove('savedAt');
        final s = Summary.fromJson(json);
        expect(s.savedAt, s.createdAt);
      });

      test('should parse notes when present', () {
        final s = Summary.fromJson(summaryFixture2);
        expect(s.notes, 'My note');
      });

      test('should leave notes null when absent', () {
        final json = Map<String, dynamic>.from(summaryFixture)
          ..['notes'] = null;
        final s = Summary.fromJson(json);
        expect(s.notes, isNull);
      });

      test('should parse url as null when missing', () {
        final json = Map<String, dynamic>.from(summaryFixture)..['url'] = null;
        final s = Summary.fromJson(json);
        expect(s.url, isNull);
      });
    });

    group('toJson', () {
      test('should round-trip through fromJson → toJson', () {
        final original = Summary.fromJson(summaryFixture);
        final json = original.toJson();
        final roundTripped = Summary.fromJson(json);

        expect(roundTripped.id, original.id);
        expect(roundTripped.title, original.title);
        expect(roundTripped.summary, original.summary);
        expect(roundTripped.modelUsed, original.modelUsed);
        expect(roundTripped.isRead, original.isRead);
        expect(roundTripped.tags, original.tags);
        expect(roundTripped.hasArticleText, original.hasArticleText);
      });

      test('should include all fields in output', () {
        final s = Summary.fromJson(summaryFixture);
        final json = s.toJson();

        expect(json.containsKey('id'), isTrue);
        expect(json.containsKey('url'), isTrue);
        expect(json.containsKey('title'), isTrue);
        expect(json.containsKey('summary'), isTrue);
        expect(json.containsKey('modelUsed'), isTrue);
        expect(json.containsKey('createdAt'), isTrue);
        expect(json.containsKey('isRead'), isTrue);
        expect(json.containsKey('savedAt'), isTrue);
        expect(json.containsKey('notes'), isTrue);
        expect(json.containsKey('tags'), isTrue);
        expect(json.containsKey('hasArticleText'), isTrue);
      });
    });

    group('copyWith', () {
      test('should update isRead without changing other fields', () {
        final original = Summary.fromJson(summaryFixture);
        final copy = original.copyWith(isRead: true);

        expect(copy.isRead, true);
        expect(copy.id, original.id);
        expect(copy.title, original.title);
        expect(copy.tags, original.tags);
      });

      test('should update notes', () {
        final original = Summary.fromJson(summaryFixture);
        final copy = original.copyWith(notes: 'Updated note');
        expect(copy.notes, 'Updated note');
        expect(copy.title, original.title);
      });

      test('should update tags', () {
        final original = Summary.fromJson(summaryFixture);
        final copy = original.copyWith(tags: ['flutter', 'dart']);
        expect(copy.tags, ['flutter', 'dart']);
        expect(copy.isRead, original.isRead);
      });

      test('should preserve original values when no args provided', () {
        final original = Summary.fromJson(summaryFixture2);
        final copy = original.copyWith();

        expect(copy.isRead, original.isRead);
        expect(copy.notes, original.notes);
        expect(copy.tags, original.tags);
      });
    });

    group('domain getter', () {
      test('should return hostname from url', () {
        final s = Summary.fromJson(summaryFixture);
        expect(s.domain, 'example.com');
      });

      test('should return empty string when url is null', () {
        final json = Map<String, dynamic>.from(summaryFixture)..['url'] = null;
        final s = Summary.fromJson(json);
        expect(s.domain, '');
      });

      test('should handle malformed url gracefully', () {
        final s = Summary(
          id: 'x',
          url: 'not-a-url',
          title: 'T',
          summary: 'S',
          modelUsed: 'm',
          createdAt: DateTime(2026),
          isRead: false,
          savedAt: DateTime(2026),
        );
        expect(s.domain, '');
      });
    });
  });

  group('PaginatedSummaries', () {
    group('fromJson', () {
      test('should parse content list', () {
        final p = PaginatedSummaries.fromJson(paginatedSummariesFixture);
        expect(p.content, hasLength(1));
        expect(p.content.first.id, summaryFixture['id']);
      });

      test('should parse pagination metadata', () {
        final p = PaginatedSummaries.fromJson(paginatedSummariesFixture);
        expect(p.totalElements, 1);
        expect(p.totalPages, 1);
        expect(p.first, true);
        expect(p.last, true);
      });

      test('should default isOffline to false', () {
        final p = PaginatedSummaries.fromJson(paginatedSummariesFixture);
        expect(p.isOffline, false);
      });

      test('should accept isOffline=true override', () {
        final p = PaginatedSummaries.fromJson(
          paginatedSummariesFixture,
          isOffline: true,
        );
        expect(p.isOffline, true);
      });

      test('should parse multi-page response', () {
        final p = PaginatedSummaries.fromJson(
          paginatedSummariesMultiPageFixture,
        );
        expect(p.content, hasLength(2));
        expect(p.totalElements, 5);
        expect(p.totalPages, 3);
        expect(p.last, false);
      });
    });
  });
}
