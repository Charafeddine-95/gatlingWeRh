package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;

import endpoints.apiEndpoints.ExecutionApiEndpoints;
import endpoints.apiEndpoints.MandatApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;
import endpoints.webEndpoints.WebPages;

/** Approval of the general conditions of use (GCU) on first login. */
public final class ExecutionGroup {

        private ExecutionGroup() {
        }

        /**
         * The user accepts the GCU dialog — only part of the first-connection journey.
         */
        public static final ChainBuilder Titre = group("Titre").on(
                        ExecutionApiEndpoints.chargerExerciceComptable,
                        ExecutionApiEndpoints.fournirListeSerieBordereauxTitre,
                        ExecutionApiEndpoints.chargerFields,
                        ExecutionApiEndpoints.chargerTailleLimite,
                        ExecutionApiEndpoints.liquidationsTitre,
                        ExecutionApiEndpoints.gestionFonctionsEtAxesAnalytiques,
                        ExecutionApiEndpoints.chargerListe,
                        ExecutionApiEndpoints.chargerListeCompteUtilParModeleMvtTitre,
                        ExecutionApiEndpoints.chargerListeBordereauPreparatoireTitre,
                        WebPages.Titre);

        /**
         * The user accepts the GCU dialog — only part of the first-connection journey.
         */
        public static final ChainBuilder Mandat = group("Mandat").on(
                        ExecutionApiEndpoints.fournirListeSerieBordereauxMandat,
                        ExecutionApiEndpoints.chargerExerciceComptable,
                        ExecutionApiEndpoints.chargerFields,
                        ExecutionApiEndpoints.chargerTailleLimite,
                        ExecutionApiEndpoints.liquidationsMandat,
                        ExecutionApiEndpoints.gestionFonctionsEtAxesAnalytiques,
                        ExecutionApiEndpoints.chargerListe,
                        ExecutionApiEndpoints.chargerListeCompteUtilParModeleMvtMandat,
                        ExecutionApiEndpoints.chargerListeBordereauPreparatoireMandat,
                        WebPages.Mandat);

        public static final ChainBuilder PJ = group("Pièces Justificatives").on(
                        ExecutionApiEndpoints.chargerCollectivte,
                        ExecutionApiEndpoints.chargerListeBudget,
                        ExecutionApiEndpoints.piecesJustificatives,
                        WebPages.PJ);

        public static final ChainBuilder Engagements = group("Engagements").on(
                        ExecutionApiEndpoints.operationInvestissement,
                        ExecutionApiEndpoints.apae,
                        ExecutionApiEndpoints.exerciceComptable,
                        ExecutionApiEndpoints.gestionFonctionsEtAxesAnalytiques,
                        ExecutionApiEndpoints.engagements);

        public static final ChainBuilder OpenMandat = group("Ouverture mandat").on(
                        MandatApiEndpoints.chargerLiqComplet,
                        MandatApiEndpoints.chargerAxeAnalitique,
                        MandatApiEndpoints.chargerListeObjetExecution,
                        MandatApiEndpoints.activiteAFinancer,
                        MandatApiEndpoints.fournirListeSerieBordereauxMandatOuvert,
                        MandatApiEndpoints.executionsBudget,
                        MandatApiEndpoints.chargerFieldNames,
                        MandatApiEndpoints.fournirListeServiceTCParBudget,
                        MandatApiEndpoints.majDonneesComboNature);

}
