package biocode.fims.validation.rules;

import biocode.fims.models.records.RecordSet;
import biocode.fims.renderers.EntityMessages;
import biocode.fims.renderers.SimpleMessage;
import org.springframework.util.Assert;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
public class ValidForURIRule extends AbstractRule {
    private static final String NAME = "validForURI";
    private static final String GROUP_MESSAGE = "Non-valid URI characters";
    private static final Pattern pattern = Pattern.compile("[^ %$&+,\\\\/:;=?@<>#%\\\\]+");

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet);

        String uri = recordSet.entity().getAttributeUri(column);

        List<String> invalidValues = recordSet.records()
                .stream()
                .map(r -> r.get(uri))
                .filter(v -> !pattern.matcher(v).matches())
                .collect(Collectors.toList());

        if (invalidValues.size() == 0) {
            return true;
        }

        setMessages(invalidValues, messages);
        return false;
    }

    private void setMessages(List<String> invalidValues, EntityMessages messages) {
        messages.addMessage(
                GROUP_MESSAGE,
                new SimpleMessage(
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
