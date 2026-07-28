package endpoints.apiEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/** Referential data (configuration and context) served by the WeGf API. */
public final class ReferentialApiEndpoints {

    private ReferentialApiEndpoints() {
    }

    private static final String WEGF_API = "https://wegf-api.uat.wemagnus.com";

    /** WeGf application configuration for the ROLE_AGENT profile. */
    public static final HttpRequestActionBuilder agentConfiguration =
            http("Agent configuration")
                    .get(WEGF_API + "/config/configuration/ROLE_USER")
                    .headers(ApiHeaders.bearerForAllTenants());

    /**
     * Saves the whole response as a Map so the EL can walk into it — for example
     * "#{userContextCBE.exercice.exercice.id}". bodyString() would save the JSON as a
     * plain String, which the EL cannot traverse with a dotted path.
     */
    public static final HttpRequestActionBuilder contextCBE =
            http("ContextCBE")
                    .get(WEGF_API + "/compta/contextCBE/userContextCBE")
                    .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"))
                    .check(jsonPath("$").ofMap().saveAs("userContextCBE"));
}
