package ma.translator.auth;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.config.AuthConfigProvider;
import jakarta.security.auth.message.config.ClientAuthConfig;
import jakarta.security.auth.message.config.ServerAuthConfig;
import jakarta.security.auth.message.config.ServerAuthContext;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import java.util.Map;

/** Plugs BasicAuthModule into the container's Jakarta Authentication machinery. */
public class BasicAuthConfigProvider implements AuthConfigProvider {

    @Override
    public ClientAuthConfig getClientAuthConfig(String layer, String appContext, CallbackHandler handler)
            throws AuthException {
        return null; // server side only
    }

    @Override
    public ServerAuthConfig getServerAuthConfig(String layer, String appContext, CallbackHandler handler)
            throws AuthException {
        return new Config(layer, appContext, handler);
    }

    @Override
    public void refresh() {
        // nothing to reload
    }

    private static class Config implements ServerAuthConfig {

        private final String layer;
        private final String appContext;
        private final CallbackHandler handler;

        Config(String layer, String appContext, CallbackHandler handler) {
            this.layer = layer;
            this.appContext = appContext;
            this.handler = handler;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public ServerAuthContext getAuthContext(String authContextID, Subject serviceSubject, Map properties)
                throws AuthException {
            BasicAuthModule module = new BasicAuthModule();
            module.initialize(null, null, handler, properties);

            // A ServerAuthContext that simply delegates to our single module.
            return new ServerAuthContext() {
                @Override
                public jakarta.security.auth.message.AuthStatus validateRequest(
                        MessageInfo info, Subject client, Subject service) throws AuthException {
                    return module.validateRequest(info, client, service);
                }

                @Override
                public jakarta.security.auth.message.AuthStatus secureResponse(
                        MessageInfo info, Subject service) throws AuthException {
                    return module.secureResponse(info, service);
                }

                @Override
                public void cleanSubject(MessageInfo info, Subject subject) throws AuthException {
                    module.cleanSubject(info, subject);
                }
            };
        }

        @Override public String getMessageLayer() { return layer; }
        @Override public String getAppContext() { return appContext; }
        @Override public String getAuthContextID(MessageInfo messageInfo) { return "translator"; }
        @Override public void refresh() { }
        @Override public boolean isProtected() { return false; }
    }
}
