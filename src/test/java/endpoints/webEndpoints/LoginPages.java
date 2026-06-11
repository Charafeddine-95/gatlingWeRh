package endpoints.webEndpoints;

import static io.gatling.javaapi.core.CoreDsl.css;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.http.HttpDsl.headerRegex;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.util.Map;
import java.util.UUID;

/**
 * Keycloak (blauth) HTML login pages.
 *
 * state/nonce: random values that the real client generates before each authorization
 * request — run generateStateAndNonce first to put fresh ones in the session. Keycloak
 * echoes state back in the redirect and embeds nonce in the ID token; the simulation
 * verifies neither.
 *
 * Correlation: the authorization page saves the login form action URL (which carries
 * session_code/execution/tab_id) as "loginActionUrl"; submitting credentials and the
 * silent re-authorization save the authorization code from the redirect Location
 * fragment as "authorizationCode".
 *
 * Credentials are read from the JVM system properties werh.username/werh.password
 * (or the WERH_USERNAME/WERH_PASSWORD environment variables) so they are never
 * committed with the sources.
 */
public final class LoginPages {

    private LoginPages() {
    }

    private static final String AUTH = "https://blauth.berger-levrault.com";
    private static final String HTML_ACCEPT =
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7";

    private static final Map<String, String> AUTH_PAGE_HEADERS = Map.of(
            "accept", HTML_ACCEPT,
            "upgrade-insecure-requests", "1");

    private static final Map<String, String> LOGIN_FORM_HEADERS = Map.of(
            "accept", HTML_ACCEPT,
            "origin", "null",
            "upgrade-insecure-requests", "1");

    private static final String USERNAME = credential("werh.username", "WERH_USERNAME");
    private static final String PASSWORD = credential("werh.password", "WERH_PASSWORD");

    // TODO: untested against the live Keycloak responses — validate on the first real run
    // and fix the selector/regex if the checks fail.
    private static final String LOGIN_FORM_SELECTOR = "form[action*='login-actions']";
    private static final String CODE_IN_LOCATION = "[#&]code=([^&]+)";

    private static final String AUTHORIZATION_URL =
            AUTH + "/auth/realms/saas/protocol/openid-connect/auth"
                    + "?client_id=WERH_UAT"
                    + "&redirect_uri=https%3A%2F%2Fwerh.uat.wemagnus.com%2F"
                    + "&state=#{state}"
                    + "&response_mode=fragment&response_type=code&scope=openid"
                    + "&nonce=#{nonce}";

    private static String credential(String property, String envVariable) {
        String value = System.getProperty(property, System.getenv(envVariable));
        return value != null ? value : "";
    }

    /** Fresh random state/nonce, like the real client generates before each authorization request. */
    public static final ChainBuilder generateStateAndNonce =
            exec(session -> session
                    .set("state", UUID.randomUUID().toString())
                    .set("nonce", UUID.randomUUID().toString()));

    /** First visit, without a Keycloak session: renders the login form. */
    public static final HttpRequestActionBuilder authorizationPage =
            http("Authorization page")
                    .get(AUTHORIZATION_URL)
                    .headers(AUTH_PAGE_HEADERS)
                    .check(css(LOGIN_FORM_SELECTOR, "action").saveAs("loginActionUrl"));

    /** Re-authorization with the Keycloak SSO cookie: redirects straight back with a fresh code. */
    public static final HttpRequestActionBuilder silentAuthorizationPage =
            http("Authorization page")
                    .get(AUTHORIZATION_URL)
                    .headers(AUTH_PAGE_HEADERS)
                    .disableFollowRedirect()
                    .check(status().is(302),
                            headerRegex("Location", CODE_IN_LOCATION).saveAs("authorizationCode"));

    /**
     * Posts username/password to the form action saved by authorizationPage; a successful
     * login answers 302 with the authorization code in the Location fragment.
     */
    public static final HttpRequestActionBuilder submitCredentials =
            http("Submit credentials")
                    .post("#{loginActionUrl}")
                    .headers(LOGIN_FORM_HEADERS)
                    .formParam("username", USERNAME)
                    .formParam("password", PASSWORD)
                    .disableFollowRedirect()
                    .check(status().is(302),
                            headerRegex("Location", CODE_IN_LOCATION).saveAs("authorizationCode"));
}
