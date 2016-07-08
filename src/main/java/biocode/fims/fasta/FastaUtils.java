package biocode.fims.fasta;

import biocode.fims.digester.Entity;
import biocode.fims.digester.Mapping;
import biocode.fims.fimsExceptions.ServerErrorException;

import java.util.ArrayList;

/**
 * Utility class to support working with Fasta files
 */
public class FastaUtils {
    public static Entity getEntityRoot(Mapping mapping, String uri) {
        ArrayList<Entity> entitiesWithAttribute = mapping.getEntititesWithAttributeUri(uri);
        if (entitiesWithAttribute.size() == 0) {
            return null;
        }

        // assuming that there is only 1 entity with a sequence attribute
        return entitiesWithAttribute.get(0);
    }
}
