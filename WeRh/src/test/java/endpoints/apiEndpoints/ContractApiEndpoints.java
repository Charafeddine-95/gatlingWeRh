package endpoints.apiEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

/**
 * Contract API calls.
 *
 * The contract is looked up by tenant id (the part before the "--"), read from the
 * "tenantId" session attribute seeded by {@link ApiHeaders#initTenants}. The application
 * name comes from the JVM system property werh.applicationName (or the WERH_APPLICATION_NAME
 * environment variable), defaulting to DELIB.
 */
public final class ContractApiEndpoints {

    private ContractApiEndpoints() {
    }

    private static final String APPLICATION_NAME = config("werh.applicationName", "WERH_APPLICATION_NAME", "DELIB");

    private static final String CONTRACT_EXISTS_PATH =
            "/users/api/v1/contracts/exists/#{tenantId}?applicationName=" + APPLICATION_NAME;

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
