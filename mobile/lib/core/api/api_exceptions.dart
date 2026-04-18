sealed class ApiException implements Exception {
  final String message;
  const ApiException(this.message);

  @override
  String toString() => message;
}

class AuthException extends ApiException {
  const AuthException([super.message = 'Invalid credentials']);
}

class NetworkException extends ApiException {
  const NetworkException([super.message = 'Network error']);
}

class ApiTimeoutException extends ApiException {
  const ApiTimeoutException([super.message = 'Request timed out']);
}

class NotFoundException extends ApiException {
  const NotFoundException([super.message = 'Not found']);
}

class ConflictException extends ApiException {
  const ConflictException([super.message = 'Conflict']);
}

class ServerException extends ApiException {
  final int? statusCode;
  const ServerException(super.message, {this.statusCode});
}
