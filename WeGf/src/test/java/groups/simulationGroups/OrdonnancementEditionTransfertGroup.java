package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.repeat;

import endpoints.apiEndpoints.EditionTransfertApiEndpoints;
import endpoints.apiEndpoints.ExecutionApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;

/**
 * "Edition / transfert de bordereaux existants": the other tab of the ordonnancement screen.
 * Where {@code OrdonnancementSelectionLiquidationsGroup} + {@code NumerotationGroup} number
 * liquidations into a brand new bordereau, this one picks bordereaux that already exist and
 * asks for their PES flux.
 *
 * <p>Runs after {@code OrdonnancementGroup.open}, which loads the exercice comptable both chains
 * below send back.
 */
public final class OrdonnancementEditionTransfertGroup {

    private OrdonnancementEditionTransfertGroup() {
    }

    /**
     * Opening the tab and ticking bordereaux: the grid of existing bordereaux, the per-tick
     * sizing calls, then the burst the screen fires to render the selection.
     */
    public static final ChainBuilder open =
            group("Open edition transfert bordereaux").on(
                    ExecutionApiEndpoints.chargerTailleLimite,
                    EditionTransfertApiEndpoints.fournirListeBordereauxAvecMontant,
                    EditionTransfertApiEndpoints.choisirBordereaux,
                    // Every tick re-sizes the PES flux for the whole current selection, one call
                    // per already-ticked row — so tick 1 fires 1 call, tick 2 fires 2, tick 3
                    // fires 3. Outer loop = the ticks, inner loop = the rows selected so far,
                    // giving nbSelection*(nbSelection+1)/2 calls: 1, 3 or 6.
                    repeat("#{nbSelection}", "tick").on(
                            repeat(session -> session.getInt("tick") + 1, "indexBordereau").on(
                                    EditionTransfertApiEndpoints.fournirTailleBordereauPourPES)),
                    EditionTransfertApiEndpoints.fournirListeLiquidationsParBordereaux,
                    // The browser sends the rest in parallel, and repeats the two signataire
                    // calls once each; kept here so the request count matches what the server
                    // really sees.
                    EditionTransfertApiEndpoints.fournirListePJATransmettre,
                    EditionTransfertApiEndpoints.fournirSignataireActifCollectivite,
                    EditionTransfertApiEndpoints.rechercherLibelleSignataireParDefaut,
                    EditionTransfertApiEndpoints.liquidationsParBordereauxHasBienIncomplet,
                    EditionTransfertApiEndpoints.fournirListeComptablesAssignataires,
                    EditionTransfertApiEndpoints.chargerConfigEchangeComptable,
                    EditionTransfertApiEndpoints.fournirListeCA_NCValide,
                    ExecutionApiEndpoints.chargerTailleLimite,
                    EditionTransfertApiEndpoints.fournirSignataireActifCollectivite,
                    EditionTransfertApiEndpoints.rechercherLibelleSignataireParDefaut,
                    EditionTransfertApiEndpoints.chargerListeCircuit,
                    EditionTransfertApiEndpoints.fournirCircuitParDefaut);

    /**
     * "Generer le flux PES" on the ticked bordereaux: the attachments are re-read, the
     * bordereaux are saved, the generation is controlled, then the dossier is filed and the flux
     * queued against it.
     *
     * <p>Note this chain writes: every pass creates a real dossier in the suivi des echanges.
     */
    public static final ChainBuilder genererFluxPES =
            group("Generer flux PES").on(
                    EditionTransfertApiEndpoints.fournirListePJATransmettre,
                    EditionTransfertApiEndpoints.processModifierBordereauListe,
                    EditionTransfertApiEndpoints.preparerCorpsFluxPES,
                    EditionTransfertApiEndpoints.controleGenerationPes,
                    EditionTransfertApiEndpoints.envoyerNouveauDossierExecutionPlusTard,
                    EditionTransfertApiEndpoints.majPesDansSuiviEchange);
}
