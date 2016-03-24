package biocode.fims.digester;

import biocode.fims.settings.FimsPrinter;

/**
 * Relation representation
 */
public class Relation {
    private String subject;
    private String predicate;
    private String object;

    public String getSubject() {
        return subject;
    }

    /**
     * Set Subject by looking up the subject from the mapping file.
     *
     * @param subject
     */
    public void addSubject(String subject) {
        //this.subject = findEntity(subject.getEntityId());
        this.subject = subject;
    }

    public void addPredicate(String predicate) {
        this.predicate = predicate;
    }

    public String getPredicate() {
        return predicate;
    }

    public String getObject() {
        return object;
    }

    public void addObject(String object) {
        //this.object = mapping.findEntity(object.getEntityId());
        this.object = object;
    }

    public void print() {
        FimsPrinter.out.println("  Relation:");
        FimsPrinter.out.println("    subject=" + subject.toString());
        FimsPrinter.out.println("    predicate=" + predicate.toString());
        FimsPrinter.out.println("    object=" + object.toString());
    }
}
