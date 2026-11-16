# Darija Translator - LLM-powered RESTful web service

Mini Project 2 - Pr. El Habib Nfaoui

A Java RESTful web service that translates text (English, French, Spanish...) into **Moroccan Arabic
(Darija)** using a Large Language Model (Google Gemini), secured with **HTTP Basic authentication
through the Jakarta Authentication (JASPIC) API**, plus several clients: a Chrome extension, a
Python app, a PHP app and a React Native mobile app.

## Architecture

| Part | Technology |
|---|---|
| REST endpoint `TranslatorResource.translate()` | Jakarta RESTful Web Services (Jersey 3.1) |
| Embedded container | Apache Tomcat 10.1 (embedded) |
| Authentication | Jakarta Authentication: `ServerAuthModule` + `AuthConfigProvider`, HTTP Basic |
| Translation | Google Gemini API (`generateContent`) |
| Clients | Chrome extension (Manifest V3, side panel), Python, PHP, React Native (Expo) |

UML diagrams (PNG + PlantUML sources) are in [`docs/uml`](docs/uml):
class diagram, deployment diagram and sequence diagram.

```
Clients (Chrome ext, Python, PHP, React Native)
        |  HTTP POST /api/translate + Authorization: Basic
        v
Tomcat (embedded) -> BasicAuthModule (Jakarta Authentication)
        -> TranslatorResource (Jakarta REST) -> GeminiClient -> Gemini API (HTTPS)
```

## Repository layout

```
server/            Maven project (the REST service)
chrome-extension/  Manifest V3 extension with side panel
clients/
  python-client/   command-line client
  php-client/      small web client (cURL)
mobile/            React Native (Expo) app
docs/uml/          class, deployment and sequence diagrams
```

## Run the server

Prerequisites: JDK 17+ and Maven.

1. Get a free API key at <https://aistudio.google.com/apikey>.
2. Set the environment variables, then start the server (Windows cmd):

```cmd
set "GEMINI_API_KEY=your_key"
set "API_USER=admin"
set "API_PASSWORD=choose_a_password"
cd server
mvn clean compile exec:java
```

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `GEMINI_API_KEY` | yes | - | Gemini API key |
| `GEMINI_MODEL` | no | `gemini-3.1-flash-lite` | Gemini model name |
| `API_USER` | no | `admin` | Basic auth username |
| `API_PASSWORD` | no | `changeme` | Basic auth password (change it) |

The service listens on `http://localhost:8080/api/`. Never commit your API key or password.

## API

`POST /api/translate` with `Content-Type: application/json` and Basic authentication.

Request:
```json
{ "text": "Good morning, how are you?", "source": "en" }
```
`source` is optional (empty = auto-detect).

Response `200 OK`:
```json
{ "translation": "..." }
```

Errors: `400` empty text, `401` missing or wrong credentials, `502` Gemini unavailable or failed.

Test with cURL (Windows cmd):
```cmd
curl -u admin:choose_a_password -X POST http://localhost:8080/api/translate -H "Content-Type: application/json" -d "{\"text\":\"Good morning\",\"source\":\"en\"}"
```
Without `-u` you get `401 Unauthorized`.

## Clients

### Chrome extension (Manifest V3, `chrome.sidePanel`)
1. Open `chrome://extensions`, enable **Developer mode**, click **Load unpacked**, choose `chrome-extension/`.
2. Open the side panel and enter the endpoint, username and password in **Settings**.
3. Select text on any page, then right-click and choose **Translate to Darija** (or click the toolbar icon).

The selected text is pasted into the side panel and translated automatically. Extras: read aloud
(text-to-speech), copy. Requires Chrome 116+.

### Python
```cmd
cd clients\python-client
set "API_PASSWORD=choose_a_password"
python translate_client.py "Good morning"
```
Without arguments it starts an interactive loop.

### PHP
```cmd
cd clients\php-client
set "API_PASSWORD=choose_a_password"
php -S localhost:8000
```
Open <http://localhost:8000>. Requires the PHP `curl` extension enabled in `php.ini`.

### Mobile (React Native / Expo)
See [`mobile/README.md`](mobile/README.md).

## Security notes
- Authentication is implemented with the Jakarta Authentication SPI (`BasicAuthModule`), registered
  through `AuthConfigFactory`; every URL requires the `translator` role.
- Basic authentication sends credentials in every request: use HTTPS in any real deployment.
- Secrets come from environment variables, not from the source code.
- The Chrome extension and mobile app store the password locally on the device (acceptable for this project).

## Troubleshooting
- `API key not valid`: the key is wrong or still a placeholder.
- `404 ... model no longer available`: set `GEMINI_MODEL` to a current model, listed by
  `curl -H "x-goog-api-key: %GEMINI_API_KEY%" https://generativelanguage.googleapis.com/v1beta/models`.
- `503 high demand` / timeout: retry, or try another model (for example a `-lite` one).
- Phone cannot connect: use the PC's LAN IP, same Wi-Fi, and allow port 8080 in the Windows firewall.

## Possible extensions
Speech-to-speech translation, a local lightweight LLM (for example through Ollama), HTTPS.
