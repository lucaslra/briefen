import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';

import '../../summarize/domain/summary.dart';

final readingListCacheProvider = Provider<ReadingListCache>(
  (_) => ReadingListCache(),
);

/// Persists the most recent reading list response (per filter) to
/// SharedPreferences so it can be shown when the device is offline.
///
/// Only caches unfiltered results (no search, no tag) to keep the
/// number of cache keys bounded.
class ReadingListCache {
  static const _prefix = 'briefen_rl_';

  String _key(String filter) => '$_prefix$filter';

  Future<void> save(String filter, PaginatedSummaries data) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final json = jsonEncode(data.toJson());
      await prefs.setString(_key(filter), json);
    } catch (_) {
      // Cache writes are best-effort — never fail the caller.
    }
  }

  Future<PaginatedSummaries?> load(String filter) async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final raw = prefs.getString(_key(filter));
      if (raw == null) return null;
      final json = jsonDecode(raw) as Map<String, dynamic>;
      return PaginatedSummaries.fromJson(json, isOffline: true);
    } catch (_) {
      return null;
    }
  }

  Future<void> clear() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final keys = prefs.getKeys().where((k) => k.startsWith(_prefix));
      for (final k in keys) {
        await prefs.remove(k);
      }
    } catch (_) {}
  }
}
