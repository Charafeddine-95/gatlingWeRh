package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.sse;

import io.gatling.javaapi.core.ActionBuilder;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import io.gatling.javaapi.http.SseConnectActionBuilder;
import org.jspecify.annotations.NonNull;

/** Referential data (configuration and context) served by the WeRH API. */
public final class ReferentialApiEndpoints {

    private ReferentialApiEndpoints() {
    }

    private static final String WERH_API = "https://werh-api.uat.wemagnus.com";

    /** WeRH application configuration for the ROLE_AGENT profile. */
    public static final HttpRequestActionBuilder agentConfiguration =
            http("Agent configuration")
                    .get(WERH_API + "/config/configuration/ROLE_AGENT")
                    .headers(ApiHeaders.bearerForAllTenants());

    /** Collectivites (local authorities) the connected user can work on. */
    public static final HttpRequestActionBuilder collectivites =
            http("Collectivites")
                    .get(WERH_API + "/context/v1/collectivite")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

    /** Etablissements (sites) the connected user can work on. */
    public static final HttpRequestActionBuilder etablissements =
            http("Etablissements")
                    .get(WERH_API + "/context/v1/etablissement")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));

    public static final HttpRequestActionBuilder contract =
            http("Contrat")
                    .get(WERH_API + "/career/bff/dossier-agent/2026-06-01/contract?filters=%7B%22activite%22%3A%221%22%7D")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));

    public static final HttpRequestActionBuilder resume =
            http("resume")
                    .get(WERH_API + "/pay/cycle-paie/resume")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));

    public static final HttpRequestActionBuilder monthPay =
            http("month pay by etablissment")
                    .get(WERH_API + "/pay/cycle-paie/month-pay-by-etablissement")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));


    public static final HttpRequestActionBuilder contratActif =
            http("Contrat actif")
                    .post(WERH_API + "/pay/cycle-paie/contrat-actif?month=2026-01-01")
                    .body(StringBody("65d8a498-d9c1-40ef-9648-8f90731ce92b"))
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));

    public static final HttpRequestActionBuilder agentUnpaid =
            http("Agent unpaid")
                    .get(WERH_API + "/pay/cycle-paie/agent-unpaid?month=2026-01-01")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));


    public static final HttpRequestActionBuilder contractPayCycle =
            http("Contrat cycle de paie")
                    .get(WERH_API + "/career/bff/cycle-paie/2026-01-01/preparer/controler-bulletin/contrat?filters=%7B%22activite%22%3A%22")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));

    public static final HttpRequestActionBuilder dateRange =
            http("date range")
                    .post(WERH_API + "/pay/cycle-paie/date-range")
                    .body(StringBody("65d8a498-d9c1-40ef-9648-8f90731ce92b"))
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));

    public static final HttpRequestActionBuilder fonction =
            http("fonction")
                    .get(WERH_API + "/career/fonction")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));

    public static final HttpRequestActionBuilder service =
            http("service")
                    .get(WERH_API + "/context/v1/service")
                    .headers(ApiHeaders.bearerWithTenant("accept", "application/json","user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"));

    // --- Server-Sent Events (SSE) -------------------------------------------------

    /**
     * Opens the payroll-computation SSE stream. Unlike a plain GET, the connection
     * stays open and the server pushes events, so we use Gatling's SSE DSL:
     * open the stream, then wait up to 10s for a message whose "calculStatus"
     * equals "DONE". The relative URL resolves against the protocol baseUrl.
     *
     * <p>Add authenticated headers if the endpoint requires them, e.g.:
     * {@code .headers(ApiHeaders.bearerWithTenant("accept", "text/event-stream"))}
     */
    public static final @NonNull ChainBuilder calculPaie =
            exec(sse("Calcul paie")
                    .sseName("bulletinsSynthese")
                    .get("/pay/paie/bulletins/calcul/stream-info?month=2026-01-01&etablissementIds=65d8a498-d9c1-40ef-9648-8f90731ce92b")
                    .headers(ApiHeaders.bearerForAllTenants())
                    .await(10).on(
                            sse.checkMessage("check calcul status")
                                    .check(jmesPath("calculStatus").is("DONE"))));

    /** Closes the SSE stream opened by {@link #calculPaie}. */
    public static final ActionBuilder calculPaieClose =
            sse("Close").close();

}
