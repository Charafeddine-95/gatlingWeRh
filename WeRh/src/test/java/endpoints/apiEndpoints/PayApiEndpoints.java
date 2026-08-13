package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.regex;
import static io.gatling.javaapi.core.CoreDsl.substring;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.sse;
import static io.gatling.javaapi.http.HttpDsl.status;
import static io.gatling.javaapi.core.CoreDsl.bodyLength;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.util.Map;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/**
 * Pay API calls used by WeRH pages: pay assistant, bulletin control and
 * recompute.
 */
public final class PayApiEndpoints {

        private PayApiEndpoints() {
        }

        private static final String WERH_API = "https://werh-api.uat.wemagnus.com";

        /** Bulletin-control list filter, e.g. {"activite":"1"} (URL-encoded). */
        private static final String CONTROLE_BULLETIN_FILTERS = encode("{\"activite\":\"1\"}");

        /**
         * A single-etablissement request body: a JSON array holding the active
         * etablissement id.
         */

        // TODO TEST
        private static final String ETABLISSEMENT_BODY = "#{etablissementIds.jsonStringify()}";

        private static String encode(String value) {
                return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        public static final ChainBuilder computeNextMonth = exec(session -> {
                String ouvert = session.getString("payOnlyMonth");
                String cloture = session.getString("closedOnlyMonth");
                if (ouvert == null || !ouvert.equals(cloture)) {
                        return session.set("moisOuvertTrue", ouvert);
                }
                try {
                        return session.set("nextMonth", YearMonth.parse(ouvert).plusMonths(1).toString());
                } catch (DateTimeParseException e) {
                        return session.markAsFailed();
                }
        });

        public static final HttpRequestActionBuilder cycleResume = http("Payroll cycle resume")
                        .get(WERH_API + "/pay/cycle-paie/resume")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                        .check(
                                        jsonPath("$[*]").ofMap().findAll().saveAs("cycles"),
                                        jsonPath("$[*]").ofMap().findRandom().saveAs("activeCycle"));

        /**
         * Current payroll-cycle summary, and the correlation anchor of the pay journey:
         * it saves the
         * active etablissement id ("etablissementId"), the open payroll month as a full
         * date
         * ("payMonth", e.g. "2026-02" -> "2026-02-01"), the last closed month
         * ("closedMonth", used by
         * the "visualiser les bulletins" journey) and the open cycle's id
         * ("cyclePaieId") that
         * downstream pay calls reuse — cyclePaieId notably feeds the agent payslip
         * stream.
         */
        public static final ChainBuilder spreadActiveCycle = exec(session -> {
                Map<String, Object> cycle = session.getMap("activeCycle");
                String ouvert = (String) cycle.get("dernierMoisOuvert");
                String cloture = (String) cycle.get("dernierMoisCloture");
                Session updated = session
                                .set("etablissementId", cycle.get("etablissementId"))
                                .set("cyclePaieId", cycle.get("cyclePaieId"))
                                .set("dernierMoisOuvert", ouvert)
                                .set("dernierMoisCloture", cloture)
                                .set("payOnlyMonth", ouvert)
                                .set("closedOnlyMonth", cloture);
                if (ouvert != null) {
                        updated = updated.set("payMonth", ouvert + "-01");
                }
                if (cloture != null) {
                        updated = updated.set("closedMonth", cloture + "-01");
                }
                return updated;
        });

        /** Per-etablissement payroll-cycle status for the open month. */
        public static final HttpRequestActionBuilder cycleStatus = http("Payroll cycle status")
                        .post(WERH_API + "/pay/cycle-paie/status?mois=#{payMonth}")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json", "content-type",
                                        "application/json"))
                        .body(StringBody(ETABLISSEMENT_BODY))
                        .check();

        /** Month pay summary by etablissement. */
        public static final HttpRequestActionBuilder monthPayByEtablissement = http("Month pay by etablissement")
                        .post(WERH_API + "/pay/cycle-paie/month-pay-by-etablissement")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json", "content-type",
                                        "application/json"))
                        .body(StringBody(ETABLISSEMENT_BODY));

        /** Active contracts for the open month. */
        public static final HttpRequestActionBuilder contratActif = http("Active contracts")
                        .post(WERH_API + "/pay/cycle-paie/contrat-actif?month=#{payMonth}")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json", "content-type",
                                        "application/json"))
                        .body(StringBody(ETABLISSEMENT_BODY));

        /** Unpaid agents for the open month. */
        public static final HttpRequestActionBuilder agentUnpaid = http("Unpaid agents")
                        .post(WERH_API + "/pay/cycle-paie/agent-unpaid?month=#{payMonth}")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json", "content-type",
                                        "application/json"))
                        .body(StringBody(ETABLISSEMENT_BODY));

        /**
         * Career events for the open month (the light 204 check the pay assistant
         * fires).
         */

        // TODO see to add variables for paymonth and etablissementID
        public static final HttpRequestActionBuilder contratsEvents = http("Contrats events")
                        .get(WERH_API + "/career/bff/contrats-events?month=#{payMonth}&etablissementsIds=")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));

        /** Payroll-cycle date range for the selected etablissement. */
        public static final HttpRequestActionBuilder dateRange = http("Payroll cycle date range")
                        .post(WERH_API + "/pay/cycle-paie/date-range")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json", "content-type",
                                        "application/json"))
                        .body(StringBody(ETABLISSEMENT_BODY));

        /**
         * Path of the bulletin-control contract list for the active month
         * ("#{payMonth}").
         */
        private static final String CONTROLER_BULLETIN_PATH = WERH_API
                        + "/career/bff/cycle-paie/#{payMonth}/preparer/controler-bulletin/contrat?filters="
                        + CONTROLE_BULLETIN_FILTERS;

        /** Bulletin-control contract list for the active month. */
        public static final HttpRequestActionBuilder controlerBulletinContrat = http("Controler bulletin contrat")
                        .get(CONTROLER_BULLETIN_PATH)
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));

        /**
         * Same bulletin-control list, but also saves one random agent's bulletin id
         * ("bulletinId") for
         * the "visualiser les bulletins" journey, which renders that payslip via
         * {@link #bulletinByIds}.
         */
        public static final HttpRequestActionBuilder controlerBulletinContratForView = http(
                "Controler bulletin contrat")
                .get(CONTROLER_BULLETIN_PATH)
                .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"))
                .check(jsonPath("$[?(@.bulletinId != null)].bulletinId")
                        .findRandom().saveAs("bulletinId"));

        /** Bulletin calculation summary polled right after the recompute stream. */
        public static final HttpRequestActionBuilder calculationInfo = http("Bulletin calculation info")
                        .get(WERH_API + "/pay/bulletin/all/calculationInfo?month=#{payMonth}&etablissementIds=#{etablissementId}")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));

        /**
         * Renders a payslip (bulletin) as a PDF for the "visualiser les bulletins"
         * view. The body is a
         * JSON array of {"bulletinId": …} objects; here we send the single id picked at
         * random by
         * {@link #controlerBulletinContratForView}. The response is the PDF itself
         * (application/octet-stream), so we assert a 200 and that the body really is a
         * PDF.
         */
        public static final HttpRequestActionBuilder bulletinByIds = http("Bulletin by ids (PDF)")
                        .post(WERH_API + "/pay/edition-paie/bulletins/by-bulletin-ids")
                        .headers(ApiHeaders.bearerForAllTenants(
                                        "accept", "application/octet-stream", "content-type", "application/json"))
                        .body(StringBody("[{\"bulletinId\":\"#{bulletinId}\"}]"))
                        .check(status().is(200))
                        .check(substring("%PDF").exists());

        /**
         * Server-Sent Events stream backing the "contrôle des bulletins" page summary
         * (the synthèse
         * stream the front-end opens through weCoreSseService). The bundle builds it as
         * {apiPayUrl}/bulletin/controle/synthese/stream?month=…&etablissementIds=… and
         * parses
         * "calculStatus" out of each event, closing on DONE — same shape as the calcul
         * stream below.
         *
         * It is absent from the HAR because it is a long-lived stream that never closed
         * during the
         * capture (a HAR entry is only finalized once the response completes). The URL
         * and params are
         * recovered from the JS bundle; the auth scope below mirrors the other pay
         * calls and should be
         * confirmed on a live run.
         */
        public static final ChainBuilder bulletinsControleSyntheseStream = exec(sse("Bulletins synthese stream")
                        .sseName("bulletinsSynthese")
                        .get(WERH_API + "/pay/bulletin/controle/synthese/stream?month=#{payMonth}&etablissementIds=#{etablissementId}")
                        .headers(ApiHeaders.bearerWithTenant())
                        .await(30).on(
                                        sse.checkMessage("synthese event")
                                                        .check(regex("\"calculStatus\":\"(.*?)\"")
                                                                        .saveAs("syntheseStatus"))))
                        .exec(sse("Bulletins synthese stream").sseName("bulletinsSynthese").close());

        /**
         * Server-Sent Events stream reporting the bulletin (payslip) recompute
         * progress. The server
         * pushes "controle-bulletin" events whose JSON data carries "calculStatus"
         * (IN_PROGRESS ->
         * DONE) and per-contract counters (totalContrats, bulletinsCalcules, enCours,
         * enErreur,
         * nonCalcules). Here we open the stream, await the first status event (saved as
         * "calculStatus"), then close it. In the recorded journey the payslips were
         * already computed,
         * so a single DONE event arrives and the server ends the stream almost
         * immediately. To model a
         * real recompute, replace the single await with a loop that consumes events
         * until DONE.
         */
        public static final ChainBuilder bulletinsCalculStream = exec(sse("Bulletins calcul stream")
                        .sseName("bulletinsCalcul")
                        .get(WERH_API + "/pay/paie/bulletins/calcul/stream-info?month=#{payMonth}&etablissementIds=#{etablissementId}")
                        .headers(ApiHeaders.bearerWithTenant())
                        .await(30).on(
                                        sse.checkMessage("controle-bulletin event")
                                                        .check(regex("\"calculStatus\":\"(.*?)\"")
                                                                        .saveAs("calculStatus"))))
                        .exec(sse("Bulletins calcul stream").sseName("bulletinsCalcul").close());

        // ---- Individual agent payslip (bulletin de paie) tab ----

        /**
         * Payroll cycles for the agent's etablissement, loaded with the payslip tab.
         */
        public static final HttpRequestActionBuilder cyclePaieByEtablissement = http("Pay cycles by etablissement")
                        .get(WERH_API + "/pay/cycle-paie?etablissementId=#{etablissementId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /** Contract situation for the open month, loaded with the payslip tab. */
        public static final HttpRequestActionBuilder situationContrat = http("Contract situation")
                        .get(WERH_API + "/career/situation/contrat/#{active_agent.contratId}?mois=#{payMonth}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /**
         * Payslip list for the agent's contract in the open cycle, fetched right after
         * the stream.
         */
        public static final HttpRequestActionBuilder listeBulletin = http("Agent bulletin list")
                        .get(WERH_API + "/pay/paie/cycle-paie/#{cyclePaieId}/agent/#{active_agent.agentId}/contrat/#{active_agent.contratId}/listeBulletin")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                        .check(status().in(200, 404));

        /**
         * Server-Sent Events stream backing an individual agent's payslip (bulletin de
         * paie) tab. When
         * the user opens the pay-stub tab, the front-end opens
         * {apiPayUrl}/sse/bulletin?agentId=…&contratId=…&cyclePaieId=… and the server
         * pushes a single
         * "bulletin" event whose data carries the payslip (lignesPaie +
         * resumeCumulatifPaie), then ends
         * the stream. We open it, await that event (asserting the payslip lines
         * arrived), then close.
         * The three ids are correlated earlier in the journey: agentId/contratId from
         * the agent
         * contracts list (AgentApiEndpoints.contracts) and cyclePaieId from the
         * payroll-cycle resume.
         */
        public static final ChainBuilder bulletinStream = exec(sse("Agent bulletin stream").sseName("agentBulletin")
                        .get(WERH_API + "/pay/sse/bulletin?agentId=#{active_agent.agentId}&contratId=#{active_agent.contratId}&cyclePaieId=#{cyclePaieId}")
                        .headers(ApiHeaders.bearerWithTenant())
                        .await(30).on(
                                        sse.checkMessage("bulletin event")))
                        .exec(sse("Agent bulletin stream").sseName("agentBulletin").close());

        // OUVERTURE MOIS DE PAIE

        public static final HttpRequestActionBuilder cyclePaieByEtablissementOuverturePaie = http(
                        "Pay cycles by etablissement")
                        .post(WERH_API + "/pay/cycle-paie")
                        .headers(ApiHeaders.bearerForAllTenants("content-type", "application/json"))
                        .body(StringBody(
                                        """
                                                        [{"etablissementId":"#{etablissementId}","collectiviteId":"#{collectivite.id}","mois":"#{nextMonth}","etapePaie":2,"statut":"OUVERT"}]
                                                        """));

        public static final HttpRequestActionBuilder calculBulletinPay = http("Calcul bulletin pay")
                        .post(WERH_API + "/pay/bulletin/calcul-bulletin-pay")
                        .headers(ApiHeaders.bearerForAllTenants(
                                        "accept", "application/json, text/plain, */*", "content-type",
                                        "application/json"))
                        .body(StringBody(
                                        StringBody("""
                                                        [{"etablissementId":"#{etablissementId}","collectiviteId":"#{collectivite.id}","mois":"#{nextMonth}","etapePaie":2,"statut":"OUVERT"}]
                                                        """)));

        public static final HttpRequestActionBuilder cloturerBulletin = http("Cloturer bulletin")
                        .patch(WERH_API + "/pay/cycle-paie/cloture")
                        .headers(ApiHeaders.bearerForAllTenants(
                                        "accept", "application/json, text/plain, */*", "content-type",
                                        "application/json"))
                        .body(StringBody(
                                        """
                                                        [{"etablissementId":"#{etablissementId}","collectiviteId":"#{collectivite.id}","mois":"#{nextMonth}","etapePaie":7,"statut":"OUVERT"}]
                                                        """));

        public static final HttpRequestActionBuilder editionEtatDeCharge = http("Edition etat de charge")
                        .get(WERH_API + "/pay/etat-caisse/#{payMonth}/edition?filters={}")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/octet-stream"))
                        .check(bodyLength().gte(1024));

        public static final HttpRequestActionBuilder organismes = http("Organismes")
                        .get(WERH_API + "/pay/etat-caisse/organismes")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"))
                        .check(jsonPath("$[0].id").transform(Integer::parseInt).gt(0));

        public static final HttpRequestActionBuilder bordereau = http("Bordereau")
                        .get(WERH_API + "/pay/bordereaux-urssaf/#{payMonth}?filters=%7B%7D")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));

}
