package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.bodyLength;
import static io.gatling.javaapi.core.CoreDsl.bodyString;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Numeroter les liquidations": the ordonnancement steps that follow the user ticking a
 * liquidation in the grid, up to the created bordereau and the screen that opens on it.
 * Picks up where {@code OrdonnancementSelectionLiquidationsGroup} stops.
 *
 * <p>The API speaks a referencing JSON dialect: an object is written once with an
 * {@code "@id"}, and every later occurrence of that same object is written as the bare
 * {@code @id} number instead ({@code "etat":88}). Two consequences drive the code below.
 *
 * <ol>
 *   <li>Numbering is scoped to each <em>top-level argument</em> of a request and restarts at
 *       2 for each one. The recorded traffic proves it: one request sends {@code param} at
 *       {@code @id} 2 and {@code bordereaux} at {@code @id} 2..11 side by side. So the
 *       captured blocks below can all reuse low ids without clashing.</li>
 *   <li>Inside one argument the ids must stay unique. {@code creerBordereauLiquidation} packs
 *       three captured responses into a single {@code ordonnancement} argument, so
 *       {@link #shiftIds} moves two of them into their own ranges first — see
 *       {@link #preparerCorpsOrdonnancement}.</li>
 * </ol>
 */
public final class NumerotationApiEndpoints {

        private NumerotationApiEndpoints() {
        }

        /**
         * The fields that carry an {@code @id} reference rather than a value, so
         * {@link #shiftIds} knows which numbers to move. Anything absent from this list
         * (notably {@code id}, {@code marque} and the {@code montant*} amounts) is business
         * data and is left alone. Longer names come first so the alternation cannot settle for
         * a prefix, and the leading quote keeps {@code "@type"} out of the {@code @id} branch.
         */
        private static final Pattern ID_OR_REFERENCE = Pattern.compile(
                        "\"(@id|etatPrecedent|etat|typeNormalOuAnnulatif|codeNature|produitWS"
                                        + "|utilisationASAP|sectionRecette|section|statut|reelOrdre|cppStatut|pmm|type)\""
                                        + "\\s*:\\s*(\\d+)");

        /** {@code dateOrdonnancement} is sent as "dd/MM/yyyy 12:00:00", i.e. today at noon. */
        private static final DateTimeFormatter JOUR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        /** Jackson comes with Gatling; used only by {@link #compact}. */
        private static final ObjectMapper JSON = new ObjectMapper();

        /**
         * Strips the indentation the API answers with. The responses are re-sent almost
         * verbatim, and left as-is they would put ~72 KB on the wire where the browser puts
         * ~46 KB — enough to skew what the report says about the request. Key order and values
         * are preserved.
         */
        private static String compact(String json) {
                try {
                        return JSON.readTree(json).toString();
                } catch (JsonProcessingException e) {
                        throw new IllegalStateException("reponse JSON illisible: " + e.getMessage(), e);
                }
        }

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
         * The bordereau created by {@link #creerBordereauLiquidation}, rebuilt as the list
         * argument its four follow-up calls expect. Only the five values captured from the
         * creation response vary; the serie and budget underneath are configuration.
         * {@code type} is the wrapper class name, which differs per endpoint
         * ({@code Association}, or {@code TableauEntitePersistante} for the PJ call).
         */
        private static String bordereauListe(String type) {
                return "{\"donnees\":[{\"id\":#{bordereauId},\"marque\":#{bordereauMarque},\"etat\":{\"_id\":4,\"_lib\":\"RIEN_A_FAIRE\",\"@id\":4,\"@type\":\"EtatEnum\"},\"etatPrecedent\":4,\"numeroBordereau\":#{bordereauNumero},\"millesime\":#{bordereauMillesime},\"dateEmission\":\"#{bordereauDateEmission}\",\"serieBordereauLiquidationRef\":{\"id\":4,\"marque\":1,\"etat\":{\"_id\":4,\"_lib\":\"RIEN_A_FAIRE\",\"@id\":6,\"@type\":\"EtatEnum\"},\"etatPrecedent\":6,\"ordrePieces\":{\"_id\":3,\"_lib\":\"Compte puis tiers\",\"@id\":7,\"@type\":\"TypeOrdrePieces\"},\"detailFonction\":false,\"budgetaire\":true,\"interne\":false,\"annulatif\":false,\"sens\":{\"_id\":2,\"_lib\":\"Dépense\",\"@id\":8,\"@type\":\"TypeGestionSens\"},\"libelle\":\"Mandats ordinaires\",\"code\":\"M+\",\"piecesMonoImputation\":false,\"piecesMonoSection\":false,\"numerotationParBordPrep\":false,\"budgetRef\":{\"id\":1,\"marque\":0,\"etat\":6,\"etatPrecedent\":6,\"budgetPrincipal\":false,\"budgetAutonome\":false,\"regimeFiscal\":{\"_id\":0,\"_lib\":\"Non renseigné\",\"@id\":10,\"@type\":\"TypeRegimeFiscal\"},\"sectionsGerees\":{\"_id\":0,\"_lib\":\"Non renseigné\",\"libelleCollectivite\":\"Non renseigné\",\"libelleEHPAD\":\"Non renseigné\",\"@id\":11,\"@type\":\"TypeGestionSections\"},\"dateMigrationSEPA\":\"01/02/2014 0:00:00\",\"dataMatrix\":false,\"utilisationTipi\":false,\"utilisationTipiParDefaut\":true,\"gestionASAP\":false,\"editionTIP\":false,\"tipSepa\":false,\"asapUtilisationChorusPro\":false,\"asapUtilisationServiceEditique\":false,\"asapGenerationParTitre\":false,\"asapUtilisationFE\":false,\"asapLogoColl\":false,\"@id\":9,\"@type\":\"Budget\"},\"@id\":5,\"@type\":\"SerieBordereauLiquidation\"},\"@id\":3,\"@type\":\"Bordereau\"}],\"@id\":2,\"@type\":\""
                                + type + "\"}";
        }

        /** The budget as the circuit call sends it: the collectivite's own configuration. */
        private static final String BUDGET = "{\"id\":1,\"marque\":2,\"etat\":{\"_id\":4,\"_lib\":\"RIEN_A_FAIRE\",\"@id\":3,\"@type\":\"EtatEnum\"},\"etatPrecedent\":3,\"code\":\"ST\",\"libelle\":\"SAINT ALBAN D'HURTIERES\",\"nic\":\"00014\",\"budgetPrincipal\":true,\"budgetAutonome\":false,\"codificationTresorerie1\":\"037\",\"codificationTresorerie2\":\"00\",\"regimeFiscal\":{\"_id\":1,\"_lib\":\"TTC\",\"@id\":4,\"@type\":\"TypeRegimeFiscal\"},\"sectionsGerees\":{\"_id\":1,\"_lib\":\"Fonctionnement et Investissement\",\"libelleCollectivite\":\"Fonctionnement et Investissement\",\"libelleEHPAD\":\"Exploitation et Investissement\",\"@id\":5,\"@type\":\"TypeGestionSections\"},\"collectiviteRef\":{\"id\":1,\"marque\":4,\"etat\":3,\"etatPrecedent\":3,\"code\":\"SAIN\",\"numeroSiren\":\"217302207\",\"titreResponsable\":\"MAIRE\",\"populationReelle\":404,\"assembleeDeliberante\":{\"_id\":0,\"_lib\":\"Non Renseigné\",\"@id\":7,\"@type\":\"TypeAssembleeDeliberante\"},\"typeCollectiviteRef\":{\"id\":-100,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":8,\"@type\":\"TypeCollectivite\"},\"apenaf700Ref\":{\"id\":-641,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":9,\"@type\":\"APENAF700\"},\"archiverPJTraite\":true,\"circuitValidationPJ\":false,\"circuit\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"parDefaut\":false,\"delaiViseur\":0,\"@id\":10,\"@type\":\"Circuit\"},\"circuitValidationBC\":false,\"engagementBCNonValide\":false,\"editBDCNonValide\":false,\"editBDCNonEng\":false,\"bdcPourValidation\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":11,\"@type\":\"DocPersonnalise\"},\"gestionMultiBudget\":false,\"transfoBDCEnPJ\":false,\"transfoDocBDCEnPJ\":false,\"transmissionBDCSurLiquidation\":false,\"collectiviteHistoriqueRef\":{\"id\":1,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":12,\"@type\":\"CollectiviteHistorique\"},\"gestionPESMarche\":false,\"tiersAcheteur\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"inactif\":false,\"epl_chorus\":false,\"engagementObligatoireChorus\":false,\"serviceObligatoireChorus\":false,\"serviceOuEngagementObligatoireChorus\":false,\"renseignerLibVir2\":true,\"anonymise\":false,\"entiteChorus\":false,\"@id\":13,\"@type\":\"TiersComptable\"},\"depotBDCChorusEngagement\":{\"_id\":1,\"_lib\":\"MANUEL\",\"@id\":14,\"@type\":\"TypeDepotChorusEngagement\"},\"depotDocBDCChorusEngagement\":false,\"depotBDCSigneChorusEngagement\":false,\"bdcPourChorus\":11,\"transmettreServiceEmetteurBDC\":false,\"transmettreServiceRecepteurFacture\":false,\"gestionBudgetVert\":{\"_id\":0,\"_lib\":\"Indéterminé\",\"@id\":15,\"@type\":\"TypeGestionBudgetVert\"},\"libelle\":\"ND\",\"@id\":6,\"@type\":\"Collectivite\"},\"budgetPrincipalRef\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"budgetPrincipal\":false,\"budgetAutonome\":false,\"regimeFiscal\":{\"_id\":0,\"_lib\":\"Non renseigné\",\"@id\":17,\"@type\":\"TypeRegimeFiscal\"},\"sectionsGerees\":{\"_id\":0,\"_lib\":\"Non renseigné\",\"libelleCollectivite\":\"Non renseigné\",\"libelleEHPAD\":\"Non renseigné\",\"@id\":18,\"@type\":\"TypeGestionSections\"},\"dateMigrationSEPA\":\"01/02/2014 0:00:00\",\"dataMatrix\":false,\"utilisationTipi\":false,\"utilisationTipiParDefaut\":true,\"gestionASAP\":false,\"editionTIP\":false,\"tipSepa\":false,\"asapUtilisationChorusPro\":false,\"asapUtilisationServiceEditique\":false,\"asapGenerationParTitre\":false,\"asapUtilisationFE\":false,\"asapLogoColl\":false,\"@id\":16,\"@type\":\"Budget\"},\"budgetPiloteRef\":16,\"numeroSiren\":\"217302207\",\"assembleeDeliberante\":7,\"dateMigrationSEPA\":\"01/02/2014 0:00:00\",\"dataMatrix\":false,\"modaliteReglement1\":\"\",\"modaliteReglement2\":\"- Par chèque bancaire ou postal adressé au comptable chargé du recouvrement : veuillez joindre le talon détachable à votre chèque, sans le coller ni l'agrafer.\",\"modaliteReglement3\":\"- Par mandat ou virement sur le compte courant postal du comptable chargé du recouvrement : veuillez inscrire très lisiblement dans le cadre \\\"correspondance\\\" les références portées sur le talon détachable.\",\"modaliteReglement4\":\"LIBELLEZ obligatoirement le chèque ou le mandat à l'ordre du TRESOR PUBLIC, dans votre intérêt n'envoyer en aucun cas un chèque sans indication du bénéficiaire ainsi que des références de la créance dont vous vous acquittez.\",\"modaliteReglement5\":\"- En espèces (dans la limite de 300 €) ou en carte bancaire, muni du présent avis, auprès d’un buraliste ou partenaire agréé (liste consultable sur www.impots.gouv.fr/portail/paiement-de-proximite) : veuillez rapporter le présent avis en venant payer.\",\"utilisationTipi\":true,\"utilisationTipiParDefaut\":true,\"identifiantTipi\":\"071222\",\"modaliteReglementTipi\":\"Vous pouvez payer sur internet en vous connectant sur www.payfip.gouv.fr et en saisissant les informations suivantes :\",\"siteInternetTipi\":\"www.payfip.gouv.fr\",\"connexionActe\":{\"id\":1,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"archivageSaeActe\":false,\"@id\":19,\"@type\":\"ConnexionActe\"},\"adresseRef\":{\"id\":0,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"adressePays\":false,\"@id\":20,\"@type\":\"Adresse\"},\"gestionASAP\":true,\"editionTIP\":false,\"tipSepa\":false,\"centreEncaissement\":{\"id\":-40,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":21,\"@type\":\"CentreEncaissement\"},\"emetteurFacture\":{\"id\":1,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":22,\"@type\":\"EmetteurFacture\"},\"talonOptique2Lignes\":{\"id\":1,\"marque\":0,\"etat\":3,\"etatPrecedent\":3,\"@id\":23,\"@type\":\"TalonOptique2Ligne\"},\"asapUtilisationChorusPro\":true,\"asapUtilisationServiceEditique\":true,\"asapGenerationParTitre\":false,\"asapUtilisationFE\":false,\"asapLogoColl\":false,\"@id\":2,\"@type\":\"Budget\"}";

        /**
         * Prepares the values that {@link #creerBordereauLiquidation} splices into its single
         * {@code ordonnancement} argument, giving each captured response its own {@code @id}
         * range so they cannot cross-wire each other:
         *
         * <ul>
         *   <li>{@code exerciceComptableJson} goes in untouched and keeps its native 1..~53;</li>
         *   <li>{@code selectionListJson} is the setNumerotation response moved to 1001+;</li>
         *   <li>{@code departListJson} is the full liquidations list moved to 10001+, and
         *       retyped from {@code TableauEntitePersistante} (how the list endpoint answers)
         *       to {@code Association} (what the {@code liquidationDepartListe} field is).</li>
         * </ul>
         *
         * The skeleton's own objects sit at 500..509, so the four ranges never meet. The gaps
         * are deliberately wide: the absolute values mean nothing to the server, only the fact
         * that two different objects never share a number.
         *
         * <p>Also compacts the three captured responses in place — see {@link #compact}. Must run
         * after {@link #setNumerotationParCompteSectionPossible}, whose response it reads, and
         * before {@link #controlerEclatementTitre}, which forwards the compacted version.
         */
        public static final ChainBuilder preparerCorpsOrdonnancement = exec(session -> session
                        .set("exerciceComptableJson", compact(session.getString("exerciceComptableJson")))
                        .set("numerotationJson", compact(session.getString("numerotationJson")))
                        .set("selectionListJson",
                                        compact(shiftIds(session.getString("numerotationJson"), 1000)))
                        .set("departListJson",
                                        compact(shiftIds(session.getString("liquidationsJson"), 10000)
                                                        .replace("TableauEntitePersistante", "Association")))
                        .set("dateOrdonnancement", LocalDate.now().format(JOUR) + " 12:00:00"));

        /**
         * Fired when the user ticks a liquidation. Sends the selected row back and the server
         * returns it with {@code numerotationParCompteSectionPossible} flipped to true — the
         * flag is decided here, not client side, which is why the response has to be kept.
         *
         * <p>Sends whatever {@code ExecutionApiEndpoints.choisirLiquidations} ticked — 1 to 3 rows
         * — as {@code selectionRowsJson}. The rows keep the {@code @id}s the list endpoint gave
         * them, which is why they can be concatenated as-is; the {@code @id 1} wrapper below sits
         * just under the rows' own range (2 and up) and so cannot clash with them.
         */
        public static final HttpRequestActionBuilder setNumerotationParCompteSectionPossible = http(
                        "Set numerotation par compte section possible")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/setNumerotationParCompteSectionPossible?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                        {"listeLiquidation":{"donnees":[#{selectionRowsJson}],"@id":1,"@type":"Association"}}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jsonPath("$.donnees[0].id").ofInt().gt(0))
                        .check(bodyString().saveAs("numerotationJson"));

        /**
         * Control run before the write: asks whether the selection would split a titre across
         * several accounts or sections. Answered a bare {@code false} in the recording.
         *
         * <p>The setNumerotation response is already shaped like the expected argument (an
         * {@code Association} wrapping a {@code donnees} array), so it is forwarded as-is —
         * its own argument scope means no renumbering is needed.
         */
        public static final HttpRequestActionBuilder controlerEclatementTitre = http(
                        "Controler eclatement titre")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/controlerEclatementTitre")
                        .body(StringBody(
                                        """
                                        {"liquidationOrdoListe":#{numerotationJson},"unSeulCompteParTitre":null,"uneSeuleSectionParTitre":null,"pesConfigure":false}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(bodyLength().gt(0));

        /** Where the created bordereau sits in the creation response, written out in full. */
        private static final String BORDEREAU =
                        "$.liquidationListe.donnees[0].liquidationRef.mandatTitreRef.bordereauRef";

        /**
         * The write: numbers the selected liquidation and creates the bordereau. Heaviest call
         * of the flow (~46 KB up, ~210 KB down, ~450 ms recorded).
         *
         * <p>{@code liquidationListe} holds what was selected, {@code liquidationDepartListe}
         * the whole grid the user started from — the API wants both, which is why untouched
         * rows appear in the request. Everything outside the three spliced blocks is the
         * screen's own state: ordering, edition/PES toggles, serie de bordereaux.
         *
         * <p>Saves the created bordereau for the follow-up calls. It is read off the numbered
         * liquidation rather than from {@code bordereauListe}, whose {@code donnees[0]} is only
         * an {@code @id} reference and so cannot be walked with a JSON path.
         */
        public static final HttpRequestActionBuilder creerBordereauLiquidation = http(
                        "Creer bordereau liquidation")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/creerBordereauLiquidation?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                        {"ordonnancement":{"exerciceComptableRef":#{exerciceComptableJson},"edition":true,"controleEnvoiFichier":true,"generationFichier":true,"numerotationLiquidation":true,"uniciteCompteParTitre":null,"uniciteSectionParLiquidation":null,"liquidationListe":#{selectionListJson},"numerotationParBordereauPreparatoire":null,"dateOrdonnancement":"#{dateOrdonnancement}","ordrePiece":{"_id":3,"_lib":"Compte puis tiers","@id":501,"@type":"TypeOrdrePieces"},"actionChoisie":1,"choixMouvementATraiter":{"_id":3,"_lib":"Tous","@id":502,"@type":"TypeMouvementATraiter"},"serieBordereauLiquidationRef":{"id":4,"marque":1,"etat":{"_id":4,"_lib":"RIEN_A_FAIRE","@id":504,"@type":"EtatEnum"},"etatPrecedent":504,"ordrePieces":{"_id":3,"_lib":"Compte puis tiers","@id":505,"@type":"TypeOrdrePieces"},"detailFonction":false,"budgetaire":true,"interne":false,"annulatif":false,"sens":{"_id":2,"_lib":"Dépense","@id":506,"@type":"TypeGestionSens"},"libelle":"Mandats ordinaires","code":"M+","piecesMonoImputation":false,"piecesMonoSection":false,"numerotationParBordPrep":false,"budgetRef":{"id":1,"marque":0,"etat":504,"etatPrecedent":504,"budgetPrincipal":false,"budgetAutonome":false,"regimeFiscal":{"_id":0,"_lib":"Non renseigné","@id":508,"@type":"TypeRegimeFiscal"},"sectionsGerees":{"_id":0,"_lib":"Non renseigné","libelleCollectivite":"Non renseigné","libelleEHPAD":"Non renseigné","@id":509,"@type":"TypeGestionSections"},"dateMigrationSEPA":"01/02/2014 0:00:00","dataMatrix":false,"utilisationTipi":false,"utilisationTipiParDefaut":true,"gestionASAP":false,"editionTIP":false,"tipSepa":false,"asapUtilisationChorusPro":false,"asapUtilisationServiceEditique":false,"asapGenerationParTitre":false,"asapUtilisationFE":false,"asapLogoColl":false,"@id":507,"@type":"Budget"},"@id":503,"@type":"SerieBordereauLiquidation"},"liquidationDepartListe":#{departListJson},"@id":500,"@type":"Ordonnancement"}}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jsonPath(BORDEREAU + ".numeroBordereau").ofInt().gt(0))
                        .check(jsonPath(BORDEREAU + ".id").saveAs("bordereauId"))
                        .check(jsonPath(BORDEREAU + ".marque").saveAs("bordereauMarque"))
                        .check(jsonPath(BORDEREAU + ".numeroBordereau").saveAs("bordereauNumero"))
                        .check(jsonPath(BORDEREAU + ".millesime").saveAs("bordereauMillesime"))
                        .check(jsonPath(BORDEREAU + ".dateEmission").saveAs("bordereauDateEmission"));

        // ---------------------------------------------------------------------------------
        // Follow-up burst: the screen that opens on the freshly created bordereau. The browser
        // fires these in parallel; Gatling runs them in order inside the group.
        // ---------------------------------------------------------------------------------

        /**
         * Attachments still to transmit for the new bordereau. Note the list wrapper is a
         * {@code TableauEntitePersistante} here, unlike the three calls below.
         */
        public static final HttpRequestActionBuilder fournirListePJATransmettre = http(
                        "Fournir liste PJ a transmettre")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListePJATransmettreDepuisListeBordereaux?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        "{\"param\":{\"listeCriteres\":[],\"listeCriteresRechercheGui\":[],\"listeAttributs\":[],\"listeTris\":[],\"distinct\":false,\"paginatorValues\":{\"length\":1000,\"pageIndex\":0,\"pageSize\":1000,\"previousPageIndex\":0},\"@id\":2,\"@type\":\"RechercheParametres\"},"
                                                        + "\"sortValue\":{\"active\":\"tiers\",\"direction\":\"asc\"},"
                                                        + "\"bordereaux\":" + bordereauListe("TableauEntitePersistante")
                                                        + ",\"idBudget\":1}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        /**
         * Signatory to print on the bordereau. Re-sends the whole exercice comptable; it is its
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
         * minus the collectivite id, with the bordereau under {@code bordereauRecetteListe}.
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
         * Asks whether any asset sheet is incomplete. The screen sends an empty id list even
         * with a liquidation selected, so there is nothing to correlate here.
         */
        public static final HttpRequestActionBuilder hasBienIncompletPourGenerationBienPjMandat = http(
                        "Has bien incomplet pour generation bien PJ mandat")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/hasBienIncompletPourGenerationBienPjMandat")
                        .body(StringBody("""
                                        {"idsLiquidations":[]}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(bodyLength().gt(0));

        /** Assigning accountants for the collectivite. Static search criteria. */
        public static final HttpRequestActionBuilder fournirListeComptablesAssignataires = http(
                        "Fournir liste comptables assignataires")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListeComptablesAssignataires?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                        {"param":{"listeCriteres":[{"lienClassePersistante":"ComptableAssignataire","lienAttribut":"collectiviteListe","valeur":{"id":1,"marque":4,"etat":{"_id":4,"_lib":"RIEN_A_FAIRE","@id":5,"@type":"EtatEnum"},"etatPrecedent":5,"code":"SAIN","numeroSiren":"217302207","titreResponsable":"MAIRE","populationReelle":404,"assembleeDeliberante":{"_id":0,"_lib":"Non Renseigné","@id":6,"@type":"TypeAssembleeDeliberante"},"typeCollectiviteRef":{"id":-100,"marque":0,"etat":5,"etatPrecedent":5,"@id":7,"@type":"TypeCollectivite"},"apenaf700Ref":{"id":-641,"marque":0,"etat":5,"etatPrecedent":5,"@id":8,"@type":"APENAF700"},"archiverPJTraite":true,"circuitValidationPJ":false,"circuit":{"id":0,"marque":0,"etat":5,"etatPrecedent":5,"parDefaut":false,"delaiViseur":0,"@id":9,"@type":"Circuit"},"circuitValidationBC":false,"engagementBCNonValide":false,"editBDCNonValide":false,"editBDCNonEng":false,"bdcPourValidation":{"id":0,"marque":0,"etat":5,"etatPrecedent":5,"@id":10,"@type":"DocPersonnalise"},"gestionMultiBudget":false,"transfoBDCEnPJ":false,"transfoDocBDCEnPJ":false,"transmissionBDCSurLiquidation":false,"collectiviteHistoriqueRef":{"id":1,"marque":0,"etat":5,"etatPrecedent":5,"@id":11,"@type":"CollectiviteHistorique"},"gestionPESMarche":false,"tiersAcheteur":{"id":0,"marque":0,"etat":5,"etatPrecedent":5,"inactif":false,"epl_chorus":false,"engagementObligatoireChorus":false,"serviceObligatoireChorus":false,"serviceOuEngagementObligatoireChorus":false,"renseignerLibVir2":true,"anonymise":false,"entiteChorus":false,"@id":12,"@type":"TiersComptable"},"depotBDCChorusEngagement":{"_id":1,"_lib":"MANUEL","@id":13,"@type":"TypeDepotChorusEngagement"},"depotDocBDCChorusEngagement":false,"depotBDCSigneChorusEngagement":false,"bdcPourChorus":10,"transmettreServiceEmetteurBDC":false,"transmettreServiceRecepteurFacture":false,"gestionBudgetVert":{"_id":0,"_lib":"Indéterminé","@id":14,"@type":"TypeGestionBudgetVert"},"libelle":"ND","@id":4,"@type":"Collectivite"},"operateur":{"_id":3,"_lib":"égal à","@id":15,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[{"croissant":true,"lienClassePersistante":"ComptableAssignataire","lienAttribut":"designation","@id":16,"@type":"RechercheTri"}],"distinct":false,"@id":2,"@type":"RechercheParametres"}}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        /** Comptable exchange (PES) configuration. Takes no argument. */
        public static final HttpRequestActionBuilder chargerConfigEchangeComptable = http(
                        "Charger config echange comptable")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcConfigEchangeComptable/chargerConfigEchangeComptable?fieldNames%5B%5D=**")
                        .body(StringBody("{}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        /** Valid accounting-norm periods, keyed on the exercice's millesime. */
        public static final HttpRequestActionBuilder fournirListeCA_NCValide = http(
                        "Fournir liste CA_NC valide")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/fournirListeCA_NCValide?fieldNames%5B%5D=**")
                        .body(StringBody("""
                                        {"idComptable":1,"millesime":#{millesime},"idNormeComptable":-310}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        /** PES signing circuits. Returns an empty list on this tenant. */
        public static final HttpRequestActionBuilder chargerListeCircuit = http(
                        "Charger liste circuit")
                        .post("https://wegf-api.uat.wemagnus.com/compta/Ordonnancement/chargerListeCircuit?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                        {"param":{"listeCriteres":[{"lienClassePersistante":"Circuit","lienAttribut":"typeParapheur","valeur":"PES","operateur":{"_id":3,"_lib":"égal à","@id":4,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[],"distinct":false,"@id":2,"@type":"RechercheParametres"},"elementACharger":null}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        /**
         * Default circuit for the new bordereau. Answers with an empty body on this tenant, so
         * there is nothing to assert beyond the status.
         */
        public static final HttpRequestActionBuilder fournirCircuitParDefaut = http(
                        "Fournir circuit par defaut")
                        .post("https://wegf-api.uat.wemagnus.com/compta/UcFichierLiaisonCommun/fournirCircuitParDefaut?fieldNames%5B%5D=**")
                        .body(StringBody("{\"bordereauListe\":" + bordereauListe("Association")
                                        + ",\"budget\":" + BUDGET + ",\"sensExecution\":null}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"));
}
