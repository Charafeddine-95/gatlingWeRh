package endpoints.apiEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Agent page API calls. */
public final class AgentApiEndpoints {

    private AgentApiEndpoints() {
    }

    private static final String WERH_API = "https://werh-api.uat.wemagnus.com";
    private static final String AGENT_LIST_DATE = config("werh.agentListDate", "WERH_AGENT_LIST_DATE", "2026-06-01");
    private static final String AGENT_LIST_FILTERS =
            encode(config("werh.agentListFilters", "WERH_AGENT_LIST_FILTERS", "{\"activite\":\"1\"}"));

    private static final String AGENT_LIST_PATH =
            WERH_API + "/career/bff/dossier-agent/" + AGENT_LIST_DATE + "/contract?filters=" + AGENT_LIST_FILTERS;

    private static String config(String property, String envVariable, String defaultValue) {
        String value = System.getProperty(property, System.getenv(envVariable));
        return value != null ? value : defaultValue;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Address types used by the agent search/list filters. */
    public static final HttpRequestActionBuilder addressTypes =
            http("Agent address types")
                    .get(WERH_API + "/agent/type/adresse")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

    /** Civilities used by the agent search/list filters. */
    public static final HttpRequestActionBuilder civilities =
            http("Agent civilities")
                    .get(WERH_API + "/agent/civilite")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

    /** Loads the list of active agent contracts for the configured payroll month. */
    public static final HttpRequestActionBuilder contracts =
            http("Agent contracts")
                    .get(AGENT_LIST_PATH)
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));
}
