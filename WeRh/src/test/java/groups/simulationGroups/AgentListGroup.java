package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.AgentApiEndpoints;
import endpoints.apiEndpoints.CityApiEndpoints;
import endpoints.apiEndpoints.PayApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import endpoints.webEndpoints.WebPages;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;

/** Agent list loading after the user opens the agent microfrontend. */
public final class AgentListGroup {

    private AgentListGroup() {
    }

    /** Main XHR burst observed on the agent list page, excluding telemetry and script assets. */
    public static final ChainBuilder open =
            group("Open agents list").on(
                    WebPages.listeAgent,
                    ReferentialApiEndpoints.etablissements
                            .resources(ReferentialApiEndpoints.collectivites),
                    pause(Duration.ofMillis(500)),
                    PayApiEndpoints.cycleResume
                            .resources(
                                    CityApiEndpoints.countries,
                                    CityApiEndpoints.cities,
                                    AgentApiEndpoints.addressTypes,
                                    AgentApiEndpoints.civilities),
                    pause(Duration.ofMillis(500)),
                    AgentApiEndpoints.contracts);
}
