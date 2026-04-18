import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/auth_storage.dart';
import 'api_exceptions.dart';

final apiClientProvider = Provider<ApiClient>((ref) {
  return ApiClient(ref);
});

class ApiClient {
  final Ref _ref;
  Dio? _dio;

  ApiClient(this._ref);

  Future<Dio> _getDio() async {
    if (_dio != null) return _dio!;

    final storage = _ref.read(authStorageProvider);
    final creds = await storage.readCredentials();
    if (creds == null) throw const AuthException();

    _dio = _createDio(creds.serverUrl, creds.username, creds.password);
    return _dio!;
  }

  Dio _createDio(String baseUrl, String username, String password) {
    final dio = Dio(
      BaseOptions(
        baseUrl: baseUrl,
        connectTimeout: const Duration(seconds: 30),
        receiveTimeout: const Duration(minutes: 6),
      ),
    );

    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          final encoded = base64Encode(utf8.encode('$username:$password'));
          options.headers['Authorization'] = 'Basic $encoded';
          handler.next(options);
        },
        onError: (error, handler) async {
          if (error.response?.statusCode == 401) {
            final storage = _ref.read(authStorageProvider);
            await storage.clearCredentials();
          }
          handler.next(error);
        },
      ),
    );

    return dio;
  }

  void reset() {
    _dio?.close();
    _dio = null;
  }

  /// Create a temporary Dio for login validation (before credentials are stored).
  Dio createTempDio(String baseUrl, String username, String password) {
    return _createDio(baseUrl, username, password);
  }

  Future<Response<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
    Duration? timeout,
  }) async {
    final dio = await _getDio();
    try {
      return await dio.get<T>(path, queryParameters: queryParameters);
    } on DioException catch (e) {
      throw _mapException(e);
    } catch (e) {
      throw ServerException(e.toString());
    }
  }

  Future<Response<T>> post<T>(
    String path, {
    Object? data,
    Map<String, dynamic>? queryParameters,
    Duration? timeout,
  }) async {
    final dio = await _getDio();
    try {
      return await dio.post<T>(
        path,
        data: data,
        queryParameters: queryParameters,
        options: timeout != null ? Options(receiveTimeout: timeout) : null,
      );
    } on DioException catch (e) {
      debugPrint(
        'DioException on POST $path: type=${e.type} message=${e.message} statusCode=${e.response?.statusCode} error=${e.error}',
      );
      throw _mapException(e);
    } catch (e) {
      debugPrint('Non-Dio exception on POST $path: $e');
      throw ServerException(e.toString());
    }
  }

  Future<Response<T>> put<T>(String path, {Object? data}) async {
    final dio = await _getDio();
    try {
      return await dio.put<T>(path, data: data);
    } on DioException catch (e) {
      throw _mapException(e);
    } catch (e) {
      throw ServerException(e.toString());
    }
  }

  Future<Response<T>> patch<T>(String path, {Object? data}) async {
    final dio = await _getDio();
    try {
      return await dio.patch<T>(path, data: data);
    } on DioException catch (e) {
      throw _mapException(e);
    } catch (e) {
      throw ServerException(e.toString());
    }
  }

  Future<Response<T>> delete<T>(String path) async {
    final dio = await _getDio();
    try {
      return await dio.delete<T>(path);
    } on DioException catch (e) {
      throw _mapException(e);
    } catch (e) {
      throw ServerException(e.toString());
    }
  }

  ApiException _mapException(DioException e) {
    if (e.type == DioExceptionType.connectionTimeout ||
        e.type == DioExceptionType.receiveTimeout ||
        e.type == DioExceptionType.sendTimeout) {
      return const ApiTimeoutException();
    }
    if (e.type == DioExceptionType.connectionError) {
      return const NetworkException();
    }
    final status = e.response?.statusCode;
    if (status == null) return NetworkException(e.message ?? 'Network error');

    final body = e.response?.data;
    final message = body is Map ? (body['error'] as String?) ?? '' : '';

    return switch (status) {
      401 => const AuthException(),
      404 => NotFoundException(message),
      409 => ConflictException(message),
      504 => const ApiTimeoutException(),
      _ => ServerException(
        message.isNotEmpty ? message : 'Server error ($status)',
        statusCode: status,
      ),
    };
  }
}
