package biocode.fims.models.dataTypes.converters;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * @author rjewing
 */
public class DateAdaptor extends XmlAdapter<String, LocalDate> {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public String marshal(LocalDate date) throws Exception {
        return date.format(DTF);
    }

    @Override
    public LocalDate unmarshal(String string) throws Exception {
        return LocalDate.parse(string, DTF);
    }
}
