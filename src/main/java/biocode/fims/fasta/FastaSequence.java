package biocode.fims.fasta;

/**
 * fasta sequence domain object
 */
public class FastaSequence {
    private final String localIdentifier;
    private final String sequence;

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
}
