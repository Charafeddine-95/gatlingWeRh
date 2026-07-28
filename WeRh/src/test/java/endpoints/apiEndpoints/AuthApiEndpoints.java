package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.util.Map;

/** Authentication and IAM API calls. */
public final class AuthApiEndpoints {

    private AuthApiEndpoints() {
    }

    private static final String AUTH = "https://blauth.berger-levrault.com";

    /**
     * Exchanges the authorization code saved by the login pages ("authorizationCode")
     * and saves the issued access token as "accessToken"; the authenticated endpoints
     * send it back through the "Bearer #{accessToken}" header.
     */
    public static final HttpRequestActionBuilder exchangeToken =
            http("Exchange token")
                    .post(AUTH + "/auth/realms/saas/protocol/openid-connect/token")
                    .headers(Map.of("accept", "*/*", "origin", ApiHeaders.ORIGIN))
                    .formParam("code", "#{authorizationCode}")
                    .formParam("grant_type", "authorization_code")
                    .formParam("client_id", ApiHeaders.APP_ID)
                    .formParam("redirect_uri", "https://werh.uat.wemagnus.com/")
                    .check(jsonPath("$.access_token").saveAs("accessToken"));

    /** Loads the connected user's IAM profile, right after authentication (no tenant selected yet). */
    public static final HttpRequestActionBuilder userLoginNoTenant =
            http("User login")
                    .get("/iam/users/login")
                    .headers(ApiHeaders.bearer());

    /** Loads the connected user's IAM profile for the selected tenant. */
    public static final HttpRequestActionBuilder userLogin =
            http("User login")
                    .get("/iam/users/login")
                    .headers(ApiHeaders.bearerWithTenant());

    /** Accepts the general conditions of use — shown once, on a user's first connection. */
    public static final HttpRequestActionBuilder approveGcu =
            http("Approve GCU")
                    .post("/iam/gcu")
                    .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                    .body(StringBody("{\"gcuVersion\":\"08-2025.02\",\"isApproved\":true}"));
}
