package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.PayApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;

/**
 * "Contrôle des bulletins" page: the bulletin-control contract list plus the reference and
 * context lookups that populate the page filters, fetched concurrently as page resources, then
 * the synthèse SSE stream the page opens to report the bulletin calculation status. See
 * {@link PayApiEndpoints#bulletinsControleSyntheseStream} for why that stream is absent from the
 * recorded HAR.
 */
public final class ControleBulletinsGroup {

    private ControleBulletinsGroup() {
    }

    public static final ChainBuilder open =
            group("Open controle bulletins").on(
                    PayApiEndpoints.controlerBulletinContrat
                            .resources(
                                    PayApiEndpoints.dateRange,
                                    ReferentialApiEndpoints.fonction,
                                    ReferentialApiEndpoints.service,
                                    ReferentialApiEndpoints.statut,
                                    ReferentialApiEndpoints.sousStatut,
                                    ReferentialApiEndpoints.position,
                                    ReferentialApiEndpoints.grade,
                                    ReferentialApiEndpoints.etablissements,
                                    ReferentialApiEndpoints.collectivites),
                    pause(Duration.ofMillis(500)),
                    PayApiEndpoints.bulletinsControleSyntheseStream);
}
