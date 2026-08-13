package groups.simulationGroups;

import endpoints.apiEndpoints.AgentApiEndpoints;
import endpoints.apiEndpoints.PayApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

public class CreateAgentGroup {

    private CreateAgentGroup() {}

    public static final ChainBuilder open =
            group("Create Agent").on(
                    AgentApiEndpoints.setDataAgent,
                    AgentApiEndpoints.checkPresent,
                    AgentApiEndpoints.agentCommand,
                    AgentApiEndpoints.cities,
                    AgentApiEndpoints.countries,
                    AgentApiEndpoints.addressTypes,
                    AgentApiEndpoints.civilites,
                    AgentApiEndpoints.etablissementBancaire,
                    AgentApiEndpoints.situationFamiliale)
                    ;


}
