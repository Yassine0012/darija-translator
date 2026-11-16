package ma.translator.auth;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.callback.CallerPrincipalCallback;
import jakarta.security.auth.message.callback.GroupPrincipalCallback;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Map;

/**
 * Jakarta Authentication (JASPIC) Server Authentication Module implementing HTTP Basic.
 * The container calls validateRequest() for every request to a protected resource.
 */
public class BasicAuthModule implements ServerAuthModule {

    public static final String ROLE = "translator";

    // Credentials come from environment variables (defaults are for local testing only).
    private static final String USER = System.getenv().getOrDefault("API_USER", "admin");
    private static final String PASSWORD = System.getenv().getOrDefault("API_PASSWORD", "changeme");

    private CallbackHandler handler;

    @Override
    @SuppressWarnings("rawtypes")
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
                           CallbackHandler handler, Map options) throws AuthException {
        this.handler = handler;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Class[] getSupportedMessageTypes() {
        return new Class[] { HttpServletRequest.class, HttpServletResponse.class };
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject,
                                      Subject serviceSubject) throws AuthException {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();

        String[] credentials = parseBasicHeader(request.getHeader("Authorization"));

        if (credentials != null && same(credentials[0], USER) & same(credentials[1], PASSWORD)) {
            try {
                // Tell the container who the caller is and which role (group) they have.
                handler.handle(new Callback[] {
                        new CallerPrincipalCallback(clientSubject, credentials[0]),
                        new GroupPrincipalCallback(clientSubject, new String[] { ROLE })
                });
            } catch (IOException | javax.security.auth.callback.UnsupportedCallbackException e) {
                AuthException ex = new AuthException("Callback failure");
                ex.initCause(e);
                throw ex;
            }
            return AuthStatus.SUCCESS;
        }

        // Missing or wrong credentials: send the Basic challenge.
        response.setHeader("WWW-Authenticate", "Basic realm=\"Translator\", charset=\"UTF-8\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return AuthStatus.SEND_CONTINUE;
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) throws AuthException {
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) throws AuthException {
        if (subject != null) {
            subject.getPrincipals().clear();
        }
    }

    /** Returns {user, password} or null if the header is missing or malformed. */
    private static String[] parseBasicHeader(String header) {
        if (header == null || !header.regionMatches(true, 0, "Basic ", 0, 6)) {
            return null;
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(header.substring(6).trim()),
                    StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon <= 0) {
                return null;
            }
            return new String[] { decoded.substring(0, colon), decoded.substring(colon + 1) };
        } catch (IllegalArgumentException e) {
            return null; // malformed Base64
        }
    }

    /** Constant-time comparison, to avoid leaking information through timing. */
    private static boolean same(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
