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

        public static final HttpRequestActionBuilder PJ = http("PJ page")
                .get("https://wegf.uat.wemagnus.com/wegf-mfe-compta/Execution/PieceJustificativeListeFormulaire")
                .headers(NAVIGATION_HEADERS);

        public static final HttpRequestActionBuilder Ordonnancement = http("ORDONNANCEMENT page")
                .get("https://wegf.uat.wemagnus.com/wegf-mfe-compta/Execution/Ordonnancement")
                .headers(NAVIGATION_HEADERS);

        public static final HttpRequestActionBuilder Grandlivre = http("GRANDLIVRE page")
                .get("https://wegf.uat.wemagnus.com/wegf-mfe-compta/Editions/EditionGrandLivreListeFormulaire")
                .headers(NAVIGATION_HEADERS);

        public static final HttpRequestActionBuilder Situationbudgetaire = http("SITUATION Budgetaire page")
                .get("https://wegf.uat.wemagnus.com/wegf-mfe-compta/Editions/EditionCommunListeFormulaire")
                .headers(NAVIGATION_HEADERS);


// mandat webpage
// https://wegf.uat.wemagnus.com/wegf-mfe-compta/Execution/MandatListeFormulaire/MandatTitreFicheFormulaire/update?t=1785849794723&id=17068&from=MandatListeFormulaire&params=eyJzZW5zIjoyLCJhbm51bGVlUGFydGllbGxlbWVudCI6ImZhbHNlIiwiYW5udWxlZVRvdGFsZW1lbnQiOiJmYWxzZSIsImFubnVsYXRpZiI6ImZhbHNlIiwiZmlsdGVyIjp7ImFubnVsYXRpZiI6ZmFsc2UsImJ1ZGdldGFpcmUiOnRydWUsImludGVybmVzIjpmYWxzZSwibm9uTnVtZXJvdGUiOnRydWUsIm51bWVyb3RlIjpmYWxzZSwiZGViaXRPZmZpY2UiOmZhbHNlLCJmaW5FeG8iOmZhbHNlLCJhZ0dyaWRDb25maWdzdGF0ZSI6eyJmaWx0ZXJzIjp7fSwicXVpY2tTZWFyY2hGaWx0ZXIiOm51bGx9LCJwYWdlU2l6ZSI6MTAsImN1cnJlbnRQYWdlIjowfSwiaWRFeGVyY2ljZSI6MjUsImlkQ29sbGVjdGl2aXRlIjoxfQ&pn=W3sicHJldmlvdXNQYWdlIjoiTWFuZGF0TGlzdGVGb3JtdWxhaXJlIiwiYWN0aW9uIjpudWxsfV0
}
