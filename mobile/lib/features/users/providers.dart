import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'data/users_repository.dart';
import 'domain/app_user.dart';

final usersProvider = FutureProvider.autoDispose<List<AppUser>>((ref) async {
  final repo = ref.read(usersRepositoryProvider);
  return repo.getUsers();
});
