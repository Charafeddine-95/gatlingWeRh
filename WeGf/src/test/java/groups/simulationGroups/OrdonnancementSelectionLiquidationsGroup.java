
package groups.simulationGroups;

import endpoints.apiEndpoints.EditionBordereauApiEndpoints;
import endpoints.apiEndpoints.ExecutionApiEndpoints;
import endpoints.apiEndpoints.NotificationApiEndpoints;
import endpoints.apiEndpoints.NumerotationApiEndpoints;
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
            )
            // Nothing downstream works without a ticked liquidation: numeroter would answer
            // without a numeroBordereau and every call after it would miss bordereauId. A user
            // that gets no grid leaves here, with the reason choisirLiquidations printed,
            // instead of trailing six "No attribute named" errors behind it.
            .exitHereIfFailed();


    /**
     * "Edition de bordereau": the grid of bordereaux already created with their amounts, the
     * user ticking a few of them, and the burst the screen fires to render the edition panel.
     *
     * <p>The tail of that burst is the same set of calls the numerotation flow ends on, and
     * four of them send the exact same static payload, so those are taken from
     * {@code NumerotationApiEndpoints} rather than recorded twice. The seven that carry the
     * ticked bordereaux have their own bodies in {@code EditionBordereauApiEndpoints}.
     *
     * <p>The browser fires the burst in parallel and repeats the two signataire calls once
     * each; kept in order here so the request count matches what the server really sees.
     */
    public static final ChainBuilder editionBordereau =
            group("Edition bordereau").on(
                    ExecutionApiEndpoints.chargerTailleLimite,
                    ExecutionApiEndpoints.fournirListeBordereauxAvecMontant,
                    EditionBordereauApiEndpoints.choisirBordereaux,
                    // Same shape as the liquidation ticks above: every tick re-sizes the PES flux
                    // for the whole current selection, one call per already-ticked bordereau, so
                    // nbSelectionBordereau*(nbSelectionBordereau+1)/2 calls in total.
                    repeat("#{nbSelectionBordereau}", "tickBordereau").on(
                            repeat(session -> session.getInt("tickBordereau") + 1, "indexBordereau").on(
                                    EditionBordereauApiEndpoints.fournirTailleBordereauPourPES)),
                    EditionBordereauApiEndpoints.fournirListeLiquidationsParBordereaux,
                    EditionBordereauApiEndpoints.fournirListePJATransmettre,
                    EditionBordereauApiEndpoints.fournirSignataireActifCollectivite,
                    EditionBordereauApiEndpoints.rechercherLibelleSignataireParDefaut,
                    EditionBordereauApiEndpoints.liquidationsParBordereauxHasBienIncomplet,
                    NumerotationApiEndpoints.fournirListeComptablesAssignataires,
                    NumerotationApiEndpoints.chargerConfigEchangeComptable,
                    NumerotationApiEndpoints.fournirListeCA_NCValide,
                    ExecutionApiEndpoints.chargerTailleLimite,
                    EditionBordereauApiEndpoints.fournirSignataireActifCollectivite,
                    EditionBordereauApiEndpoints.rechercherLibelleSignataireParDefaut,
                    NumerotationApiEndpoints.chargerListeCircuit,
                    EditionBordereauApiEndpoints.fournirCircuitParDefaut);

    /**
     * "Generer le flux PES" on the bordereaux ticked by {@code editionBordereau}: refreshes the
     * attachments, flags the bordereaux, runs the last control, then writes.
     *
     * <p>Note this chain writes. Every pass creates a real dossier PES Aller queued for
     * transmission and marks the ticked bordereaux as generated, so a second pass over the same
     * bordereaux is not the same test as the first.
     *
     * <p>Runs after {@code editionBordereau}, which ticks the bordereaux and saves the
     * attachments list the payloads send back. The recording also carries three Whatfix beacons
     * around this burst; they are telemetry, not app load, and are left out on purpose — same
     * call as for the Dynatrace RUM beacons.
     */
    public static final ChainBuilder genererBordereau =
            group("Generer bordereau").on(
                    EditionBordereauApiEndpoints.fournirListePJATransmettre,
                    EditionBordereauApiEndpoints.processModifierBordereauListe,
                    // No request of its own: reads the two responses above, so it sits between
                    // processModifierBordereauListe and the first call that sends their rows.
                    EditionBordereauApiEndpoints.preparerCorpsGeneration,
                    EditionBordereauApiEndpoints.controleGenerationPes,
                    EditionBordereauApiEndpoints.envoyerNouveauDossierExecutionPlusTard,
                    EditionBordereauApiEndpoints.majPesDansSuiviEchange);

}
