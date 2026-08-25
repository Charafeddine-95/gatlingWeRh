package utils;

import static io.gatling.javaapi.core.CoreDsl.csv;
import static io.gatling.javaapi.core.CoreDsl.listFeeder;

import io.gatling.javaapi.core.FeederBuilder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feeders holding the enumerated values of the agent form. The values live in
 * {@code src/test/resources/data}, so the set a run draws from is changed by editing a CSV rather
 * than the simulation.
 *
 * <p>Every feeder is random, so it never runs out and each virtual user draws independently.
 * Pinning a field to a single value is a matter of reducing its file to one row.
 *
 * <p>The columns are named after the session attributes the body template reads, so a feed fills
 * the payload directly. Communes are the exception: the payload carries them twice, once for the
 * birth place and once for the address, so {@link #communes(String)} renames the columns with the
 * prefix of the section it feeds.
 */
public final class AgentFeeders {

    private AgentFeeders() {
    }

    /** Communes, with the designation, codes and departement the payload sends together. */
    private static final List<Map<String, Object>> COMMUNES = csv("data/communes.csv").readRecords();

    /**
     * Civilites, each with the digit that opens a numero de securite sociale ("sexeInsee": 2 for a
     * woman, 1 for a man), so adding a civilite does not mean touching the NIR code.
     */
    public static final FeederBuilder<?> CIVILITES = csv("data/civilites.csv").random();

    /** Street names, filling "adresseNom". */
    public static final FeederBuilder<?> VOIES = csv("data/voies.csv").random();

    /** Street number complements, filling "adresseType" — as served by {@code /agent/type/adresse}. */
    public static final FeederBuilder<?> TYPES_ADRESSE = csv("data/typesAdresse.csv").random();

    /**
     * Family statuses, filling "situationFamiliale", each with the "avecConjoint" flag telling
     * whether the payload carries the conjoint fields.
     */
    public static final FeederBuilder<?> SITUATIONS_FAMILIALES =
            csv("data/situationsFamiliales.csv").random();

    /**
     * Genders of a child, filling "enfantGenre". A child has no civilite to derive it from, unlike
     * the agent whose "genre" follows the civilite drawn from {@link #CIVILITES}.
     */
    public static final FeederBuilder<?> GENRES_ENFANT = csv("data/genresEnfant.csv").random();

    /**
     * The communes feeder, with its columns renamed for the section it feeds: {@code
     * communes("communeNaissance")} fills "communeNaissanceDesignation",
     * "communeNaissanceCodeInsee" and the rest.
     */
    public static FeederBuilder<?> communes(String attributePrefix) {
        List<Map<String, Object>> records = COMMUNES.stream()
                .map(commune -> prefix(commune, attributePrefix))
                .toList();
        return listFeeder(records).random();
    }

    private static Map<String, Object> prefix(Map<String, Object> record, String attributePrefix) {
        Map<String, Object> prefixed = new LinkedHashMap<>();
        record.forEach((column, value) -> prefixed.put(
                attributePrefix + Character.toUpperCase(column.charAt(0)) + column.substring(1),
                value));
        return prefixed;
    }
}
