
package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.AuthApiEndpoints;
import endpoints.apiEndpoints.NotificationApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import endpoints.webEndpoints.LoginPages;
import endpoints.webEndpoints.WebPages;
import endpoints.apiEndpoints.EditionApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;
public final class EditionGroup {

    private EditionGroup() {
    }


    /**
     * First dashboard load after login: SPA reload, silent SSO, then the initial burst of context/contract/notification calls.
     */
    public static final ChainBuilder open =
            group("Open Editio grand livre").on(
                    WebPages.Grandlivre,
                    EditionApiEndpoints.chargerFieldsGrandlivre,
                    EditionApiEndpoints.chargerListeCollectiviteByCritereLieBudget,
                    EditionApiEndpoints.chargerListeBudget,
                    EditionApiEndpoints.chargerListe
            );

}