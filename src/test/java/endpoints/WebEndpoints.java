package endpoints;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.util.Map;

import static io.gatling.javaapi.http.HttpDsl.*;

public class WebEndpoints {

    public static final Map<String, String> commonHeaders_1 = Map.ofEntries(
            Map.entry("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"),
            Map.entry("priority", "u=0, i"),
            Map.entry("upgrade-insecure-requests", "1")
    );
    private static final String WERH_UAT_WEMAGNUS_COM = "https://werh.uat.wemagnus.com";
    public static final HttpRequestActionBuilder home = http("home").get(WERH_UAT_WEMAGNUS_COM + "/").header(Map.ofEntries(
            Map.entry("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"),
            Map.entry("priority", "u=0, i"),
            Map.entry("upgrade-insecure-requests", "1"));
}
