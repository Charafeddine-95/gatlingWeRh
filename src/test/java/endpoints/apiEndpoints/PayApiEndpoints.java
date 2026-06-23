package endpoints.apiEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

/** Pay API calls used by WeRH pages. */
public final class PayApiEndpoints {

    private PayApiEndpoints() {
    }

    private static final String WERH_API = "https://werh-api.uat.wemagnus.com";

    /** Current payroll-cycle summary used to resolve the agent-list period. */
    public static final HttpRequestActionBuilder cycleResume =
            http("Payroll cycle resume")
                    .get(WERH_API + "/pay/cycle-paie/resume")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));
}
