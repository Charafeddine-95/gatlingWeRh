package endpoints.apiEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;

/** Referential data (configuration and context) served by the WeRH API. */
public final class ReferentialApiEndpoints {

        private ReferentialApiEndpoints() {
        }

        private static final String WERH_API = "https://werh-api.uat.wemagnus.com";

        /** WeRH application configuration for the ROLE_AGENT profile. */
        public static final HttpRequestActionBuilder agentConfiguration = http("Agent configuration")
                        .get(WERH_API + "/config/configuration/ROLE_AGENT")
                        .headers(ApiHeaders.bearerForAllTenants());

        /** Collectivites (local authorities) the connected user can work on. */
        public static final HttpRequestActionBuilder collectivites = http("Collectivites")
                        .get(WERH_API + "/context/v1/collectivite")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                        .check(jsonPath("$[*].id").findAll().saveAs("collectiviteIds")) // tous les ids
                        .check(jsonPath("$[*]").ofMap().findRandom().saveAs("collectivite")); // l'objet entier;

        /** Establishments: the full list, plus one picked at random. */
        public static final HttpRequestActionBuilder etablissements = http("Etablissements")
                        .get(WERH_API + "/context/v1/etablissement")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                        .check(
                                        jsonPath("$[*]").ofMap().findAll().saveAs("etablissements"),
                                        jsonPath("$[*]").ofMap().findRandom().saveAs("activeEtablissement"),
                                        jsonPath("$[*].id").findAll().saveAs("etablissementIds"));

        /** Services (org units) used by the bulletin-control page filters. */
        public static final HttpRequestActionBuilder service = http("Services")
                        .get(WERH_API + "/context/v1/service")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));

        /** Job functions used by the bulletin-control page filters. */
        public static final HttpRequestActionBuilder fonction = http("Fonctions")
                        .get(WERH_API + "/career/fonction")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));

        // The lookups below are served by the wecross referential service (the protocol
        // base URL).

        /** Statut referential. */
        public static final HttpRequestActionBuilder statut = http("Referential statut")
                        .get("/ref/referential/1.14/statut?full=true")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));

        /** Sous-statut referential. */
        public static final HttpRequestActionBuilder sousStatut = http("Referential sousStatut")
                        .get("/ref/referential/1.14/sousStatut?full=true")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));

        /** Position referential. */
        public static final HttpRequestActionBuilder position = http("Referential position")
                        .get("/ref/referential/1.14/position?full=true")
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));

        /** Reference date (MM/DD/YYYY) bounding the grade referential, as captured. */
        private static final String CURRENT_PERIOD = "06/01/2026";

        /** Grade referential, bounded to the current pay period. */
        public static final HttpRequestActionBuilder grade = http("Referential grade")
                        .get("/ref/referential/1.14/grade?full=true&odm_data=dateDebut<=" + CURRENT_PERIOD
                                        + ",dateFin>=" + CURRENT_PERIOD)
                        .headers(ApiHeaders.bearerForAllTenants("accept", "application/json"));
}
