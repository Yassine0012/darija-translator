package ma.translator;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;

@Path("/translate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public class TranslatorResource {

    // One shared client (thread-safe) is enough for the whole service.
    private static final GeminiClient GEMINI = new GeminiClient();

    @POST
    public TranslationResponse translate(TranslationRequest request) {
        if (request == null || request.text() == null || request.text().isBlank()) {
            throw new BadRequestException("Field 'text' is required");
        }
        try {
            return new TranslationResponse(GEMINI.translate(request.text(), request.source()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WebApplicationException("Translation interrupted", 500);
        } catch (Exception e) {
            e.printStackTrace(); // visible in the server console while debugging
            throw new WebApplicationException("Translation failed: " + e.getMessage(), 502);
        }
    }
}
