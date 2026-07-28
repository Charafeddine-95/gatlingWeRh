package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;

import endpoints.apiEndpoints.ExecutionApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;
import endpoints.webEndpoints.WebPages;

/** Approval of the general conditions of use (GCU) on first login. */
public final class ExecutionGroup {

    private ExecutionGroup() {
    }

    /** The user accepts the GCU dialog — only part of the first-connection journey. */
    public static final ChainBuilder Titre =
            group("Titre").on(
                ExecutionApiEndpoints.chargerExerciceComptable,
                ExecutionApiEndpoints.fournirListeSerieBordereaux,
                ExecutionApiEndpoints.chargerFields,
                ExecutionApiEndpoints.chargerTailleLimite,
                ExecutionApiEndpoints.liquidationsTitre,
                ExecutionApiEndpoints.gestionFonctionsEtAxesAnalytiques,
                ExecutionApiEndpoints.chargerListe,
                ExecutionApiEndpoints.chargerListeCompteUtilParModeleMvt,
                ExecutionApiEndpoints.chargerListeBordereauPreparatoire,
                WebPages.Titre
            );      

}
