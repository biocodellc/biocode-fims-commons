package biocode.fims.bcid;


/**
 * Class for decoding an ark identifier. An identifier consists of the schema, naan, shoulder, and suffix.
 */
public class Identifier {
    private String identifier;
    private String scheme;
    private String naan;
    private String shoulder;
    private String suffix;
    private String divider;

    public Identifier(String identifier, String divider) {
        this.identifier = identifier;
        this.divider = divider;

        decode(identifier);
    }


    /**
     * get the ark root identifier. If not suffix, this is the same as the identifier
     *
     * @return
     */
    public String getRootIdentifier() {
        return scheme + "/" + naan + "/" + shoulder;
    }

    public String getSuffix() {
        return suffix;
    }

    public String getIdentifier() {
        return identifier;
    }

    private void decode(String identifier) {
        // Pull off potential last piece of string which would represent the local Identifier
        // The piece to decode is ark:/NAAN/bcidIdentifer (anything else after a last trailing "/" not decoded)
        String bits[] = identifier.split("/", 3);

        // the scheme is the first chunk
        scheme = bits[0];
        // the naan is the first chunk between the "/"'s
        naan = bits[1];

        // Now decipher the shoulder and suffix in the next bit
        setShoulderAndSuffix(bits[2]);
    }

    /**
     * Set the shoulder and suffix variables for this identifier
     *
     * @param a
     */
    private void setShoulderAndSuffix(String a) {
        boolean reachedShoulder = false;
        StringBuilder sbShoulder = new StringBuilder();
        StringBuilder sbSuffix = new StringBuilder();

        for (int i = 0; i < a.length(); i++) {
            char c = a.charAt(i);
            if (!reachedShoulder)
                sbShoulder.append(c);
            else
                sbSuffix.append(c);
            if (Character.isDigit(c))
                reachedShoulder = true;
        }
        shoulder = sbShoulder.toString();
        suffix = sbSuffix.toString();

        // String the slash between the shoulder and the suffix
        if (!divider.equals("") && suffix.startsWith(divider)) {
            suffix = suffix.replaceFirst(divider, "");
        }
    }

    public boolean hasSuffix() {
        return suffix != null && !suffix.isEmpty();
    }
}
