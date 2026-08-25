package groups.simulationGroups;

import endpoints.apiEndpoints.PayApiEndpoints;
import endpoints.webEndpoints.WebPages;
import io.gatling.javaapi.core.ChainBuilder;

import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;

public class DematerialisationGroup {

    private DematerialisationGroup() {    }


    //TODO corriger
    public static final ChainBuilder open = group("Dematerialisation").on(
            WebPages.dematerialisationpage,
            PayApiEndpoints.spreadActiveCycle,
            PayApiEndpoints.cycleStatus,
            pause(1),
            PayApiEndpoints.generateFileDemat,
            pause(2),
            PayApiEndpoints.generatedFileDemat,
            pause(2),
            PayApiEndpoints.documentsAgent,
            PayApiEndpoints.dematPivotStream
            );

}
