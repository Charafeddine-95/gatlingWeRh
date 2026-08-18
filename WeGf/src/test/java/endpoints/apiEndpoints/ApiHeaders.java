package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.exec;

import io.gatling.javaapi.core.ChainBuilder;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds the headers shared by the authenticated API calls: a common bearer base
 * (authorization resolved from the "accessToken" session attribute, origin, priority,
 * app id) extended with the name/value pairs specific to each endpoint.
 *
 * <p>A tenant is identified by a string such as "548040--BL00105678": the part before the
 * "--" is the tenant id, the part after is the tenant context, and the whole string is the
 * tenant id with context. {@link #initTenants} seeds both known tenants into every virtual
 * user's session; the single-tenant calls read the active one through the
 * "#{tenantIdWithContext}" (whole string) and "#{tenantId}" (part before the "--") session
 * attributes. The active tenant defaults to tenant 1; pass {@code -Dtenant_nb=2} (or the
 * TENANT_NB environment variable) at launch to switch to tenant 2.
 */
public final class ApiHeaders {

    private ApiHeaders() {
    }

    static final String ORIGIN = "https://wegf.uat.wemagnus.com";
    static final String APP_ID = "WEGF_UAT";

    private static final String TENANT_1_ID_WITH_CONTEXT = "548040--BL00105678";
    private static final String TENANT_2_ID_WITH_CONTEXT = "548041--BL00105683";
    private static final String ACTIVE_TENANT_ID_WITH_CONTEXT =
            selectTenant(config("tenant_nb", "TENANT_NB", "1"));
    private static final String ACTIVE_TENANT_ID = tenantIdOf(ACTIVE_TENANT_ID_WITH_CONTEXT);

    private static final Map<String, String> BASE = Map.of(
            "authorization", "Bearer #{accessToken}",
            "origin", ORIGIN,
            "priority", "u=1, i",
            "x-origin-app-id", APP_ID);

    /**
     * Seeds both tenants into the user session. "tenantIdWithContext" / "tenantId" hold the
     * active tenant (whole string and the part before the "--") used by the single-tenant
     * calls. Prepend this to a scenario before any authenticated call.
     */
    public static final ChainBuilder initTenants =
            exec(session -> session
                    .set("tenant1IdWithContext", TENANT_1_ID_WITH_CONTEXT)
                    .set("tenant2IdWithContext", TENANT_2_ID_WITH_CONTEXT)
                    .set("tenantIdWithContext", ACTIVE_TENANT_ID_WITH_CONTEXT)
                    .set("tenantId", ACTIVE_TENANT_ID));

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

    /** Bearer headers scoped to the active tenant, resolved from the session at runtime. */
    static Map<String, String> bearerWithTenant(String... extraPairs) {
        Map<String, String> headers = new HashMap<>(bearer(extraPairs));
        headers.put("x-tenant-id", "#{tenantId}");
        return Map.copyOf(headers);
    }

    /**
     * Bearer headers across all collectivites/etablissements, extended with extra
     * "name", "value" pairs (e.g. accept, content-type).
     */
    static Map<String, String> bearerForAllTenants(String... extraPairs) {
        if (extraPairs.length % 2 != 0) {
            throw new IllegalArgumentException("expected \"name\", \"value\" pairs");
        }
        Map<String, String> headers =
                new HashMap<>(bearerWithTenant("collectiviteid", "all", "etablissementid", "all"));
        for (int i = 0; i < extraPairs.length; i += 2) {
            headers.put(extraPairs[i], extraPairs[i + 1]);
        }
        return Map.copyOf(headers);
    }

    private static String selectTenant(String tenantNb) {
        switch (tenantNb) {
            case "1":
                return TENANT_1_ID_WITH_CONTEXT;
            case "2":
                return TENANT_2_ID_WITH_CONTEXT;
            default:
                throw new IllegalArgumentException("tenant_nb must be 1 or 2, was: " + tenantNb);
        }
    }

    /** The tenant id, i.e. the part before the "--" (e.g. "548040" in "548040--BL00105678"). */
    private static String tenantIdOf(String tenantIdWithContext) {
        return tenantIdWithContext.substring(0, tenantIdWithContext.indexOf("--"));
    }

    private static String config(String property, String envVariable, String defaultValue) {
        String value = System.getProperty(property, System.getenv(envVariable));
        return value != null ? value : defaultValue;
    }
}
