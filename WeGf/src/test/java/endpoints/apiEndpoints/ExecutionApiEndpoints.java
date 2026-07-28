package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

/** Notification API calls. */
public final class ExecutionApiEndpoints {
    private ExecutionApiEndpoints(){}


    /** Loads the connected user's IAM profile for the selected tenant. */
    public static final HttpRequestActionBuilder title =
            http("Visiter page titre")
                    .post("https://wegf-api.uat.wemagnus.com/compta/UcExerciceComptable/chargerExerciceComptable?fieldNames%5B%5D=**")
                        .body(StringBody("{\"id\":#{userContextCBE.exercice.exercice.id},\"element\":[\"budgetRef\",\"budgetRef.collectiviteRef\",\"comptableAssignataireRef\",\"normeComptableRef\"]}"))
                   .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                    .check(jsonPath("$.budgetRef.id").ofInt().gt(0));

}