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
public final class EditionApiEndpoints {
    private EditionApiEndpoints() {
    }

    //Grand livre

    // TODO check if same id and millesime in usercontextcbe
    public static final HttpRequestActionBuilder chargerFieldsGrandlivre = http("Charger fields grand livre")
            .post("https://wegf-api.uat.wemagnus.com/compta/UseCaseTechnique/charger2?fieldNames%5B%5D=id,%20millesime,%20budgetRef,*.id,*.collectiviteRef,*.collectiviteRef.id,%20normeComptableRef\n")
            .body(StringBody(
                    "{\"classePersistante\":\"ExerciceComptable\",\"id\":#{userContextCBE.exercice.exercice.id},\"elementACharger\":[\"budgetRef.collectiviteRef\",\"normeComptableRef\"]}"))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("budgetRef.id").ofInt().gt(0),
                  jmesPath("budgetRef.id").saveAs("IdbudgetRef"),
                  jmesPath("millesime").saveAs("millesime"));



    public static final HttpRequestActionBuilder chargerListeCollectiviteByCritereLieBudget = http("charger ListeCollectiviteByCritereLieBudget")
            .post("https://wegf-api.uat.wemagnus.com/compta/Collectivite/chargerListeCollectiviteByCritereLieBudget?fieldNames%5B%5D=*.id,%20*.code,*.libelle\n")
            .body(StringBody(
                    """
                            {"param":{"listeCriteres":[],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[],"distinct":false,"@id":2,"@type":"RechercheParametres"},"elements":null,"isAfficherUniquementColSuiviesBudget":true}
                            """))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("donnees[0].id").ofInt().gt(0));



    public static final HttpRequestActionBuilder chargerListeBudget = http("charger ListeBudget")
            .post("https://wegf-api.uat.wemagnus.com/compta/Budget/chargerListeBudget?fieldNames%5B%5D=*.id,%20*.code,*.libelle,*.collectiviteRef,*.collectiviteRef.id\n")
            .body(StringBody(
                    """
                            {"param":{"listeCriteres":[],"listeCriteresRechercheGui":[{"lienClassePersistante":"Budget","lienAttribut":"collectiviteRef.id","valeur":1,"operateur":{"_id":3,"_lib":"égal à","@id":4,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"}],"listeAttributs":[],"listeTris":[],"distinct":false,"@id":2,"@type":"RechercheParametres"}}
                            """))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("donnees[0].id").ofInt().gt(0));


    public static final HttpRequestActionBuilder chargerListe = http("chargerListe")
            .post("https://wegf-api.uat.wemagnus.com/compta/UseCaseTechnique/chargerListe?fieldNames%5B%5D=donnees,*.millesime,%20*.id\n")
            .body(StringBody(
                    """
                            {"classePersistante":"ExerciceComptable","parametresRecherche":{"listeCriteres":[],"listeCriteresRechercheGui":[{"lienClassePersistante":"ExerciceComptable","lienAttribut":"budgetRef.id","valeur":#{IdbudgetRef},"operateur":{"_id":3,"_lib":"égal à","@id":4,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"}],"listeAttributs":[],"listeTris":[{"croissant":true,"lienClassePersistante":"ExerciceComptable","lienAttribut":"millesime","@id":5,"@type":"RechercheTri"}],"distinct":false,"paginatorValues":{"length":10000,"pageIndex":0,"pageSize":10000,"previousPageIndex":0},"@id":2,"@type":"RechercheParametres"}}
                            
                            """))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("donnees[0].id").ofInt().gt(0));


            // TODO check if there can be multiple ids
            public static final HttpRequestActionBuilder fournirListeElementAnalytiqueParListeBudget = http("fournirListeElementAnalytiqueParListeBudget")
            .post("https://wegf-api.uat.wemagnus.com/compta/UcEditionCommun/fournirListeElementAnalytiqueParListeBudget?fieldNames%5B%5D=**")
            .body(StringBody(
                    """
                        {"idsBudget":[#{IdbudgetRef}],"priorite":1,"millesimeFinValid":#{millesime}}
                            """))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("donnees").exists());


            public static final HttpRequestActionBuilder fournirListeElementAnalytiqueParListeBudget2 = http("fournirListeElementAnalytiqueParListeBudget2")
            .post("https://wegf-api.uat.wemagnus.com/compta/UcEditionCommun/fournirListeElementAnalytiqueParListeBudget?fieldNames%5B%5D=**")
            .body(StringBody(
                    """
                        {"idsBudget":[#{IdbudgetRef}],"priorite":2,"millesimeFinValid":#{millesime}}
                            """))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("donnees").exists());

            public static final HttpRequestActionBuilder chargerVisibilitecolonne = http("chargerVisibilitecolonne")
            .post("https://wegf-api.uat.wemagnus.com/compta/Baocomptabilite/chargerVisibilitecolonne?fieldNames%5B%5D=**")
            .body(StringBody(
                    """
                {"idExercice":#{userContextCBE.exercice.exercice.id}}
                            """))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("visibiliteAxe1").exists());


            // Check where does millesime fin comes from and if he sometimes is different from millesime
            public static final HttpRequestActionBuilder fournirDonneesMultiCollectivite = http("chargerVisibilitecolonne")
            .post("https://wegf-api.uat.wemagnus.com/compta/UcEditionCommun/fournirDonneesMultiCollectivite?fieldNames%5B%5D=tableauInfoErreur,%20nbLignesAAffficher,%20nbLignesMax,%20limiteAtteinte,%20tableauDonnee,%20*.donnees,%20*.donnees.etat.**,%20*.donnees.idBudget.**,%20*.donnees.idEtapeBudget.**,%20*.donnees.idPrevisionBudget.**,*.donnees.codeCollectivite.**,*.donnees.libelleCollectivite.**,*.donnees.codeBudget.**,*.donnees.libBudget.**,%20*.donnees.idLiquidation,%20*.donnees.idEngagement,%20*.donnees.sens.**,,%20*.donnees.compte.**,,%20*.donnees.millesimeCourant.**,,%20*.donnees.date.**,,%20*.donnees.type.**,,%20*.donnees.objet.**,,%20*.donnees.serieBordereauLiquidation.**,,%20*.donnees.numBordereau.**,,%20*.donnees.numPiece.**,,%20*.donnees.numEngagement.**,,%20*.donnees.section.**,,%20*.donnees.chapitre.**,,%20*.donnees.imputation.**,,%20*.donnees.tiersComptable.**,,%20*.donnees.totalRV.**,,%20*.donnees.engage.**,,%20*.donnees.resteEngage.**,,%20*.donnees.liquide.**,,%20*.donnees.realise.**,,%20*.donnees.idCollectivite.**,,%20*.donnees.idBudget.**,,%20*.donnees.idEtapeBudget.**,,%20*.donnees.codeCollectivite.**,,%20*.donnees.libelleCollectivite.**,,%20*.donnees.codeCompte.**,,%20*.donnees.codeBudget.**,,%20*.donnees.libBudget.**,,%20*.donnees.idPrevisionBudget.**")
            .body(StringBody(
                    """
{"criteres":{"id":-1,"marque":0,"eliminerEngagementSolde":true,"exclurePrevisionLieesHypothesesSurExerciceCourant":true,"exclurePrevisionLieesHypothesesSurExercicesAnterieurs":true,"nbExercicesATraiter":0,"recupererLignesExecutions":true,"recupererLignesPrevisions":true,"section":{"_id":1,"_lib":"Fonctionnement et Investissement","libelleCollectivite":"Fonctionnement et Investissement","libelleEHPAD":"Exploitation et Investissement","@id":3,"@type":"TypeGestionSections"},"sens":{"_id":1,"_lib":"Dépense et Recette","@id":4,"@type":"TypeGestionSens"},"type":{"_id":4,"_lib":"Réalisé et prévu","@id":5,"@type":"Type"},"typeIb":{"_id":3,"_lib":"Réel et Ordre","@id":6,"@type":"TypeIB"},"typePiece":{"_id":1,"_lib":"Toutes pièces","@id":7,"@type":"TypePiece"},"utiliserEAFilsSiEAClassement":true,"traiterTousBudgetsDeCollectivite":false,"traiterToutesCollectivites":false,"paginatorValues":{"length":10000,"pageIndex":0,"pageSize":10000,"previousPageIndex":0},"millesimeDebut":#{millesime},"millesimeFin":#{millesime},"tiers":null,"compteUtilisateur":null,"fonctionUtilisateur":null,"operation":null,"@id":2,"@type":"CriteresSelectionSituation"},"colonnesAffichees":["sens","compte","millesimeCourant","date","type","objet","serieBordereauLiquidation","numBordereau","numPiece","numEngagement","section","chapitre","imputation","tiersComptable","totalRV","engage","resteEngage","liquide","realise","idCollectivite","idBudget","idEtapeBudget","codeCollectivite","libelleCollectivite","codeCompte","codeBudget","libBudget","idPrevisionBudget"],"type":{"_id":2,"_lib":"Grand Livre par chapitre","@id":2,"@type":"TypeUtilisationEditionCommun"},"listBudget":{"donnees":[{"id":#{IdbudgetRef},"marque":0,"collectiviteRef":{"id":#{userContextCBE.exercice.collectivite.id},"marque":0,"@id":4,"@type":"Collectivite"},"@id":3,"@type":"Budget"}],"@id":2,"@type":"TableauEntitePersistante"}}
                            """))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("tableauDonnee.donnees").exists());


}