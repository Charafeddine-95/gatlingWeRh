
package groups.simulationGroups;

import endpoints.apiEndpoints.ExecutionApiEndpoints;
import endpoints.webEndpoints.WebPages;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.repeat;

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
                    ExecutionApiEndpoints.choisirLiquidations,
                    // Every tick re-sizes the PES flux for the whole current selection, one call
                    // per already-ticked row — so tick 1 fires 1 call, tick 2 fires 2, tick 3
                    // fires 3. Outer loop = the ticks, inner loop = the rows selected so far,
                    // giving nbSelection*(nbSelection+1)/2 calls: 1, 3 or 6.
                    repeat("#{nbSelection}", "tick").on(
                            repeat(session -> session.getInt("tick") + 1, "indexLiquidation").on(
                                    ExecutionApiEndpoints.fournirTailleLiquidationPourPES))
            );

}
