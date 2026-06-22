package simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import endpoints.apiEndpoints.ReferentialApiEndpoints;
import endpoints.webEndpoints.WebPages;
import groups.simulationGroups.DashboardGroup;
import groups.simulationGroups.GcuApprovalGroup;
import groups.simulationGroups.LoginGroup;
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
   * First connection of a user: the GCU approval dialog shows up once and has to be
   * accepted. Kept for reference but not injected below — to test it, swap the
   * scenario passed to setUp.
   */
  ScenarioBuilder firstConnection =
      scenario("WeRH first connection").exec(
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
                  WebPages.home,
                  pause(1),
                  LoginGroup.login,
                  pause(Duration.ofMillis(500)),
                  // DashboardGroup.open,
                  pause(6),
                  // DashboardGroup.refresh
                  WebPages.dossiersAgent,
                  ReferentialApiEndpoints.collectivites.check(status().is(200),jsonPath("$[0].identite.designationCollectivite").is("BL UN")),
                  ReferentialApiEndpoints.etablissements.check(status().is(200),jsonPath("$[0].designation").is("ETS BL UN")),
                  ReferentialApiEndpoints.contract.check(status().is(200),jsonPath("$[0].nomUsage").is("KRUMAN")

                  ) );

  {
    setUp(userJourney.injectOpen(atOnceUsers(1)).protocols(httpProtocol));
  }
}
