package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;

import endpoints.apiEndpoints.AuthApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;

/** Approval of the general conditions of use (GCU) on first login. */
public final class GcuApprovalGroup {

    private GcuApprovalGroup() {
    }

    /** The user accepts the GCU dialog — only part of the first-connection journey. */
    public static final ChainBuilder approve =
            group("Approve GCU").on(AuthApiEndpoints.approveGcu);
}
