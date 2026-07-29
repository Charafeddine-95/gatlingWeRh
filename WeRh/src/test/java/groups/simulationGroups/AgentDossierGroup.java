package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.AgentApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.repeat;

/** Agent list loading after the user opens the agent microfrontend. */
public final class AgentDossierGroup {

    private AgentDossierGroup() {
    }

    /** Main XHR burst observed on the agent list page, excluding telemetry and script assets. */
    public static final ChainBuilder open =
            group("Open agents dossier").on(
                    ReferentialApiEndpoints.etablissements
                            .resources(ReferentialApiEndpoints.collectivites),

                    AgentApiEndpoints.addressTypes,

                    AgentApiEndpoints.latestSituation,

                    repeat(5).on(AgentApiEndpoints.agentDetail),

                    AgentApiEndpoints.agentIdentite,

                    AgentApiEndpoints.agentAdresse,

                    AgentApiEndpoints.agentBank,

                    AgentApiEndpoints.agentNaissance,

                    AgentApiEndpoints.agentContact,

                    ReferentialApiEndpoints.fonction,

                    AgentApiEndpoints.contratDetail,

                    ReferentialApiEndpoints.sousStatut,

                    AgentApiEndpoints.droitStatus





            );
}
