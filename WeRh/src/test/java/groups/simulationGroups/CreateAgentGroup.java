package groups.simulationGroups;

import endpoints.apiEndpoints.AgentApiEndpoints;
import endpoints.apiEndpoints.ReferentialApiEndpoints;
import io.gatling.javaapi.core.ChainBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.group;
import static io.gatling.javaapi.core.CoreDsl.pause;
import static io.gatling.javaapi.core.CoreDsl.repeat;

/**
 * Creation of an agent, from the creation dialog to the dossier the page reloads once the form has
 * been saved.
 *
 * <p>The flow is split in three chains so a scenario can stop after any of them: {@link #create}
 * opens the dialog and creates the agent, {@link #fillForm} uploads the supporting documents and
 * saves the whole form, {@link #reloadDossier} replays the burst of calls the dossier fires
 * afterwards. {@link #open} runs the three of them, with the pauses of the recorded session.
 *
 * <p>The values the form sends are drawn per virtual user by
 * {@link AgentApiEndpoints#setDataAgent} and {@link AgentApiEndpoints#setAgentFormData}; see
 * {@code utils.AgentDataGenerator} for what varies and the {@code werh.agent*} launch properties
 * for what can be pinned.
 */
public class CreateAgentGroup {

    private CreateAgentGroup() {}

    /** Creation dialog: draws an identity, checks it is free, and creates the agent. */
    public static final ChainBuilder create =
            group("Create Agent").on(
                    AgentApiEndpoints.setDataAgent,
                    AgentApiEndpoints.checkPresent,
                    AgentApiEndpoints.agentCommand);

    /** Referentials the creation form loads to feed its dropdowns. */
    public static final ChainBuilder loadReferentials =
            group("Create Agent referentials").on(
                    AgentApiEndpoints.cities,
                    AgentApiEndpoints.countries,
                    AgentApiEndpoints.addressTypes,
                    AgentApiEndpoints.civilites,
                    AgentApiEndpoints.etablissementBancaire,
                    AgentApiEndpoints.situationFamiliale);

    /**
     * Filling the form: the three supporting documents are uploaded as they are attached, then the
     * whole agent is saved and the form reports its completion rate.
     */
    public static final ChainBuilder fillForm =
            group("Fill Agent form").on(
                    AgentApiEndpoints.setAgentFormData,
                    AgentApiEndpoints.uploadDocumentBancaire,
                    pause(Duration.ofMillis(500)),
                    AgentApiEndpoints.uploadDocumentAdresse,
                    pause(Duration.ofMillis(500)),
                    AgentApiEndpoints.uploadDocumentNaissance,
                    AgentApiEndpoints.collectDocuments,
                    pause(1),
                    AgentApiEndpoints.updateAgent,
                    AgentApiEndpoints.tauxCompletion);

    /**
     * Dossier of the freshly created agent, in the order recorded after the save: the context
     * referentials, then each section interleaved with a reload of the agent record.
     */
    public static final ChainBuilder reloadDossier =
            group("Open new agent dossier").on(
                    ReferentialApiEndpoints.etablissements
                            .resources(ReferentialApiEndpoints.collectivites),
                    AgentApiEndpoints.addressTypes,
                    AgentApiEndpoints.newAgentLatestSituation,
                    AgentApiEndpoints.newAgentDetail,
                    AgentApiEndpoints.newAgentIdentite,
                    AgentApiEndpoints.newAgentDetail,
                    AgentApiEndpoints.newAgentAdresse,
                    AgentApiEndpoints.newAgentDetail,
                    AgentApiEndpoints.newAgentBank,
                    AgentApiEndpoints.newAgentBank,
                    AgentApiEndpoints.newAgentDetail,
                    AgentApiEndpoints.newAgentNaissance,
                    AgentApiEndpoints.newAgentDetail,
                    AgentApiEndpoints.newAgentContact,
                    AgentApiEndpoints.newAgentDetail,
                    ReferentialApiEndpoints.fonction);

    /**
     * Adds one child to the agent: a single multipart request carrying the child and its
     * supporting document. Runs on the agent {@link #create} created, so it goes after
     * {@link #fillForm}.
     */
    public static final ChainBuilder addEnfant =
            group("Add enfant").on(
                    AgentApiEndpoints.setEnfantData,
                    AgentApiEndpoints.addEnfant);

    /** Adds {@code nombre} children, each drawn on its own. */
    public static ChainBuilder addEnfants(int nombre) {
        return repeat(nombre).on(exec(addEnfant, pause(2)));
    }

    /** The whole creation journey, with the pauses of the recorded session. */
    public static final ChainBuilder open =
            exec(
                    create,
                    loadReferentials,
                    pause(3),
                    fillForm,
                    pause(1),
                    reloadDossier);

    /** The creation journey, then {@code nombreEnfants} children added to the new agent. */
    public static ChainBuilder openWithEnfants(int nombreEnfants) {
        return exec(open, pause(2), addEnfants(nombreEnfants));
    }
}
