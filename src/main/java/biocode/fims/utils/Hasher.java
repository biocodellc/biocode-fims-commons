package biocode.fims.utils;

import biocode.fims.fimsExceptions.FimsRuntimeException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by IntelliJ IDEA.
 * User: jdeck
 * Date: 1/30/14
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class Hasher {
    private static String algorithm = "MD5";

    /* MD5 creates a MD of 32 chars the following
     * string can be changed to SHA it will create
     * a MD of 40 chars
    */
    public static String hash(String password) {
        // Don't create a hash for empty content!
        if (password.trim().equals("")) return "";
        byte[] plainText = password.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        }
        catch (NoSuchAlgorithmException e) {
            throw new FimsRuntimeException(500, e);
        }
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
    }
}
