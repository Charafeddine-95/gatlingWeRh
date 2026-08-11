package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;

import endpoints.apiEndpoints.ExecutionApiEndpoints;
import endpoints.apiEndpoints.NumerotationApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;

public final class NumerotationGroup {

    private NumerotationGroup() {
    }

    /**
     * Numbers the liquidation selected on the ordonnancement screen and creates its bordereau.
     * Runs after {@code OrdonnancementSelectionLiquidationsGroup.open}, which loads the grid and
     * saves the responses this chain correlates from.
     *
     * <p>Three user gestures, in order:
     *
     * <ol>
     *   <li>ticking the row — setNumerotation, then the eclatement control;</li>
     *   <li>confirming — creerBordereauLiquidation, the actual write;</li>
     *   <li>landing on the new bordereau — the burst the screen fires to render it. The browser
     *       sends those in parallel, and repeats the two signataire calls once each; kept here
     *       so the request count matches what the server really sees.</li>
     * </ol>
     *
     * <p>Note this chain writes: every pass creates a real bordereau and takes the selected
     * liquidation out of the grid.
     */
    public static final ChainBuilder numeroter =
            group("Numeroter les liquidations").on(
                    NumerotationApiEndpoints.setNumerotationParCompteSectionPossible,
                    NumerotationApiEndpoints.preparerCorpsOrdonnancement,
                    NumerotationApiEndpoints.controlerEclatementTitre,
                    NumerotationApiEndpoints.creerBordereauLiquidation,
                    NumerotationApiEndpoints.fournirListePJATransmettre,
                    NumerotationApiEndpoints.fournirSignataireActifCollectivite,
                    NumerotationApiEndpoints.rechercherLibelleSignataireParDefaut,
                    NumerotationApiEndpoints.hasBienIncompletPourGenerationBienPjMandat,
                    NumerotationApiEndpoints.fournirListeComptablesAssignataires,
                    NumerotationApiEndpoints.chargerConfigEchangeComptable,
                    NumerotationApiEndpoints.fournirListeCA_NCValide,
                    ExecutionApiEndpoints.chargerTailleLimite,
                    NumerotationApiEndpoints.fournirSignataireActifCollectivite,
                    NumerotationApiEndpoints.rechercherLibelleSignataireParDefaut,
                    NumerotationApiEndpoints.chargerListeCircuit,
                    NumerotationApiEndpoints.fournirCircuitParDefaut);
}
