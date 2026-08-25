package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.ElFileBodyPart;
import static io.gatling.javaapi.http.HttpDsl.RawFileBodyPart;
import static io.gatling.javaapi.http.HttpDsl.StringBodyPart;
import static io.gatling.javaapi.http.HttpDsl.http;

import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.core.Session;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import utils.AgentDataGenerator;
import utils.AgentFeeders;

/** Agent page API calls. */
public final class AgentApiEndpoints {

        private AgentApiEndpoints() {
        }

        private static final String WERH_API = "https://werh-api.uat.wemagnus.com";
        private static final String  WECROSS_API = "https://wecross-api.uat.wemagnus.com";
        private static final String AGENT_LIST_DATE = config("werh.agentListDate", "WERH_AGENT_LIST_DATE",
                        "2026-06-01");
        private static final String AGENT_LIST_FILTERS = encode(
                        config("werh.agentListFilters", "WERH_AGENT_LIST_FILTERS", "{\"activite\":\"1\"}"));

        private static final String AGENT_LIST_PATH = WERH_API + "/career/bff/dossier-agent/" + AGENT_LIST_DATE
                        + "/contract?filters=" + AGENT_LIST_FILTERS;

        /** The agent picked at random from the agents list, i.e. the one an existing dossier opens. */
        private static final String SELECTED_AGENT = "#{active_agent.agentId}";

        /** The agent the creation flow just created, as saved by {@link #agentCommand}. */
        private static final String CREATED_AGENT = "#{agentId}";

        // -------------------------------------------------------------------------
        // Creation-flow settings. Every one of them is overridable at launch, either
        // with a -Dproperty or with the matching environment variable.
        // -------------------------------------------------------------------------

        /** Number of letters of the generated nom and prenom. */
        private static final int NAME_LENGTH = Integer
                        .parseInt(config("werh.agentNameLength", "WERH_AGENT_NAME_LENGTH", "8"));

        /** Age bracket the generated birth date falls in. */
        private static final int AGE_MIN = Integer.parseInt(config("werh.agentAgeMin", "WERH_AGENT_AGE_MIN", "20"));
        private static final int AGE_MAX = Integer.parseInt(config("werh.agentAgeMax", "WERH_AGENT_AGE_MAX", "60"));

        /** Birth country sent by the identity section. */
        private static final String PAYS_NAISSANCE = config("werh.agentPaysNaissance", "WERH_AGENT_PAYS_NAISSANCE",
                        "FRANCE");

        /** Payment mode of the bank section ("3" is the transfer selected in the recording). */
        private static final String MODE_REGLEMENT_ID = config("werh.agentModeReglementId",
                        "WERH_AGENT_MODE_REGLEMENT_ID", "3");

        /** Completion rate the form reports once every section has been filled. */
        private static final String TAUX_COMPLETION = config("werh.agentTauxCompletion",
                        "WERH_AGENT_TAUX_COMPLETION", "88");

        /** Classpath resource uploaded as the supporting document of each section. */
        private static final String DOCUMENT_FILE = config("werh.agentDocumentFile", "WERH_AGENT_DOCUMENT_FILE",
                        "data/justificatif.pdf");

        /** Age bracket the generated birth date of a child falls in. */
        private static final int ENFANT_AGE_MIN = Integer
                        .parseInt(config("werh.enfantAgeMin", "WERH_ENFANT_AGE_MIN", "0"));
        private static final int ENFANT_AGE_MAX = Integer
                        .parseInt(config("werh.enfantAgeMax", "WERH_ENFANT_AGE_MAX", "18"));

        /**
         * Age at which the supplement familial de traitement stops being due, i.e. what the
         * "dateEcheance" of a child is computed from.
         */
        private static final int ENFANT_SFT_AGE_LIMITE = Integer
                        .parseInt(config("werh.enfantSftAgeLimite", "WERH_ENFANT_SFT_AGE_LIMITE", "20"));

        /** SFT flags of the child form, as ticked in the recording. */
        private static final String ENFANT_CALCUL_SFT = config("werh.enfantCalculSft", "WERH_ENFANT_CALCUL_SFT",
                        "true");
        private static final String ENFANT_REVERSEMENT_SFT = config("werh.enfantReversementSft",
                        "WERH_ENFANT_REVERSEMENT_SFT", "false");

        private static final String DOCUMENT_UPLOAD_PATH = WECROSS_API + "/storage/doc/batch?type=APP";

        private static String config(String property, String envVariable, String defaultValue) {
                String value = System.getProperty(property, System.getenv(envVariable));
                return value != null ? value : defaultValue;
        }

        private static String encode(String value) {
                return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }

        /** Address types used by the agent search/list filters. */
        public static final HttpRequestActionBuilder addressTypes = http("Agent address types")
                        .get(WERH_API + "/agent/type/adresse")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /** Civilities used by the agent search/list filters. */
        public static final HttpRequestActionBuilder civilities = http("Agent civilities")
                        .get(WERH_API + "/agent/civilite")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /**
         * Loads the list of active agent contracts for the configured payroll month,
         * and captures the
         * first row's agent and contract ids ("agentId"/"contratId") so the
         * agent-detail and payslip
         * calls can target that agent.
         */
        public static final HttpRequestActionBuilder contracts = http("Agent contracts")
                        .get(AGENT_LIST_PATH)
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));
        // .check(
        // jsonPath("$[0].agentId").saveAs("agentId"),
        // jsonPath("$[0].contratId").saveAs("contratId"));

        public static final HttpRequestActionBuilder listeAgents = http("Agent contracts")
                        .get(WERH_API + "/career/bff/dossier-agent/" + AGENT_LIST_DATE + "/contract")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                        .check(jmesPath("[*].{agentId: agentId, contratId: contratId, fonctionId: fonctionId,"
                                        + " droitId: droitId, statutId: statutId, collectiviteId: collectiviteId,"
                                        + " etablissementId: etablissementId}")
                                        .ofList().saveAs("agents"),
                                        jsonPath("$[?(@.droitId)]").ofMap().findRandom().saveAs("active_agent"));

        // -------------------------------------------------------------------------
        // Dossier sections. Each one is built by a factory taking the Gatling
        // expression that resolves the agent id, so the same call can target either
        // the agent picked from the list or the one the creation flow just created.
        // -------------------------------------------------------------------------

        private static HttpRequestActionBuilder agentSection(String requestName, String path, String agentIdEl) {
                return http(requestName)
                                .get(WERH_API + path + agentIdEl)
                                .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));
        }

        /** Agent latest situation, the first call fired when an agent is opened. */
        public static HttpRequestActionBuilder latestSituationOf(String agentIdEl) {
                return http("Agent latest situation")
                                .post(WERH_API + "/career/bff/agents-with-latest-situation/" + agentIdEl + "/last")
                                .headers(ApiHeaders.bearerWithTenant("accept", "application/json", "content-type",
                                                "application/json"));
        }

        /** Agent core record (fired as the detail tabs mount). */
        public static HttpRequestActionBuilder agentDetailOf(String agentIdEl) {
                return agentSection("Agent detail", "/agent/agent/", agentIdEl);
        }

        /** Agent identity section. */
        public static HttpRequestActionBuilder agentIdentiteOf(String agentIdEl) {
                return agentSection("Agent identite", "/agent/agent/identite/", agentIdEl);
        }

        /** Agent address section. */
        public static HttpRequestActionBuilder agentAdresseOf(String agentIdEl) {
                return agentSection("Agent adresse", "/agent/agent/adresse/", agentIdEl);
        }

        /** Agent birth section. */
        public static HttpRequestActionBuilder agentNaissanceOf(String agentIdEl) {
                return agentSection("Agent naissance", "/agent/agent/naissance/", agentIdEl);
        }

        /** Agent contact section. */
        public static HttpRequestActionBuilder agentContactOf(String agentIdEl) {
                return agentSection("Agent contact", "/agent/agent/contact/", agentIdEl);
        }

        /** Agent bank details section. */
        public static HttpRequestActionBuilder agentBankOf(String agentIdEl) {
                return agentSection("Agent bank details", "/agent/agent/domiciliationBancaire/", agentIdEl);
        }

        /** Agent latest situation, the first call fired when an agent is opened. */
        public static final HttpRequestActionBuilder latestSituation = latestSituationOf(SELECTED_AGENT);

        /** Agent core record (fired as the detail tabs mount). */
        public static final HttpRequestActionBuilder agentDetail = agentDetailOf(SELECTED_AGENT);

        /** Agent identity section. */
        public static final HttpRequestActionBuilder agentIdentite = agentIdentiteOf(SELECTED_AGENT);

        /** Agent address section. */
        public static final HttpRequestActionBuilder agentAdresse = agentAdresseOf(SELECTED_AGENT);

        /** Agent birth section. */
        public static final HttpRequestActionBuilder agentNaissance = agentNaissanceOf(SELECTED_AGENT);

        /** Agent contact section. */
        public static final HttpRequestActionBuilder agentContact = agentContactOf(SELECTED_AGENT);

        /** Agent bank details section. */
        public static final HttpRequestActionBuilder agentBank = agentBankOf(SELECTED_AGENT);

        /** Contract detail for the selected agent contract. */
        public static final HttpRequestActionBuilder contratDetail = http("Contract detail")
                        .get(WERH_API + "/career/contrat/#{active_agent.contratId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                        .check(jsonPath("$.situations[0].statut_id").saveAs("contratStatutId"));

        public static final HttpRequestActionBuilder droitStatus = http("Droit status")
                        .get(WERH_API + "/career/droit/status/#{contratStatutId}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        // TODO check for agent with multiple contracts
        public static final HttpRequestActionBuilder calculBulletin = http("Calculer bulletin")
                        .post(WERH_API + "/pay/paie/cycle-paie/#{cyclePaieId}/agent/#{active_agent.agentId}/contrat/#{active_agent.contratId}/calculerBulletin")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        public static String genererNomPrenom(int longueur) {
                String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
                Random random = ThreadLocalRandom.current();
                StringBuilder result = new StringBuilder(longueur);
                for (int i = 0; i < longueur; i++) {
                        result.append(CHARS.charAt(random.nextInt(CHARS.length())));
                }
                return result.toString();
        }

        // -------------------------------------------------------------------------
        // Agent creation
        // -------------------------------------------------------------------------

        /**
         * Draws the identity the creation dialog is filled with: the "civilite" comes from the
         * civilites feeder — along with the "sexeInsee" digit {@link #setAgentFormData} needs — and
         * the names are generated, "nomNaissance" being what the form pre-fills from the nom
         * d'usage.
         */
        public static final ChainBuilder setDataAgent = exec(
                        feed(AgentFeeders.CIVILITES),
                        exec(session -> session
                                        .set("nomUsage", AgentDataGenerator.nom(NAME_LENGTH))
                                        .set("prenom", AgentDataGenerator.prenom(NAME_LENGTH))
                                        .set("nomNaissance", AgentDataGenerator.nom(NAME_LENGTH))));

        /**
         * Fills the rest of the agent form: birth, contact, address, bank and family sections.
         *
         * <p>The enumerated fields come from the feeders, which write straight into the session
         * attributes the body template reads — the two commune feeds fill the birth place and the
         * address. The exec then computes what has to agree with them: the "genre" from the
         * civilite, the numero de securite sociale from the sex digit, the birth date and the INSEE
         * code of the birth commune, the IBAN from the account number, and the conjoint fields only
         * when the family status drawn calls for them.
         *
         * <p>The three "document" attributes hold the JSON arrays of supporting documents: they
         * start out empty and {@link #collectDocuments} fills them in once the uploads have
         * answered.
         *
         * <p>The civilite is drawn only when the session does not carry one yet, so the chain can
         * run on its own to refill an existing agent without contradicting the civilite
         * {@link #setDataAgent} already sent to the creation dialog.
         */
        public static final ChainBuilder setAgentFormData = exec(
                        doIf(session -> !session.contains("sexeInsee")).then(feed(AgentFeeders.CIVILITES)),
                        feed(AgentFeeders.communes("communeNaissance")),
                        feed(AgentFeeders.communes("communeAdresse")),
                        feed(AgentFeeders.VOIES),
                        feed(AgentFeeders.TYPES_ADRESSE),
                        feed(AgentFeeders.SITUATIONS_FAMILIALES),
                        exec(session -> {
                                String nomUsage = session.getString("nomUsage");
                                String prenom = session.getString("prenom");
                                String civilite = session.getString("civilite");

                                String dateNaissance = AgentDataGenerator.dateNaissance(AGE_MIN, AGE_MAX);
                                String numInsee = AgentDataGenerator.numInsee(
                                                session.getString("sexeInsee"),
                                                dateNaissance,
                                                session.getString("communeNaissanceCodeInsee"));
                                String numeroCompte = AgentDataGenerator.numeroCompte();
                                String cleRib = AgentDataGenerator.cleRib(numeroCompte);
                                boolean avecConjoint = Boolean.parseBoolean(session.getString("avecConjoint"));

                                return session
                                                // identity section
                                                .set("genre", "MADAME".equals(civilite) ? "FEMININ" : "MASCULIN")
                                                // birth section
                                                .set("dateNaissance", dateNaissance)
                                                .set("paysNaissance", PAYS_NAISSANCE)
                                                .set("numInsee", numInsee)
                                                .set("cleInsee", AgentDataGenerator.cleInsee(numInsee))
                                                // contact section
                                                .set("emailPerso", AgentDataGenerator.email(prenom, nomUsage))
                                                .set("telephoneDomicile", AgentDataGenerator.telephone())
                                                // address section
                                                .set("adresseNumero", AgentDataGenerator.adresseNumero())
                                                .set("adresseBatImmRes", AgentDataGenerator.adresseBatiment())
                                                // bank section
                                                .set("modeReglementId", MODE_REGLEMENT_ID)
                                                .set("titulaireCompte", nomUsage + " " + prenom)
                                                .set("numeroCompte", numeroCompte)
                                                .set("cleRib", cleRib)
                                                .set("iban", AgentDataGenerator.iban(numeroCompte, cleRib))
                                                // family section: the conjoint fields are written
                                                // unquoted in the body template, so they hold a JSON
                                                // literal rather than a value
                                                .set("nomConjoint", AgentDataGenerator.jsonNullable(
                                                                avecConjoint ? AgentDataGenerator.nom(NAME_LENGTH)
                                                                                : null))
                                                .set("prenomConjoint", AgentDataGenerator.jsonNullable(
                                                                avecConjoint ? AgentDataGenerator.prenom(NAME_LENGTH)
                                                                                : null))
                                                .set("telephoneConjoint", AgentDataGenerator.jsonNullable(
                                                                avecConjoint ? AgentDataGenerator.telephone() : null))
                                                .set("enfants", "[]")
                                                // supporting documents
                                                .set("docNaissanceFileName", "acte-naissance.pdf")
                                                .set("docAdresseFileName", "justificatif-domicile.pdf")
                                                .set("docBancaireFileName", "releve-identite-bancaire.pdf")
                                                .set("naissanceDocuments", "[]")
                                                .set("adresseDocuments", "[]")
                                                .set("domiciliationBancaireDocuments", "[]")
                                                .set("tauxCompletion", TAUX_COMPLETION);
                        }));

        /** Checks the nom/prenom pair is free before the creation dialog is submitted. */
        public static final HttpRequestActionBuilder checkPresent = http("checkIfPresent")
                .get(WERH_API + "/agent/agentQuery/checkIsPresent?nomUsage=#{nomUsage}&prenom=#{prenom}")
                .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                .check(bodyString().in("true", "false"));

        /**
         * Creates the agent from the creation dialog and saves the identifiers the rest of the flow
         * targets: "agentId" and the "matricule" the back office allocates.
         */
        public static final HttpRequestActionBuilder agentCommand = http("agentCreate")
                .post(WERH_API + "/agent/agentCommand")
                .headers(ApiHeaders.bearerForAllTenants("accept", "application/json", "content-type",
                        "application/json"))
                .body(StringBody("""
                        {"nomUsage":"#{nomUsage}","civilite":"#{civilite}","prenom":"#{prenom}"}
                        """))
                .check(jsonPath("$.agentId").saveAs("agentId"), jsonPath("$.matricule").saveAs("matricule"));

        /**
         * Uploads one supporting document and saves the metadata the agent payload sends back:
         * "&lt;prefix&gt;StorageId", "Nom", "Taille" and "Date". The prefix is the session attribute
         * prefix of the section the document belongs to, e.g. "docNaissance".
         */
        private static HttpRequestActionBuilder uploadDocument(String requestName, String attributePrefix) {
                String fileName = "#{" + attributePrefix + "FileName}";
                return http(requestName)
                                .post(DOCUMENT_UPLOAD_PATH)
                                .headers(ApiHeaders.bearerWithTenant("accept", "application/json, text/plain, */*"))
                                .asMultipartForm()
                                .bodyPart(RawFileBodyPart("files", DOCUMENT_FILE)
                                                .fileName(fileName)
                                                .contentType("application/pdf"))
                                .bodyPart(StringBodyPart("docRequests", "[{\"fileName\":\"" + fileName + "\"}]")
                                                .fileName("blob")
                                                .contentType("application/json"))
                                .check(jsonPath("$.documents[0].id").saveAs(attributePrefix + "StorageId"),
                                                jsonPath("$.documents[0].name").saveAs(attributePrefix + "Nom"),
                                                jsonPath("$.documents[0].size").saveAs(attributePrefix + "Taille"),
                                                jsonPath("$.documents[0].createdDate")
                                                                .saveAs(attributePrefix + "Date"));
        }

        /** Uploads the birth certificate attached to the identity section. */
        public static final HttpRequestActionBuilder uploadDocumentNaissance =
                        uploadDocument("Upload document naissance", "docNaissance");

        /** Uploads the proof of address attached to the address section. */
        public static final HttpRequestActionBuilder uploadDocumentAdresse =
                        uploadDocument("Upload document adresse", "docAdresse");

        /** Uploads the RIB attached to the bank section. */
        public static final HttpRequestActionBuilder uploadDocumentBancaire =
                        uploadDocument("Upload document bancaire", "docBancaire");

        /**
         * Turns the metadata returned by the uploads into the document arrays of the agent payload.
         * A section whose upload did not run keeps an empty array, so the payload stays valid when
         * the flow skips the uploads.
         */
        public static final ChainBuilder collectDocuments = exec(session -> session
                        .set("naissanceDocuments", documentArray(session, "docNaissance"))
                        .set("adresseDocuments", documentArray(session, "docAdresse"))
                        .set("domiciliationBancaireDocuments", documentArray(session, "docBancaire")));

        private static String documentArray(Session session, String attributePrefix) {
                String storageId = session.getString(attributePrefix + "StorageId");
                if (storageId == null) {
                        return "[]";
                }
                return """
                                [{"storageId":"%s","nom":"%s","taille":%s,"type":"DEFAULT","date":"%s","rubrique":"APP"}]"""
                                .formatted(
                                                storageId,
                                                session.getString(attributePrefix + "Nom"),
                                                session.getString(attributePrefix + "Taille"),
                                                session.getString(attributePrefix + "Date"));
        }

        /**
         * Saves the whole agent form: identity, birth, contact, address, bank, family and the
         * supporting documents. Expects {@link #setAgentFormData} to have filled the session and
         * {@link #agentCommand} to have saved "agentId" and "matricule".
         */
        public static final HttpRequestActionBuilder updateAgent = http("agentUpdate")
                        .put(WERH_API + "/agent/agentCommand/" + CREATED_AGENT)
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json", "content-type",
                                        "application/json"))
                        .body(ElFileBody("bodies/agentUpdate.json"))
                        .check(jsonPath("$.id").isEL(CREATED_AGENT));

        /** Reports the completion rate the form reaches once the sections have been saved. */
        public static final HttpRequestActionBuilder tauxCompletion = http("tauxCompletion")
                        .put(WERH_API + "/agent/agentQuery/" + CREATED_AGENT + "/tauxCompletion?taux=#{tauxCompletion}")
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        // -------------------------------------------------------------------------
        // Adding a child to the agent
        // -------------------------------------------------------------------------

        /**
         * Draws a child of the agent: the genre comes from its feeder — a child has no civilite to
         * derive it from — and the exec computes the "dateEcheance" the SFT runs to from the birth
         * date drawn.
         *
         * <p>"enfantDocumentId" is the identifier the form allocates to the supporting document
         * before sending it. The payload carries it three times, so the back office can match the
         * item, the document metadata and the file content of the same multipart request.
         */
        public static final ChainBuilder setEnfantData = exec(
                        feed(AgentFeeders.GENRES_ENFANT),
                        exec(session -> {
                                String dateNaissance = AgentDataGenerator.dateNaissance(ENFANT_AGE_MIN,
                                                ENFANT_AGE_MAX);
                                return session
                                                .set("enfantNomUsage", AgentDataGenerator.nom(NAME_LENGTH))
                                                .set("enfantPrenom", AgentDataGenerator.prenom(NAME_LENGTH))
                                                .set("enfantDateNaissance", dateNaissance)
                                                .set("enfantDateEcheance", AgentDataGenerator.dateEcheanceSft(
                                                                dateNaissance, ENFANT_SFT_AGE_LIMITE))
                                                .set("enfantCalculSft", ENFANT_CALCUL_SFT)
                                                .set("enfantReversementSft", ENFANT_REVERSEMENT_SFT)
                                                .set("enfantDocumentId", AgentDataGenerator.documentId())
                                                .set("enfantDocumentFileName", "acte-naissance-enfant.pdf");
                        }));

        /**
         * Adds the child to the agent, in one multipart request: the child itself, the documents it
         * gains, and the content of each of them — the file part is named after the document id,
         * which is how the back office ties the content to the metadata.
         *
         * <p>Expects {@link #setEnfantData} to have filled the session and the agent to be the one
         * {@link #agentCommand} created. Saves the child as "enfantId".
         */
        public static final HttpRequestActionBuilder addEnfant = http("Ajouter enfant")
                        .post(WERH_API + "/agent/agentCommand/enfant/" + CREATED_AGENT)
                        .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                        .asMultipartForm()
                        .bodyPart(ElFileBodyPart("item", "bodies/enfantItem.json")
                                        .fileName("blob")
                                        .contentType("application/json"))
                        .bodyPart(ElFileBodyPart("documentChanges", "bodies/enfantDocumentChanges.json")
                                        .fileName("blob")
                                        .contentType("application/json"))
                        .bodyPart(RawFileBodyPart("addedDocumentsContent", DOCUMENT_FILE)
                                        .fileName("#{enfantDocumentId}")
                                        .contentType("application/pdf"))
                        .check(jsonPath("$.agentId").isEL(CREATED_AGENT),
                                        jsonPath("$.id").saveAs("enfantId"));

        // Dossier sections of the agent the creation flow just created, reloaded by the
        // page once the form has been saved.

        public static final HttpRequestActionBuilder newAgentLatestSituation = latestSituationOf(CREATED_AGENT);
        public static final HttpRequestActionBuilder newAgentDetail = agentDetailOf(CREATED_AGENT);
        public static final HttpRequestActionBuilder newAgentIdentite = agentIdentiteOf(CREATED_AGENT);
        public static final HttpRequestActionBuilder newAgentAdresse = agentAdresseOf(CREATED_AGENT);
        public static final HttpRequestActionBuilder newAgentNaissance = agentNaissanceOf(CREATED_AGENT);
        public static final HttpRequestActionBuilder newAgentContact = agentContactOf(CREATED_AGENT);
        public static final HttpRequestActionBuilder newAgentBank = agentBankOf(CREATED_AGENT);

        // -------------------------------------------------------------------------
        // Referentials loaded by the creation form
        // -------------------------------------------------------------------------

        public static final HttpRequestActionBuilder cities = http("cities")
                .get(WECROSS_API + "/city/ville?size=100")
                .headers(ApiHeaders.bearerWithTenant("accept", "application/json, text/plain, */*"))
                .check(jsonPath("$[0].designation").exists());

        public static final HttpRequestActionBuilder countries = http("countries")
                .get(WECROSS_API + "/city/country")
                .headers(ApiHeaders.bearerWithTenant("accept", "application/json, text/plain, */*"))
                .check(jsonPath("$[0].designation").exists());

        public final HttpRequestActionBuilder adresse = http("typeAdresse")
                .get(WERH_API + "/agent/type/adresse")
                .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        public static final HttpRequestActionBuilder civilites = http("civilities")
                .get(WERH_API + "/agent/civilite")
                .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

        /**
         * Banking institutions of the bank section; saves one of them at random as
         * "etablissementBancaireId", which the agent payload sends.
         */
        public static final HttpRequestActionBuilder etablissementBancaire = http("etabBancaires")
                .get(WERH_API + "/context/v1/etablissementBancaire")
                .headers(ApiHeaders.bearerWithTenant("accept", "application/json"))
                .check(jsonPath("$[0].nom").exists(),
                        jsonPath("$[*].id").findRandom().saveAs("etablissementBancaireId"));

        public static final HttpRequestActionBuilder situationFamiliale = http("situationFamiliale")
                .get(WERH_API + "/agent/agentQuery/situationFamiliale")
                .headers(ApiHeaders.bearerWithTenant("accept", "application/json"));

}
