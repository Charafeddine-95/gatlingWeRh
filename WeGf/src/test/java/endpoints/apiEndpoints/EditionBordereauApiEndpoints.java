package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.bodyLength;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Edition de bordereau": the screen that lists the bordereaux already created with their
 * amounts, and what the API is asked once the user ticks some of them. Picks up where
 * {@code ExecutionApiEndpoints.fournirListeBordereauxAvecMontant} — the grid itself — stops.
 *
 * <p>Same referencing JSON dialect as {@code NumerotationApiEndpoints}: an object is written
 * once with an {@code "@id"}, later occurrences are the bare number ({@code "etat":3}), and
 * the numbering is scoped to each top-level argument of the request. The bordereaux the user
 * ticked are sent back to seven of the calls below, so they are kept from the grid response
 * rather than rebuilt — see {@link #choisirBordereaux} and {@link #bordereauListe}.
 */
public final class EditionBordereauApiEndpoints {

        private EditionBordereauApiEndpoints() {
        }

        /** Jackson comes with Gatling; used only by {@link #compact}. */
        private static final ObjectMapper JSON = new ObjectMapper();

        /**
         * Strips the indentation the API answers with. {@code exerciceComptableJson} is re-sent
         * almost verbatim on two of the calls below, and left as-is it would put ~34 KB on the
         * wire where the browser puts ~15 KB — enough to skew what the report says about the
         * request. Key order and values are preserved.
         */
        private static String compact(String json) {
                try {
                        return JSON.readTree(json).toString();
                } catch (JsonProcessingException e) {
                        throw new IllegalStateException("reponse JSON illisible: " + e.getMessage(), e);
                }
        }

        /**
         * Compacts a captured list and drops {@code nombreTotalElements}: the field is part of
         * the {@code TableauEntitePersistante} the API answers with, but the screen does not send
         * it back, and this payload is kept to what the screen sends.
         */
        private static String sansTotal(String json) {
                try {
                        ObjectNode liste = (ObjectNode) JSON.readTree(json);
                        liste.remove("nombreTotalElements");
                        return liste.toString();
                } catch (JsonProcessingException e) {
                        throw new IllegalStateException("reponse JSON illisible: " + e.getMessage(), e);
                }
        }

        /**
         * Ticks the bordereau this virtual user owns and builds the two shapes the calls below
         * need: the row itself ({@code selectionBordereauxJson}, spliced into
         * {@link #bordereauListe}) and its id alone ({@code selectionBordereauIds}, for the two
         * calls that take a bare id array). {@code nbSelectionBordereau} is how many were taken,
         * which drives the {@code fournirTailleBordereauPourPES} loop.
         *
         * <p>Takes the whole page, because the page is already this user's own single bordereau:
         * {@code ExecutionApiEndpoints.reserverBordereau} gives every user a {@code pageIndex} of
         * its own and the grid is asked with a page size of 1. That is what keeps two concurrent
         * users off the same bordereau — ticking from the top of a full grid had them all land on
         * row 0, and the API answered ERR_ACCES_CONCURRENT_MODIF on
         * {@link #processModifierBordereauListe} and a SQL Server deadlock (500) on
         * {@link #envoyerNouveauDossierExecutionPlusTard}.
         *
         * <p>Letting the server paginate also sidesteps the {@code @id} problem that made an
         * arbitrary row unusable: a row only ever points at an object written out by an earlier
         * row, so lifting row 20 out of a full page leaves it with dangling references, and only
         * a prefix could be taken. A one-row page is written out self-contained.
         *
         * <p>Also compacts {@code exerciceComptableJson} in place — see {@link #compact}. Must
         * run after {@code ExecutionApiEndpoints.fournirListeBordereauxAvecMontant}, whose
         * response it reads.
         */
        public static final ChainBuilder choisirBordereaux = exec(session -> {
                // An empty page fails the grid checks, so the rows never reach the session at
                // all; caught here rather than as "No attribute named bordereauRows".
                if (!session.contains("bordereauRows")) {
                        throw new IllegalStateException("page " + session.getInt("slotBordereau")
                                        + " de la grille vide: il y a moins de bordereaux disponibles que"
                                        + " d'utilisateurs virtuels");
                }
                List<String> rows = session.getList("bordereauRows");
                List<String> ids = session.getList("idBordereaux");
                return session
                                .set("nbSelectionBordereau", rows.size())
                                .set("selectionBordereauxJson", String.join(",", rows))
                                .set("selectionBordereauIds", String.join(",", ids))
                                .set("exerciceComptableJson", compact(session.getString("exerciceComptableJson")));
        });

        /**
         * The ticked bordereaux, rebuilt as the list argument seven of the calls below expect.
         * {@code type} is the wrapper class name, which differs per endpoint
         * ({@code Association}, or {@code TableauEntitePersistante} for the PJ call). The
         * wrapper takes {@code @id} 1, just under the rows' own range (2 and up), so it cannot
         * clash with them.
         */
        private static String bordereauListe(String type) {
                return "{\"donnees\":[#{selectionBordereauxJson}],\"@id\":1,\"@type\":\"" + type + "\"}";
        }

        /**
         * The same bordereaux as {@link #bordereauListe}, but as
         * {@link #processModifierBordereauListe} answered them: that call bumps their
         * {@code marque} and drops the four {@code montant*} fields, and every call after it
         * sends the rows it returned rather than the ones the grid gave. Sending the grid rows
         * on instead is what the API rejects with "Error parsing type weGfOrdonnancement".
         */
        private static String bordereauListeModifie(String type) {
                return "{\"donnees\":[#{bordereauxModifiesJson}],\"@id\":1,\"@type\":\"" + type + "\"}";
        }

        /**
         * Sizes the PES flux for one bordereau. The screen calls it once per ticked row, so the
         * body reads the {@code indexBordereau} counter of the surrounding repeat rather than a
         * fixed index — see {@code OrdonnancementSelectionLiquidationsGroup.editionBordereau}.
         * The body is built by a function, and Gatling does not run EL over what a function
         * returns: {@code idBudget} has to be read off the session here, not written as
         * {@code #{idBudget}}, or the placeholder goes out verbatim and the API answers 500.
         */
        public static final HttpRequestActionBuilder fournirTailleBordereauPourPES = http(
                        "Fournir taille bordereau pour PES")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirTailleBordereauPourPES?fieldNames%5B%5D=**")
                        .body(StringBody(session -> "{\"bordereauId\":"
                                        + session.getList("idBordereaux").get(session.getInt("indexBordereau"))
                                        + ",\"idBudget\":" + session.getInt("idBudget") + "}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        /**
         * The liquidations carried by the ticked bordereaux, i.e. the detail the screen unfolds
         * under them. Same endpoint name as the ordonnancement grid's
         * {@code ExecutionApiEndpoints.fournirListeLiquidations}, but selected on
         * {@code listeIdBordereau} instead of on a serie, and asking for every field.
         */
        public static final HttpRequestActionBuilder fournirListeLiquidationsParBordereaux = http(
                        "Fournir liste liquidations par bordereaux")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListeLiquidations?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        "{\"selCrit\":{\"id\":-1,\"marque\":0,\"listeIdBordereau\":[#{selectionBordereauIds}],\"@id\":2,\"@type\":\"SelectionLiquidation\"}}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jsonPath("$.donnees[0].id").ofInt().gt(0));

        /**
         * Attachments still to transmit for the ticked bordereaux. Note the list wrapper is a
         * {@code TableauEntitePersistante} here, unlike the three calls below.
         */
        public static final HttpRequestActionBuilder fournirListePJATransmettre = http(
                        "Fournir liste PJ a transmettre")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListePJATransmettreDepuisListeBordereaux?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        "{\"param\":{\"listeCriteres\":[],\"listeCriteresRechercheGui\":[],\"listeAttributs\":[],\"listeTris\":[],\"distinct\":false,\"paginatorValues\":{\"length\":1000,\"pageIndex\":0,\"pageSize\":1000,\"previousPageIndex\":0},\"@id\":2,\"@type\":\"RechercheParametres\"},"
                                                        + "\"sortValue\":{\"active\":\"tiers\",\"direction\":\"asc\"},"
                                                        + "\"bordereaux\":" + bordereauListe("TableauEntitePersistante")
                                                        + ",\"idBudget\":#{idBudget}}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0))
                        // Kept whole: the generation payload sends this list back as
                        // pjAPASTransmettreAfficheeListe, wrapper included — the response already
                        // has the TableauEntitePersistante shape that field expects.
                        .check(bodyString().saveAs("pjATransmettreJson"));

        /**
         * Signatory to print on the bordereaux. Re-sends the whole exercice comptable; it is its
         * own argument, so it keeps the numbering the API answered with and needs no shifting.
         */
        public static final HttpRequestActionBuilder fournirSignataireActifCollectivite = http(
                        "Fournir signataire actif collectivite")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirSignataireActifCollectiteByCollectiviteId?fieldNames%5B%5D=**")
                        .body(StringBody("{\"bordereauListe\":" + bordereauListe("Association")
                                        + ",\"idCollectivite\":1,\"exerciceComptable\":#{exerciceComptableJson}}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        /**
         * Default signatory label, e.g. "Patrick GADROY-LEGENVRE, Maire". Same payload as above
         * minus the collectivite id, with the bordereaux under {@code bordereauRecetteListe}.
         * Answers a bare JSON string, so only its length is checked.
         */
        public static final HttpRequestActionBuilder rechercherLibelleSignataireParDefaut = http(
                        "Rechercher libelle signataire par defaut")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/rechercherLibelleSignataireParDefaut?fieldNames%5B%5D=**")
                        .body(StringBody("{\"bordereauRecetteListe\":" + bordereauListe("Association")
                                        + ",\"exerciceComptable\":#{exerciceComptableJson}}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(bodyLength().gt(0));

        /**
         * Asks whether any asset sheet behind the ticked bordereaux is incomplete. Answered a
         * bare {@code false} in the recording, so only the length is checked. The numerotation
         * flow asks the same question the other way round, per liquidation
         * ({@code NumerotationApiEndpoints.hasBienIncompletPourGenerationBienPjMandat}).
         */
        public static final HttpRequestActionBuilder liquidationsParBordereauxHasBienIncomplet = http(
                        "Liquidations par bordereaux has bien incomplet")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/liquidationsParBordereauxHasBienIncomplet")
                        .body(StringBody("{\"idsBordereaux\":[#{selectionBordereauIds}]}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(bodyLength().gt(0));

        /** The budget as the circuit call sends it: the collectivite's own configuration. */
        private static final String BUDGET = "{\"id\":#{userContextCBE.exercice.budget.id},\"marque\":2,\"etat\":{\"_id\":4,\"_lib\":\"RIEN_A_FAIRE\",\"@id\":3,\"@type\":\"EtatEnum\"},\"etatPrecedent\":3,\"code\":\"ST\",\"libelle\":\"SAINT ALBAN D'HURTIERES\",\"nic\":\"00014\",\"budgetPrincipal\":true,\"budgetAutonome\":false,\"codificationTresorerie1\":\"037\",\"codificationTresorerie2\":\"00\",\"regimeFiscal\":{\"_id\":1,\"_lib\":\"TTC\",\"@id\":4,\"@type\":\"TypeRegimeFiscal\"},\"sectionsGerees\":{\"_id\":1,\"_lib\":\"Fonctionnement et Investissement\",\"libelleCollectivite\":\"Fonctionnement et Investissement\",\"libelleEHPAD\":\"Exploitation et Investissement\",\"@id\":5,\"@type\":\"TypeGestionSections\"},\"collectiviteRef\":{\"id\":1,\"marque\":4,\"etat\":3,\"etatPrecedent\":3,\"code\":\"SAIN\",\"numeroSiren\":\"217302207\",\"titreResponsable\":\"MAIRE\",\"populationReelle\":404,\"assembleeDeliberante\":{\"_id\":0,\"_lib\":\"Non Renseigné\",\"@id\":7,\"@type\":\"TypeAssembleeDeliberante\"},\"typeCollectiviteRef\":{\"id\":-100,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":8,\"@type\":\"TypeCollectivite\"},\"apenaf700Ref\":{\"id\":-641,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":9,\"@type\":\"APENAF700\"},\"archiverPJTraite\":true,\"circuitValidationPJ\":false,\"circuit\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"parDefaut\":false,\"delaiViseur\":0,\"@id\":10,\"@type\":\"Circuit\"},\"circuitValidationBC\":false,\"engagementBCNonValide\":false,\"editBDCNonValide\":false,\"editBDCNonEng\":false,\"bdcPourValidation\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":11,\"@type\":\"DocPersonnalise\"},\"gestionMultiBudget\":false,\"transfoBDCEnPJ\":false,\"transfoDocBDCEnPJ\":false,\"transmissionBDCSurLiquidation\":false,\"collectiviteHistoriqueRef\":{\"id\":1,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":12,\"@type\":\"CollectiviteHistorique\"},\"gestionPESMarche\":false,\"tiersAcheteur\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"inactif\":false,\"epl_chorus\":false,\"engagementObligatoireChorus\":false,\"serviceObligatoireChorus\":false,\"serviceOuEngagementObligatoireChorus\":false,\"renseignerLibVir2\":true,\"anonymise\":false,\"entiteChorus\":false,\"@id\":13,\"@type\":\"TiersComptable\"},\"depotBDCChorusEngagement\":{\"_id\":1,\"_lib\":\"MANUEL\",\"@id\":14,\"@type\":\"TypeDepotChorusEngagement\"},\"depotDocBDCChorusEngagement\":false,\"depotBDCSigneChorusEngagement\":false,\"bdcPourChorus\":11,\"transmettreServiceEmetteurBDC\":false,\"transmettreServiceRecepteurFacture\":false,\"gestionBudgetVert\":{\"_id\":0,\"_lib\":\"Indéterminé\",\"@id\":15,\"@type\":\"TypeGestionBudgetVert\"},\"libelle\":\"ND\",\"@id\":6,\"@type\":\"Collectivite\"},\"budgetPrincipalRef\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"budgetPrincipal\":false,\"budgetAutonome\":false,\"regimeFiscal\":{\"_id\":0,\"_lib\":\"Non renseigné\",\"@id\":17,\"@type\":\"TypeRegimeFiscal\"},\"sectionsGerees\":{\"_id\":0,\"_lib\":\"Non renseigné\",\"libelleCollectivite\":\"Non renseigné\",\"libelleEHPAD\":\"Non renseigné\",\"@id\":18,\"@type\":\"TypeGestionSections\"},\"dateMigrationSEPA\":\"01/02/2014 0:00:00\",\"dataMatrix\":false,\"utilisationTipi\":false,\"utilisationTipiParDefaut\":true,\"gestionASAP\":false,\"editionTIP\":false,\"tipSepa\":false,\"asapUtilisationChorusPro\":false,\"asapUtilisationServiceEditique\":false,\"asapGenerationParTitre\":false,\"asapUtilisationFE\":false,\"asapLogoColl\":false,\"@id\":16,\"@type\":\"Budget\"},\"budgetPiloteRef\":16,\"numeroSiren\":\"217302207\",\"assembleeDeliberante\":7,\"dateMigrationSEPA\":\"01/02/2014 0:00:00\",\"dataMatrix\":false,\"modaliteReglement1\":\"\",\"modaliteReglement2\":\"- Par chèque bancaire ou postal adressé au comptable chargé du recouvrement : veuillez joindre le talon détachable à votre chèque, sans le coller ni l'agrafer.\",\"modaliteReglement3\":\"- Par mandat ou virement sur le compte courant postal du comptable chargé du recouvrement : veuillez inscrire très lisiblement dans le cadre \\\"correspondance\\\" les références portées sur le talon détachable.\",\"modaliteReglement4\":\"LIBELLEZ obligatoirement le chèque ou le mandat à l'ordre du TRESOR PUBLIC, dans votre intérêt n'envoyer en aucun cas un chèque sans indication du bénéficiaire ainsi que des références de la créance dont vous vous acquittez.\",\"modaliteReglement5\":\"- En espèces (dans la limite de 300 €) ou en carte bancaire, muni du présent avis, auprès d’un buraliste ou partenaire agréé (liste consultable sur www.impots.gouv.fr/portail/paiement-de-proximite) : veuillez rapporter le présent avis en venant payer.\",\"utilisationTipi\":true,\"utilisationTipiParDefaut\":true,\"identifiantTipi\":\"071222\",\"modaliteReglementTipi\":\"Vous pouvez payer sur internet en vous connectant sur www.payfip.gouv.fr et en saisissant les informations suivantes :\",\"siteInternetTipi\":\"www.payfip.gouv.fr\",\"connexionActe\":{\"id\":1,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"archivageSaeActe\":false,\"@id\":19,\"@type\":\"ConnexionActe\"},\"adresseRef\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"adressePays\":false,\"@id\":20,\"@type\":\"Adresse\"},\"gestionASAP\":true,\"editionTIP\":false,\"tipSepa\":false,\"centreEncaissement\":{\"id\":-40,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":21,\"@type\":\"CentreEncaissement\"},\"emetteurFacture\":{\"id\":1,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":22,\"@type\":\"EmetteurFacture\"},\"talonOptique2Lignes\":{\"id\":1,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":23,\"@type\":\"TalonOptique2Ligne\"},\"asapUtilisationChorusPro\":true,\"asapUtilisationServiceEditique\":true,\"asapGenerationParTitre\":false,\"asapUtilisationFE\":false,\"asapLogoColl\":false,\"@id\":2,\"@type\":\"Budget\"}";

        /**
         * Default circuit for the ticked bordereaux. Answers with an empty body on this tenant,
         * so there is nothing to assert beyond the status.
         */
        public static final HttpRequestActionBuilder fournirCircuitParDefaut = http(
                        "Fournir circuit par defaut")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcFichierLiaisonCommun/fournirCircuitParDefaut?fieldNames%5B%5D=**")
                        .body(StringBody("{\"bordereauListe\":" + bordereauListe("Association")
                                        + ",\"budget\":" + BUDGET + ",\"sensExecution\":null}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"));

        // ---------------------------------------------------------------------------------
        // "Generer le flux PES": the write. Turns the ticked bordereaux into a PES Aller
        // dossier — one real dossier per pass, and the bordereaux come back flagged as sent.
        // ---------------------------------------------------------------------------------

        /**
         * The fields that carry an {@code @id} reference rather than a value in the blocks
         * {@link #shiftIds} moves — the bordereau rows, the PJ list and the CA_NC. Anything
         * absent is business data and is left alone; {@code tailleEstimePourFluxPES} is the
         * cautionary case, a byte count that happens to look like a reference. Longer names come
         * first so the alternation cannot settle for a prefix, and the leading quote keeps
         * {@code "@type"} out of the {@code "type"} branch.
         *
         * <p>A reference field missing from this list does not fail loudly: the number stays
         * small, and inside {@code weGfOrdonnancement} it then resolves against the exercice
         * comptable, which occupies 1..~53 untouched. {@code formatFichier} was the case — the PJ
         * list writes it out as a {@code TypeFormatFichier} and re-references it a few rows later,
         * and unshifted those references landed on the exercice's {@code TypeRegimeFiscal},
         * {@code TiersComptable} and {@code TypeCodeNorme}, which the API answers with
         * "Error parsing type weGfOrdonnancement". So a name belongs here as soon as the API
         * writes that field out as an object anywhere in a moved block.
         */
        private static final Pattern ID_OR_REFERENCE = Pattern.compile(
                        "\"(@id|signataireCollectiviteRef|etatPrecedent|protocoleMetier|formatFichier"
                                        + "|ordrePieces|budgetRef|etat|sens|type)\"\\s*:\\s*(\\d+)");

        /** {@code dateCreation} and {@code dateTransmission} are sent as "dd/MM/yyyy HH:mm:ss". */
        private static final DateTimeFormatter JOUR_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        /**
         * Adds {@code offset} to every {@code @id} and to every reference pointing at one, so a
         * captured response can be spliced into a larger request without its object graph
         * overlapping a neighbour's. Only the numbers change: the objects, their order and the
         * references between them stay exactly as the server sent them.
         */
        private static String shiftIds(String json, int offset) {
                Matcher matcher = ID_OR_REFERENCE.matcher(json);
                StringBuilder shifted = new StringBuilder();
                while (matcher.find()) {
                        int renumbered = Integer.parseInt(matcher.group(2)) + offset;
                        matcher.appendReplacement(shifted,
                                        Matcher.quoteReplacement("\"" + matcher.group(1) + "\":" + renumbered));
                }
                matcher.appendTail(shifted);
                return shifted.toString();
        }

        /**
         * The connected user's display name, e.g. "Jules DUPAS". It is what the screen stamps on
         * the dossier as {@code expediteur} and on its history line as {@code acteur}; the SPA
         * reads it off the {@code name} claim of the access token, so the same claim is read here
         * rather than pinning one operator's name into the payload.
         */
        private static String nomUtilisateur(String accessToken) {
                String[] parts = accessToken.split("\\.");
                if (parts.length < 2) {
                        throw new IllegalStateException("access token illisible: " + parts.length + " segments");
                }
                String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                Matcher name = Pattern.compile("\"name\"\\s*:\\s*\"([^\"]*)\"").matcher(claims);
                if (!name.find()) {
                        throw new IllegalStateException("claim \"name\" absente du access token");
                }
                return name.group(1);
        }

        /**
         * Where the CA_NC row is moved to inside {@code weGfOrdonnancement}, past the ranges the
         * bordereaux (10001+) and the PJ list (20001+) already occupy.
         */
        private static final int DECALAGE_CA_NC = 30000;

        /**
         * The CA_NC the generation runs on, taken from the {@code fournirListeCA_NCValide}
         * response rather than from the recording: the row is tenant data — tenant 1 answers
         * ids 21 (PES) and 22 (INDIGO) against tenant 2's id 1 — so a captured copy only ever
         * generates on the tenant it was captured from.
         *
         * <p>Picks the PES row, which is the protocole the recording generated on, and the only
         * one the rest of the payload is coherent with: the dossier it builds is a PES Aller sent
         * to a "Tiers de télétransmission".
         *
         * <p>That row has to be the first of the list. A row only ever points at an object an
         * earlier row wrote out — tenant 1's INDIGO row arrives as {@code
         * "comptableAssignataireRef":8}, {@code "normeComptableRef":6}, both written out by the
         * PES row before it — so only the first row can be lifted out on its own, and {@code
         * ca_nc} takes one object, not a prefix of the list. Both known tenants answer PES first;
         * anywhere else this stops rather than send a dangling reference.
         */
        private static JsonNode ligneCaNcPES(String listeJson) {
                JsonNode donnees;
                try {
                        donnees = JSON.readTree(listeJson).path("donnees");
                } catch (JsonProcessingException e) {
                        throw new IllegalStateException("reponse CA_NC illisible: " + e.getMessage(), e);
                }
                for (int i = 0; i < donnees.size(); i++) {
                        if (!"PES".equals(donnees.get(i).path("protocoleRef").path("code").asText())) {
                                continue;
                        }
                        if (i != 0) {
                                throw new IllegalStateException("ligne CA_NC PES en position " + i
                                                + ": elle reference des objets ecrits par les lignes precedentes");
                        }
                        return donnees.get(i);
                }
                throw new IllegalStateException(
                                "aucune ligne CA_NC de protocole PES parmi les " + donnees.size() + " renvoyees");
        }

        /**
         * The {@code @id} the API gave one of the CA_NC's sub-objects, moved by the same offset as
         * the row itself. {@code weGfOrdonnancement} points back at three of them — the protocole,
         * the comptable assignataire and the regroupement — and in this dialect that back-reference
         * is the sub-object's {@code @id}, so it cannot be written out until the row is known.
         */
        private static int atIdDecale(JsonNode ligneCaNc, String champ) {
                JsonNode sousObjet = ligneCaNc.path(champ);
                if (!sousObjet.hasNonNull("@id")) {
                        throw new IllegalStateException("ligne CA_NC sans " + champ + " developpe");
                }
                return sousObjet.get("@id").asInt() + DECALAGE_CA_NC;
        }

        /**
         * Prepares the values the three generation calls splice into their payloads. Must run
         * after {@link #fournirListePJATransmettre} and {@code
         * NumerotationApiEndpoints.fournirListeCA_NCValide}, whose responses it reads.
         *
         * <p>{@code controleGenerationPes} and the two calls after it pack several captured
         * responses into a single {@code weGfOrdonnancement} argument, and inside one argument the
         * {@code @id}s must stay unique. The exercice comptable goes in untouched and keeps its
         * native 1..~53, the payload's own objects sit at 502..611 (renumbered once, when the
         * template below was taken from the recording), so the captured blocks are each moved into
         * a range of their own: bordereaux at 10001+, the PJ list at 20001+, the CA_NC at 30001+.
         * The gaps are deliberately wide — the absolute values mean nothing to the server, only
         * the fact that two different objects never share a number. 517..534 are left unused where
         * the captured CA_NC sat, for the same reason.
         */
        public static final ChainBuilder preparerCorpsGeneration = exec(session -> {
                String maintenant = LocalDateTime.now().format(JOUR_HEURE);
                String bordereaux = String.join(",", session.getList("bordereauxModifies"));
                JsonNode caNc = ligneCaNcPES(session.getString("caNcListeJson"));
                return session
                                .set("bordereauxModifiesJson", bordereaux)
                                .set("bordereauxPourGeneration", shiftIds(bordereaux, 10000))
                                .set("pjPourGeneration",
                                                shiftIds(sansTotal(session.getString("pjATransmettreJson")), 20000))
                                .set("caNcPourGeneration", shiftIds(caNc.toString(), DECALAGE_CA_NC))
                                .set("caNcProtocoleAtId", atIdDecale(caNc, "protocoleRef"))
                                .set("caNcComptableAtId", atIdDecale(caNc, "comptableAssignataireRef"))
                                .set("caNcRegroupementAtId", atIdDecale(caNc, "regroupementInventaire"))
                                .set("dateGeneration", maintenant)
                                .set("expediteur", nomUtilisateur(session.getString("accessToken")));
        });

        /**
         * The dossier as the screen posts it: a brand new PES Aller ({@code id} -1, status
         * "Demande de generation du Flux"), carrying the exercice comptable and pointing at the
         * budget and collectivite written out inside it — hence the two captured {@code @id}s
         * rather than fixed numbers.
         */
        private static final String DOSSIER = "{\"id\":-1,\"marque\":0,\"etat\":{\"_id\":1,\"_lib\":\"INSERE\",\"@id\":503,\"@type\":\"EtatEnum\"},\"typeDossier\":{\"_id\":1,\"_lib\":\"Aller\",\"@id\":504,\"@type\":\"TypeDossier\"},\"expediteur\":\"#{expediteur}\",\"archive\":false,\"envoiAutoTDT\":false,\"modeSignature\":{\"_id\":0,\"_lib\":\"Non Renseigné\",\"@id\":505,\"@type\":\"TypeModeLiaison\"},\"modeTeletransmission\":505,\"statut\":{\"_id\":56,\"_lib\":\"Demande de génération du Flux\",\"@id\":506,\"@type\":\"EtatDossier\"},\"dateCreation\":\"#{dateGeneration}\",\"destination\":{\"_id\":5,\"_lib\":\"Tiers de télétransmission\",\"@id\":507,\"@type\":\"TypeDestinationFichier\"},\"exerciceComptableRef\":#{exerciceComptableJson},\"budgetRef\":#{exerciceBudgetAtId},\"collectiviteRef\":#{exerciceCollectiviteAtId},\"dossierDocumentListe\":{\"donnees\":[{\"id\":-1,\"marque\":0,\"document\":{\"id\":-1,\"marque\":0,\"protocoleMetier\":{\"_id\":1,\"_lib\":\"PES Aller\",\"@id\":564,\"@type\":\"TypeProtocoleMetier\"},\"etat\":503,\"dateCreation\":\"#{dateGeneration}\",\"de_Doc\":{\"@id\":565,\"@type\":\"Blob\"},\"compresse\":false,\"nomFichierOrigine\":null,\"@id\":563,\"@type\":\"Document\"},\"etat\":503,\"typeFichier\":{\"_id\":1,\"_lib\":\"Flux 1 métier\",\"@id\":566,\"@type\":\"TypeFichier\"},\"dossier\":502,\"@id\":562,\"@type\":\"DossierDocument\"}],\"@id\":561,\"@type\":\"Association\"},\"histoDossierListe\":{\"donnees\":[{\"id\":-1,\"marque\":0,\"etat\":503,\"acteur\":\"#{expediteur}\",\"annotation\":null,\"statut\":506,\"dateAction\":\"#{dateGeneration}\",\"dossier\":502,\"@id\":568,\"@type\":\"HistoDossier\"}],\"@id\":567,\"@type\":\"Association\"},\"@id\":502,\"@type\":\"Dossier\"}";

        /**
         * Everything the generation reasons about, in one argument: the ticked bordereaux, the CA_NC
         * and its protocole, the exercice, the serie, the signataire and the attachments that will
         * not be transmitted. {@code idDossier} is null until the dossier exists, then the created
         * id — the only thing that changes between the three calls that send this.
         *
         * <p>{@code protocole}, {@code comptaAssign} and {@code regroupementInventaire} are the
         * three places this argument points back into the CA_NC, so they are the {@code @id}s
         * {@link #preparerCorpsGeneration} reads off the correlated row rather than fixed numbers.
         */
        private static String weGfOrdonnancement(String idDossier) {
                return "{\"liquidationListe\":{\"donnees\":[],\"@id\":503,\"@type\":\"Association\"},\"bordereauListe\":{\"donnees\":[#{bordereauxPourGeneration}],\"@id\":504,\"@type\":\"Association\"},\"choixMouvementATraiter\":{\"_id\":3,\"_lib\":\"Tous\",\"@id\":516,\"@type\":\"TypeMouvementATraiter\"},\"ca_nc\":#{caNcPourGeneration},\"protocole\":#{caNcProtocoleAtId},\"nomFichier\":null,\"exerciceComptableRef\":#{exerciceComptableJson},\"controleAutomatiquePES\":true,\"serieBordereauLiquidationRef\":{\"id\":4,\"marque\":1,\"etat\":{\"_id\":4,\"_lib\":\"RIEN_A_FAIRE\",\"@id\":589,\"@type\":\"EtatEnum\"},\"etatPrecedent\":589,\"ordrePieces\":{\"_id\":3,\"_lib\":\"Compte puis tiers\",\"@id\":590,\"@type\":\"TypeOrdrePieces\"},\"detailFonction\":false,\"budgetaire\":true,\"interne\":false,\"annulatif\":false,\"sens\":{\"_id\":2,\"_lib\":\"Dépense\",\"@id\":591,\"@type\":\"TypeGestionSens\"},\"libelle\":\"Mandats ordinaires\",\"code\":\"M+\",\"piecesMonoImputation\":false,\"piecesMonoSection\":false,\"numerotationParBordPrep\":false,\"budgetRef\":{\"id\":1,\"marque\":0,\"etat\":589,\"etatPrecedent\":589,\"budgetPrincipal\":false,\"budgetAutonome\":false,\"regimeFiscal\":{\"_id\":0,\"_lib\":\"Non renseigné\",\"@id\":593,\"@type\":\"TypeRegimeFiscal\"},\"sectionsGerees\":{\"_id\":0,\"_lib\":\"Non renseigné\",\"libelleCollectivite\":\"Non renseigné\",\"libelleEHPAD\":\"Non renseigné\",\"@id\":594,\"@type\":\"TypeGestionSections\"},\"dateMigrationSEPA\":\"01/02/2014 0:00:00\",\"dataMatrix\":false,\"utilisationTipi\":false,\"utilisationTipiParDefaut\":true,\"gestionASAP\":false,\"editionTIP\":false,\"tipSepa\":false,\"asapUtilisationChorusPro\":false,\"asapUtilisationServiceEditique\":false,\"asapGenerationParTitre\":false,\"asapUtilisationFE\":false,\"asapLogoColl\":false,\"@id\":592,\"@type\":\"Budget\"},\"@id\":588,\"@type\":\"SerieBordereauLiquidation\"},\"idPJAEnvoyerListe\":[],\"regroupementInventaire\":#{caNcRegroupementAtId},\"signataireIHM\":{\"id\":1,\"marque\":2,\"etat\":{\"_id\":4,\"_lib\":\"RIEN_A_FAIRE\",\"@id\":596,\"@type\":\"EtatEnum\"},\"etatPrecedent\":596,\"idUtilisateur\":0,\"intituleFonction\":\"Maire\",\"libelle\":\"Patrick GADROY-LEGENVRE, Maire\",\"nomSignataire\":\"GADROY-LEGENVRE\",\"prenomSignataire\":\"Patrick\",\"collectiviteRef\":{\"id\":1,\"marque\":0,\"etat\":596,\"etatPrecedent\":596,\"archiverPJTraite\":false,\"circuitValidationPJ\":false,\"circuitValidationBC\":false,\"engagementBCNonValide\":false,\"editBDCNonValide\":false,\"editBDCNonEng\":false,\"gestionMultiBudget\":false,\"transfoBDCEnPJ\":false,\"transfoDocBDCEnPJ\":false,\"transmissionBDCSurLiquidation\":false,\"gestionPESMarche\":false,\"depotBDCChorusEngagement\":{\"_id\":1,\"_lib\":\"MANUEL\",\"@id\":598,\"@type\":\"TypeDepotChorusEngagement\"},\"depotDocBDCChorusEngagement\":false,\"depotBDCSigneChorusEngagement\":false,\"transmettreServiceEmetteurBDC\":false,\"transmettreServiceRecepteurFacture\":false,\"gestionBudgetVert\":{\"_id\":0,\"_lib\":\"Indéterminé\",\"@id\":599,\"@type\":\"TypeGestionBudgetVert\"},\"libelle\":\"\",\"@id\":597,\"@type\":\"Collectivite\"},\"inactif\":false,\"parDefaut\":true,\"@id\":595,\"@type\":\"SignataireCollectivite\"},\"comptaAssign\":#{caNcComptableAtId},\"informationSignature\":false,\"enteteASAP\":null,\"infoComplementaireASAP\":null,\"pjATransmettreAfficheeListe\":{\"donnees\":[],\"@id\":600,\"@type\":\"TableauEntitePersistante\"},\"pjATransmettreNonAfficheeListe\":{\"donnees\":[],\"@id\":601,\"@type\":\"TableauEntitePersistante\"},\"pjAPASTransmettreAfficheeListe\":#{pjPourGeneration},"
                                + idDossier
                                + "\"@id\":502,\"@type\":\"WeGfOrdonnancement\"}";
        }

        /**
         * Stamps the ticked bordereaux as being generated. Answers the same list back with a
         * {@code tableauInfoErreur} — empty when nothing blocks the generation.
         */
        public static final HttpRequestActionBuilder processModifierBordereauListe = http(
                        "Process modifier bordereau liste")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/processModifierBordereauListe?fieldNames%5B%5D=**")
                        .body(StringBody("{\"bordereauListe\":" + bordereauListe("Association") + "}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0))
                        // The rows come back with a new marque and without the montants; the
                        // three calls after this one send these, not the ones from the grid.
                        .check(jsonPath("$.bordereauListe.donnees[*]").findAll().saveAs("bordereauxModifies"))
                        // Business refusals come back 200 with the reasons in tableauInfoErreur,
                        // so an empty error list is the pass condition. Checked as "no first
                        // element" rather than by counting: a body without the field at all then
                        // fails the @id check above with something readable.
                        .check(jsonPath("$.tableauInfoErreur.donnees[0]").notExists());

        /**
         * Last control before the write: asks whether the selection can produce a PES flux.
         * Answers a {@code TableauInfoErreur}, empty when it can — so an empty list is the pass
         * condition, not just a 200.
         */
        public static final HttpRequestActionBuilder controleGenerationPes = http(
                        "Controle generation PES")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/controleGenerationPes")
                        .body(StringBody("{\"weGfOrdonnancement\":" + weGfOrdonnancement("") + "}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@type\"").is("TableauInfoErreur"))
                        .check(jsonPath("$.donnees[0]").notExists());

        /**
         * The write: creates the dossier and queues the flux for later transmission. Heaviest call
         * of the flow (~50 KB up, ~37 KB down recorded). Six arguments, each with its own
         * {@code @id} scope restarting at 2, which is why the exercice can be sent twice over —
         * once on its own and once inside the dossier — without renumbering.
         *
         * <p>Saves the created dossier id, which {@link #majPesDansSuiviEchange} sends back.
         */
        public static final HttpRequestActionBuilder envoyerNouveauDossierExecutionPlusTard = http(
                        "Envoyer nouveau dossier execution plus tard")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/envoyerNouveauDossierExecutionPlusTard?fieldNames%5B%5D=**")
                        .body(StringBody("{\"dossier\":" + DOSSIER
                                        + ",\"bordereauListe\":" + bordereauListeModifie("Association")
                                        + ",\"pjUtilisateurListe\":{\"donnees\":[],\"@id\":2,\"@type\":\"Association\"}"
                                        + ",\"exerciceComptable\":#{exerciceComptableJson}"
                                        + ",\"dateTransmission\":\"#{dateGeneration}\""
                                        + ",\"weGfOrdonnancement\":" + weGfOrdonnancement("") + "}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jsonPath("$.entite.id").ofInt().gt(0).saveAs("idDossier"))
                        .check(jsonPath("$.tableauInfoErreur.donnees[0]").notExists());

        /**
         * Files the generated flux in the exchange monitoring screen. Same
         * {@code weGfOrdonnancement} as the two calls above, now carrying the dossier id.
         * Answers an empty body, so there is nothing to assert beyond the status.
         */
        public static final HttpRequestActionBuilder majPesDansSuiviEchange = http(
                        "Maj PES dans suivi echange")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/majPesDansSuiviEchange?fieldNames%5B%5D=**")
                        .body(StringBody("{\"weGfOrdonnancement\":" + weGfOrdonnancement("\"idDossier\":#{idDossier},") + "}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"));
}
