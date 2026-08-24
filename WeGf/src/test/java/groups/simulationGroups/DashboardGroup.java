package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.AuthApiEndpoints;
import endpoints.apiEndpoints.NotificationApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import endpoints.webEndpoints.WebPages;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;

/** Dashboard loading after login: silent SSO then user context, contracts and notifications. */
public final class DashboardGroup {

    private DashboardGroup() {
    }

    /**
     * First dashboard load after login: SPA reload, silent SSO, then the initial burst of
     * context/contract/notification calls. The silent SSO only happens for the virtual user that
     * actually logged in — see {@link LoginGroup#silentReauthentication}.
     */
    public static final ChainBuilder open =
            group("Open dashboard").on(
                    WebPages.home,
                    pause(Duration.ofMillis(700)),
                    LoginGroup.silentReauthentication,
                    AuthApiEndpoints.userLoginNoTenant
                            .resources(ReferentialApiEndpoints.agentConfiguration),
                    ReferentialApiEndpoints.contextCBE);

    /** Dashboard refresh while the user keeps browsing: profile, contracts and notifications are re-checked. */
    public static final ChainBuilder refresh =
            group("Refresh dashboard").on(
                    AuthApiEndpoints.userLogin
                            .resources(ReferentialApiEndpoints.agentConfiguration),
                    pause(Duration.ofMillis(600)),
                    NotificationApiEndpoints.unreadCount
                            .resources(
                                    ReferentialApiEndpoints.contextCBE));
}
