class AppUser {
  final String id;
  final String username;
  final String role;
  final DateTime createdAt;
  final bool mainAdmin;

  const AppUser({
    required this.id,
    required this.username,
    required this.role,
    required this.createdAt,
    required this.mainAdmin,
  });

  bool get isAdmin => role == 'ADMIN';

  factory AppUser.fromJson(Map<String, dynamic> json) {
    return AppUser(
      id: json['id'] as String,
      username: json['username'] as String,
      role: json['role'] as String,
      createdAt: DateTime.parse(json['createdAt'] as String),
      mainAdmin: json['mainAdmin'] as bool? ?? false,
    );
  }
}
