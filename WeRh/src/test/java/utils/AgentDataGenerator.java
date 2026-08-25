package utils;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Computes the agent payload values that are not drawn from a list: the free-text fields and the
 * numbers the back office cross-checks.
 *
 * <p>The recorded creation form sends data the back office validates against each other: the
 * numero de securite sociale is derived from the civilite, the birth date and the INSEE code of
 * the birth commune, its key is the 97-complement of the number, and the same holds for the RIB
 * key and the IBAN check digits. Generating those fields independently would produce payloads the
 * API rejects, so every checksum is computed here from the values actually sent.
 *
 * <p>The enumerated values — communes, civilites, street names and family statuses — come from the
 * feeders in {@link AgentFeeders} instead.
 */
public final class AgentDataGenerator {

    private AgentDataGenerator() {
    }

    private static final String LETTRES = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /** Bank codes of the recorded IBAN; the account number is generated. */
    private static final String CODE_BANQUE = "16106";
    private static final String CODE_GUICHET = "00001";

    private static ThreadLocalRandom random() {
        return ThreadLocalRandom.current();
    }

    // ---------------------------------------------------------------- identity

    /** A random upper-case name of {@code longueur} letters, e.g. "RTRT". */
    public static String nom(int longueur) {
        StringBuilder result = new StringBuilder(longueur);
        for (int i = 0; i < longueur; i++) {
            result.append(LETTRES.charAt(random().nextInt(LETTRES.length())));
        }
        return result.toString();
    }

    /** A random capitalised first name of {@code longueur} letters, e.g. "Rtry". */
    public static String prenom(int longueur) {
        String letters = nom(longueur);
        return letters.charAt(0) + letters.substring(1).toLowerCase(Locale.ROOT);
    }

    /** An ISO birth date for an agent between {@code ageMin} and {@code ageMax} years old. */
    public static String dateNaissance(int ageMin, int ageMax) {
        LocalDate today = LocalDate.now();
        int age = random().nextInt(ageMin, ageMax + 1);
        LocalDate birth = today.minusYears(age)
                .withDayOfMonth(1)
                .minusDays(random().nextInt(365));
        return birth.toString();
    }

    /**
     * The date the supplement familial de traitement stops being due for a child born on
     * {@code dateNaissance}: the birthday on which it reaches {@code ageLimite}, brought back to
     * the first of that month — a child born 2021-03-12 gives 2041-03-01 for a limit of 20 years.
     */
    public static String dateEcheanceSft(String dateNaissance, int ageLimite) {
        return LocalDate.parse(dateNaissance)
                .plusYears(ageLimite)
                .withDayOfMonth(1)
                .toString();
    }

    /**
     * The identifier the form allocates to a document before uploading it. The child payload sends
     * it three times — in the item, in the document changes and as the name of the file part — so
     * the back office can match the metadata with the content.
     */
    public static String documentId() {
        return UUID.randomUUID().toString();
    }

    // ------------------------------------------------------- securite sociale

    /**
     * The 13-digit numero de securite sociale: the sex digit of the civilite (see the "sexeInsee"
     * column of the civilites feeder), the last two digits of the birth year, the birth month, the
     * INSEE code of the birth commune and a random 3-digit sequence number.
     */
    public static String numInsee(String sexeInsee, String dateNaissance, String communeCodeInsee) {
        LocalDate naissance = LocalDate.parse(dateNaissance);
        return "%s%02d%02d%s%03d".formatted(
                sexeInsee,
                naissance.getYear() % 100,
                naissance.getMonthValue(),
                communeCodeInsee,
                random().nextInt(1000));
    }

    /** The control key of a numero de securite sociale: 97 minus the number modulo 97. */
    public static String cleInsee(String numInsee) {
        return "%02d".formatted(97 - mod97(numInsee));
    }

    // ------------------------------------------------------------------- bank

    /** A random 11-digit account number. */
    public static String numeroCompte() {
        StringBuilder compte = new StringBuilder(11);
        for (int i = 0; i < 11; i++) {
            compte.append(random().nextInt(10));
        }
        return compte.toString();
    }

    /** The RIB key of an account held in the default bank and branch. */
    public static String cleRib(String numeroCompte) {
        return cleRib(CODE_BANQUE, CODE_GUICHET, numeroCompte);
    }

    /** The RIB key: 97 minus "code banque + code guichet + numero de compte + 00" modulo 97. */
    public static String cleRib(String codeBanque, String codeGuichet, String numeroCompte) {
        return "%02d".formatted(97 - mod97(codeBanque + codeGuichet + numeroCompte + "00"));
    }

    /** The IBAN of an account held in the default bank and branch. */
    public static String iban(String numeroCompte, String cleRib) {
        return iban(CODE_BANQUE, CODE_GUICHET, numeroCompte, cleRib);
    }

    /**
     * A French IBAN: "FR", the two check digits, then the BBAN (code banque, code guichet,
     * numero de compte, cle RIB). The check digits are 98 minus the BBAN followed by "FR00"
     * rearranged and modulo 97, with the letters replaced by their position plus 9 ("F" = 15,
     * "R" = 27).
     */
    public static String iban(String codeBanque, String codeGuichet, String numeroCompte, String cleRib) {
        String bban = codeBanque + codeGuichet + numeroCompte + cleRib;
        int cle = 98 - mod97(bban + "152700");
        return "FR%02d%s".formatted(cle, bban);
    }

    // ---------------------------------------------------------------- contact

    /** A random French landline number, e.g. "0554679876". */
    public static String telephone() {
        StringBuilder numero = new StringBuilder("0").append(random().nextInt(1, 6));
        for (int i = 0; i < 8; i++) {
            numero.append(random().nextInt(10));
        }
        return numero.toString();
    }

    /** A mail address built from the agent name, e.g. "rtry.rtrt@example.com". */
    public static String email(String prenom, String nom) {
        return (prenom + "." + nom + "@example.com").toLowerCase(Locale.ROOT);
    }

    // ---------------------------------------------------------------- address

    /** A street number between 1 and 199. */
    public static String adresseNumero() {
        return String.valueOf(random().nextInt(1, 200));
    }

    /** A building name, e.g. "Bat C". */
    public static String adresseBatiment() {
        return "Bat " + LETTRES.charAt(random().nextInt(6));
    }

    // ---------------------------------------------------------------- helpers

    /**
     * The value as a JSON string literal, or the {@code null} literal when it is absent — for the
     * payload fields the form leaves empty, which are written unquoted in the body template.
     */
    public static String jsonNullable(String value) {
        return value == null || value.isEmpty() ? "null" : "\"" + value + "\"";
    }

    /** The number modulo 97, digit by digit, so arbitrarily long numbers stay exact. */
    static int mod97(String digits) {
        int remainder = 0;
        for (int i = 0; i < digits.length(); i++) {
            remainder = (remainder * 10 + Character.digit(digits.charAt(i), 10)) % 97;
        }
        return remainder;
    }
}
