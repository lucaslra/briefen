import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_exceptions.dart';

final setupRepositoryProvider = Provider<SetupRepository>((ref) {
  return SetupRepository();
});

class SetupRepository {
  Future<bool> checkSetupRequired(String serverUrl) async {
    try {
      final dio = Dio(BaseOptions(
        baseUrl: serverUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
      ));
      final response = await dio.get('/api/setup/status');
      final data = response.data as Map<String, dynamic>;
      return data['setupRequired'] == true;
    } catch (_) {
      rethrow;
    }
  }

  Future<void> createAdmin(
    String serverUrl,
    String username,
    String password,
  ) async {
    try {
      final dio = Dio(BaseOptions(
        baseUrl: serverUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
      ));
      await dio.post('/api/setup', data: {
        'username': username,
        'password': password,
      });
    } on DioException catch (e) {
      final status = e.response?.statusCode;
      final body = e.response?.data;
      final message = body is Map ? body['error'] as String? : null;
      throw ServerException(message ?? 'Setup failed', statusCode: status);
    }
  }
}
