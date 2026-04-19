// ignore: unused_import
import 'package:intl/intl.dart' as intl;
import 'app_localizations.dart';

// ignore_for_file: type=lint

/// The translations for Portuguese (`pt`).
class AppLocalizationsPt extends AppLocalizations {
  AppLocalizationsPt([String locale = 'pt']) : super(locale);

  @override
  String get appName => 'Briefen';

  @override
  String get login => 'Entrar';

  @override
  String get logout => 'Sair';

  @override
  String get serverUrl => 'URL do Servidor';

  @override
  String get serverUrlHint => 'https://briefen.exemplo.com';

  @override
  String get username => 'Usuário';

  @override
  String get password => 'Senha';

  @override
  String get loginError => 'Credenciais inválidas ou servidor inacessível';

  @override
  String get setupTitle => 'Criar Conta de Administrador';

  @override
  String get setupSubtitle => 'Configure sua instância do Briefen';

  @override
  String get confirmPassword => 'Confirmar Senha';

  @override
  String get createAccount => 'Criar Conta';

  @override
  String get passwordsDoNotMatch => 'As senhas não coincidem';

  @override
  String get passwordTooShort => 'A senha deve ter pelo menos 8 caracteres';

  @override
  String get passwordRequirements =>
      'Deve incluir maiúscula, minúscula, dígito e caractere especial';

  @override
  String get setupError => 'Falha ao criar conta';

  @override
  String get summarize => 'Resumir';

  @override
  String get summarizeHint => 'Cole a URL do artigo...';

  @override
  String get summarizing => 'Resumindo...';

  @override
  String get summarizeError => 'Falha ao resumir artigo';

  @override
  String get readingList => 'Lista de Leitura';

  @override
  String get settings => 'Configurações';

  @override
  String get all => 'Todos';

  @override
  String get unread => 'Não lidos';

  @override
  String get read => 'Lidos';

  @override
  String get search => 'Buscar';

  @override
  String get noSummaries => 'Nenhum resumo ainda';

  @override
  String get noResults => 'Nenhum resultado encontrado';

  @override
  String get markAsRead => 'Marcar como lido';

  @override
  String get markAsUnread => 'Marcar como não lido';

  @override
  String get delete => 'Excluir';

  @override
  String get deleteConfirmTitle => 'Excluir Resumo';

  @override
  String get deleteConfirmMessage =>
      'Tem certeza que deseja excluir este resumo?';

  @override
  String get cancel => 'Cancelar';

  @override
  String get openArticle => 'Abrir Artigo';

  @override
  String get share => 'Compartilhar';

  @override
  String get copyToClipboard => 'Copiar';

  @override
  String get copied => 'Copiado';

  @override
  String get notes => 'Notas';

  @override
  String get notesHint => 'Adicionar nota...';

  @override
  String get tags => 'Tags';

  @override
  String get addTag => 'Adicionar tag...';

  @override
  String get model => 'Modelo';

  @override
  String get source => 'Fonte';

  @override
  String get savedAt => 'Salvo';

  @override
  String get theme => 'Tema';

  @override
  String get darkMode => 'Modo Escuro';

  @override
  String get lightMode => 'Modo Claro';

  @override
  String get version => 'Versão';

  @override
  String get server => 'Servidor';

  @override
  String get account => 'Conta';

  @override
  String get retry => 'Tentar novamente';

  @override
  String get networkError => 'Erro de rede. Verifique sua conexão.';

  @override
  String get timeoutError => 'Tempo esgotado. O servidor pode estar ocupado.';

  @override
  String get unknownError => 'Algo deu errado';

  @override
  String get urlTab => 'URL';

  @override
  String get textTab => 'Texto';

  @override
  String get textInputHint => 'Cole ou digite o texto do artigo...';

  @override
  String get titleHint => 'Título (opcional)';

  @override
  String get summarizeText => 'Resumir Texto';

  @override
  String get saveNotes => 'Salvar';

  @override
  String get notesUpdated => 'Notas salvas';

  @override
  String get removeTag => 'Remover tag';

  @override
  String get tagHint => 'Nova tag';

  @override
  String get tagsUpdated => 'Tags atualizadas';

  @override
  String get export => 'Exportar';

  @override
  String get exportReadingList => 'Exportar lista de leitura';

  @override
  String get markAllRead => 'Marcar todos como lidos';

  @override
  String get markAllUnread => 'Marcar todos como não lidos';

  @override
  String get bulkUpdated => 'Lista de leitura atualizada';

  @override
  String get batchTab => 'Lote';

  @override
  String get batchInputHint => 'Cole URLs, uma por linha...';

  @override
  String get batchSummarize => 'Resumir Tudo';

  @override
  String batchProgress(int current, int total) {
    return 'Resumindo $current de $total…';
  }

  @override
  String batchComplete(int count) {
    String _temp0 = intl.Intl.pluralLogic(
      count,
      locale: localeName,
      other: '$count resumos prontos',
      one: '$count resumo pronto',
    );
    return '$_temp0';
  }

  @override
  String get makeShorter => 'Resumir mais';

  @override
  String get makeLonger => 'Ampliar';

  @override
  String get regenerate => 'Regenerar';

  @override
  String get adjustingSummary => 'Ajustando…';

  @override
  String get filterByTag => 'Filtrar por tag';

  @override
  String get summarizationSettings => 'Resumo';

  @override
  String get defaultLength => 'Comprimento Padrão';

  @override
  String get lengthDefault => 'Padrão';

  @override
  String get lengthShort => 'Curto';

  @override
  String get lengthMedium => 'Médio';

  @override
  String get lengthLong => 'Longo';

  @override
  String get customPrompt => 'Prompt Personalizado';

  @override
  String get customPromptHint => 'Substituir o prompt de resumo padrão...';

  @override
  String get integrations => 'Integrações';

  @override
  String get openaiApiKey => 'Chave API OpenAI';

  @override
  String get anthropicApiKey => 'Chave API Anthropic';

  @override
  String get readeckApiKey => 'Chave API Readeck';

  @override
  String get readeckUrl => 'URL do Readeck';

  @override
  String get webhookUrl => 'URL do Webhook';

  @override
  String get keyNotSet => 'Não configurado';

  @override
  String get settingsSaved => 'Configurações salvas';

  @override
  String get language => 'Idioma';

  @override
  String get selectModel => 'Selecionar Modelo';

  @override
  String get appearance => 'Aparência';

  @override
  String get notificationsEnabled => 'Notificações';

  @override
  String get about => 'Sobre';

  @override
  String get manageUsers => 'Gerenciar Usuários';

  @override
  String get createUser => 'Criar Usuário';

  @override
  String get deleteUser => 'Excluir Usuário';

  @override
  String deleteUserConfirm(String username) {
    return 'Excluir usuário \"$username\"? Esta ação não pode ser desfeita.';
  }

  @override
  String get roleLabel => 'Função';

  @override
  String get adminRole => 'Administrador';

  @override
  String get userRole => 'Usuário';

  @override
  String get userCreated => 'Usuário criado';

  @override
  String get userDeleted => 'Usuário excluído';

  @override
  String get userCreateError => 'Falha ao criar usuário';

  @override
  String get administration => 'Administração';
}

/// The translations for Portuguese, as used in Brazil (`pt_BR`).
class AppLocalizationsPtBr extends AppLocalizationsPt {
  AppLocalizationsPtBr() : super('pt_BR');

  @override
  String get appName => 'Briefen';

  @override
  String get login => 'Entrar';

  @override
  String get logout => 'Sair';

  @override
  String get serverUrl => 'URL do Servidor';

  @override
  String get serverUrlHint => 'https://briefen.exemplo.com';

  @override
  String get username => 'Usuário';

  @override
  String get password => 'Senha';

  @override
  String get loginError => 'Credenciais inválidas ou servidor inacessível';

  @override
  String get setupTitle => 'Criar Conta de Administrador';

  @override
  String get setupSubtitle => 'Configure sua instância do Briefen';

  @override
  String get confirmPassword => 'Confirmar Senha';

  @override
  String get createAccount => 'Criar Conta';

  @override
  String get passwordsDoNotMatch => 'As senhas não coincidem';

  @override
  String get passwordTooShort => 'A senha deve ter pelo menos 8 caracteres';

  @override
  String get passwordRequirements =>
      'Deve incluir maiúscula, minúscula, dígito e caractere especial';

  @override
  String get setupError => 'Falha ao criar conta';

  @override
  String get summarize => 'Resumir';

  @override
  String get summarizeHint => 'Cole a URL do artigo...';

  @override
  String get summarizing => 'Resumindo...';

  @override
  String get summarizeError => 'Falha ao resumir artigo';

  @override
  String get readingList => 'Lista de Leitura';

  @override
  String get settings => 'Configurações';

  @override
  String get all => 'Todos';

  @override
  String get unread => 'Não lidos';

  @override
  String get read => 'Lidos';

  @override
  String get search => 'Buscar';

  @override
  String get noSummaries => 'Nenhum resumo ainda';

  @override
  String get noResults => 'Nenhum resultado encontrado';

  @override
  String get markAsRead => 'Marcar como lido';

  @override
  String get markAsUnread => 'Marcar como não lido';

  @override
  String get delete => 'Excluir';

  @override
  String get deleteConfirmTitle => 'Excluir Resumo';

  @override
  String get deleteConfirmMessage =>
      'Tem certeza que deseja excluir este resumo?';

  @override
  String get cancel => 'Cancelar';

  @override
  String get openArticle => 'Abrir Artigo';

  @override
  String get share => 'Compartilhar';

  @override
  String get copyToClipboard => 'Copiar';

  @override
  String get copied => 'Copiado';

  @override
  String get notes => 'Notas';

  @override
  String get notesHint => 'Adicionar nota...';

  @override
  String get tags => 'Tags';

  @override
  String get addTag => 'Adicionar tag...';

  @override
  String get model => 'Modelo';

  @override
  String get source => 'Fonte';

  @override
  String get savedAt => 'Salvo';

  @override
  String get theme => 'Tema';

  @override
  String get darkMode => 'Modo Escuro';

  @override
  String get lightMode => 'Modo Claro';

  @override
  String get version => 'Versão';

  @override
  String get server => 'Servidor';

  @override
  String get account => 'Conta';

  @override
  String get retry => 'Tentar novamente';

  @override
  String get networkError => 'Erro de rede. Verifique sua conexão.';

  @override
  String get timeoutError => 'Tempo esgotado. O servidor pode estar ocupado.';

  @override
  String get unknownError => 'Algo deu errado';
}
