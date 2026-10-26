package com.capysoft.tuevento.shared.infrastructure.config;

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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * APP_OAUTH_GOOGLE_REDIRECT_URI is the full callback URL. The authorization dialog and the
 * code exchange must send exactly that value (Google rejects the exchange when redirect_uri
 * differs), and nothing may append the callback path to it.
 */
@DisplayName("OauthConfig redirect_uri")
class OauthConfigRedirectUriTest {

    private static final String GOOGLE_CALLBACK = "https://app.example.com/api/v1/auth/oauth/google/callback";

    @Nested
    @DisplayName("Dialog and code exchange")
    class DialogAndExchange {

        private OauthConfig config;

        @BeforeEach
        void setUp() {
            config = new OauthConfig();
            ReflectionTestUtils.setField(config, "googleClientId", "google-app");
            ReflectionTestUtils.setField(config, "googleClientSecret", "google-secret");
            ReflectionTestUtils.setField(config, "googleRedirectUri", GOOGLE_CALLBACK);
            ReflectionTestUtils.setField(config, "googleTokenUri", "https://oauth2.googleapis.com/token");
            ReflectionTestUtils.setField(config, "googleUserinfoUri", "https://www.googleapis.com/oauth2/v3/userinfo");
            ReflectionTestUtils.setField(config, "googleAuthorizationUri", "https://accounts.google.com/o/oauth2/v2/auth");
            ReflectionTestUtils.setField(config, "allowedFrontendRedirectUris",
                    java.util.List.of("https://app.example.com/login"));
        }

        private String redirectUriOf(String url) {
            String raw = UriComponentsBuilder.fromUriString(url).build().getQueryParams().getFirst("redirect_uri");
            return URLDecoder.decode(raw, StandardCharsets.UTF_8);
        }

        @Test
        @DisplayName("Google: the authorization request sends exactly the configured URL")
        void google_dialog_usesTheFullUrl() {
            assertThat(redirectUriOf(config.oauthAuthorizationUrls().get("google"))).isEqualTo(GOOGLE_CALLBACK);
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
        @DisplayName("prod: Google redirect URI variable is used as-is, without appending the callback path")
        void prod_googleVariableIsTheFullUrl() throws IOException {
            StandardEnvironment env = environment("application-prod.yml",
                    Map.of("APP_OAUTH_GOOGLE_REDIRECT_URI", GOOGLE_CALLBACK));

            assertThat(env.getProperty("app.oauth.google.redirect-uri")).isEqualTo(GOOGLE_CALLBACK);
        }

        @Test
        @DisplayName("dev: without the variable, the Google default is the full local callback URL")
        void dev_googleDefaultIsTheFullLocalUrl() throws IOException {
            StandardEnvironment env = environment("application-dev.yml", Map.of());

            assertThat(env.getProperty("app.oauth.google.redirect-uri"))
                    .isEqualTo("http://localhost:8080/api/v1/auth/oauth/google/callback");
        }

        @Test
        @DisplayName("dev: the Google variable overrides the default with the same full-URL meaning")
        void dev_googleVariableIsTheFullUrl() throws IOException {
            StandardEnvironment env = environment("application-dev.yml",
                    Map.of("APP_OAUTH_GOOGLE_REDIRECT_URI", GOOGLE_CALLBACK));

            assertThat(env.getProperty("app.oauth.google.redirect-uri")).isEqualTo(GOOGLE_CALLBACK);
        }

        @Test
        @DisplayName("variable from a non-environment source (e.g. .env via spring-dotenv) is not concatenated")
        void variableResolvedOnlyThroughThePlaceholder_isNotConcatenated() throws IOException {
            for (String profile : new String[]{"application-dev.yml", "application-prod.yml"}) {
                StandardEnvironment env = environment(profile, Map.of());
                env.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("dotenv",
                        Map.of("APP_OAUTH_GOOGLE_REDIRECT_URI", GOOGLE_CALLBACK)));

                assertThat(env.getProperty("app.oauth.google.redirect-uri")).as(profile).isEqualTo(GOOGLE_CALLBACK);
            }
        }
    }
}
