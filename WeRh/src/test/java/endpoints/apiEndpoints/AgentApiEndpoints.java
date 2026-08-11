package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Agent page API calls. */
public final class AgentApiEndpoints {

        private AgentApiEndpoints() {
        }

        private static final String WERH_API = "https://werh-api.uat.wemagnus.com";
        private static final String AGENT_LIST_DATE = config("werh.agentListDate", "WERH_AGENT_LIST_DATE",
                        "2026-06-01");
        private static final String AGENT_LIST_FILTERS = encode(
                        config("werh.agentListFilters", "WERH_AGENT_LIST_FILTERS", "{\"activite\":\"1\"}"));

        private static final String AGENT_LIST_PATH = WERH_API + "/career/bff/dossier-agent/" + AGENT_LIST_DATE
                        + "/contract?filters=" + AGENT_LIST_FILTERS;

        private static String config(String property, String envVariable, String defaultValue) {
                String value = System.getProperty(property, System.getenv(envVariable));
                return value != null ? value : defaultValue;
        }

        private static String encode(String value) {
                return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        /** Address types used by the agent search/list filters. */
        public static final HttpRequestActionBuilder addressTypes = http("Agent address types")
                        .get(WERH_API + "/agent/type/adresse")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /** Civilities used by the agent search/list filters. */
        public static final HttpRequestActionBuilder civilities = http("Agent civilities")
                        .get(WERH_API + "/agent/civilite")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /**
         * Loads the list of active agent contracts for the configured payroll month,
         * and captures the
         * first row's agent and contract ids ("agentId"/"contratId") so the
         * agent-detail and payslip
         * calls can target that agent.
         */
        public static final HttpRequestActionBuilder contracts = http("Agent contracts")
                        .get(AGENT_LIST_PATH)
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));
        // .check(
        // jsonPath("$[0].agentId").saveAs("agentId"),
        // jsonPath("$[0].contratId").saveAs("contratId"));

        public static final HttpRequestActionBuilder listeAgents = http("Agent contracts")
                        .get(WERH_API + "/career/bff/dossier-agent/" + AGENT_LIST_DATE + "/contract")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                        .check(jmesPath("[*].{agentId: agentId, contratId: contratId, fonctionId: fonctionId,"
                                        + " droitId: droitId, statutId: statutId, collectiviteId: collectiviteId,"
                                        + " etablissementId: etablissementId}")
                                        .ofList().saveAs("agents"),
                                        jsonPath("$[?(@.droitId)]").ofMap().findRandom().saveAs("active_agent"));

        /** Agent latest situation, the first call fired when an agent is opened. */
        public static final HttpRequestActionBuilder latestSituation = http("Agent latest situation")
                        .post(WERH_API + "/career/bff/agents-with-latest-situation/#{active_agent.agentId}/last")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json", "content-type",
                                        "application/json"));

        /** Agent core record (fired as the detail tabs mount). */
        public static final HttpRequestActionBuilder agentDetail = http("Agent detail")
                        .get(WERH_API + "/agent/agent/#{active_agent.agentId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /** Agent identity section. */
        public static final HttpRequestActionBuilder agentIdentite = http("Agent identite")
                        .get(WERH_API + "/agent/agent/identite/#{active_agent.agentId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /** Agent address section. */
        public static final HttpRequestActionBuilder agentAdresse = http("Agent adresse")
                        .get(WERH_API + "/agent/agent/adresse/#{active_agent.agentId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /** Agent birth section. */
        public static final HttpRequestActionBuilder agentNaissance = http("Agent naissance")
                        .get(WERH_API + "/agent/agent/naissance/#{active_agent.agentId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /** Agent contact section. */
        public static final HttpRequestActionBuilder agentContact = http("Agent contact")
                        .get(WERH_API + "/agent/agent/contact/#{active_agent.agentId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /** Agent bank details section. */
        public static final HttpRequestActionBuilder agentBank = http("Agent bank details")
                        .get(WERH_API + "/agent/agent/domiciliationBancaire/#{active_agent.agentId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /** Contract detail for the selected agent contract. */
        public static final HttpRequestActionBuilder contratDetail = http("Contract detail")
                        .get(WERH_API + "/career/contrat/#{active_agent.contratId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                        .check(jsonPath("$.situations[0].statut_id").saveAs("contratStatutId"));

        public static final HttpRequestActionBuilder droitStatus = http("Droit status")
                        .get(WERH_API + "/career/droit/status/#{contratStatutId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        // TODO check for agent with multiple contracts
        public static final HttpRequestActionBuilder calculBulletin = http("Calculer bulletin")
                        .post(WERH_API + "/pay/paie/cycle-paie/#{cyclePaieId}/agent/#{active_agent.agentId}/contrat/#{active_agent.contratId}/calculerBulletin")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

}
