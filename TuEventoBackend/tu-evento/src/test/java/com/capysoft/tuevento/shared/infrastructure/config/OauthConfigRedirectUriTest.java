package com.capysoft.tuevento.shared.infrastructure.config;

import com.capysoft.tuevento.modules.security.application.dto.OauthProfile;
import com.capysoft.tuevento.shared.domain.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * APP_OAUTH_{GOOGLE,FACEBOOK}_REDIRECT_URI is the full callback URL. The authorization dialog
 * and the code exchange must send exactly that value (OAuth providers reject the exchange when
 * redirect_uri differs), and nothing may append the callback path to it.
 */
@DisplayName("OauthConfig redirect_uri")
class OauthConfigRedirectUriTest {

    private static final String FACEBOOK_CALLBACK = "https://app.example.com/api/v1/auth/oauth/facebook/callback";
    private static final String GOOGLE_CALLBACK   = "https://app.example.com/api/v1/auth/oauth/google/callback";

    @Nested
    @DisplayName("Dialog and code exchange")
    class DialogAndExchange {

        private HttpServer server;
        private final AtomicReference<String> tokenRequestBody = new AtomicReference<>();
        private volatile int tokenStatus = 200;
        private OauthConfig config;

        @BeforeEach
        void startFakeGraphApi() throws IOException {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/token", exchange -> {
                tokenRequestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] body = (tokenStatus == 200
                        ? "{\"access_token\":\"graph-token\"}"
                        : "{\"error\":{\"message\":\"Error validating verification code. redirect_uri mismatch\"}}")
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(tokenStatus, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.createContext("/me", exchange -> {
                byte[] body = "{\"id\":\"fb-1\",\"name\":\"Ana Perez\",\"email\":\"ana@gmail.com\"}"
                        .getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();

            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            config = new OauthConfig();
            ReflectionTestUtils.setField(config, "facebookClientId", "fb-app");
            ReflectionTestUtils.setField(config, "facebookClientSecret", "fb-secret");
            ReflectionTestUtils.setField(config, "facebookRedirectUri", FACEBOOK_CALLBACK);
            ReflectionTestUtils.setField(config, "facebookAuthUri", "https://www.facebook.com/v18.0/dialog/oauth");
            ReflectionTestUtils.setField(config, "facebookTokenUri", base + "/token");
            ReflectionTestUtils.setField(config, "facebookProfileUri", base + "/me");
            ReflectionTestUtils.setField(config, "googleClientId", "google-app");
            ReflectionTestUtils.setField(config, "googleRedirectUri", GOOGLE_CALLBACK);
            ReflectionTestUtils.setField(config, "googleAuthorizationUri", "https://accounts.google.com/o/oauth2/v2/auth");
        }

        @AfterEach
        void stopFakeGraphApi() {
            server.stop(0);
        }

        private String redirectUriOf(String url) {
            String raw = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("redirect_uri");
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        }

        private Map<String, String> formParams(String body) {
            return Arrays.stream(body.split("&"))
                    .map(pair -> pair.split("=", 2))
                    .collect(Collectors.toMap(
                            kv -> URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                            kv -> URLDecoder.decode(kv[1], StandardCharsets.UTF_8)));
        }

        @Test
        @DisplayName("Facebook: the dialog and the code exchange send exactly the configured URL")
        void facebook_dialogAndExchange_useTheSameFullUrl() {
            String dialogUrl = config.oauthAuthorizationUrls().get("facebook");

            OauthProfile profile = config.oauthProviderResolvers().get("facebook").apply("auth-code");

            assertThat(redirectUriOf(dialogUrl)).isEqualTo(FACEBOOK_CALLBACK);
            Map<String, String> exchange = formParams(tokenRequestBody.get());
            assertThat(exchange.get("redirect_uri")).isEqualTo(FACEBOOK_CALLBACK);
            assertThat(exchange.get("code")).isEqualTo("auth-code");
            assertThat(profile.getProviderUserId()).isEqualTo("fb-1");
            assertThat(profile.getEmail()).isEqualTo("ana@gmail.com");
        }

        @Test
        @DisplayName("Google: the authorization request sends exactly the configured URL")
        void google_dialog_usesTheFullUrl() {
            assertThat(redirectUriOf(config.oauthAuthorizationUrls().get("google"))).isEqualTo(GOOGLE_CALLBACK);
        }

        @Test
        @DisplayName("Facebook exchange failure → FACEBOOK_OAUTH_FAILED with a fixed message, no Graph API detail")
        void facebook_exchangeFailure_hidesDetails() {
            tokenStatus = 400;

            assertThatThrownBy(() -> config.oauthProviderResolvers().get("facebook").apply("bad-code"))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("code", "FACEBOOK_OAUTH_FAILED")
                    .hasMessage("Facebook sign-in failed");
        }
    }

    @Nested
    @DisplayName("Property resolution (application-*.yml + environment)")
    class PropertyResolution {

        /** Mirrors how Spring Boot resolves @Value: environment variables first, then the profile YAML. */
        private StandardEnvironment environment(String profileYaml, Map<String, Object> envVars) throws IOException {
            StandardEnvironment env = new StandardEnvironment();
            MutablePropertySources sources = env.getPropertySources();
            sources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            sources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            sources.addFirst(new SystemEnvironmentPropertySource(
                    StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, envVars));
            new YamlPropertySourceLoader()
                    .load(profileYaml, new ClassPathResource(profileYaml))
                    .forEach(sources::addLast);
            ConfigurationPropertySources.attach(env);
            return env;
        }

        @Test
        @DisplayName("prod: the variable is used as-is, without appending the callback path")
        void prod_variableIsTheFullUrl() throws IOException {
            StandardEnvironment env = environment("application-prod.yml", Map.of(
                    "APP_OAUTH_FACEBOOK_REDIRECT_URI", FACEBOOK_CALLBACK,
                    "APP_OAUTH_GOOGLE_REDIRECT_URI", GOOGLE_CALLBACK));

            assertThat(env.getProperty("app.oauth.facebook.redirect-uri")).isEqualTo(FACEBOOK_CALLBACK);
            assertThat(env.getProperty("app.oauth.google.redirect-uri")).isEqualTo(GOOGLE_CALLBACK);
        }

        @Test
        @DisplayName("dev: without the variable, the default is the full local callback URL")
        void dev_defaultIsTheFullLocalUrl() throws IOException {
            StandardEnvironment env = environment("application-dev.yml", Map.of());

            assertThat(env.getProperty("app.oauth.facebook.redirect-uri"))
                    .isEqualTo("http://localhost:8080/api/v1/auth/oauth/facebook/callback");
            assertThat(env.getProperty("app.oauth.google.redirect-uri"))
                    .isEqualTo("http://localhost:8080/api/v1/auth/oauth/google/callback");
        }

        @Test
        @DisplayName("dev: the variable overrides the default with the same full-URL meaning")
        void dev_variableIsTheFullUrl() throws IOException {
            StandardEnvironment env = environment("application-dev.yml",
                    Map.of("APP_OAUTH_FACEBOOK_REDIRECT_URI", FACEBOOK_CALLBACK));

            assertThat(env.getProperty("app.oauth.facebook.redirect-uri")).isEqualTo(FACEBOOK_CALLBACK);
        }

        @Test
        @DisplayName("variable from a non-environment source (e.g. .env via spring-dotenv) is not concatenated either")
        void variableResolvedOnlyThroughThePlaceholder_isNotConcatenated() throws IOException {
            // Only the ${...} placeholder applies here (no relaxed env-var binding). The old YAML
            // appended /api/v1/auth/oauth/.../callback in this case, doubling the path.
            for (String profile : new String[]{"application-dev.yml", "application-prod.yml"}) {
                StandardEnvironment env = environment(profile, Map.of());
                env.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("dotenv",
                        Map.of("APP_OAUTH_FACEBOOK_REDIRECT_URI", FACEBOOK_CALLBACK,
                               "APP_OAUTH_GOOGLE_REDIRECT_URI", GOOGLE_CALLBACK)));

                assertThat(env.getProperty("app.oauth.facebook.redirect-uri")).as(profile).isEqualTo(FACEBOOK_CALLBACK);
                assertThat(env.getProperty("app.oauth.google.redirect-uri")).as(profile).isEqualTo(GOOGLE_CALLBACK);
            }
        }
    }
}
