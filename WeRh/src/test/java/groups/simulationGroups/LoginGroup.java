package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.AuthApiEndpoints;
import endpoints.webEndpoints.LoginPages;
import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;

/**
 * Authentication against Keycloak: login form, token exchange and IAM login.
 * A virtual user that fails any of these steps exits the scenario at the end of the
 * group instead of cascading "attribute not defined" errors through the journey.
 */
public final class LoginGroup {

    private LoginGroup() {
    }

    /** Complete login transaction: authorization page → credentials → token exchange → IAM profile. */
    public static final ChainBuilder login =
            group("Login").on(
                    LoginPages.generateStateAndNonce,
                    LoginPages.authorizationPage,
                    pause(1),
                    LoginPages.submitCredentials,
                    pause(1),
                    AuthApiEndpoints.exchangeToken,
                    pause(Duration.ofMillis(300)),
                    AuthApiEndpoints.userLoginNoTenant)
                    .exitHereIfFailed();
}
