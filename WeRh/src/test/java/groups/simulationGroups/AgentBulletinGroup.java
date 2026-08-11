package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.AgentApiEndpoints;
import endpoints.apiEndpoints.PayApiEndpoints;
import endpoints.webEndpoints.WebPages;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;

/**
 * Opening an agent from the list, then its pay-stub (bulletin de paie) tab. Relies on the agents
 * list having captured agentId/contratId and the payroll-cycle resume having captured cyclePaieId.
 * The generic reference-data lookups the agent detail page also fires (DSN referentials,
 * statut/service, fonts, telemetry) are intentionally left out to keep the journey focused on the
 * payslip flow.
 */
public final class AgentBulletinGroup {

    private AgentBulletinGroup() {
    }

    /** Click an agent (profile + contract burst), open the pay-stub tab, consume the SSE. */
    public static final ChainBuilder open =
            group("Open agent bulletin").on(
                    PayApiEndpoints.cyclePaieByEtablissement
                            .resources(PayApiEndpoints.situationContrat),
                    pause(Duration.ofMillis(500)),
                    PayApiEndpoints.bulletinStream,
                    PayApiEndpoints.listeBulletin);
}
