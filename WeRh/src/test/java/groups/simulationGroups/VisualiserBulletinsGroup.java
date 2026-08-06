package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.PayApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.doIf;
import java.util.Objects;

/**
 * "Visualiser les bulletins": the pay-preparation view is loaded for the last CLOSED month (where
 * payslips are finalized), then one random agent's bulletin is rendered as a PDF to confirm it
 * returns a real document.
 *
 * <p>The reused month-dependent endpoints read "#{payMonth}", so the group first switches payMonth
 * to the correlated "closedMonth" (from {@link PayApiEndpoints#cycleResume}). The bulletin list is
 * loaded with {@link PayApiEndpoints#controlerBulletinContratForView}, which picks a random
 * "bulletinId" that {@link PayApiEndpoints#bulletinByIds} then renders.
 */
public final class VisualiserBulletinsGroup {

    private VisualiserBulletinsGroup() {
    }

    public static final ChainBuilder open =
        exec(session -> session.set("payMonth", session.getString("closedMonth")))
                .exec(group("Visualiser bulletins").on(
                        PayApiEndpoints.controlerBulletinContratForView
                                .resources(
                                        PayApiEndpoints.dateRange,
                                        ReferentialApiEndpoints.fonction,
                                        ReferentialApiEndpoints.service),
                        pause(Duration.ofMillis(500)),
                        PayApiEndpoints.bulletinsCalculStream,
                        doIf(session -> !Objects.equals(
                                session.getString("payOnlyMonth"),
                                session.getString("closedOnlyMonth")))
                                .then(
                                        pause(Duration.ofSeconds(1)),
                                        PayApiEndpoints.bulletinByIds))
                        .exitHereIfFailed());

}
