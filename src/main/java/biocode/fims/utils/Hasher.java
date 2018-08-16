package biocode.fims.utils;

import biocode.fims.fimsExceptions.FimsRuntimeException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hasher {
    private static String algorithm = "MD5";

    /* MD5 creates a MD of 32 chars the following
     * string can be changed to SHA it will create
     * a MD of 40 chars
    */
    public static String hash(String s) {
        // Don't create a hash for empty content!
        if (s.trim().equals("")) return "";
        byte[] plainText = s.getBytes();
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            md.reset();
            md.update(plainText);
            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                if ((digest[i] & 0xff) < 0x10) {
                    sb.append("0");
                }
                sb.append(Long.toString(digest[i] & 0xff, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new FimsRuntimeException(500, e);
        }
    }
}
