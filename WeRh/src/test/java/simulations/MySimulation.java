package simulations;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.core.CoreDsl.doIfOrElse;
import static io.gatling.javaapi.core.CoreDsl.exec;


import endpoints.apiEndpoints.ApiHeaders;
import endpoints.webEndpoints.WebPages;
import groups.simulationGroups.*;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;

public class MySimulation extends Simulation {

  /** Browser-like defaults; requests with a relative path resolve against the wecross API base URL. */
  HttpProtocolBuilder httpProtocol =
      http.baseUrl("https://wecross-api.uat.wemagnus.com")
          .header("accept", "application/json, text/plain, */*")
          .header("accept-encoding", "gzip, deflate, br, zstd")
          .header("accept-language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
          .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36");

  /**
   * Renews the token shared by all the virtual users. Not injected below because a short run
   * fits in a single token lifetime — for a run longer than that, add it to setUp next to the
   * journey and give it a duration that covers the injection profile:
   * {@code setUp(tokenRefresher.injectOpen(atOnceUsers(1)).protocols(httpProtocol), ...)}.
   */
  ScenarioBuilder tokenRefresher =
      scenario("Token refresh").exec(LoginGroup.keepTokenFresh(Duration.ofMinutes(30)));

  /**
   * First connection of a user: the GCU approval dialog shows up once and has to be
   * accepted. Kept for reference but not injected below — to test it, swap the
   * scenario passed to setUp.
   */
  ScenarioBuilder firstConnection =
      scenario("WeRH first connection").exec(
          ApiHeaders.initTenants,
          WebPages.home,
          pause(1),
          LoginGroup.login,
          pause(Duration.ofMillis(500)),
          DashboardGroup.open,
          pause(14),
          GcuApprovalGroup.approve,
          pause(2),
          DashboardGroup.refresh);

  /** Returning user: GCU already accepted on a previous connection. */
  ScenarioBuilder userJourney =
      scenario("WeRH user journey").exec(
          ApiHeaders.initTenants,
          WebPages.home,
          pause(1),
          LoginGroup.login,
          pause(Duration.ofMillis(500)),
          DashboardGroup.open,
          pause(6),
          DashboardGroup.refresh);

  /** Returning user opening the agents list page. */
  ScenarioBuilder agentsList =
      scenario("WeRH agents list").exec(
          ApiHeaders.initTenants,
          WebPages.home,
          pause(1),
          LoginGroup.login,
          pause(Duration.ofMillis(500)),
          DashboardGroup.open,
          pause(2),
          AgentListGroup.open);

  /** Returning user opening an agent from the list, then its pay-stub (bulletin) tab. */
  ScenarioBuilder agentBulletin =
      scenario("WeRH agent bulletin").exec(
          ApiHeaders.initTenants,
          WebPages.home,
          pause(1),
          LoginGroup.login,
          pause(Duration.ofMillis(500)),
          DashboardGroup.open,
          pause(2),
          AgentListGroup.open,
          pause(3),
          AgentBulletinGroup.open);

  /**
   * Returning user opening the pay assistant, controlling the bulletins, then recomputing them.
   * The recompute step opens the SSE stream that reports the calculation progress.
   */
  ScenarioBuilder payControlBulletins =
      scenario("WeRH pay control bulletins").exec(
          ApiHeaders.initTenants,
          WebPages.home,
          pause(1),
          LoginGroup.login,
          pause(Duration.ofMillis(500)),
          DashboardGroup.open,
          pause(2),
          PayAssistantGroup.open,
          pause(3),
          ControleBulletinsGroup.open,
          pause(4),
          RecalculBulletinsGroup.recompute);

  /**
   * Returning user opening the pay assistant, preparing the pay, then viewing the payslips and
   * opening one random agent's bulletin — rendered as a PDF — to check it returns a document. Runs
   * on the last closed month, where payslips are finalized.
   */
  ScenarioBuilder payVisualiserBulletins =
      scenario("WeRH pay visualiser bulletins").exec(
          ApiHeaders.initTenants,
          WebPages.home,
          pause(1),
          LoginGroup.login,
          pause(Duration.ofMillis(500)),
          DashboardGroup.open,
          pause(2),
          PayAssistantGroup.open,
          pause(3),
          VisualiserBulletinsGroup.open,
          pause(1),
          EtatDeChargeGroup.open
        );


    ScenarioBuilder ouvertureMoisPaie =
    scenario("Ouverture mois paie").exec(
        ApiHeaders.initTenants,
        WebPages.home,
        pause(1),
        LoginGroup.login,
        pause(Duration.ofMillis(500)),
        DashboardGroup.open,
        pause(2),
        PayAssistantGroup.open,
        pause(3),
        doIfOrElse(session -> session.contains("nextMonth"))
                .then(OuverturePaieGroup.open)
                .orElse(exec(session -> {
                    System.out.println(">>> nextMonth absent — ouverture ignorée");
                    return session;
                })));

  ScenarioBuilder createAgent =
          scenario("Create agent").exec(
                  ApiHeaders.initTenants,
                  WebPages.home,
                  pause(1),
                  LoginGroup.login,
                  pause(Duration.ofMillis(500)),
                  CreateAgentGroup.open
          );

  {
    setUp(createAgent.injectOpen(atOnceUsers(1)).protocols(httpProtocol));
  }
}
