package biocode.fims.fasta;

/**
 * fasta sequence domain object
 */
public class FastaSequence {
    public final static String SEQUENCE_URI = "urn:sequence";
    private final String localIdentifier;
    private final String sequence;
    private String identifier;
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

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }
}
