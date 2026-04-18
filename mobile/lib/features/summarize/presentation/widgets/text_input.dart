import 'package:flutter/material.dart';
import 'package:briefen/l10n/generated/app_localizations.dart';

class TextInput extends StatefulWidget {
  final void Function(String text, String? title) onSubmit;
  final bool loading;

  const TextInput({super.key, required this.onSubmit, required this.loading});

  @override
  State<TextInput> createState() => _TextInputState();
}

class _TextInputState extends State<TextInput> {
  final _textController = TextEditingController();

  @override
  void dispose() {
    _textController.dispose();
    super.dispose();
  }

  void _submit() {
    final text = _textController.text.trim();
    if (text.isEmpty || widget.loading) return;
    widget.onSubmit(text, null);
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context)!;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        TextField(
          controller: _textController,
          decoration: InputDecoration(
            hintText: l10n.textInputHint,
            border: const OutlineInputBorder(),
            alignLabelWithHint: true,
          ),
          maxLines: 8,
          minLines: 4,
          textInputAction: TextInputAction.done,
          onSubmitted: (_) => _submit(),
        ),
        const SizedBox(height: 12),
        FilledButton(
          onPressed: widget.loading ? null : _submit,
          child: Text(l10n.summarizeText),
        ),
      ],
    );
  }
}
