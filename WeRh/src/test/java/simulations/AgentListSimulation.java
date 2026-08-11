package simulations;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;

import endpoints.apiEndpoints.ApiHeaders;
import endpoints.webEndpoints.WebPages;
import groups.simulationGroups.*;
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

  /**
   * Renews the token shared by all the virtual users. Not injected below because a short run
   * fits in a single token lifetime — for a run longer than that, add it to setUp next to the
   * journey and give it a duration that covers the injection profile.
   */
  ScenarioBuilder tokenRefresher =
      scenario("Token refresh").exec(LoginGroup.keepTokenFresh(Duration.ofMinutes(30)));

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
          AgentListGroup.open,

              AgentDossierGroup.open
              );

  ScenarioBuilder agentsListThenDossierAgent =
          scenario("Agents List then Dossier Agent")
                  .exitBlockOnFail()
                  .on(
                          exec(
                                  ApiHeaders.initTenants,
                                  WebPages.home,
                                  pause(1),
                                  LoginGroup.login,
                                  pause(Duration.ofMillis(500)),
                                  DashboardGroup.open,
                                  pause(2),
                                  AgentListGroup.open,
                                  AgentDossierGroup.open,
                                  pause(2)
                          ),
                          exec(
                                  randomSwitch()
                                          .on(
                                                  percent(50.0)
                                                          .then(
                                                                  exec(
                                                                          AgentBulletinGroup.open
                                                                  )
                                                          )
                                          )
                          )
                  );

  {
    setUp(agentsListThenDossierAgent.injectOpen(atOnceUsers(10)).protocols(httpProtocol));
  }
}
