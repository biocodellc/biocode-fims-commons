package biocode.fims.fasta;

/**
 * fasta sequence domain object
 */
public class FastaSequence {
    public final static String SEQUENCE_URI = "urn:sequence";
    private final String localIdentifier;
    private final String sequence;
    private String rootIdentifier;
    private String organism;

    public FastaSequence(String localIdentifier, String sequence) {
        this.localIdentifier = localIdentifier;
        this.sequence = sequence;
    }

    public String getLocalIdentifier() {
        return localIdentifier;
    }

    public String getSequence() {
        return sequence;
    }

    public String getRootIdentifier() {
        return rootIdentifier;
    }

    public void setRootIdentifier(String rootIdentifier) {
        this.rootIdentifier = rootIdentifier;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }
}
