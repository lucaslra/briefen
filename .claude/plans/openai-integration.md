# OpenAI Integration Plan

## Overview
Add OpenAI as an alternative summarization provider. Users can input an API key in Settings, and then select OpenAI models alongside the existing local Ollama models.

## Decisions (from user Q&A)
- **Key storage**: Backend (MongoDB), plaintext — acceptable for local single-user app
- **Model list**: Curated defaults + "Other" option for custom model IDs
- **Prompts**: Provider-optimized (OpenAI Chat Completions uses system/user message split)

## Architecture

### New concept: "Provider"
Each model belongs to a provider (`ollama` or `openai`). The summarization request includes the model ID, and the backend routes to the correct service based on the provider.

### Backend Changes

**1. New: `OpenAiProperties.java`** (`config/`)
- `@ConfigurationProperties(prefix = "openai")`
- Fields: `timeout` (Duration, default 120s)
- No API key here — it comes from user settings in MongoDB

**2. New: `OpenAiSummarizerService.java`** (`service/`)
- Uses Spring `RestClient` to call `https://api.openai.com/v1/chat/completions`
- Provider-optimized prompt: system message with guidelines, user message with article text
- Maps length hints to `max_tokens` (shorter=400, default=1200, longer=2500)
- `temperature: 0.3` (same as Ollama)
- Accepts API key as parameter (from settings, not from config file)
- Throws `SummarizationException` on errors (reuses existing exception hierarchy)

**3. New: `OpenAiRestClientConfig.java`** (`config/`)
- Separate `RestClient` bean (named `openAiRestClient`) for OpenAI
- Base URL: `https://api.openai.com`
- Timeout from `OpenAiProperties`
- No auth header baked in — API key set per-request via `.header("Authorization", "Bearer " + key)`

**4. Update: `UserSettings.java`** (`model/`)
- Add `openaiApiKey` field (String, nullable)
- Add getter/setter

**5. Update: `UserSettingsDto.java`** (`dto/`)
- Add `openaiApiKey` field
- The GET endpoint returns the key (masked in a future iteration if needed — local app for now)

**6. Update: `SettingsController.java`**
- Handle `openaiApiKey` in PUT (persist to MongoDB)

**7. Update: `ModelsController.java`**
- Restructure response to include provider info
- Group models by provider: `ollama` (always available) and `openai` (only if API key is set)
- Read API key from `UserSettings` to determine if OpenAI section should appear
- Each model entry gets: `{ id, name, description, provider }`
- OpenAI curated list: `gpt-4o-mini`, `gpt-4o`, `gpt-4.1-nano`, `gpt-4.1-mini`

**8. Update: `SummaryService.java`**
- Route to correct summarizer based on model provider
- Logic: if model starts with `gpt-` or settings have an OpenAI key + model is in OpenAI list → use OpenAiSummarizerService
- Otherwise → use OllamaSummarizerService
- Pass API key from settings to OpenAI service

**9. Update: `SummarizeRequest.java`**
- No changes needed — already has `model` field

### Frontend Changes

**10. Update: `strings.js`**
- Add strings for API key input section, OpenAI provider label, model descriptions
- Add validation messages (invalid key, etc.)

**11. Update: `Settings.jsx`**
- New section: "API Keys" (between model selection and notifications)
- OpenAI API key input: password-type field with show/hide toggle
- "Save" button that PUTs to `/api/settings`
- Visual feedback: saved/error state
- Model list section: groups models by provider with headers ("Local (Ollama)" / "OpenAI")
- Models from providers without a configured key are shown grayed out with "Add API key to enable"

**12. Update: `Settings.module.css`**
- Styles for API key input, provider group headers, disabled model cards

**13. Update: `useSettings.js`**
- Add `openaiApiKey: null` to defaults

## File creation/modification summary

| Action | File |
|--------|------|
| Create | `config/OpenAiProperties.java` |
| Create | `config/OpenAiRestClientConfig.java` |
| Create | `service/OpenAiSummarizerService.java` |
| Modify | `model/UserSettings.java` — add `openaiApiKey` |
| Modify | `dto/UserSettingsDto.java` — add `openaiApiKey` |
| Modify | `controller/SettingsController.java` — persist key |
| Modify | `controller/ModelsController.java` — provider-grouped response |
| Modify | `service/SummaryService.java` — route by provider |
| Modify | `resources/application.yml` — add `openai:` section |
| Modify | `frontend/src/constants/strings.js` — new strings |
| Modify | `frontend/src/hooks/useSettings.js` — add key default |
| Modify | `frontend/src/components/Settings.jsx` — API key input + provider groups |
| Modify | `frontend/src/components/Settings.module.css` — new styles |

## Implementation order
1. Backend: OpenAI config + service + RestClient (can be tested with curl independently)
2. Backend: Settings model/DTO/controller updates for API key
3. Backend: ModelsController provider-grouped response
4. Backend: SummaryService routing logic
5. Frontend: Settings UI for API key + provider-grouped model list
6. Integration test: end-to-end with real OpenAI key via curl
