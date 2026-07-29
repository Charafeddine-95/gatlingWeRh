package endpoints.webEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;
import io.gatling.javaapi.core.Session;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** WeRH web application pages. */
public final class WebPages {

        private WebPages() {
        }

        private static final Map<String, String> NAVIGATION_HEADERS = Map.of(
                        "accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                        "priority", "u=0, i",
                        "upgrade-insecure-requests", "1");

        /**
         * Landing page of the WeRH web app — serves the single-page application shell.
         */
        public static final HttpRequestActionBuilder home = http("Home page")
                        .get("https://werh.uat.wemagnus.com/")
                        .headers(NAVIGATION_HEADERS);

        public static final HttpRequestActionBuilder listeAgent = http("Liste agents")
                        .get("https://werh.uat.wemagnus.com/mfe-agent/liste-agents")
                        .headers(NAVIGATION_HEADERS);

        private static final String PARAMS_TEMPLATE = "{\"contractId\":\"%s\",\"defaultTab\":%d,\"returnParams\":{"
                        + "\"idCollectivite\":\"%s\",\"nomCollectivite\":\"%s\","
                        + "\"idEtablissement\":\"%s\",\"nomEtablissement\":\"%s\","
                        + "\"moisPaie\":\"%s\",\"_from\":\"%s\"}}";

        
        // Reconstruit le params puis l'encode en base64 sans padding, comme le front.
        // TODO s'assurer qu'on a ces variables présentes dans la session
        private static String encodeParams(Session session) {
                String json = String.format(PARAMS_TEMPLATE,
                                session.getString("contractId"),
                                session.getInt("defaultTab"),
                                session.getString("idCollectivite"),
                                session.getString("nomCollectivite"),
                                session.getString("idEtablissement"),
                                session.getString("nomEtablissement"),
                                session.getString("moisPaie"),
                                session.getString("returnFrom"));
                return Base64.getEncoder().withoutPadding()
                                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
        }

        public static final HttpRequestActionBuilder dossierAgentFrom = http("Dossier agent from")
                        .get("https://werh.uat.wemagnus.com/mfe-agent/dossier-agent")
                        .queryParam("id", "#{active_agent.agentId}")
                        .queryParam("from", "#{fromModule}")
                        .queryParam("params", WebPages::encodeParams)
                        .queryParam("moisCycle", "#{moisCycle}");


        public static final HttpRequestActionBuilder dossierAgentDepuisListe =
        http("Dossier agent")
                .get("https://werh.uat.wemagnus.com/mfe-agent/dossier-agent")
                .queryParam("id", "#{active_agent.agentId}")
                .queryParam("from", "mfe-agent_liste-agents");                        
}
