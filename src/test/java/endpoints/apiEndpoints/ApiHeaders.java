package endpoints.apiEndpoints;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds the headers shared by the authenticated API calls: a common bearer base
 * (authorization resolved from the "accessToken" session attribute, origin, priority,
 * app id) extended with the name/value pairs specific to each endpoint.
 */
final class ApiHeaders {

    private ApiHeaders() {
    }

    static final String ORIGIN = "https://werh.uat.wemagnus.com";
    static final String APP_ID = "WERH_UAT";
    static final String TENANT_ID = "539596--BL00102516";

    private static final Map<String, String> BASE = Map.of(
            "authorization", "Bearer #{accessToken}",
            "origin", ORIGIN,
            "priority", "u=1, i",
            "x-origin-app-id", APP_ID);

    /** Base bearer headers, extended with extra "name", "value" pairs. */
    static Map<String, String> bearer(String... extraPairs) {
        if (extraPairs.length % 2 != 0) {
            throw new IllegalArgumentException("expected \"name\", \"value\" pairs");
        }
        Map<String, String> headers = new HashMap<>(BASE);
        for (int i = 0; i < extraPairs.length; i += 2) {
            headers.put(extraPairs[i], extraPairs[i + 1]);
        }
        return Map.copyOf(headers);
    }

    /** Bearer headers scoped to the recorded tenant. */
    static Map<String, String> bearerWithTenant(String... extraPairs) {
        Map<String, String> headers = new HashMap<>(bearer(extraPairs));
        headers.put("x-tenant-id", TENANT_ID);
        return Map.copyOf(headers);
    }

    /** Bearer headers across all collectivites/etablissements. */
    static Map<String, String> bearerForAllTenants() {
        return bearerWithTenant("collectiviteid", "all", "etablissementid", "all");
    }
}
