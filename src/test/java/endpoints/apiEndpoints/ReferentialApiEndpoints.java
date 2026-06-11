package endpoints.apiEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

/** Referential data (configuration and context) served by the WeRH API. */
public final class ReferentialApiEndpoints {

    private ReferentialApiEndpoints() {
    }

    private static final String WERH_API = "https://werh-api.uat.wemagnus.com";

    /** WeRH application configuration for the ROLE_AGENT profile. */
    public static final HttpRequestActionBuilder agentConfiguration =
            http("Agent configuration")
                    .get(WERH_API + "/config/configuration/ROLE_AGENT")
                    .headers(ApiHeaders.bearerForAllTenants());

    /** Collectivites (local authorities) the connected user can work on. */
    public static final HttpRequestActionBuilder collectivites =
            http("Collectivites")
                    .get(WERH_API + "/context/v1/collectivite")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

    /** Etablissements (sites) the connected user can work on. */
    public static final HttpRequestActionBuilder etablissements =
            http("Etablissements")
                    .get(WERH_API + "/context/v1/etablissement")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));
}
