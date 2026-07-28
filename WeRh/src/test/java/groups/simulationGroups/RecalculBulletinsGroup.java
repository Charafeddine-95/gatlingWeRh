package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.PayApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;

/**
 * "Recalcul des bulletins": the page re-fetches the bulletin-control list, opens the
 * Server-Sent Events stream reporting the recompute progress, then polls the calculation
 * summary. See {@link PayApiEndpoints#bulletinsCalculStream} for the SSE details.
 */
public final class RecalculBulletinsGroup {

    private RecalculBulletinsGroup() {
    }

    public static final ChainBuilder recompute =
            group("Recalcul bulletins").on(
                    PayApiEndpoints.controlerBulletinContrat
                            .resources(
                                    PayApiEndpoints.dateRange,
                                    ReferentialApiEndpoints.fonction,
                                    ReferentialApiEndpoints.service),
                    pause(Duration.ofMillis(500)),
                    PayApiEndpoints.bulletinsCalculStream,
                    pause(Duration.ofSeconds(6)),
                    PayApiEndpoints.calculationInfo);
}
