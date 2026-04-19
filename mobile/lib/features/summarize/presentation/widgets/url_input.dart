import 'package:flutter/material.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';

class UrlInput extends StatefulWidget {
  final ValueChanged<String> onSubmit;
  final bool loading;
  final String? initialUrl;

  const UrlInput({
    super.key,
    required this.onSubmit,
    this.loading = false,
    this.initialUrl,
  });

  @override
  State<UrlInput> createState() => _UrlInputState();
}

class _UrlInputState extends State<UrlInput> {
  late final TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.initialUrl ?? '');
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _submit() {
    final url = _controller.text.trim();
    if (url.isEmpty) return;

    final uri = Uri.tryParse(url);
    if (uri == null || !uri.hasScheme || !uri.hasAuthority) return;

    widget.onSubmit(url);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        TextField(
          controller: _controller,
          decoration: InputDecoration(
            hintText: l10n.summarizeHint,
            prefixIcon: const Icon(Icons.link),
            suffixIcon: _controller.text.isNotEmpty
                ? IconButton(
                    icon: const Icon(Icons.clear),
                    onPressed: () {
                      _controller.clear();
                      setState(() {});
                    },
                  )
                : null,
          ),
          keyboardType: TextInputType.url,
          autocorrect: false,
          textInputAction: TextInputAction.go,
          onChanged: (_) => setState(() {}),
          onSubmitted: (_) => _submit(),
          enabled: !widget.loading,
        ),
        const SizedBox(height: 16),
        SizedBox(
          width: double.infinity,
          child: FilledButton.icon(
            onPressed: widget.loading ? null : _submit,
            icon: widget.loading
                ? const SizedBox(
                    height: 18,
                    width: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.summarize),
            label: Text(widget.loading ? l10n.summarizing : l10n.summarize),
          ),
        ),
      ],
    );
  }
}
