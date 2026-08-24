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

    public static final HttpRequestActionBuilder chargerFieldsGrandlivre = http("Charger fields grand livre")
            .post("https://wegf-api.uat.wemagnus.com/compta/UseCaseTechnique/charger2?fieldNames%5B%5D=id,%20millesime,%20budgetRef,*.id,*.collectiviteRef,*.collectiviteRef.id,%20normeComptableRef\n")
            .body(StringBody(
                    "{\"classePersistante\":\"ExerciceComptable\",\"id\":#{userContextCBE.exercice.exercice.id},\"elementACharger\":[\"budgetRef.collectiviteRef\",\"normeComptableRef\"]}"))
            .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
            .check(jmesPath("budgetRef.id").ofInt().gt(0),
                  jmesPath("budgetRef.id").saveAs("IdbudgetRef"));



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

}