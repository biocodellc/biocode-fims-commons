package biocode.fims.validation.rules;

import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * checks to see if each value is a valid Url, with the schemes {"http", "https"}
 *
 * @author rjewing
 */
public class ValidURLRule extends SingleColumnRule {
    private static final String NAME = "ValidURL";
    private static final String GROUP_MESSAGE = "Invalid URL";

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private ValidURLRule() {}

    public ValidURLRule(String column, RuleLevel level) {
        super(column, level);
    }

    public ValidURLRule(String column) {
        this(column, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);

        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        List<String> invalidValues = new ArrayList<>();

        for (Record r: recordSet.records()) {

            String value = r.get(uri);

            if (!value.equals("") && !urlValidator.isValid(value)) {
                invalidValues.add(value);
            }

        }

        if (invalidValues.size() == 0) {
            return true;
        }

        setMessages(invalidValues, messages);
        setError();
        return false;
    }

    private void setMessages(List<String> invalidValues, EntityMessages messages) {
        for (String v: invalidValues) {
            messages.addMessage(
                    GROUP_MESSAGE,
                    new Message("\"" + v + "\" is not a valid URL for \"" + column + "\""),
                    level()
            );
        }
    }

    @Override
    public String name() {
        return NAME;
    }

}
