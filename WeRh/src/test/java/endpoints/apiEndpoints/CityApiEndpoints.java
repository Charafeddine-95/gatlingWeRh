package endpoints.apiEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

/** City referential API calls served by the WeCross API. */
public final class CityApiEndpoints {

    private CityApiEndpoints() {
    }

    private static final String CITY_LIST_SIZE = config("werh.cityListSize", "WERH_CITY_LIST_SIZE", "100");

    private static String config(String property, String envVariable, String defaultValue) {
        String value = System.getProperty(property, System.getenv(envVariable));
        return value != null ? value : defaultValue;
    }

    /** Countries used by agent address data. */
    public static final HttpRequestActionBuilder countries =
            http("Countries")
                    .get("/city/country")
                    .headers(ApiHeaders.bearerWithTenant());

    /** First page of cities loaded by the agent list page. */
    public static final HttpRequestActionBuilder cities =
            http("Cities")
                    .get("/city/ville?size=" + CITY_LIST_SIZE)
                    .headers(ApiHeaders.bearerWithTenant());
}
