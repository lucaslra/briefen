import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_svg/flutter_svg.dart';

import '../../../core/auth/auth_provider.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _serverUrlController = TextEditingController();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _loading = false;
  bool _ssoLoading = false;
  bool _serverUrlValid = false;
  bool _obscurePassword = true;

  @override
  void initState() {
    super.initState();
    _serverUrlController.addListener(_onServerUrlChanged);
  }

  @override
  void dispose() {
    _serverUrlController.removeListener(_onServerUrlChanged);
    _serverUrlController.dispose();
    _usernameController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  void _onServerUrlChanged() {
    final valid = _normalizedServerUrl() != null;
    if (valid != _serverUrlValid) {
      setState(() => _serverUrlValid = valid);
    }
  }

  /// Returns the trimmed, trailing-slash-stripped server URL when it is a valid
  /// absolute http(s) URL, or null otherwise.
  String? _normalizedServerUrl() {
    final url = _serverUrlController.text.trim();
    if (url.isEmpty) return null;
    final uri = Uri.tryParse(url);
    if (uri == null || !uri.hasScheme || !uri.hasAuthority) return null;
    return url.endsWith('/') ? url.substring(0, url.length - 1) : url;
  }

  Future<bool> _isSsoEnabled(String serverUrl) async {
    try {
      final dio = Dio(BaseOptions(
        baseUrl: serverUrl,
        connectTimeout: const Duration(seconds: 10),
        receiveTimeout: const Duration(seconds: 10),
      ));
      final res = await dio.get('/api/auth/config');
      final data = res.data as Map<String, dynamic>;
      final oidc = data['oidc'];
      return oidc is Map && oidc['enabled'] == true;
    } catch (_) {
      return false;
    }
  }

  Future<void> _signInWithSso() async {
    final url = _normalizedServerUrl();
    if (url == null) return;
    final l10n = AppLocalizations.of(context)!;

    setState(() => _ssoLoading = true);
    try {
      final enabled = await _isSsoEnabled(url);
      if (!enabled) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text(l10n.ssoNotEnabled)),
          );
        }
        return;
      }
      await ref.read(authProvider.notifier).loginWithSso(url);
    } finally {
      if (mounted) setState(() => _ssoLoading = false);
    }
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _loading = true);

    await ref
        .read(authProvider.notifier)
        .login(
          _serverUrlController.text.trim(),
          _usernameController.text.trim(),
          _passwordController.text,
        );

    if (mounted) setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;
    final authState = ref.watch(authProvider);
    final colorScheme = Theme.of(context).colorScheme;

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: Form(
              key: _formKey,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  SvgPicture.asset(
                    'assets/images/logo.svg',
                    width: 56,
                    height: 56,
                    colorFilter: ColorFilter.mode(
                      colorScheme.primary,
                      BlendMode.srcIn,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    l10n.appName,
                    style: Theme.of(context).textTheme.headlineMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 32),
                  TextFormField(
                    controller: _serverUrlController,
                    decoration: InputDecoration(
                      labelText: l10n.serverUrl,
                      hintText: l10n.serverUrlHint,
                      prefixIcon: const Icon(Icons.dns_outlined),
                    ),
                    keyboardType: TextInputType.url,
                    autocorrect: false,
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) {
                        return l10n.serverUrl;
                      }
                      final uri = Uri.tryParse(value.trim());
                      if (uri == null || !uri.hasScheme || !uri.hasAuthority) {
                        return l10n.serverUrl;
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _usernameController,
                    decoration: InputDecoration(
                      labelText: l10n.username,
                      prefixIcon: const Icon(Icons.person_outlined),
                    ),
                    autocorrect: false,
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) {
                        return l10n.username;
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  TextFormField(
                    controller: _passwordController,
                    decoration: InputDecoration(
                      labelText: l10n.password,
                      prefixIcon: const Icon(Icons.lock_outlined),
                      suffixIcon: IconButton(
                        icon: Icon(
                          _obscurePassword
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                        ),
                        onPressed: () => setState(
                          () => _obscurePassword = !_obscurePassword,
                        ),
                      ),
                    ),
                    obscureText: _obscurePassword,
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return l10n.password;
                      }
                      return null;
                    },
                    onFieldSubmitted: (_) => _submit(),
                  ),
                  if (authState.error != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      authState.error!,
                      style: TextStyle(color: colorScheme.error),
                    ),
                  ],
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    child: FilledButton(
                      onPressed: (_loading || _ssoLoading) ? null : _submit,
                      child: _loading
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : Text(l10n.login),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Row(
                    children: [
                      const Expanded(child: Divider()),
                      Padding(
                        padding: const EdgeInsets.symmetric(horizontal: 12),
                        child: Text(
                          l10n.orDivider,
                          style: TextStyle(color: colorScheme.outline),
                        ),
                      ),
                      const Expanded(child: Divider()),
                    ],
                  ),
                  const SizedBox(height: 16),
                  SizedBox(
                    width: double.infinity,
                    child: OutlinedButton.icon(
                      onPressed: (_loading || _ssoLoading || !_serverUrlValid)
                          ? null
                          : _signInWithSso,
                      icon: _ssoLoading
                          ? const SizedBox(
                              height: 20,
                              width: 20,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.shield_outlined),
                      label: Text(l10n.loginWithSso),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
