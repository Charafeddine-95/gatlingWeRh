package endpoints.apiEndpoints;

import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.http.HttpRequestActionBuilder;

/** Notification API calls. */
public final class NotificationApiEndpoints {

    private NotificationApiEndpoints() {
    }

    /** Number of unread notifications for the connected user (header badge). */
    public static final HttpRequestActionBuilder unreadCount =
            http("Unread notifications count")
                    .get("/notif/notification/count?status=UNREAD")
                    .headers(ApiHeaders.bearerWithTenant(
                            "bluserid", "81227566-4b5f-46e8-bf64-bba0b4f20188",
                            "productid", ApiHeaders.APP_ID,
                            "tenantid", "#{tenantIdWithContext}"));
}
