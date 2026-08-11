package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;

import endpoints.apiEndpoints.PayApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;

/**
 * "Assistant de paie" landing page. The payroll-cycle resume runs first and correlates the
 * active etablissement and open month; the per-etablissement status burst then fires
 * concurrently, mirroring the XHR fan-out seen on the page.
 */
public final class PayAssistantGroup {

    private PayAssistantGroup() {
    }

    public static final ChainBuilder open =
            group("Open pay assistant").on(
                    PayApiEndpoints.cycleResume,
                    PayApiEndpoints.spreadActiveCycle,
                    PayApiEndpoints.computeNextMonth,
                    PayApiEndpoints.cycleStatus
                            .resources(
                                    PayApiEndpoints.monthPayByEtablissement,
                                    PayApiEndpoints.contratActif,
                                    PayApiEndpoints.agentUnpaid,
                                    PayApiEndpoints.contratsEvents));
}
