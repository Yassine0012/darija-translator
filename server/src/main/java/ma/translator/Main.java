package ma.translator;

import jakarta.security.auth.message.config.AuthConfigFactory;
import ma.translator.auth.BasicAuthConfigProvider;
import ma.translator.auth.BasicAuthModule;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.SecurityCollection;
import org.apache.tomcat.util.descriptor.web.SecurityConstraint;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import java.nio.file.Files;

public class Main {

    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(Files.createTempDirectory("translator-tomcat").toString());
        tomcat.setPort(8080);
        tomcat.getConnector(); // creates the default HTTP connector

        // Root context with no web.xml: everything is configured in code.
        Context context = tomcat.addContext("", System.getProperty("java.io.tmpdir"));

        // --- Jakarta Authentication: register our provider for every HttpServlet application ---
        AuthConfigFactory.getFactory().registerConfigProvider(
                new BasicAuthConfigProvider(), "HttpServlet", null, "Translator Basic authentication");

        // --- Mark every URL as protected: callers need the "translator" role ---
        context.addSecurityRole(BasicAuthModule.ROLE);
        SecurityConstraint constraint = new SecurityConstraint();
        constraint.addAuthRole(BasicAuthModule.ROLE);
        SecurityCollection collection = new SecurityCollection();
        collection.addPattern("/*");
        constraint.addCollection(collection);
        context.addConstraint(constraint);

        // --- Jersey (Jakarta REST) as a servlet under /api/* ---
        ResourceConfig config = new ResourceConfig()
                .packages("ma.translator")        // scans for @Path / @Provider classes
                .register(JacksonFeature.class);  // JSON (de)serialization

        Wrapper jersey = Tomcat.addServlet(context, "jersey", new ServletContainer(config));
        jersey.setLoadOnStartup(1);
        context.addServletMappingDecoded("/api/*", "jersey");

        tomcat.start();
        System.out.println("Translator service running at http://localhost:8080/api/");
        System.out.println("Press Ctrl+C to stop.");
        tomcat.getServer().await();
    }
}
