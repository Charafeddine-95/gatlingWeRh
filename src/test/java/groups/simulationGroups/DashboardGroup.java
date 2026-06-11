package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.AuthApiEndpoints;
import endpoints.apiEndpoints.ContractApiEndpoints;
import endpoints.apiEndpoints.NotificationApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import endpoints.webEndpoints.LoginPages;
import endpoints.webEndpoints.WebPages;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;

/** Dashboard loading after login: silent SSO then user context, contracts and notifications. */
public final class DashboardGroup {

    private DashboardGroup() {
    }

    /** First dashboard load after login: SPA reload, silent SSO, then the initial burst of context/contract/notification calls. */
    public static final ChainBuilder open =
            group("Open dashboard").on(
                    WebPages.home,
                    pause(Duration.ofMillis(700)),
                    LoginPages.generateStateAndNonce,
                    LoginPages.silentAuthorizationPage,
                    pause(Duration.ofMillis(700)),
                    AuthApiEndpoints.exchangeToken
                            .resources(
                                    AuthApiEndpoints.userLoginNoTenant,
                                    ReferentialApiEndpoints.agentConfiguration),
                    pause(1),
                    ContractApiEndpoints.contractExists
                            .resources(
                                    NotificationApiEndpoints.unreadCount,
                                    AuthApiEndpoints.userLogin,
                                    ReferentialApiEndpoints.collectivites,
                                    ReferentialApiEndpoints.etablissements));

    /** Dashboard refresh while the user keeps browsing: profile, contracts and notifications are re-checked. */
    public static final ChainBuilder refresh =
            group("Refresh dashboard").on(
                    AuthApiEndpoints.userLogin
                            .resources(
                                    ContractApiEndpoints.contractExistsAllTenants,
                                    ReferentialApiEndpoints.agentConfiguration,
                                    ContractApiEndpoints.contractExists),
                    pause(Duration.ofMillis(600)),
                    NotificationApiEndpoints.unreadCount
                            .resources(
                                    ReferentialApiEndpoints.etablissements,
                                    ReferentialApiEndpoints.collectivites));
}
