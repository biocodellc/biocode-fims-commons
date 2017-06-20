package biocode.fims.validation.rules;

import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Checks that characters in a string can become a portion of a valid URI
 * This is necessary for cases where data is constructed as a URI, such as uniqueKey values.
 * One approach is to encode all characters, however, this creates a mis-leading ARK identifier.
 * <p/>
 * Characters that are disallowed are: %$&+,/:;=?@<>#%\
 * <p/>
 * Note that this rule does not check if this a valid URI in its entirety, only that the portion of
 * the string, when appended onto other valid URI syntax, will not break the URI itself
 *
 * @author rjewing
 */
public class ValidForURIRule extends SingleColumnRule {
    private static final String NAME = "ValidForURI";
    private static final String GROUP_MESSAGE = "Non-valid URI characters";
//    private static final Pattern pattern = Pattern.compile("[^ %$&+,\\\\/:;=?@<>#%\\\\]+");
    private static final Pattern pattern = Pattern.compile("[a-zA-Z0-9+=:._()~*]+");

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private ValidForURIRule() {}

    public ValidForURIRule(String column, RuleLevel level) {
        super(column, level);
    }

    public ValidForURIRule(String column) {
        this(column, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);
        List<String> invalidValues = new ArrayList<>();

        for (Record r: recordSet.records()) {

            String value = r.get(uri);

            if (!pattern.matcher(value).matches()) {
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
        messages.addMessage(
                GROUP_MESSAGE,
                new Message(
                        "\"" + column + "\" contains some invalid URI characters: \"" + String.join("\", \"", invalidValues) + "\""
                ),
                level()
        );
    }

    @Override
    public String name() {
        return NAME;
    }
}
