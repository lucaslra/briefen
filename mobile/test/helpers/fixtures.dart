/// JSON fixtures matching exact backend response shapes.
library;

const summaryFixture = {
  'id': '550e8400-e29b-41d4-a716-446655440000',
  'url': 'https://example.com/article',
  'title': 'Example Article',
  'summary': '# Summary\n\nThis is the summary content.',
  'modelUsed': 'gemma3:4b',
  'createdAt': '2026-04-17T10:30:00Z',
  'isRead': false,
  'savedAt': '2026-04-17T10:30:00Z',
  'notes': null,
  'tags': ['tech', 'ai'],
  'hasArticleText': true,
};

const summaryFixture2 = {
  'id': '660f9511-f3ac-52e5-b827-557766551111',
  'url': 'https://other.com/post',
  'title': 'Second Article',
  'summary': 'Second summary text.',
  'modelUsed': 'gpt-4o',
  'createdAt': '2026-04-18T08:00:00Z',
  'isRead': true,
  'savedAt': '2026-04-18T08:00:00Z',
  'notes': 'My note',
  'tags': <String>[],
  'hasArticleText': false,
};

const paginatedSummariesFixture = {
  'content': [summaryFixture],
  'totalElements': 1,
  'totalPages': 1,
  'first': true,
  'last': true,
};

const paginatedSummariesMultiPageFixture = {
  'content': [summaryFixture, summaryFixture2],
  'totalElements': 5,
  'totalPages': 3,
  'first': true,
  'last': false,
};

const unreadCountFixture = {'count': 7};

const userSettingsFixture = {
  'defaultLength': 'default',
  'model': 'gemma3:4b',
  'notificationsEnabled': false,
  'openaiApiKey': null,
  'anthropicApiKey': null,
  'readeckApiKey': null,
  'readeckUrl': null,
  'webhookUrl': null,
  'customPrompt': null,
};
