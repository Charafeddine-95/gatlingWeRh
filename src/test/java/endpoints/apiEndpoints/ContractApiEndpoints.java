package endpoints.apiEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

/**
 * Contract API calls.
 *
 * The collectivity id and application name come from the JVM system properties
 * werh.collectiviteId/werh.applicationName (or the WERH_COLLECTIVITE_ID/
 * WERH_APPLICATION_NAME environment variables), defaulting to the recorded
 * values 39443/DELIB.
 */
public final class ContractApiEndpoints {

    private ContractApiEndpoints() {
    }

    private static final String COLLECTIVITE_ID = config("werh.collectiviteId", "WERH_COLLECTIVITE_ID", "39443");
    private static final String APPLICATION_NAME = config("werh.applicationName", "WERH_APPLICATION_NAME", "DELIB");

    private static final String CONTRACT_EXISTS_PATH =
            "/users/api/v1/contracts/exists/" + COLLECTIVITE_ID + "?applicationName=" + APPLICATION_NAME;

    private static String config(String property, String envVariable, String defaultValue) {
        String value = System.getProperty(property, System.getenv(envVariable));
        return value != null ? value : defaultValue;
    }

    /** Checks whether the collectivity holds a contract for the application. */
    public static final HttpRequestActionBuilder contractExists =
            http("Contract exists")
                    .get(CONTRACT_EXISTS_PATH)
                    .headers(ApiHeaders.bearerWithTenant());

    /** Same contract check, but across all the user's collectivites/etablissements. */
    public static final HttpRequestActionBuilder contractExistsAllTenants =
            http("Contract exists")
                    .get(CONTRACT_EXISTS_PATH)
                    .headers(ApiHeaders.bearerForAllTenants());
}
