package ma.translator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Calls the Gemini REST API (generateContent) to translate text into Moroccan Darija. */
public class GeminiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "You are a translator into Moroccan Arabic (Darija). "
          + "Translate the user's text into natural Moroccan Darija written in Arabic script. "
          + "Output ONLY the translation, with no explanations, quotes or transliteration.";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final String apiKey = System.getenv("GEMINI_API_KEY");
    private final String model = System.getenv().getOrDefault("GEMINI_MODEL", "gemini-3.1-flash-lite");

    public String translate(String text, String source) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY environment variable is not set");
        }

        String userText = (source == null || source.isBlank())
                ? text
                : "Source language: " + source + "\nText: " + text;

        ObjectNode body = MAPPER.createObjectNode();
        body.putObject("systemInstruction")
            .putArray("parts").addObject().put("text", SYSTEM_PROMPT);
        body.putArray("contents")
            .addObject().putArray("parts").addObject().put("text", userText);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://generativelanguage.googleapis.com/v1beta/models/"
                        + model + ":generateContent"))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = http.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new IOException("Gemini returned HTTP " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = MAPPER.readTree(response.body());
        String result = root.path("candidates").path(0).path("content")
                .path("parts").path(0).path("text").asText("").trim();
        if (result.isEmpty()) {
            throw new IOException("Gemini returned an empty translation: " + response.body());
        }
        return result;
    }
}
