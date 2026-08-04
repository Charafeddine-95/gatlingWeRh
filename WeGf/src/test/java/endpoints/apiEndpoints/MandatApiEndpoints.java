package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;
import static io.gatling.javaapi.core.CoreDsl.*;

public class MandatApiEndpoints {

    private MandatApiEndpoints() {
    }

    // id":17068 should be fetched from a previous request
    public static final HttpRequestActionBuilder chargerLiqComplet = http(
            "Charger Liq Complet")
            .post("https://wegf-api.uat.wemagnus.com/compta/UcMandatTitre/chargerLiqComplet?fieldNames%5B%5D=**,!**.executionBudgetListe.**.exerciceComptableRef.de_Axe1,!**.executionBudgetListe.**.exerciceComptableRef.de_Axe2")
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .body(StringBody(
                    """
                                {"id":17068,"idExercice":#{userContextCBE.exercice.exercice.id},"type":{"_id":2,"_lib":"Dépense","@id":2,"@type":"TypeGestionSens"},"liquidationAreduire":false,"liquidationSimple":false,"serieBordereauLiquidationId":null,"bordereauPreparatoireId":null,"objet":null,"recrediterEngagement":false,"pjSelectionneeListe":{"donnees":[],"@id":2,"@type":"Association"},"annulerSurExSuivant":false}
                            """))
            .check(jmesPath("type._id").ofInt().gt(0));

    public static final HttpRequestActionBuilder chargerAxeAnalitique = http(
            "Charger Axe Analitique")
            .post("https://wegf-api.uat.wemagnus.com/compta/UcMandatTitre/chargerAxeAnalitique?fieldNames%5B%5D=**")
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .body(StringBody(
                    """
                                {"idExercice":#{userContextCBE.exercice.exercice.id},"priorite":1}
                            """))
            .check(jmesPath("etat._id").ofInt().gt(0));

    // TODO Make sure all value are static, or where do we get them
    public static final HttpRequestActionBuilder chargerListeObjetExecution = http(
            "Charger Liste Objet Execution")
            .post("https://wegf-api.uat.wemagnus.com/compta/UcGestionObjetExecution/chargerListeObjetExecution?fieldNames%5B%5D=**")
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .body(StringBody(
                    """
                            {"param":{"listeCriteres":[{"lienClassePersistante":"ObjetExecution","lienAttribut":"id","valeur":0,"operateur":{"_id":8,"_lib":"différent de","@id":4,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[{"croissant":true,"lienClassePersistante":"ObjetExecution","lienAttribut":"libelle","@id":5,"@type":"RechercheTri"}],"distinct":false,"paginatorValues":{"length":1000,"pageIndex":0,"pageSize":1000,"previousPageIndex":0},"@id":2,"@type":"RechercheParametres"}}
                                                    """))
            .check(jmesPath("donnees").exists());

    public static final HttpRequestActionBuilder activiteAFinancer = http(
            "Activite a financer")
            .get("https://wegf-api.uat.wemagnus.com/compta/wegfActiviteAFinancer/isCodificationAutoActivePourImmo?idCollectivite=1")
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .body(StringBody(
                    """
                            {"param":{"listeCriteres":[{"lienClassePersistante":"ObjetExecution","lienAttribut":"id","valeur":0,"operateur":{"_id":8,"_lib":"différent de","@id":4,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[{"croissant":true,"lienClassePersistante":"ObjetExecution","lienAttribut":"libelle","@id":5,"@type":"RechercheTri"}],"distinct":false,"paginatorValues":{"length":1000,"pageIndex":0,"pageSize":1000,"previousPageIndex":0},"@id":2,"@type":"RechercheParametres"}}
                                                    """))
            .check(bodyString().in("true", "false"));

    public static final HttpRequestActionBuilder fournirListeSerieBordereauxMandatOuvert = http(
            "Fournir liste serie bordereaux pour manda ouvert")
            .post("https://wegf-api.uat.wemagnus.com/compta/UcMandatTitre/fournirListeSeriesBordereaux?fieldNames%5B%5D=nombreTotalElements,%20donnees%20,*.id,*.code,%20*.libelle")
            .body(StringBody(
                    """
                            {"exercice":#{userContextCBE.exercice.exercice.id},"sens":{"_id":2,"_lib":"Dépense","@id":2,"@type":"TypeGestionSens"},"annulatif":false,"defaut":null,"elementsACharger":["code","libelle","compteursBordereauListe"]}
                            """))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("donnees[0].id").ofInt().gt(0));

    public static final HttpRequestActionBuilder executionsBudget = http(
            "Executions budget")
            .get("https://wegf-api.uat.wemagnus.com/compta/liquidations/17068/executions-budgets")
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("data[0].attributes.annuleeTotalement").exists());

    public static final HttpRequestActionBuilder chargerFieldNames = http(
            "Charger field names")
            .post("https://wegf-api.uat.wemagnus.com/compta/UseCaseTechnique/charger1?fieldNames%5B%5D=**")
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .body(StringBody(
                    """
                            {"classePersistante":"Tva","id":-20}
                                    """))
            .check(jmesPath("etatPrecedent").exists());

    public static final HttpRequestActionBuilder fournirListeServiceTCParBudget = http(
            "Charger la fiche des mandats")
            .post("https://wegf-api.uat.wemagnus.com/compta/UcMandatTitre/fournirListeServiceTCParBudget?fieldNames%5B%5D=**")
            .queryParam("fieldNames[]", "**")
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .body(StringBody(
                    """
                            {"idBudget": 1}
                            """))
            .check(jmesPath("\"@id\"").ofInt().gt(0));

    public static final HttpRequestActionBuilder majDonneesComboNature = http(
            "maj donnees combo nature")
            .post("https://wegf-api.uat.wemagnus.com/compta/UcMandatTitre/majDonneesComboNature")
            .queryParam("fieldNames[]", "")
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .body(StringBody(
                    "{\"liq\":{\"id\":#{liqId},\"marque\":0,\"annulatif\":false,\"marchePublic\":{\"id\":0,\"marque\":0,\"@id\":3,\"@type\":\"MarchePublic\"},\"executionBudgetListe\":{\"donnees\":[{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":{\"_id\":0,\"_lib\":\"Normal\",\"@id\":6,\"@type\":\"TypeNormalOuAnnulatif\"},\"etat\":{\"_id\":4,\"_lib\":\"RIEN_A_FAIRE\",\"@id\":7,\"@type\":\"EtatEnum\"},\"reelOuOrdre\":{\"_id\":2,\"_lib\":\"Réel\",\"@id\":8,\"@type\":\"TypeGestionReelOrdre\"},\"compteUtilisateurRef\":{\"id\":1895,\"marque\":0,\"@id\":9,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":10,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":{\"_id\":1,\"_lib\":\"DGCP\",\"@id\":12,\"@type\":\"TypeSourceInformation\"},\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":13,\"@type\":\"NormeComptable\"},\"@id\":11,\"@type\":\"ExerciceComptable\"},\"@id\":5,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":1897,\"marque\":0,\"@id\":15,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":16,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":18,\"@type\":\"NormeComptable\"},\"@id\":17,\"@type\":\"ExerciceComptable\"},\"@id\":14,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":1899,\"marque\":0,\"@id\":20,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":21,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":23,\"@type\":\"NormeComptable\"},\"@id\":22,\"@type\":\"ExerciceComptable\"},\"@id\":19,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":1965,\"marque\":0,\"@id\":25,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":26,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":28,\"@type\":\"NormeComptable\"},\"@id\":27,\"@type\":\"ExerciceComptable\"},\"@id\":24,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":2263,\"marque\":0,\"@id\":30,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":31,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":33,\"@type\":\"NormeComptable\"},\"@id\":32,\"@type\":\"ExerciceComptable\"},\"@id\":29,\"@type\":\"ExecutionBudget\"},{\"id\":-1,\"marque\":0,\"typeNormalOuAnnulatif\":6,\"etat\":7,\"reelOuOrdre\":8,\"compteUtilisateurRef\":{\"id\":2174,\"marque\":0,\"@id\":35,\"@type\":\"CompteUtilisateur\"},\"activiteAFinancer\":{\"id\":12,\"marque\":0,\"@id\":36,\"@type\":\"ActiviteAFinancer\"},\"exerciceComptableRef\":{\"id\":25,\"marque\":0,\"sourceInformationRatios\":12,\"normeComptableRef\":{\"id\":-310,\"marque\":0,\"@id\":38,\"@type\":\"NormeComptable\"},\"@id\":37,\"@type\":\"ExerciceComptable\"},\"@id\":34,\"@type\":\"ExecutionBudget\"}],\"@id\":4,\"@type\":\"Association\"},\"codeNature\":{\"_id\":8,\"_lib\":\"Paie\",\"@id\":39,\"@type\":\"TypeNaturePiece\"},\"regularisationTresorerie\":false,\"pieceRecapitulative\":false,\"enPlusieursAnnees\":false,\"@id\":2,\"@type\":\"Liq\"}}"))
            .check(jmesPath("[0].\"@id\"").ofInt().gt(0));
}
