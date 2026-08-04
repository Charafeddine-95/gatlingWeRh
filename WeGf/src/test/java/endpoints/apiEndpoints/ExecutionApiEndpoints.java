package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;
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
                                        jmesPath("millesime").saveAs("millesime"));

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
                                        "{\"classePersistante\":\"OperationdInvestissement\",\"parametresRecherche\":{\"listeCriteres\":[{\"lienClassePersistante\":\"OperationdInvestissement\",\"lienAttribut\":\"budgetRef\",\"valeur\":{\"id\":1,\"marque\":0,\"collectiviteRef\":{\"id\":1,\"marque\":0,\"@id\":5,\"@type\":\"Collectivite\"},\"@id\":4,\"@type\":\"Budget\"},\"operateur\":{\"_id\":3,\"_lib\":\"égal à\",\"@id\":6,\"@type\":\"RechercheOperateurEnum\"},\"@id\":3,\"@type\":\"RechercheCritere\"},{\"lienClassePersistante\":\"OperationdInvestissement\",\"lienAttribut\":\"exerciceDebut\",\"valeur\":#{millesime},\"operateur\":{\"_id\":7,\"_lib\":\"inférieur ou égal à\",\"@id\":8,\"@type\":\"RechercheOperateurEnum\"},\"@id\":7,\"@type\":\"RechercheCritere\"},{\"lienClassePersistante\":\"OperationdInvestissement\",\"lienAttribut\":\"exerciceFin\",\"valeur\":2026,\"operateur\":{\"_id\":261,\"_lib\":\"supérieur ou égal à or Null\",\"@id\":10,\"@type\":\"RechercheOperateurEnum\"},\"@id\":9,\"@type\":\"RechercheCritere\"}],\"listeCriteresRechercheGui\":[],\"listeAttributs\":[],\"listeTris\":[{\"croissant\":true,\"lienClassePersistante\":\"OperationdInvestissement\",\"lienAttribut\":\"code\",\"@id\":11,\"@type\":\"RechercheTri\"}],\"distinct\":false,\"paginatorValues\":{\"length\":10000,\"pageIndex\":0,\"pageSize\":10000,\"previousPageIndex\":0},\"@id\":2,\"@type\":\"RechercheParametres\"}}"))
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

        // TODO : Randomize the liqID we pick instead of picking the first one of the
        // response
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
                                        jsonPath("$.donnees[0].id").findRandom().saveAs("liqId"));

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
}
