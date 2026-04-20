import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/api/api_client.dart';
import '../domain/app_user.dart';

final usersRepositoryProvider = Provider<UsersRepository>((ref) {
  return UsersRepository(ref.read(apiClientProvider));
});

class UsersRepository {
  final ApiClient _client;

  UsersRepository(this._client);

  Future<List<AppUser>> getUsers() async {
    final response = await _client.get('/api/users');
    return (response.data as List)
        .map((e) => AppUser.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<AppUser> createUser({
    required String username,
    required String password,
    required String role,
  }) async {
    final response = await _client.post(
      '/api/users',
      data: {'username': username, 'password': password, 'role': role},
    );
    return AppUser.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> deleteUser(String id) async {
    await _client.delete('/api/users/$id');
  }
}
