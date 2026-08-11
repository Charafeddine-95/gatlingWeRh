package simulations;

import static io.gatling.javaapi.core.CoreDsl.atOnceUsers;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;

import endpoints.apiEndpoints.ApiHeaders;
import endpoints.webEndpoints.WebPages;
import groups.simulationGroups.DashboardGroup;
import groups.simulationGroups.LoginGroup;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.time.Duration;

/**
 * Standalone simulation running only the agents-list journey (the same scenario also defined in
 * {@link MySimulation}). Kept in its own file so it can be launched on its own with
 * {@code -Dgatling.simulationClass=simulations.AgentListSimulation}.
 */
public class AgentListSimulation extends Simulation {

  /** Browser-like defaults; requests with a relative path resolve against the wecross API base URL. */
  HttpProtocolBuilder httpProtocol =
      http.baseUrl("https://wecross-api.uat.wemagnus.com")
          .header("accept", "application/json, text/plain, */*")
          .header("accept-encoding", "gzip, deflate, br, zstd")
          .header("accept-language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
          .header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36");

  /** Returning user opening the agents list page. */
  ScenarioBuilder agentsList =
      scenario("WeGf agents list").exec(
          ApiHeaders.initTenants,
          WebPages.home,
          pause(1),
          LoginGroup.login,
          pause(Duration.ofMillis(500)),
          DashboardGroup.open,
          pause(2));

  {
    setUp(agentsList.injectOpen(atOnceUsers(1)).protocols(httpProtocol));
  }
}
