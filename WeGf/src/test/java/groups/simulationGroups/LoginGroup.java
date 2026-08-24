package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.asLongAs;
import static io.gatling.javaapi.core.CoreDsl.doIf;
import static io.gatling.javaapi.core.CoreDsl.doIfOrElse;
import static io.gatling.javaapi.core.CoreDsl.during;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

import endpoints.apiEndpoints.AuthApiEndpoints;
import endpoints.webEndpoints.LoginPages;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Authentication against Keycloak, performed once for the whole run.
 *
 * <p>One virtual user takes the login: it plays the real Keycloak flow (login form, token
 * exchange, IAM profile) and publishes its access token. Every other virtual user never talks
 * to Keycloak — it waits for that token and copies it into its own session, so the
 * "Bearer #{accessToken}" headers of the authenticated endpoints keep working unchanged. All
 * users log in with the same credentials anyway, so only the login round-trips disappear from
 * the load, not a distinct identity.
 *
 * <p>Only that one user owns a Keycloak SSO cookie, so the silent re-authorization the SPA
 * replays on reload lives here as {@link #silentReauthentication} and is a no-op for the others.
 *
 * <p>A token has a limited lifetime. For a run shorter than that lifetime, {@link #login} is
 * enough. For a longer run, inject {@link #keepTokenFresh(Duration)} as its own single-user
 * scenario: it owns the login and renews the shared token in the background, without any user
 * ever blocking on it.
 */
public final class LoginGroup {

    private LoginGroup() {
    }

    /** The access token every virtual user sends; null until the login owner publishes it. */
    private static final AtomicReference<String> SHARED_ACCESS_TOKEN = new AtomicReference<>();

    /** Taken by the single user performing the login, released again if that login fails. */
    private static final AtomicBoolean LOGIN_TAKEN = new AtomicBoolean(false);

    /** Session attribute marking the one user that holds the Keycloak SSO cookie. */
    private static final String KEYCLOAK_SESSION = "hasKeycloakSession";

    /** How long a user waits for the shared token before giving up, and how often it looks for it. */
    private static final long WAIT_TIMEOUT_MS = 60_000;
    private static final Duration WAIT_POLL = Duration.ofMillis(200);
    private static final String WAIT_DEADLINE = "sharedTokenDeadline";

    /** Renewal delay bounds, and the lifetime assumed when Keycloak announces none. */
    private static final Duration MIN_RENEWAL_DELAY = Duration.ofSeconds(30);
    private static final Duration MAX_RENEWAL_DELAY = Duration.ofMinutes(10);
    private static final Duration ASSUMED_TOKEN_LIFETIME = Duration.ofMinutes(5);

    /** Hands the token to the other users, or releases the login so the next user retries it. */
    private static final ChainBuilder publishSharedToken =
            exec(session -> {
                if (!session.contains("accessToken")) {
                    LOGIN_TAKEN.set(false);
                    System.out.println(">>> pas de token obtenu — login libéré pour le prochain utilisateur");
                    return session;
                }
                SHARED_ACCESS_TOKEN.set(session.getString("accessToken"));
                System.out.println(">>> token partagé publié, valable " + tokenLifetime(session).getSeconds() + "s");
                return session.set(KEYCLOAK_SESSION, true);
            });

    /** The real login, played by one user: authorization page → credentials → token exchange → IAM profile. */
    private static final ChainBuilder keycloakLogin =
            group("Login").on(
                    LoginPages.generateStateAndNonce,
                    LoginPages.authorizationPage,
                    pause(1),
                    LoginPages.submitCredentials,
                    pause(1),
                    AuthApiEndpoints.exchangeToken,
                    pause(Duration.ofMillis(300)),
                    AuthApiEndpoints.userLoginNoTenant,
                    publishSharedToken);

    /** Waits for the login owner's token — no request sent — then adopts it as this user's own. */
    private static final ChainBuilder useSharedToken =
            exec(session -> session.set(WAIT_DEADLINE, System.currentTimeMillis() + WAIT_TIMEOUT_MS))
                    .exec(asLongAs(session -> SHARED_ACCESS_TOKEN.get() == null
                            && System.currentTimeMillis() < session.getLong(WAIT_DEADLINE))
                            .on(pause(WAIT_POLL)))
                    .exec(session -> {
                        String token = SHARED_ACCESS_TOKEN.get();
                        if (token == null) {
                            System.out.println(">>> pas de token partagé — le login n'a jamais abouti");
                            return session.markAsFailed();
                        }
                        return session.set("accessToken", token);
                    });

    /**
     * Login step of every scenario: one virtual user authenticates, all the others reuse its token.
     * A user left without a token exits here instead of cascading "attribute not defined" errors
     * through the rest of the journey.
     */
    public static final ChainBuilder login =
            doIfOrElse(session -> LOGIN_TAKEN.compareAndSet(false, true))
                    .then(keycloakLogin)
                    .orElse(useSharedToken)
                    .exitHereIfFailed();

    /**
     * Silent re-authorization that the SPA replays when the dashboard reloads: it needs the
     * Keycloak SSO cookie, which only the user that logged in has, so it runs for that user only.
     */
    public static final ChainBuilder silentReauthentication =
            doIf(session -> session.contains(KEYCLOAK_SESSION)).then(
                    LoginPages.generateStateAndNonce,
                    LoginPages.silentAuthorizationPage,
                    pause(Duration.ofMillis(700)),
                    AuthApiEndpoints.exchangeToken);

    /**
     * Keeps the shared token fresh for {@code runFor} — inject it as its own scenario with a
     * single user, alongside the journey scenarios:
     *
     * <pre>{@code
     * ScenarioBuilder tokenRefresher =
     *     scenario("Token refresh").exec(LoginGroup.keepTokenFresh(Duration.ofMinutes(30)));
     *
     * setUp(tokenRefresher.injectOpen(atOnceUsers(1)).protocols(httpProtocol),
     *       userJourney.injectOpen(rampUsers(200).during(Duration.ofMinutes(20))).protocols(httpProtocol));
     * }</pre>
     *
     * <p>That user claims the login as soon as it starts, then replays the silent re-authorization
     * the SPA itself uses — the Keycloak SSO cookie buys a brand new token without ever resending
     * the credentials — every half token lifetime, and republishes the result. Nothing blocks: the
     * journey users only read the shared token, and the renewal happens between two of the
     * refresher's own pauses. Each renewal also keeps the Keycloak SSO session alive, so the loop
     * can run for hours. A journey user starting at the very same instant may still take the login
     * itself, which costs one extra login and nothing else.
     *
     * <p>Because a user copies the token when it logs in, the token it starts its journey with is
     * at most one renewal delay old, i.e. it always has at least half a lifetime left — with the
     * usual 5 minute Keycloak token, at least 2m30s, far longer than a journey.
     *
     * <p>{@code runFor} has to cover the injection profile: the refresher stops renewing when it
     * elapses. It also keeps the simulation alive until then, so size it to the run rather than
     * leaving a long tail behind the last journey.
     */
    public static ChainBuilder keepTokenFresh(Duration runFor) {
        return exec(session -> {
            // Reserves the login for this refresher, so the journey users go down the
            // "reuse the shared token" branch instead of authenticating themselves.
            LOGIN_TAKEN.set(true);
            return session;
        }).exec(
                keycloakLogin,
                during(runFor).on(
                        pause(LoginGroup::renewalDelay),
                        group("Renew token").on(
                                LoginPages.generateStateAndNonce,
                                LoginPages.silentAuthorizationPage,
                                AuthApiEndpoints.exchangeToken,
                                publishSharedToken)));
    }

    /** Half the lifetime Keycloak announced, so the shared token is never older than that. */
    private static Duration renewalDelay(Session session) {
        Duration half = tokenLifetime(session).dividedBy(2);
        if (half.compareTo(MIN_RENEWAL_DELAY) < 0) {
            return MIN_RENEWAL_DELAY;
        }
        return half.compareTo(MAX_RENEWAL_DELAY) > 0 ? MAX_RENEWAL_DELAY : half;
    }

    /** Lifetime of the token this user last obtained, as announced in the token response. */
    private static Duration tokenLifetime(Session session) {
        if (!session.contains("expiresIn")) {
            return ASSUMED_TOKEN_LIFETIME;
        }
        try {
            return Duration.ofSeconds(Long.parseLong(session.getString("expiresIn").trim()));
        } catch (NumberFormatException e) {
            return ASSUMED_TOKEN_LIFETIME;
        }
    }
}
