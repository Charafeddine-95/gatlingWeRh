
package groups.simulationGroups;

import endpoints.apiEndpoints.ExecutionApiEndpoints;
import endpoints.webEndpoints.WebPages;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.group;

public final class OrdonnancementSelectionLiquidationsGroup {

    private OrdonnancementSelectionLiquidationsGroup() {
    }


    /**
     * First dashboard load after login: SPA reload, silent SSO, then the initial burst of context/contract/notification calls.
     */
    public static final ChainBuilder open =
            group("Open ordonancement").on(
                    WebPages.Ordonnancement,
                    ExecutionApiEndpoints.fournirListeLiquidations,
                    ExecutionApiEndpoints.fournirListeLiquidations,
                    ExecutionApiEndpoints.fournirListeBordereauxPreparatoire,
                    ExecutionApiEndpoints.chargerTailleLimite,
                    ExecutionApiEndpoints.fournirTailleLiquidationPourPES

            );

}