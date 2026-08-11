package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.asLongAs;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

import io.gatling.javaapi.core.ChainBuilder;
import java.time.Duration;

/**
 * Reference example only — NOT wired into any scenario and NOT pointing at a real API. It shows the
 * Gatling pattern for an <b>asynchronous</b> request handled by fire-and-poll: submit a job, then
 * poll its status until the server reports it finished.
 *
 * <p>The point of the example is {@code .silent()} on the polling request. A job can take many
 * polls to finish; without {@code .silent()} every poll lands in the report and drowns the two
 * requests you actually care about (the submit and the final result), and inflates the error/OK
 * counts. {@code .silent()} removes a request from the stats entirely — use it for
 * background/polling traffic and keep the meaningful requests measured.
 *
 * <p>Note {@code .silent()} lives on {@code HttpRequestActionBuilder} (plain HTTP requests) only —
 * the SSE builders do not expose it, so the streaming flavour of async (see the real
 * {@code bulletinsCalculStream} / {@code bulletinStream} in {@link PayApiEndpoints}) cannot be
 * silenced this way.
 */
public final class AsyncRequestExample {

    private AsyncRequestExample() {
    }

    /**
     * 1. submit the job (measured) and save its id + initial status;
     * 2. poll the status once a second until it is no longer IN_PROGRESS — the polls are
     *    {@code .silent()} so they stay out of the report;
     * 3. fetch the finished result (measured again).
     *
     * <p>URLs and payloads are placeholders — replace them with a real endpoint.
     */
    public static final ChainBuilder submitPollResult =
            exec(http("Async submit job")
                    .post("https://example.test/async/jobs")
                    .body(StringBody("{\"work\":\"demo\"}")).asJson()
                    .check(status().is(202))
                    .check(jsonPath("$.jobId").saveAs("jobId"))
                    .check(jsonPath("$.status").saveAs("jobStatus")))
                    .exec(asLongAs(session -> "IN_PROGRESS".equals(session.getString("jobStatus"))).on(
                            pause(Duration.ofSeconds(1))
                                    .exec(http("Async poll status")
                                            .get("https://example.test/async/jobs/#{jobId}")
                                            .check(jsonPath("$.status").saveAs("jobStatus"))
                                            .silent())))
                    .exec(http("Async get result")
                            .get("https://example.test/async/jobs/#{jobId}/result")
                            .check(status().is(200)));
}
