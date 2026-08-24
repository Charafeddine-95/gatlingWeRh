package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import static io.gatling.javaapi.core.CoreDsl.*;

/** Notification API calls. */
public final class ExecutionApiEndpoints {
        private ExecutionApiEndpoints() {
        }

        // TITRES

        private static final String LIQUIDATIONS_FIELDS = String.join(",",
                        "numeroBordereau", "numeroPiece", "codeAliasPrefTiers", "objet", "codeCompteUtilisateur",
                        "montantHt", "montantTtc", "dateEmissionBordereau", "etatPieceTresorerieId",
                        "typeUtilisationAsapId");

        private static final String LISTE_FIELDS = "nombreTotalElements, donnees ,*.id,*.code, *.libelle";

        /** Loads the connected user's IAM profile for the selected tenant. */
        public static final HttpRequestActionBuilder chargerExerciceComptable = http("Charger exercice comptable")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcExerciceComptable/chargerExerciceComptable?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        "{\"id\":#{userContextCBE.exercice.exercice.id},\"element\":[\"budgetRef\",\"budgetRef.collectiviteRef\",\"comptableAssignataireRef\",\"normeComptableRef\"]}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(
                                        jsonPath("$.budgetRef.id").ofInt().gt(0),
                                        jmesPath("millesime").saveAs("millesime"),
                                        jmesPath("id").saveAs("idExerciceComptable"),
                                        // The budget and the collectivite are written out inside this
                                        // response; the generation payload points at them by @id from a
                                        // sibling field, so the two numbers are read off here rather than
                                        // assumed — see EditionBordereauApiEndpoints.DOSSIER.
                                        // Two different numbers, easy to mix up: "id" is the budget
                                        // itself (idBudget on the sizing and PJ calls), "@id" is only
                                        // its slot in this response's object graph (what a sibling
                                        // field points at). Sending the @id as idBudget asks for a
                                        // budget that does not exist and the API answers 500.
                                        jmesPath("budgetRef.id").saveAs("idBudget"),
                                        jmesPath("budgetRef.\"@id\"").saveAs("exerciceBudgetAtId"),
                                        jmesPath("budgetRef.collectiviteRef.\"@id\"")
                                                        .saveAs("exerciceCollectiviteAtId"),
                                        // The assigning accountant is tenant data, not referential:
                                        // tenant 1 answers 11 ("TRESORERIE BERGER-LEVRAULT") where
                                        // tenant 2 answers 1. Read off here so the CA_NC lookup asks
                                        // for the comptable this exercice actually hangs off — see
                                        // NumerotationApiEndpoints.fournirListeCA_NCValide.
                                        jmesPath("comptableAssignataireRef.id").saveAs("idComptable"),
                                        // Kept whole: NumerotationApiEndpoints sends this exercice back verbatim,
                                        // both inside the creerBordereauLiquidation payload and as its own
                                        // argument on the two signataire calls.
                                        bodyString().saveAs("exerciceComptableJson"));

        // TODO parameters for multiple uses
        public static final HttpRequestActionBuilder fournirListeSerieBordereauxTitre = http(
                        "Fournir liste serie bordereaux")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcMandatTitre/fournirListeSeriesBordereaux?fieldNames%5B%5D=nombreTotalElements,%20donnees%20,*.id,*.code,%20*.libelle")
                        .body(StringBody(
                                        "{\"exercice\":#{userContextCBE.exercice.exercice.id},\"sens\":{\"_id\":3,\"_lib\":\"Recette\",\"@id\":2,\"@type\":\"TypeGestionSens\"},\"annulatif\":false,\"defaut\":null,\"elementsACharger\":null}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        public static final HttpRequestActionBuilder chargerFields = http("Charger fields")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UseCaseTechnique/charger2?fieldNames%5B%5D=id,%20millesime,%20budgetRef,%20comptableAssignataireRef,%20*.id,*.collectiviteRef,*.collectiviteRef.id,normeComptableRef")
                        .body(StringBody(
                                        "{\"classePersistante\":\"ExerciceComptable\",\"id\":#{userContextCBE.exercice.exercice.id},\"elementACharger\":[\"budgetRef.collectiviteRef\",\"normeComptableRef\"]}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("budgetRef.id").ofInt().gt(0));

        public static final HttpRequestActionBuilder chargerTailleLimite = http("Charger taille limite")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/chargerTailleLimitePES?fieldNames%5B%5D=**")
                        // The screen posts an empty JSON object, not an empty body.
                        .body(StringBody("{}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(bodyLength().gt(0));

        public static final HttpRequestActionBuilder liquidationsTitre = http("Liquidations titre")
                        .get("https://wegf-api.uat.wemagnus.com/compta/liquidations")
                        .queryParam("fields[liquidations]", LIQUIDATIONS_FIELDS)
                        .queryParam("filter[exerciceComptableId]", "#{userContextCBE.exercice.exercice.id}")
                        .queryParam("filter[type]", "titres")
                        .queryParam("filter[annulatif]", false)
                        .queryParam("filter[budgetaire]", true)
                        .queryParam("filter[interne]", false)
                        .queryParam("filter[numerote]", false)
                        .queryParam("filter[debitOffice]", false)
                        .queryParam("filter[finExo]", false)
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("data[0].id").ofInt().gt(0));

        public static final HttpRequestActionBuilder gestionFonctionsEtAxesAnalytiques = http(
                        "Gestion fonctions et axes analytiques")
                        .get("https://wegf-api.uat.wemagnus.com/compta/collectivite/gestionFonctionsEtAxesAnalytiques")
                        .queryParam("exerciceComptableId", "#{userContextCBE.exercice.exercice.id}")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("visibiliteAxe1").exists());

        // TODO check if values are hardcoded or not
        public static final HttpRequestActionBuilder chargerListe = http("Charger liste")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UseCaseTechnique/chargerListe")
                        .queryParam("fieldNames[]", "nombreTotalElements, donnees ,*.id,*.code, *.libelle")
                        .body(StringBody(
                                        "{\"classePersistante\":\"OperationdInvestissement\",\"parametresRecherche\":{\"listeCriteres\":[{\"lienClassePersistante\":\"OperationdInvestissement\",\"lienAttribut\":\"budgetRef\",\"valeur\":{\"id\":1,\"marque\":0,\"collectiviteRef\":{\"id\":1,\"marque\":0,\"@id\":5,\"@type\":\"Collectivite\"},\"@id\":4,\"@type\":\"Budget\"},\"operateur\":{\"_id\":3,\"_lib\":\"égal à\",\"@id\":6,\"@type\":\"RechercheOperateurEnum\"},\"@id\":3,\"@type\":\"RechercheCritere\"},{\"lienClassePersistante\":\"OperationdInvestissement\",\"lienAttribut\":\"exerciceDebut\",\"valeur\":#{millesime},\"operateur\":{\"_id\":7,\"_lib\":\"inférieur ou égal à\",\"@id\":8,\"@type\":\"RechercheOperateurEnum\"},\"@id\":7,\"@type\":\"RechercheCritere\"},{\"lienClassePersistante\":\"OperationdInvestissement\",\"lienAttribut\":\"exerciceFin\",\"valeur\":#{millesime},\"operateur\":{\"_id\":261,\"_lib\":\"supérieur ou égal à or Null\",\"@id\":10,\"@type\":\"RechercheOperateurEnum\"},\"@id\":9,\"@type\":\"RechercheCritere\"}],\"listeCriteresRechercheGui\":[],\"listeAttributs\":[],\"listeTris\":[{\"croissant\":true,\"lienClassePersistante\":\"OperationdInvestissement\",\"lienAttribut\":\"code\",\"@id\":11,\"@type\":\"RechercheTri\"}],\"distinct\":false,\"paginatorValues\":{\"length\":10000,\"pageIndex\":0,\"pageSize\":10000,\"previousPageIndex\":0},\"@id\":2,\"@type\":\"RechercheParametres\"}}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        // TODO make dynamic body instead of hardcoded values
        public static final HttpRequestActionBuilder chargerListeCompteUtilParModeleMvtTitre = http(
                        "Charger liste compte util par modele mvt")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Budget/chargerListeCompteUtilParModeleMvt")
                        .queryParam("fieldNames[]", "nombreTotalElements, donnees ,*.id,*.code, *.libelle")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .body(StringBody(
                                        "{\"exercice\":{\"id\":#{userContextCBE.exercice.exercice.id},\"marque\":0,\"sourceInformationRatios\":{\"_id\":1,\"_lib\":\"DGCP\",\"@id\":3,\"@type\":\"TypeSourceInformation\"},\"millesime\":#{millesime},\"budgetRef\":{\"id\":1,\"marque\":0,\"collectiviteRef\":{\"id\":1,\"marque\":0,\"@id\":5,\"@type\":\"Collectivite\"},\"@id\":4,\"@type\":\"Budget\"},\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":6,\"@type\":\"NormeComptable\"},\"comptableAssignataireRef\":{\"id\":1,\"marque\":0,\"@id\":7,\"@type\":\"ComptableAssignataire\"},\"@id\":2,\"@type\":\"ExerciceComptable\"},\"sens\":{\"_id\":3,\"_lib\":\"Recette\",\"@id\":2,\"@type\":\"TypeGestionSens\"},\"pmmvt\":null}"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        public static final HttpRequestActionBuilder chargerListeBordereauPreparatoireTitre = http(
                        "Charger liste bordereaux preparatoires")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UseCaseTechnique/chargerListe")
                        .queryParam("fieldNames[]", LISTE_FIELDS)
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .body(StringBody(
                                        "{\"classePersistante\":\"BordereauPreparatoire\",\"parametresRecherche\":{\"listeCriteres\":[{\"lienClassePersistante\":\"BordereauPreparatoire\",\"lienAttribut\":\"id\",\"valeur\":0,\"operateur\":{\"_id\":8,\"_lib\":\"différent de\",\"@id\":4,\"@type\":\"RechercheOperateurEnum\"},\"@id\":3,\"@type\":\"RechercheCritere\"},{\"lienClassePersistante\":\"BordereauPreparatoire\",\"lienAttribut\":\"sens\",\"valeur\":{\"_id\":2,\"_lib\":\"Dépense\",\"@id\":6,\"@type\":\"TypeGestionSens\"},\"operateur\":4,\"@id\":5,\"@type\":\"RechercheCritere\"}],\"listeCriteresRechercheGui\":[],\"listeAttributs\":[],\"listeTris\":[{\"croissant\":true,\"lienClassePersistante\":\"BordereauPreparatoire\",\"lienAttribut\":\"code\",\"@id\":7,\"@type\":\"RechercheTri\"}],\"distinct\":false,\"paginatorValues\":{\"length\":10000,\"pageIndex\":0,\"pageSize\":10000,\"previousPageIndex\":0},\"@id\":2,\"@type\":\"RechercheParametres\"}}"))
                        .check(bodyLength().gt(0));

        // MANDATS

        public static final HttpRequestActionBuilder fournirListeSerieBordereauxMandat = http(
                        "Fournir liste serie bordereaux")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcMandatTitre/fournirListeSeriesBordereaux?fieldNames%5B%5D=nombreTotalElements,%20donnees%20,*.id,*.code,%20*.libelle")
                        .body(StringBody(
                                        "{\"exercice\":#{userContextCBE.exercice.exercice.id},\"sens\":{\"_id\":2,\"_lib\":\"Dépense\",\"@id\":2,\"@type\":\"TypeGestionSens\"},\"annulatif\":false,\"defaut\":null,\"elementsACharger\":null}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        // JSON:API shape: the list hangs off "data", not the "donnees" envelope the
        // UseCaseTechnique/chargerListe calls answer with. jsonPath (not jmesPath, which
        // is single-value) so findRandom picks one liquidation out of the whole page.
        public static final HttpRequestActionBuilder liquidationsMandat = http("Liquidations titre")
                        .get("https://wegf-api.uat.wemagnus.com/compta/liquidations")
                        .queryParam("fields[liquidations]", LIQUIDATIONS_FIELDS)
                        .queryParam("filter[exerciceComptableId]", "#{userContextCBE.exercice.exercice.id}")
                        .queryParam("filter[type]", "mandats")
                        .queryParam("filter[annulatif]", false)
                        .queryParam("filter[budgetaire]", true)
                        .queryParam("filter[interne]", false)
                        .queryParam("filter[numerote]", false)
                        .queryParam("filter[debitOffice]", false)
                        .queryParam("filter[finExo]", false)
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("data[0].id").ofInt().gt(0),
                                        jsonPath("$.data[*].id").findRandom().saveAs("liqId"));

        // TODO Hardcoded values for now, need to know where does the values come from
        public static final HttpRequestActionBuilder chargerListeBordereauPreparatoireMandat = http(
                        "Charger liste bordereaux preparatoires")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UseCaseTechnique/chargerListe")
                        .queryParam("fieldNames[]", LISTE_FIELDS)
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .body(StringBody(
                                        "{\"classePersistante\":\"BordereauPreparatoire\",\"parametresRecherche\":{\"listeCriteres\":[{\"lienClassePersistante\":\"BordereauPreparatoire\",\"lienAttribut\":\"id\",\"valeur\":0,\"operateur\":{\"_id\":8,\"_lib\":\"différent de\",\"@id\":4,\"@type\":\"RechercheOperateurEnum\"},\"@id\":3,\"@type\":\"RechercheCritere\"},{\"lienClassePersistante\":\"BordereauPreparatoire\",\"lienAttribut\":\"sens\",\"valeur\":{\"_id\":2,\"_lib\":\"Dépense\",\"@id\":6,\"@type\":\"TypeGestionSens\"},\"operateur\":4,\"@id\":5,\"@type\":\"RechercheCritere\"}],\"listeCriteresRechercheGui\":[],\"listeAttributs\":[],\"listeTris\":[{\"croissant\":true,\"lienClassePersistante\":\"BordereauPreparatoire\",\"lienAttribut\":\"code\",\"@id\":7,\"@type\":\"RechercheTri\"}],\"distinct\":false,\"paginatorValues\":{\"length\":10000,\"pageIndex\":0,\"pageSize\":10000,\"previousPageIndex\":0},\"@id\":2,\"@type\":\"RechercheParametres\"}}"))
                        .check(bodyLength().gt(0));

        // TODO make dynamic body instead of hardcoded values
        public static final HttpRequestActionBuilder chargerListeCompteUtilParModeleMvtMandat = http(
                        "Charger liste compte util par modele mvt")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Budget/chargerListeCompteUtilParModeleMvt")
                        .queryParam("fieldNames[]", "nombreTotalElements, donnees ,*.id,*.code, *.libelle")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .body(StringBody(
                                        "{\"exercice\":{\"id\":#{userContextCBE.exercice.exercice.id},\"marque\":0,\"sourceInformationRatios\":{\"_id\":1,\"_lib\":\"DGCP\",\"@id\":3,\"@type\":\"TypeSourceInformation\"},\"millesime\":#{millesime},\"budgetRef\":{\"id\":1,\"marque\":0,\"collectiviteRef\":{\"id\":1,\"marque\":0,\"@id\":5,\"@type\":\"Collectivite\"},\"@id\":4,\"@type\":\"Budget\"},\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":6,\"@type\":\"NormeComptable\"},\"comptableAssignataireRef\":{\"id\":1,\"marque\":0,\"@id\":7,\"@type\":\"ComptableAssignataire\"},\"@id\":2,\"@type\":\"ExerciceComptable\"},\"sens\":{\"_id\":2,\"_lib\":\"Dépense\",\"@id\":2,\"@type\":\"TypeGestionSens\"},\"pmmvt\":null}"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        public static final HttpRequestActionBuilder majDonneesComboNature = http(
                        "maj donnees combo nature")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcMandatTitre/majDonneesComboNature")
                        .queryParam("fieldNames[]", "")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .body(StringBody(
                                        "{\"liq\":{\"id\":17068,\"marque\":0,\"annulatif\":false,\"marchePublic\":{\"id\":0,\"marque\":0,\"@id\":3,\"@type\":\"MarchePublic\"},\"executionBudgetListe\":{\"donnees\":[{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":{\"_id\":0,\"_lib\":\"Normal\",\"@id\":6,\"@type\":\"TypeNormalOuAnnulatif\"},\"etat\":{\"_id\":4,\"_lib\":\"RIEN_A_FAIRE\",\"@id\":7,\"@type\":\"EtatEnum\"},\"reelOuOrdre\":{\"_id\":2,\"_lib\":\"Réel\",\"@id\":8,\"@type\":\"TypeGestionReelOrdre\"},\"compteUtilisateurRef\":{\"id\":1895,\"marque\":0,\"@id\":9,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":10,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":{\"_id\":1,\"_lib\":\"DGCP\",\"@id\":12,\"@type\":\"TypeSourceInformation\"},\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":13,\"@type\":\"NormeComptable\"},\"@id\":11,\"@type\":\"ExerciceComptable\"},\"@id\":5,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":1897,\"marque\":0,\"@id\":15,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":16,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":18,\"@type\":\"NormeComptable\"},\"@id\":17,\"@type\":\"ExerciceComptable\"},\"@id\":14,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":1899,\"marque\":0,\"@id\":20,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":21,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":23,\"@type\":\"NormeComptable\"},\"@id\":22,\"@type\":\"ExerciceComptable\"},\"@id\":19,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":1965,\"marque\":0,\"@id\":25,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":26,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":28,\"@type\":\"NormeComptable\"},\"@id\":27,\"@type\":\"ExerciceComptable\"},\"@id\":24,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":2263,\"marque\":0,\"@id\":30,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":31,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":33,\"@type\":\"NormeComptable\"},\"@id\":32,\"@type\":\"ExerciceComptable\"},\"@id\":29,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":2174,\"marque\":0,\"@id\":35,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":36,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":38,\"@type\":\"NormeComptable\"},\"@id\":37,\"@type\":\"ExerciceComptable\"},\"@id\":34,\"@type\":\"ExecutionBudget\"}],\"@id\":4,\"@type\":\"Association\"},\"codeNature\":{\"_id\":8,\"_lib\":\"Paie\",\"@id\":39,\"@type\":\"TypeNaturePiece\"},\"regularisationTresorerie\":false,\"pieceRecapitulative\":false,\"enPlusieursAnnees\":false,\"@id\":2,\"@type\":\"Liq\"}}"))
                        .check(jmesPath("[0].\"@id\"").ofInt().gt(0));

        // Pièces Justificatives
        public static final HttpRequestActionBuilder chargerCollectivte = http(
                        "Charger la collectivité")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcPJUtilisateur/chargerCollectivite?")
                        .queryParam("fieldNames[]", "circuitValidationPJ")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .body(StringBody(
                                        """
                                                        {"idCol":1,"elementACharger":["circuitValidationPJ"]}
                                                        """))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        public static final HttpRequestActionBuilder chargerListeBudget = http(
                        "Charger la Liste de budget")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcPJUtilisateur/chargerListeBudget?")
                        .queryParam("fieldNames[]", "*.id,*.code, *.libelle")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .body(StringBody(
                                        """
                                                        {"idCol":1}
                                                        """))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        public static final HttpRequestActionBuilder piecesJustificatives = http(
                        "Charger la Liste de pieces Justificatives")
                        .get("https://wegf-api.uat.wemagnus.com/compta/piecesJustificatives?archivee=false&transmise=false&numeroPiece=0&piecesComplementaires=false&colonnes=de_DelaiPaiementAffichage%2CetatLiquidation%2CdelaiPaiement%2Ctype%2CtiersAliasPrefCode%2CdateReception%2Cdescription%2CmontantTtc%2CidInterne%2CnomPieceJustificative%2CidUniquePes%2CcodeLibelle%2CdateTransmission%2CdateAcquittement%2Cstatut%2CidBudgetDestinatairePJ%2CcodeBudgetDestinataire%2ClibelleBudgetDestinatairePJ%2CnbPJComplementaires%2CjustificatifPaiement%2Carchivee")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("data[0].type").exists());

        // ENGAGEMENTS

        public static final HttpRequestActionBuilder operationInvestissement = http(
                        "Operation investissement")
                        .get("https://wegf-api.uat.wemagnus.com/compta/engagements/operation-investissement?exerciceId=#{userContextCBE.exercice.exercice.id}")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("data[0].id").exists());

        public static final HttpRequestActionBuilder apae = http(
                        "apae")
                        .get("https://wegf-api.uat.wemagnus.com/compta/engagements/apae?exerciceId=#{userContextCBE.exercice.exercice.id}")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("data[0].id").exists());

        public static final HttpRequestActionBuilder exerciceComptable = http(
                        "Exercice Comptable")
                        .get("https://wegf-api.uat.wemagnus.com/compta/exerciceComptable?exerciceComptableId=#{userContextCBE.exercice.exercice.id}")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("id").ofInt().gt(0));

        // TODO Query String
        public static final HttpRequestActionBuilder engagements = http(
                        "Liste Engagements")
                        .get("https://wegf-api.uat.wemagnus.com/compta/engagements?sensId=2&urgent=false&exerciceId=#{userContextCBE.exercice.exercice.id}&solde=false&echeancier=false&colonnes=numeroEngagement%2CtiersComptable.codeAliasPrefTiers%2Cobjet%2CcodeCompteUtilisateur%2CmontantHT%2CmontantTTC%2CmontantResteEngage%2Cdate")
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("data[0].id").exists());

        // Ordonnanncement
        public static final HttpRequestActionBuilder fournirListeSerieBordereauxOrdonnancement = http(
                        "Fournir liste serie bordereaux")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListeSeriesBordereaux?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                                        {"param":{"listeCriteres":[{"lienClassePersistante":"SerieBordereauLiquidation","lienAttribut":"sens","valeur":{"_id":1,"_lib":"Dépense et Recette","@id":4,"@type":"TypeGestionSens"},"operateur":{"_id":8,"_lib":"différent de","@id":5,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"},{"lienClassePersistante":"SerieBordereauLiquidation","lienAttribut":"compteursBordereauListe.exerciceComptableRef.id","valeur":#{idExerciceComptable},"operateur":{"_id":3,"_lib":"égal à","@id":7,"@type":"RechercheOperateurEnum"},"@id":6,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[{"croissant":true,"lienClassePersistante":"SerieBordereauLiquidation","lienAttribut":"annulatif","@id":8,"@type":"RechercheTri"},{"croissant":true,"lienClassePersistante":"SerieBordereauLiquidation","lienAttribut":"sens","@id":9,"@type":"RechercheTri"}],"distinct":false,"@id":2,"@type":"RechercheParametres"}}
                                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        public static final HttpRequestActionBuilder fournirListeLiquidationsCount = http(
                        "Fournir liste serie LiquidationsCount")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListeLiquidationsCount")
                        .body(StringBody(
                                        """
                                                             {"selCrit":{"id":-1,"marque":0,"exercice":{"id":#{idExerciceComptable},"marque":0,"sourceInformationRatios":{"_id":1,"_lib":"DGCP","@id":4,"@type":"TypeSourceInformation"},"@id":3,"@type":"ExerciceComptable"},"mandatTitre":{"id":0,"marque":0,"@id":5,"@type":"MandatTitre"},"operateurSelMandat":{"_id":259,"_lib":"égal à or Null","@id":6,"@type":"RechercheOperateurEnum"},"typeMouvement":{"_id":3,"_lib":"Tous","@id":7,"@type":"TypeMouvementATraiter"},"@id":2,"@type":"SelectionLiquidation"}}
                                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"));

        /**
         * Same count, re-asked once the user has picked a serie: the screen adds {@code serieBord}
         * and renumbers the two {@code @id}s after it. Same request name as the call above, which
         * is what the browser does too — one endpoint, two bodies.
         */
        public static final HttpRequestActionBuilder fournirListeLiquidationsCountSerie = http(
                        "Fournir liste serie LiquidationsCount")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListeLiquidationsCount")
                        .body(StringBody(
                                        """
                                                             {"selCrit":{"id":-1,"marque":0,"exercice":{"id":#{idExerciceComptable},"marque":0,"sourceInformationRatios":{"_id":1,"_lib":"DGCP","@id":4,"@type":"TypeSourceInformation"},"@id":3,"@type":"ExerciceComptable"},"mandatTitre":{"id":0,"marque":0,"@id":5,"@type":"MandatTitre"},"serieBord":{"id":4,"marque":0,"@id":6,"@type":"SerieBordereauLiquidation"},"operateurSelMandat":{"_id":259,"_lib":"égal à or Null","@id":7,"@type":"RechercheOperateurEnum"},"typeMouvement":{"_id":3,"_lib":"Tous","@id":8,"@type":"TypeMouvementATraiter"},"@id":2,"@type":"SelectionLiquidation"}}
                                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"));

        public static final HttpRequestActionBuilder chargerConfigEditionPiecePourBudget = http(
                        "Charger chargerConfigEditionPiecePourBudget")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcMandatTitre/chargerConfigEditionPiecePourBudget?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                                        {"idBudget":#{idBudget}}
                                                         """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        public static final HttpRequestActionBuilder fournirListeLiquidations = http(
                        "Fournir liste serie Liquidations")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListeLiquidations?fieldNames%5B%5D=!liquidationRef.executionBudgetListe%20!liquidationRecetteRef.executionBudgetListe")
                        .body(StringBody(
                                        """
                                                             {"selCrit":{"id":-1,"marque":0,"exercice":{"id":#{idExerciceComptable},"marque":0,"sourceInformationRatios":{"_id":1,"_lib":"DGCP","@id":4,"@type":"TypeSourceInformation"},"@id":3,"@type":"ExerciceComptable"},"mandatTitre":{"id":0,"marque":0,"@id":5,"@type":"MandatTitre"},"serieBord":{"id":4,"marque":0,"@id":6,"@type":"SerieBordereauLiquidation"},"operateurSelMandat":{"_id":259,"_lib":"égal à or Null","@id":7,"@type":"RechercheOperateurEnum"},"typeMouvement":{"_id":3,"_lib":"Tous","@id":8,"@type":"TypeMouvementATraiter"},"@id":2,"@type":"SelectionLiquidation"}}
                                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jsonPath("$.donnees[0].id").ofInt().gt(0))
                        .check(jsonPath("$.donnees[*].id").findAll().saveAs("idLiquidation"))
                        // The grid the user starts from. Sent back whole as liquidationDepartListe when
                        // the bordereau is created, so the response is kept rather than rebuilt.
                        .check(bodyString().saveAs("liquidationsJson"))
                        // Every row, kept as raw JSON so choisirLiquidations can hand a subset of them
                        // straight back to the numerotation calls.
                        .check(jsonPath("$.donnees[*]").findAll().saveAs("liquidationRows"));

        /**
         * Ticks 1 to 3 liquidations, drawn per iteration, and builds the
         * {@code donnees} array
         * that the numerotation calls send back ({@code selectionRowsJson}) along with
         * how many
         * were taken ({@code nbSelection}).
         *
         * <p>
         * Takes them from the top of the grid, and never leaves row 0 out. Row 0 is the
         * one
         * that spells out the enum objects shared by the whole list ({@code etat},
         * {@code codeNature}, {@code statut}...); the later rows only point at them by
         * {@code @id}. Any selection that includes row 0 is therefore self-contained
         * and its
         * rows can be concatenated untouched — verified over every 1-to-3 row
         * combination.
         * Leave row 0 out and the others arrive with dangling references.
         */
        public static final ChainBuilder choisirLiquidations = exec(session -> {
                List<String> rows = session.getList("liquidationRows");
                int nb = Math.min(ThreadLocalRandom.current().nextInt(1, 4), rows.size());
                return session
                                .set("nbSelection", nb)
                                .set("selectionRowsJson", String.join(",", rows.subList(0, nb)));
        });

        public static final HttpRequestActionBuilder fournirListeBordereauxPreparatoire = http(
                        "Fournir liste bordereaux preparatoire")
                        .post(
                                        "https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListeBordereauxPreparatoire?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                                             {"param":{"listeCriteres":[{"lienClassePersistante":"BordereauPreparatoire","lienAttribut":"liquidationListe.serieBordereauLiquidationRef.id","valeur":4,"operateur":{"_id":3,"_lib":"égal à","@id":4,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[],"distinct":false,"@id":2,"@type":"RechercheParametres"}}
                                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        /**
         * Sizes the PES flux for one liquidation. The screen calls it once per ticked
         * row, so the
         * body reads the {@code indexLiquidation} counter of the surrounding repeat
         * rather than a
         * fixed index — see {@code OrdonnancementSelectionLiquidationsGroup}.
         */
        public static final HttpRequestActionBuilder fournirTailleLiquidationPourPES = http(
                        "Fournir taille liquidation pour PES")
                        .post(
                                        "https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirTailleLiquidationPourPES?fieldNames%5B%5D=**")
                        .body(StringBody(session -> "{\"liquidationId\":"
                                        + session.getList("idLiquidation").get(session.getInt("indexLiquidation"))
                                        + ",\"idBudget\":" + session.getInt("idBudget") + "}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        public static final HttpRequestActionBuilder fournirListeBordereauxAvecMontant = http(
                        "Fournir liste bordereaux avec montant")
                        .post(
                                        "https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListeBordereauxAvecMontant?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
{"param":{"listeCriteres":[{"lienClassePersistante":"Bordereau","lienAttribut":"serieBordereauLiquidationRef.id","valeur":4,"operateur":{"_id":3,"_lib":"égal à","@id":4,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"},{"lienClassePersistante":"Bordereau","lienAttribut":"millesime","valeur":2026,"operateur":4,"@id":5,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[],"distinct":false,"paginatorValues":{"length":1000,"pageIndex":0,"pageSize":1000,"previousPageIndex":0},"@id":2,"@type":"RechercheParametres"},"search":null,"sortValue":{"active":"numeroBordereau","direction":"desc"}}
                                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0))
                        // Every row, kept as raw JSON: EditionBordereauApiEndpoints.choisirBordereaux
                        // ticks a few of them and hands them straight back to the edition calls.
                        // (`idBordereaux` is the whole grid; `bordereauId` in
                        // NumerotationApiEndpoints is the single bordereau that flow creates.)
                        .check(jsonPath("$.donnees[*].id").findAll().saveAs("idBordereaux"))
                        .check(jsonPath("$.donnees[*]").findAll().saveAs("bordereauRows"));

}
