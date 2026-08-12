package endpoints.apiEndpoints;

import static io.gatling.javaapi.core.CoreDsl.StringBody;
import static io.gatling.javaapi.core.CoreDsl.bodyLength;
import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.jmesPath;
import static io.gatling.javaapi.core.CoreDsl.jsonPath;
import static io.gatling.javaapi.http.HttpDsl.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.ChainBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Edition / transfert de bordereaux existants": the other way into the ordonnancement screen.
 * Instead of numbering liquidations into a brand new bordereau
 * ({@code NumerotationApiEndpoints}), the user ticks bordereaux that already exist and asks for
 * their PES flux. Runs after {@code OrdonnancementGroup.open}, which loads the exercice this
 * chain sends back.
 *
 * <p>The API speaks the same referencing JSON dialect as the numerotation flow: an object is
 * written once with an {@code "@id"}, later occurrences are the bare {@code @id} number
 * ({@code "etat":88}). Numbering is scoped to each <em>top-level argument</em> of a request and
 * must only be unique inside it, so the captured blocks below can reuse low ids freely as long
 * as two blocks landing in the same argument are moved apart first — see {@link #shiftIds}.
 *
 * <p>{@code weGfOrdonnancement} is the one argument that packs several captured responses
 * together, so its id space is carved up once and for all:
 *
 * <ul>
 *   <li>1..999 — {@code exerciceComptableJson}, spliced with its native numbering (~53 objects);
 *   <li>1000..1299 — the skeleton and the configuration blocks written out below;
 *   <li>3000+ — the bordereaux, as {@code processModifierBordereauListe} gave them back;
 *   <li>5000+ — the pieces justificatives.
 * </ul>
 *
 * The gaps are deliberately wide: the absolute values mean nothing to the server, only the fact
 * that two different objects never share a number.
 */
public final class EditionTransfertApiEndpoints {

        private EditionTransfertApiEndpoints() {
        }

        private static final String API = "https://wegf-api.uat.wemagnus.com/compta";

        /** Dates go over the wire as "dd/MM/yyyy HH:mm:ss". */
        private static final DateTimeFormatter HORODATAGE =
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        /**
         * Name the dossier is filed under. The screen sends the connected user's display name,
         * which nothing in the journey captures, so the recorded one is used: it is only an
         * audit label in the suivi des echanges and the server does not check it against the
         * token.
         */
        private static final String EXPEDITEUR = "Jules DUPAS";

        /** Jackson comes with Gatling; used only by {@link #compact}. */
        private static final ObjectMapper JSON = new ObjectMapper();

        /**
         * Fields carrying an {@code @id} reference rather than a value, per block. Anything
         * absent is business data and is left alone — which matters here: a bordereau row has
         * {@code numeroBordereau} and its {@code montant*}, a PJ row has
         * {@code tailleEstimePourFluxPES}, and those routinely hold small numbers that would
         * otherwise be mistaken for references. The leading quote keeps {@code "@type"} out of
         * the {@code @id} branch.
         */
        private static final Pattern BORDEREAU_REFERENCE = Pattern.compile(
                        "\"(@id|etatPrecedent|etat|ordrePieces|sens|budgetRef"
                                        + "|signataireCollectiviteRef)\"\\s*:\\s*(\\d+)");

        private static final Pattern PJ_REFERENCE = Pattern.compile(
                        "\"(@id|etatPrecedent|etat|formatFichier|protocoleMetier|type)\""
                                        + "\\s*:\\s*(\\d+)");

        /**
         * Adds {@code offset} to every {@code @id} of a captured block and to every reference
         * pointing at one, so the block can be spliced into a larger request without its object
         * graph overlapping a neighbour's. Only the numbers change: the objects, their order and
         * the references between them stay exactly as the server sent them.
         */
        private static String shiftIds(String json, Pattern references, int offset) {
                Matcher matcher = references.matcher(json);
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
         * Strips the indentation the API answers with. The exercice comptable is re-sent four
         * times in this flow, and left as-is it would put ~46 KB on the wire each time where the
         * browser puts ~13 KB — enough to skew what the report says about the requests. Key order
         * and values are preserved.
         */
        private static String compact(String json) {
                try {
                        return JSON.readTree(json).toString();
                } catch (JsonProcessingException e) {
                        throw new IllegalStateException("reponse JSON illisible: " + e.getMessage(), e);
                }
        }

        /** The ticked bordereaux, wrapped as the list argument each endpoint names them with. */
        private static String bordereaux(String rows, String type, int wrapperId) {
                return "{\"donnees\":[" + rows + "],\"@id\":" + wrapperId + ",\"@type\":\"" + type + "\"}";
        }

        // ---------------------------------------------------------------------------------
        // Configuration blocks of the weGfOrdonnancement argument. Written out rather than
        // correlated: they are the collectivite's own setup — accounting norm and its PES
        // protocol, assigning accountant, serie de bordereaux, default signatory — and none of
        // them moves with the selection. Their @ids are the 1000..1299 range reserved above.
        // ---------------------------------------------------------------------------------

        /** Norme comptable / PES configuration. {@code protocole}, {@code comptaAssign} and
         * {@code regroupementInventaire} at the top of the argument point back into it. */
        private static final String CA_NC = """
                        {"id":1,"marque":2,"etat":{"_id":4,"_lib":"RIEN_A_FAIRE","@id":1101,"@type":"EtatEnum"},"etatPrecedent":1101,"exerciceDebut":2002,"equipementInformatique":{"_id":3,"_lib":"Hélios","@id":1102,"@type":"TypeEquipementInformatique"},"retourChariot":true,"forceNumeroOrdreAZero":false,"transmissionDetailLiquidationsCollectives":false,"transmissionCompteDeTiers":false,"typeDestinationFichier":{"_id":5,"_lib":"Tiers de télétransmission","@id":1103,"@type":"TypeDestinationFichier"},"normeComptableRef":{"id":-310,"marque":0,"etat":1101,"etatPrecedent":1101,"gestionFonction":{"_id":0,"_lib":"Non renseigné","@id":1105,"@type":"TypeGestionFonction"},"pdcModifie":false,"@id":1104,"@type":"NormeComptable"},"comptableAssignataireRef":{"id":1,"marque":10,"etat":{"_id":4,"_lib":"RIEN_A_FAIRE","@id":1107,"@type":"EtatEnum"},"etatPrecedent":1107,"designation":"SGC SAINT JEAN DE MAURIENNE","codic":"073040","coordonneeComListe":{"donnees":[],"@id":1108,"@type":"Association"},"ca_cbListe":{"donnees":[{"id":1,"marque":2,"etat":1107,"etatPrecedent":1107,"typeRIBComptable":{"_id":0,"_lib":"Non renseigné","libelleCollectivite":"Non renseigné","libelleEHPAD":"Non renseigné","@id":1111,"@type":"TypeRIBComptable"},"compteBancaireRef":{"id":1038,"marque":0,"etat":1107,"etatPrecedent":1107,"titCpteDiff":false,"ibanaffichageAvecNomTitulaire":"","ibanaffichage":"","@id":1112,"@type":"CompteBancaire"},"comptableAssignataireRef":{"id":1,"marque":0,"etat":1107,"etatPrecedent":1107,"@id":1113,"@type":"ComptableAssignataire"},"preferentiel":true,"@id":1110,"@type":"CA_CB"}],"@id":1109,"@type":"Association"},"adresse":{"id":1546,"marque":0,"etat":1107,"etatPrecedent":1107,"adressePays":false,"@id":1114,"@type":"Adresse"},"@id":1106,"@type":"ComptableAssignataire"},"protocoleRef":{"id":-12,"marque":0,"etat":1101,"etatPrecedent":1101,"code":"PES","designation":"Protocole d'Echange Standard","typeProtocole":{"_id":4,"_lib":"PES","@id":1116,"@type":"TypeProtocole"},"@id":1115,"@type":"Protocole"},"pesSignature":true,"regroupementInventaire":{"_id":3,"_lib":"Fonctionnement","libelleCollectivite":"Fonctionnement","libelleEHPAD":"Fonctionnement","@id":1117,"@type":"TypeGestionSections"},"pesos":false,"@id":1100,"@type":"CA_NC"}""";

        /** Serie de bordereaux the ticked bordereaux belong to. */
        private static final String SERIE_BORDEREAU = """
                        {"id":4,"marque":1,"etat":{"_id":4,"_lib":"RIEN_A_FAIRE","@id":1011,"@type":"EtatEnum"},"etatPrecedent":1011,"ordrePieces":{"_id":3,"_lib":"Compte puis tiers","@id":1012,"@type":"TypeOrdrePieces"},"detailFonction":false,"budgetaire":true,"interne":false,"annulatif":false,"sens":{"_id":2,"_lib":"Dépense","@id":1013,"@type":"TypeGestionSens"},"libelle":"Mandats ordinaires","code":"M+","piecesMonoImputation":false,"piecesMonoSection":false,"numerotationParBordPrep":false,"budgetRef":{"id":1,"marque":0,"etat":1011,"etatPrecedent":1011,"budgetPrincipal":false,"budgetAutonome":false,"regimeFiscal":{"_id":0,"_lib":"Non renseigné","@id":1015,"@type":"TypeRegimeFiscal"},"sectionsGerees":{"_id":0,"_lib":"Non renseigné","libelleCollectivite":"Non renseigné","libelleEHPAD":"Non renseigné","@id":1016,"@type":"TypeGestionSections"},"dateMigrationSEPA":"01/02/2014 0:00:00","dataMatrix":false,"utilisationTipi":false,"utilisationTipiParDefaut":true,"gestionASAP":false,"editionTIP":false,"tipSepa":false,"asapUtilisationChorusPro":false,"asapUtilisationServiceEditique":false,"asapGenerationParTitre":false,"asapUtilisationFE":false,"asapLogoColl":false,"@id":1014,"@type":"Budget"},"@id":1010,"@type":"SerieBordereauLiquidation"}""";

        /** Signatory printed on the bordereaux, as the signataire call answers it. */
        private static final String SIGNATAIRE = """
                        {"id":1,"marque":2,"etat":{"_id":4,"_lib":"RIEN_A_FAIRE","@id":1201,"@type":"EtatEnum"},"etatPrecedent":1201,"idUtilisateur":0,"intituleFonction":"Maire","libelle":"Patrick GADROY-LEGENVRE, Maire","nomSignataire":"GADROY-LEGENVRE","prenomSignataire":"Patrick","collectiviteRef":{"id":1,"marque":0,"etat":1201,"etatPrecedent":1201,"archiverPJTraite":false,"circuitValidationPJ":false,"circuitValidationBC":false,"engagementBCNonValide":false,"editBDCNonValide":false,"editBDCNonEng":false,"gestionMultiBudget":false,"transfoBDCEnPJ":false,"transfoDocBDCEnPJ":false,"transmissionBDCSurLiquidation":false,"gestionPESMarche":false,"depotBDCChorusEngagement":{"_id":1,"_lib":"MANUEL","@id":1203,"@type":"TypeDepotChorusEngagement"},"depotDocBDCChorusEngagement":false,"depotBDCSigneChorusEngagement":false,"transmettreServiceEmetteurBDC":false,"transmettreServiceRecepteurFacture":false,"gestionBudgetVert":{"_id":0,"_lib":"Indéterminé","@id":1204,"@type":"TypeGestionBudgetVert"},"libelle":"","@id":1202,"@type":"Collectivite"},"inactif":false,"parDefaut":true,"@id":1200,"@type":"SignataireCollectivite"}""";

        /** The budget as the circuit call sends it: the collectivite's own configuration. */
        private static final String BUDGET = """
                        {"id":1,"marque":2,"etat":{"_id":4,"_lib":"RIEN_A_FAIRE","@id":3,"@type":"EtatEnum"},"etatPrecedent":3,"code":"ST","libelle":"SAINT ALBAN D'HURTIERES","nic":"00014","budgetPrincipal":true,"budgetAutonome":false,"codificationTresorerie1":"037","codificationTresorerie2":"00","regimeFiscal":{"_id":1,"_lib":"TTC","@id":4,"@type":"TypeRegimeFiscal"},"sectionsGerees":{"_id":1,"_lib":"Fonctionnement et Investissement","libelleCollectivite":"Fonctionnement et Investissement","libelleEHPAD":"Exploitation et Investissement","@id":5,"@type":"TypeGestionSections"},"collectiviteRef":{"id":1,"marque":4,"etat":3,"etatPrecedent":3,"code":"SAIN","numeroSiren":"217302207","titreResponsable":"MAIRE","populationReelle":404,"assembleeDeliberante":{"_id":0,"_lib":"Non Renseigné","@id":7,"@type":"TypeAssembleeDeliberante"},"typeCollectiviteRef":{"id":-100,"marque":0,"etat":3,"etatPrecedent":3,"@id":8,"@type":"TypeCollectivite"},"apenaf700Ref":{"id":-641,"marque":0,"etat":3,"etatPrecedent":3,"@id":9,"@type":"APENAF700"},"archiverPJTraite":true,"circuitValidationPJ":false,"circuit":{"id":0,"marque":0,"etat":3,"etatPrecedent":3,"parDefaut":false,"delaiViseur":0,"@id":10,"@type":"Circuit"},"circuitValidationBC":false,"engagementBCNonValide":false,"editBDCNonValide":false,"editBDCNonEng":false,"bdcPourValidation":{"id":0,"marque":0,"etat":3,"etatPrecedent":3,"@id":11,"@type":"DocPersonnalise"},"gestionMultiBudget":false,"transfoBDCEnPJ":false,"transfoDocBDCEnPJ":false,"transmissionBDCSurLiquidation":false,"collectiviteHistoriqueRef":{"id":1,"marque":0,"etat":3,"etatPrecedent":3,"@id":12,"@type":"CollectiviteHistorique"},"gestionPESMarche":false,"tiersAcheteur":{"id":0,"marque":0,"etat":3,"etatPrecedent":3,"inactif":false,"epl_chorus":false,"engagementObligatoireChorus":false,"serviceObligatoireChorus":false,"serviceOuEngagementObligatoireChorus":false,"renseignerLibVir2":true,"anonymise":false,"entiteChorus":false,"@id":13,"@type":"TiersComptable"},"depotBDCChorusEngagement":{"_id":1,"_lib":"MANUEL","@id":14,"@type":"TypeDepotChorusEngagement"},"depotDocBDCChorusEngagement":false,"depotBDCSigneChorusEngagement":false,"bdcPourChorus":11,"transmettreServiceEmetteurBDC":false,"transmettreServiceRecepteurFacture":false,"gestionBudgetVert":{"_id":0,"_lib":"Indéterminé","@id":15,"@type":"TypeGestionBudgetVert"},"libelle":"ND","@id":6,"@type":"Collectivite"},"budgetPrincipalRef":{"id":0,"marque":0,"etat":3,"etatPrecedent":3,"budgetPrincipal":false,"budgetAutonome":false,"regimeFiscal":{"_id":0,"_lib":"Non renseigné","@id":17,"@type":"TypeRegimeFiscal"},"sectionsGerees":{"_id":0,"_lib":"Non renseigné","libelleCollectivite":"Non renseigné","libelleEHPAD":"Non renseigné","@id":18,"@type":"TypeGestionSections"},"dateMigrationSEPA":"01/02/2014 0:00:00","dataMatrix":false,"utilisationTipi":false,"utilisationTipiParDefaut":true,"gestionASAP":false,"editionTIP":false,"tipSepa":false,"asapUtilisationChorusPro":false,"asapUtilisationServiceEditique":false,"asapGenerationParTitre":false,"asapUtilisationFE":false,"asapLogoColl":false,"@id":16,"@type":"Budget"},"budgetPiloteRef":16,"numeroSiren":"217302207","assembleeDeliberante":7,"dateMigrationSEPA":"01/02/2014 0:00:00","dataMatrix":false,"modaliteReglement1":"","modaliteReglement2":"- Par chèque bancaire ou postal adressé au comptable chargé du recouvrement : veuillez joindre le talon détachable à votre chèque, sans le coller ni l'agrafer.","modaliteReglement3":"- Par mandat ou virement sur le compte courant postal du comptable chargé du recouvrement : veuillez inscrire très lisiblement dans le cadre \\"correspondance\\" les références portées sur le talon détachable.","modaliteReglement4":"LIBELLEZ obligatoirement le chèque ou le mandat à l'ordre du TRESOR PUBLIC, dans votre intérêt n'envoyer en aucun cas un chèque sans indication du bénéficiaire ainsi que des références de la créance dont vous vous acquittez.","modaliteReglement5":"- En espèces (dans la limite de 300 €) ou en carte bancaire, muni du présent avis, auprès d’un buraliste ou partenaire agréé (liste consultable sur www.impots.gouv.fr/portail/paiement-de-proximite) : veuillez rapporter le présent avis en venant payer.","utilisationTipi":true,"utilisationTipiParDefaut":true,"identifiantTipi":"071222","modaliteReglementTipi":"Vous pouvez payer sur internet en vous connectant sur www.payfip.gouv.fr et en saisissant les informations suivantes :","siteInternetTipi":"www.payfip.gouv.fr","connexionActe":{"id":1,"marque":0,"etat":3,"etatPrecedent":3,"archivageSaeActe":false,"@id":19,"@type":"ConnexionActe"},"adresseRef":{"id":0,"marque":0,"etat":3,"etatPrecedent":3,"adressePays":false,"@id":20,"@type":"Adresse"},"gestionASAP":true,"editionTIP":false,"tipSepa":false,"centreEncaissement":{"id":-40,"marque":0,"etat":3,"etatPrecedent":3,"@id":21,"@type":"CentreEncaissement"},"emetteurFacture":{"id":1,"marque":0,"etat":3,"etatPrecedent":3,"@id":22,"@type":"EmetteurFacture"},"talonOptique2Lignes":{"id":1,"marque":0,"etat":3,"etatPrecedent":3,"@id":23,"@type":"TalonOptique2Ligne"},"asapUtilisationChorusPro":true,"asapUtilisationServiceEditique":true,"asapGenerationParTitre":false,"asapUtilisationFE":false,"asapLogoColl":false,"@id":2,"@type":"Budget"}""";

        // ---------------------------------------------------------------------------------
        // 1. Opening the "Edition / transfert de bordereaux existants" tab
        // ---------------------------------------------------------------------------------

        /**
         * The grid: every bordereau of the serie for the current millesime, with its amounts.
         *
         * <p>Rows are kept as raw JSON so {@link #choisirBordereaux} can hand a subset of them
         * straight back to the calls below, and their ids are kept apart to drive the per-tick
         * sizing call.
         */
        public static final HttpRequestActionBuilder fournirListeBordereauxAvecMontant = http(
                        "Fournir liste bordereaux avec montant")
                        .post(API + "/Ordonnancement/fournirListeBordereauxAvecMontant?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                        {"param":{"listeCriteres":[{"lienClassePersistante":"Bordereau","lienAttribut":"serieBordereauLiquidationRef.id","valeur":4,"operateur":{"_id":3,"_lib":"égal à","@id":4,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"},{"lienClassePersistante":"Bordereau","lienAttribut":"millesime","valeur":#{millesime},"operateur":4,"@id":5,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[],"distinct":false,"paginatorValues":{"length":1000,"pageIndex":0,"pageSize":1000,"previousPageIndex":0},"@id":2,"@type":"RechercheParametres"},"search":null,"sortValue":{"active":"numeroBordereau","direction":"desc"}}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0))
                        .check(jsonPath("$.donnees[*]").findAll().saveAs("bordereauRows"))
                        .check(jsonPath("$.donnees[*].id").findAll().saveAs("bordereauIds"));

        /**
         * Ticks 1 to 3 bordereaux, drawn per iteration.
         *
         * <p>Takes them from the top of the grid, and never leaves row 0 out. Row 0 is the one
         * that spells out the objects shared by the whole list ({@code etat}, the serie, its
         * budget, the signataire...); the later rows only point at them by {@code @id}. Any
         * selection that includes row 0 is therefore self-contained and its rows can be
         * concatenated untouched. Leave row 0 out and the others arrive with dangling references.
         *
         * <p>Also stamps the moment the flux is asked for: the dossier carries it three times
         * over, and it has to be the same value everywhere.
         */
        public static final ChainBuilder choisirBordereaux = exec(session -> {
                List<String> rows = session.getList("bordereauRows");
                List<String> ids = session.getList("bordereauIds");
                int nb = Math.min(ThreadLocalRandom.current().nextInt(1, 4), rows.size());
                return session
                                .set("nbSelection", nb)
                                .set("selectionBordereauxJson", String.join(",", rows.subList(0, nb)))
                                .set("selectionBordereauIds", String.join(",", ids.subList(0, nb)))
                                .set("exerciceComptableJson", compact(session.getString("exerciceComptableJson")))
                                .set("dateTransmission", LocalDateTime.now().format(HORODATAGE));
        });

        /**
         * Sizes the PES flux for one bordereau. The screen calls it once per ticked row, so the
         * body reads the {@code indexBordereau} counter of the surrounding repeat rather than a
         * fixed index — see {@code OrdonnancementEditionTransfertGroup}.
         */
        public static final HttpRequestActionBuilder fournirTailleBordereauPourPES = http(
                        "Fournir taille bordereau pour PES")
                        .post(API + "/Ordonnancement/fournirTailleBordereauPourPES?fieldNames%5B%5D=**")
                        .body(StringBody(session -> "{\"bordereauId\":"
                                        + session.getList("bordereauIds").get(session.getInt("indexBordereau"))
                                        + ",\"idBudget\":1}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        /** The liquidations carried by the ticked bordereaux, shown when the selection expands. */
        public static final HttpRequestActionBuilder fournirListeLiquidationsParBordereaux = http(
                        "Fournir liste liquidations par bordereaux")
                        .post(API + "/Ordonnancement/fournirListeLiquidations?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        "{\"selCrit\":{\"id\":-1,\"marque\":0,\"listeIdBordereau\":"
                                                        + "[#{selectionBordereauIds}],\"@id\":2,"
                                                        + "\"@type\":\"SelectionLiquidation\"}}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        // ---------------------------------------------------------------------------------
        // 2. The burst the screen fires once bordereaux are ticked. The browser sends these in
        // parallel and repeats the two signataire calls once each; kept here so the request
        // count matches what the server really sees.
        // ---------------------------------------------------------------------------------

        /**
         * Attachments carried by the ticked bordereaux. Fired twice: once on selection, once
         * again when the user asks for the flux — the second answer is what
         * {@link #preparerCorpsFluxPES} splits, so both calls save it.
         *
         * <p>{@code aTransmettre} is the server's call, and it is what decides which list each
         * row ends up in, so the flag is kept alongside the rows.
         */
        public static final HttpRequestActionBuilder fournirListePJATransmettre = http(
                        "Fournir liste PJ a transmettre")
                        .post(API + "/Ordonnancement/fournirListePJATransmettreDepuisListeBordereaux"
                                        + "?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        "{\"param\":{\"listeCriteres\":[],\"listeCriteresRechercheGui\":[],"
                                                        + "\"listeAttributs\":[],\"listeTris\":[],\"distinct\":false,"
                                                        + "\"paginatorValues\":{\"length\":1000,\"pageIndex\":0,\"pageSize\":1000,"
                                                        + "\"previousPageIndex\":0},\"@id\":2,\"@type\":\"RechercheParametres\"},"
                                                        + "\"sortValue\":{\"active\":\"tiers\",\"direction\":\"asc\"},"
                                                        + "\"bordereaux\":"
                                                        + bordereaux("#{selectionBordereauxJson}", "TableauEntitePersistante", 1)
                                                        + ",\"idBudget\":1}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0))
                        .check(jsonPath("$.donnees[*]").findAll().saveAs("pjRows"))
                        .check(jsonPath("$.donnees[*].id").findAll().saveAs("pjIds"))
                        .check(jsonPath("$.donnees[*].aTransmettre").findAll().saveAs("pjATransmettre"));

        /**
         * Signatory to print on the bordereaux. Re-sends the whole exercice comptable; it is its
         * own argument, so it keeps the numbering the API answered with and needs no shifting.
         */
        public static final HttpRequestActionBuilder fournirSignataireActifCollectivite = http(
                        "Fournir signataire actif collectivite")
                        .post(API + "/Ordonnancement/fournirSignataireActifCollectiteByCollectiviteId"
                                        + "?fieldNames%5B%5D=**")
                        .body(StringBody("{\"bordereauListe\":"
                                        + bordereaux("#{selectionBordereauxJson}", "Association", 1)
                                        + ",\"idCollectivite\":1,\"exerciceComptable\":#{exerciceComptableJson}}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        /**
         * Default signatory label, e.g. "Patrick GADROY-LEGENVRE, Maire". Same payload as above
         * minus the collectivite id, with the bordereaux under {@code bordereauRecetteListe}.
         * Answers a bare JSON string, so only its length is checked.
         */
        public static final HttpRequestActionBuilder rechercherLibelleSignataireParDefaut = http(
                        "Rechercher libelle signataire par defaut")
                        .post(API + "/Ordonnancement/rechercherLibelleSignataireParDefaut?fieldNames%5B%5D=**")
                        .body(StringBody("{\"bordereauRecetteListe\":"
                                        + bordereaux("#{selectionBordereauxJson}", "Association", 1)
                                        + ",\"exerciceComptable\":#{exerciceComptableJson}}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(bodyLength().gt(0));

        /**
         * Asks whether any asset sheet behind the ticked bordereaux is incomplete. Unlike the
         * numerotation flow this one really does send the selection.
         */
        public static final HttpRequestActionBuilder liquidationsParBordereauxHasBienIncomplet = http(
                        "Liquidations par bordereaux has bien incomplet")
                        .post(API + "/Ordonnancement/liquidationsParBordereauxHasBienIncomplet")
                        .body(StringBody("{\"idsBordereaux\":[#{selectionBordereauIds}]}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(bodyLength().gt(0));

        /** Assigning accountants for the collectivite. Static search criteria. */
        public static final HttpRequestActionBuilder fournirListeComptablesAssignataires = http(
                        "Fournir liste comptables assignataires")
                        .post(API + "/Ordonnancement/fournirListeComptablesAssignataires?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                        {"param":{"listeCriteres":[{"lienClassePersistante":"ComptableAssignataire","lienAttribut":"collectiviteListe","valeur":{"id":1,"marque":4,"etat":{"_id":4,"_lib":"RIEN_A_FAIRE","@id":5,"@type":"EtatEnum"},"etatPrecedent":5,"code":"SAIN","numeroSiren":"217302207","titreResponsable":"MAIRE","populationReelle":404,"assembleeDeliberante":{"_id":0,"_lib":"Non Renseigné","@id":6,"@type":"TypeAssembleeDeliberante"},"typeCollectiviteRef":{"id":-100,"marque":0,"etat":5,"etatPrecedent":5,"@id":7,"@type":"TypeCollectivite"},"apenaf700Ref":{"id":-641,"marque":0,"etat":5,"etatPrecedent":5,"@id":8,"@type":"APENAF700"},"archiverPJTraite":true,"circuitValidationPJ":false,"circuit":{"id":0,"marque":0,"etat":5,"etatPrecedent":5,"parDefaut":false,"delaiViseur":0,"@id":9,"@type":"Circuit"},"circuitValidationBC":false,"engagementBCNonValide":false,"editBDCNonValide":false,"editBDCNonEng":false,"bdcPourValidation":{"id":0,"marque":0,"etat":5,"etatPrecedent":5,"@id":10,"@type":"DocPersonnalise"},"gestionMultiBudget":false,"transfoBDCEnPJ":false,"transfoDocBDCEnPJ":false,"transmissionBDCSurLiquidation":false,"collectiviteHistoriqueRef":{"id":1,"marque":0,"etat":5,"etatPrecedent":5,"@id":11,"@type":"CollectiviteHistorique"},"gestionPESMarche":false,"tiersAcheteur":{"id":0,"marque":0,"etat":5,"etatPrecedent":5,"inactif":false,"epl_chorus":false,"engagementObligatoireChorus":false,"serviceObligatoireChorus":false,"serviceOuEngagementObligatoireChorus":false,"renseignerLibVir2":true,"anonymise":false,"entiteChorus":false,"@id":12,"@type":"TiersComptable"},"depotBDCChorusEngagement":{"_id":1,"_lib":"MANUEL","@id":13,"@type":"TypeDepotChorusEngagement"},"depotDocBDCChorusEngagement":false,"depotBDCSigneChorusEngagement":false,"bdcPourChorus":10,"transmettreServiceEmetteurBDC":false,"transmettreServiceRecepteurFacture":false,"gestionBudgetVert":{"_id":0,"_lib":"Indéterminé","@id":14,"@type":"TypeGestionBudgetVert"},"libelle":"ND","@id":4,"@type":"Collectivite"},"operateur":{"_id":3,"_lib":"égal à","@id":15,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[{"croissant":true,"lienClassePersistante":"ComptableAssignataire","lienAttribut":"designation","@id":16,"@type":"RechercheTri"}],"distinct":false,"@id":2,"@type":"RechercheParametres"}}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        /** Comptable exchange (PES) configuration. Takes no argument. */
        public static final HttpRequestActionBuilder chargerConfigEchangeComptable = http(
                        "Charger config echange comptable")
                        .post(API + "/UcConfigEchangeComptable/chargerConfigEchangeComptable?fieldNames%5B%5D=**")
                        .body(StringBody("{}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        /** Valid accounting-norm periods, keyed on the exercice's millesime. */
        public static final HttpRequestActionBuilder fournirListeCA_NCValide = http(
                        "Fournir liste CA_NC valide")
                        .post(API + "/Ordonnancement/fournirListeCA_NCValide?fieldNames%5B%5D=**")
                        .body(StringBody("""
                                        {"idComptable":1,"millesime":#{millesime},"idNormeComptable":-310}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("donnees[0].id").ofInt().gt(0));

        /** PES signing circuits. Returns an empty list on this tenant. */
        public static final HttpRequestActionBuilder chargerListeCircuit = http("Charger liste circuit")
                        .post(API + "/Ordonnancement/chargerListeCircuit?fieldNames%5B%5D=**")
                        .body(StringBody(
                                        """
                                        {"param":{"listeCriteres":[{"lienClassePersistante":"Circuit","lienAttribut":"typeParapheur","valeur":"PES","operateur":{"_id":3,"_lib":"égal à","@id":4,"@type":"RechercheOperateurEnum"},"@id":3,"@type":"RechercheCritere"}],"listeCriteresRechercheGui":[],"listeAttributs":[],"listeTris":[],"distinct":false,"@id":2,"@type":"RechercheParametres"},"elementACharger":null}
                                        """))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("\"@id\"").ofInt().gt(0));

        /**
         * Default circuit for the ticked bordereaux. Answers with an empty body on this tenant,
         * so there is nothing to assert beyond the status.
         */
        public static final HttpRequestActionBuilder fournirCircuitParDefaut = http(
                        "Fournir circuit par defaut")
                        .post(API + "/UcFichierLiaisonCommun/fournirCircuitParDefaut?fieldNames%5B%5D=**")
                        .body(StringBody("{\"bordereauListe\":"
                                        + bordereaux("#{selectionBordereauxJson}", "Association", 1)
                                        + ",\"budget\":" + BUDGET + ",\"sensExecution\":null}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"));

        // ---------------------------------------------------------------------------------
        // 3. "Generer le flux PES"
        // ---------------------------------------------------------------------------------

        /**
         * First step of the generation: saves whatever the user changed on the bordereaux —
         * signatory, mostly — and hands them back with their {@code marque} bumped. Everything
         * downstream sends that returned version, not the one off the grid, so the response is
         * kept and moved into the 3000+ range the {@code weGfOrdonnancement} argument reserves
         * for it.
         */
        public static final HttpRequestActionBuilder processModifierBordereauListe = http(
                        "Process modifier bordereau liste")
                        .post(API + "/Ordonnancement/processModifierBordereauListe?fieldNames%5B%5D=**")
                        .body(StringBody("{\"bordereauListe\":"
                                        + bordereaux("#{selectionBordereauxJson}", "Association", 1) + "}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jmesPath("bordereauListe.donnees[0].id").ofInt().gt(0))
                        .check(jsonPath("$.bordereauListe.donnees[*]").findAll().saveAs("bordereauModifiesRows"));

        /**
         * Moves the two captured lists into their own {@code @id} ranges and splits the
         * attachments the way the screen does: {@code aTransmettre} decides whether a row is
         * offered for the flux or held back.
         *
         * <p>Both PJ lists live in the same {@code weGfOrdonnancement} argument, so they are
         * shifted as one block before being split — a row held back can then still point at an
         * object first written out by a row that goes in, exactly as the server sent them.
         *
         * <p>Must run between {@link #processModifierBordereauListe}, whose response it reads,
         * and the three calls below, which send what it builds.
         */
        public static final ChainBuilder preparerCorpsFluxPES = exec(session -> {
                List<String> bordereauRows = session.getList("bordereauModifiesRows");
                List<String> shiftedBordereaux = new ArrayList<>();
                for (String row : bordereauRows) {
                        shiftedBordereaux.add(shiftIds(row, BORDEREAU_REFERENCE, 3000));
                }

                List<String> pjRows = session.getList("pjRows");
                List<String> pjIds = session.getList("pjIds");
                List<String> pjATransmettre = session.getList("pjATransmettre");
                List<String> aTransmettre = new ArrayList<>();
                List<String> aNePasTransmettre = new ArrayList<>();
                List<String> idsAEnvoyer = new ArrayList<>();
                for (int i = 0; i < pjRows.size(); i++) {
                        String row = shiftIds(pjRows.get(i), PJ_REFERENCE, 5000);
                        if (Boolean.parseBoolean(pjATransmettre.get(i))) {
                                aTransmettre.add(row);
                                idsAEnvoyer.add(pjIds.get(i));
                        } else {
                                aNePasTransmettre.add(row);
                        }
                }

                return session
                                .set("bordereauModifiesJson", String.join(",", shiftedBordereaux))
                                .set("pjATransmettreJson", String.join(",", aTransmettre))
                                .set("pjAPasTransmettreJson", String.join(",", aNePasTransmettre))
                                .set("idPJAEnvoyer", String.join(",", idsAEnvoyer));
        });

        /**
         * Everything the generation needs, in one argument: the bordereaux, the exercice, the
         * PES configuration and the attachments. Sent three times over — control, send, then
         * suivi — so it is written once here, without the enclosing braces, and the last call
         * prepends the dossier id it does not have yet.
         */
        private static final String WEGF_ORDONNANCEMENT_FIELDS =
                        "\"liquidationListe\":{\"donnees\":[],\"@id\":1001,\"@type\":\"Association\"},"
                                        + "\"bordereauListe\":"
                                        + bordereaux("#{bordereauModifiesJson}", "Association", 3000) + ","
                                        + "\"choixMouvementATraiter\":{\"_id\":3,\"_lib\":\"Tous\",\"@id\":1002,"
                                        + "\"@type\":\"TypeMouvementATraiter\"},"
                                        + "\"ca_nc\":" + CA_NC + ","
                                        + "\"protocole\":1115,"
                                        + "\"nomFichier\":null,"
                                        + "\"exerciceComptableRef\":#{exerciceComptableJson},"
                                        + "\"controleAutomatiquePES\":true,"
                                        + "\"serieBordereauLiquidationRef\":" + SERIE_BORDEREAU + ","
                                        + "\"idPJAEnvoyerListe\":[#{idPJAEnvoyer}],"
                                        + "\"regroupementInventaire\":1117,"
                                        + "\"signataireIHM\":" + SIGNATAIRE + ","
                                        + "\"comptaAssign\":1106,"
                                        + "\"informationSignature\":false,"
                                        + "\"enteteASAP\":null,"
                                        + "\"infoComplementaireASAP\":null,"
                                        + "\"pjATransmettreAfficheeListe\":{\"donnees\":[#{pjATransmettreJson}],"
                                        + "\"@id\":5000,\"@type\":\"TableauEntitePersistante\"},"
                                        + "\"pjATransmettreNonAfficheeListe\":{\"donnees\":[],\"@id\":1003,"
                                        + "\"@type\":\"TableauEntitePersistante\"},"
                                        + "\"pjAPASTransmettreAfficheeListe\":{\"donnees\":[#{pjAPasTransmettreJson}],"
                                        + "\"@id\":5001,\"@type\":\"TableauEntitePersistante\"},"
                                        + "\"@id\":1000,\"@type\":\"WeGfOrdonnancement\"";

        private static final String WEGF_ORDONNANCEMENT = "{" + WEGF_ORDONNANCEMENT_FIELDS + "}";

        /**
         * The dossier the flux is filed under: a fresh one ({@code id} -1) waiting to be
         * generated. Its own argument, so the exercice keeps its native numbering and the
         * skeleton sits at 2000+; {@code budgetRef} and {@code collectiviteRef} point back into
         * the exercice, which is why {@code ExecutionApiEndpoints.chargerExerciceComptable} saves
         * the two {@code @id}s rather than having them guessed here.
         */
        private static final String DOSSIER =
                        "{\"id\":-1,\"marque\":0,"
                                        + "\"etat\":{\"_id\":1,\"_lib\":\"INSERE\",\"@id\":2001,\"@type\":\"EtatEnum\"},"
                                        + "\"typeDossier\":{\"_id\":1,\"_lib\":\"Aller\",\"@id\":2002,\"@type\":\"TypeDossier\"},"
                                        + "\"expediteur\":\"" + EXPEDITEUR + "\",\"archive\":false,\"envoiAutoTDT\":false,"
                                        + "\"modeSignature\":{\"_id\":0,\"_lib\":\"Non Renseigné\",\"@id\":2003,"
                                        + "\"@type\":\"TypeModeLiaison\"},"
                                        + "\"modeTeletransmission\":2003,"
                                        + "\"statut\":{\"_id\":56,\"_lib\":\"Demande de génération du Flux\",\"@id\":2004,"
                                        + "\"@type\":\"EtatDossier\"},"
                                        + "\"dateCreation\":\"#{dateTransmission}\","
                                        + "\"destination\":{\"_id\":5,\"_lib\":\"Tiers de télétransmission\",\"@id\":2005,"
                                        + "\"@type\":\"TypeDestinationFichier\"},"
                                        + "\"exerciceComptableRef\":#{exerciceComptableJson},"
                                        + "\"budgetRef\":#{exerciceBudgetRefId},"
                                        + "\"collectiviteRef\":#{exerciceCollectiviteRefId},"
                                        + "\"dossierDocumentListe\":{\"donnees\":[{\"id\":-1,\"marque\":0,"
                                        + "\"document\":{\"id\":-1,\"marque\":0,"
                                        + "\"protocoleMetier\":{\"_id\":1,\"_lib\":\"PES Aller\",\"@id\":2013,"
                                        + "\"@type\":\"TypeProtocoleMetier\"},"
                                        + "\"etat\":2001,\"dateCreation\":\"#{dateTransmission}\","
                                        + "\"de_Doc\":{\"@id\":2014,\"@type\":\"Blob\"},\"compresse\":false,"
                                        + "\"nomFichierOrigine\":null,\"@id\":2012,\"@type\":\"Document\"},"
                                        + "\"etat\":2001,"
                                        + "\"typeFichier\":{\"_id\":1,\"_lib\":\"Flux 1 métier\",\"@id\":2015,"
                                        + "\"@type\":\"TypeFichier\"},"
                                        + "\"dossier\":2000,\"@id\":2011,\"@type\":\"DossierDocument\"}],"
                                        + "\"@id\":2010,\"@type\":\"Association\"},"
                                        + "\"histoDossierListe\":{\"donnees\":[{\"id\":-1,\"marque\":0,\"etat\":2001,"
                                        + "\"acteur\":\"" + EXPEDITEUR + "\",\"annotation\":null,\"statut\":2004,"
                                        + "\"dateAction\":\"#{dateTransmission}\",\"dossier\":2000,\"@id\":2021,"
                                        + "\"@type\":\"HistoDossier\"}],\"@id\":2020,\"@type\":\"Association\"},"
                                        + "\"@id\":2000,\"@type\":\"Dossier\"}";

        /**
         * Dry run before the write: answers a {@code TableauInfoErreur}, empty when the selection
         * can be turned into a flux. A non-empty one means the flux would be refused, so the
         * check fails the request rather than letting the send below report a misleading success.
         */
        public static final HttpRequestActionBuilder controleGenerationPes = http(
                        "Controle generation PES")
                        .post(API + "/Ordonnancement/controleGenerationPes")
                        .body(StringBody("{\"weGfOrdonnancement\":" + WEGF_ORDONNANCEMENT + "}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jsonPath("$.donnees[0]").notExists());

        /**
         * The write: files the dossier and queues the PES flux for the ticked bordereaux.
         * Heaviest call of the flow (~58 KB up, ~43 KB down, ~2.4 s recorded).
         *
         * <p>Note this chain writes: every pass creates a real dossier in the suivi des echanges.
         */
        public static final HttpRequestActionBuilder envoyerNouveauDossierExecutionPlusTard = http(
                        "Envoyer nouveau dossier execution plus tard")
                        .post(API + "/Ordonnancement/envoyerNouveauDossierExecutionPlusTard?fieldNames%5B%5D=**")
                        .body(StringBody("{\"dossier\":" + DOSSIER + ","
                                        + "\"bordereauListe\":"
                                        + bordereaux("#{bordereauModifiesJson}", "Association", 2999) + ","
                                        + "\"pjUtilisateurListe\":{\"donnees\":[],\"@id\":1,\"@type\":\"Association\"},"
                                        + "\"exerciceComptable\":#{exerciceComptableJson},"
                                        + "\"dateTransmission\":\"#{dateTransmission}\","
                                        + "\"weGfOrdonnancement\":" + WEGF_ORDONNANCEMENT + "}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"))
                        .check(jsonPath("$.tableauInfoErreur.donnees[0]").notExists())
                        .check(jsonPath("$.entite.id").ofInt().gt(0))
                        .check(jsonPath("$.entite.id").saveAs("idDossier"));

        /** Files the generated flux against the dossier just created. Answers an empty body. */
        public static final HttpRequestActionBuilder majPesDansSuiviEchange = http(
                        "Maj PES dans suivi echange")
                        .post(API + "/Ordonnancement/majPesDansSuiviEchange?fieldNames%5B%5D=**")
                        .body(StringBody("{\"weGfOrdonnancement\":{\"idDossier\":#{idDossier},"
                                        + WEGF_ORDONNANCEMENT_FIELDS + "}}"))
                        .headers(ApiHeaders.bearerWithTenant("content-type", "application/json"));
}
