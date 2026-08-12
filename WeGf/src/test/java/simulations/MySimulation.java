package simulations;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

import endpoints.apiEndpoints.ApiHeaders;
import endpoints.webEndpoints.WebPages;
import groups.simulationGroups.*;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import endpoints.apiEndpoints.ExecutionApiEndpoints;

import java.time.Duration;


public class MySimulation extends Simulation {

  /**
   * Browser-like defaults; requests with a relative path resolve against the
   * wecross API base URL.
   */
  HttpProtocolBuilder httpProtocol = http.baseUrl("https://wecross-api.uat.wemagnus.com")
      .header("accept", "application/json, text/plain, */*")
      .header("accept-encoding", "gzip, deflate, br, zstd")
      .header("accept-language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
      .header("user-agent",
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36");

  /**
   * Returning user opening the pay assistant, preparing the pay, then viewing the
   * payslips and
   * opening one random agent's bulletin — rendered as a PDF — to check it returns
   * a document. Runs
   * on the last closed month, where payslips are finalized.
   */
  ScenarioBuilder titreGF = scenario("WeGF titres").exec(
      ApiHeaders.initTenants,
      WebPages.home,
      pause(1),
      LoginGroup.login,
      pause(Duration.ofMillis(500)),
      DashboardGroup.open,
      ExecutionGroup.Titre);

  ScenarioBuilder mandatGF = scenario("WeGF mandats").exec(
      ApiHeaders.initTenants,
      WebPages.home,
      pause(1),
      LoginGroup.login,
      pause(Duration.ofMillis(500)),
      DashboardGroup.open,
      ExecutionGroup.Mandat);

  ScenarioBuilder Pj = scenario("WeGF Pièces Justificatives").exec(
      ApiHeaders.initTenants,
      WebPages.home,
      pause(1),
      LoginGroup.login,
      pause(Duration.ofMillis(500)),
      DashboardGroup.open,
      ExecutionGroup.PJ);

      ScenarioBuilder Engagements = scenario("WeGF Pièces Justificatives").exec(
        ApiHeaders.initTenants,
        WebPages.home,
        pause(1),
        LoginGroup.login,
        pause(Duration.ofMillis(500)),
        DashboardGroup.open,
        ExecutionGroup.Engagements);

  ScenarioBuilder openMandat = scenario("WeGF Pièces Justificatives").exec(
      ApiHeaders.initTenants,
      WebPages.home,
      pause(1),
      LoginGroup.login,
      pause(Duration.ofMillis(500)),
      DashboardGroup.open,
      ExecutionGroup.Mandat,
      ExecutionGroup.OpenMandat);



  ScenarioBuilder openOrdonnancement = scenario("WeGF Ordonnancement").exec(
          ApiHeaders.initTenants,
          WebPages.home,
          pause(1),
          LoginGroup.login,
          pause(Duration.ofMillis(500)),
          DashboardGroup.open,
          OrdonnancementGroup.open,
          OrdonnancementSelectionLiquidationsGroup.open,
          NumerotationGroup.numeroter
          );

  /**
   * The other way through the ordonnancement screen: instead of numbering liquidations into a
   * new bordereau, tick bordereaux that already exist and generate their PES flux.
   */
  ScenarioBuilder editionTransfertBordereaux = scenario("WeGF Edition transfert bordereaux").exec(
          ApiHeaders.initTenants,
          WebPages.home,
          pause(1),
          LoginGroup.login,
          pause(Duration.ofMillis(500)),
          DashboardGroup.open,
          OrdonnancementGroup.open,
          pause(Duration.ofMillis(500)),
          OrdonnancementEditionTransfertGroup.open,
          pause(1),
          OrdonnancementEditionTransfertGroup.genererFluxPES);

          {
                  setUp(openOrdonnancement.injectOpen(atOnceUsers(1)).protocols(httpProtocol));
}

}
