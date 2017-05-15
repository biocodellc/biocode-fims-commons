package biocode.fims.run;

/**
 * Tracks status of data validation.  Helpful especially in a stateless environment.
 * This class is meant to be read/written as an attribute for an HTTPSession when
 * working in a Servlet environment.
 */
public class ProcessorStatus {
    private StringBuilder statusSB = new StringBuilder();

    public void appendStatus(String s) {
        statusSB.append(stringToHTML(s));
    }

    public String statusHtml() {
        return stringToHTML(statusSB.toString());
    }

    private String stringToHTML(String s) {
        s = s.replaceAll("\n", "<br>").replaceAll("\t", " ");
        return s;
    }
}
