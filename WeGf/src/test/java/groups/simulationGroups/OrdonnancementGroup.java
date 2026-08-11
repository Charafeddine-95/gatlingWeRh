
package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.AuthApiEndpoints;
import endpoints.apiEndpoints.NotificationApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import endpoints.webEndpoints.LoginPages;
import endpoints.webEndpoints.WebPages;
import endpoints.apiEndpoints.ExecutionApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;
public final class OrdonnancementGroup {

    private OrdonnancementGroup() {
    }


    /**
     * First dashboard load after login: SPA reload, silent SSO, then the initial burst of context/contract/notification calls.
     */
    public static final ChainBuilder open =
            group("Open ordonancement").on(
                    WebPages.Ordonnancement,
                    ExecutionApiEndpoints.chargerExerciceComptable,
                    ExecutionApiEndpoints.fournirListeSerieBordereauxOrdonnancement,
                    ExecutionApiEndpoints.fournirListeLiquidationsCount,
                    ExecutionApiEndpoints.chargerConfigEditionPiecePourBudget,
                    ExecutionApiEndpoints.fournirListeLiquidationsCount
            );

}