
package groups.simulationGroups;

import endpoints.apiEndpoints.TiersApiEndpoints;
import endpoints.webEndpoints.WebPages;
import io.gatling.javaapi.core.ChainBuilder;
import static io.gatling.javaapi.core.CoreDsl.group;

public final class TiersGroup {

    private TiersGroup() {
    }


    /**
     * First dashboard load after login: SPA reload, silent SSO, then the initial burst of context/contract/notification calls.
     */
    public static final ChainBuilder open =
            group("Open Liste Tiers Comptable").on(
                    WebPages.TiersCompable, TiersApiEndpoints.chargerListeApenaf700,
                    TiersApiEndpoints.fournirListeTiersComptablesIHM
            );

    public static final ChainBuilder openFiche =
            group("Open Fiche Tiers").on(
                    WebPages.TiersCompable, TiersApiEndpoints.chargerListeApenaf700,
                    TiersApiEndpoints.chargerListeNatureJuridique,
                    TiersApiEndpoints.chargerListeTypologieCoordonneeCom,
                    TiersApiEndpoints.chargerListePays,
                    TiersApiEndpoints.chargerListeMonnaie,
                    TiersApiEndpoints.chargerTiersComptableEtMajEnsu,
                    TiersApiEndpoints.getNatureTiersFiche,
                    TiersApiEndpoints.chargerConfigEchangeComptable

            );

}