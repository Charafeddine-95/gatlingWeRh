package endpoints.webEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.util.Map;

/** WeGf web application pages. */
public final class WebPages {

        private WebPages() {
        }

        private static final Map<String, String> NAVIGATION_HEADERS = Map.of(
                        "accept",
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7",
                        "priority", "u=0, i",
                        "upgrade-insecure-requests", "1");

        /**
         * Landing page of the WeGf web app — serves the single-page application shell.
         */
        public static final HttpRequestActionBuilder home = http("Home page")
                        .get("https://wegf.uat.wemagnus.com/wegf-mfe-compta/home")
                        .headers(NAVIGATION_HEADERS);

        public static final HttpRequestActionBuilder Titre = http("Titre page")
                        .get("https://wegf.uat.wemagnus.com/wegf-mfe-compta/Execution/TitreListeFormulaire")
                        .headers(NAVIGATION_HEADERS);

        public static final HttpRequestActionBuilder Mandat = http("Mandat page")
                        .get("https://wegf.uat.wemagnus.com/wegf-mfe-compta/Execution/MandatListeFormulaire")
                        .headers(NAVIGATION_HEADERS);

}
