package groups.simulationGroups;

import static io.gatling.javaapi.core.CoreDsl.group;

import endpoints.apiEndpoints.PayApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;

// TODO check if cycleresume still called multiple times with cache

public final class OuverturePaieGroup {

    private OuverturePaieGroup() {
    }

    /**
     * Need to close the group here also otherwise cant scale the group
     */
    public static final ChainBuilder open = group("Ouverture paie group").on(
            PayApiEndpoints.cyclePaieByEtablissementOuverturePaie,
            PayApiEndpoints.cycleResume,
            PayApiEndpoints.bulletinsCalculStream,
            PayApiEndpoints.cycleResume,
            PayApiEndpoints.cycleStatus,
            PayApiEndpoints.cycleResume,
            PayApiEndpoints.calculBulletinPay,
            PayApiEndpoints.contratActif,
            PayApiEndpoints.agentUnpaid,
            PayApiEndpoints.contratActif,
            PayApiEndpoints.agentUnpaid);


            // need to make sure we have the session variables to call it
    public static final ChainBuilder close = group("Ouverture paie group").on(
            PayApiEndpoints.cloturerBulletin);

}