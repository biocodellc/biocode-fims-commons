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

    public Identifier(String identifier) {
        this.identifier = identifier;

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
            // typically the shoulder is defined by the first digit, however the special testing
            // identifier ark:/99999/fk4, will define the should as the 2nd digit.
            if (Character.isDigit(c) && !(naan.equals("99999") && sbShoulder.toString().equals("fk4")))
                reachedShoulder = true;
        }
        shoulder = sbShoulder.toString();
        suffix = sbSuffix.toString();
    }

    public boolean hasSuffix() {
        return suffix != null && !suffix.isEmpty();
    }
}
