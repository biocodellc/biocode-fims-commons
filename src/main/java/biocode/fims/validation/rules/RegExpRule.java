package biocode.fims.validation.rules;

import biocode.fims.config.models.Entity;
import biocode.fims.records.Record;
import biocode.fims.records.RecordSet;
import biocode.fims.validation.messages.EntityMessages;
import biocode.fims.validation.messages.Message;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validate column values against the provided RegExp
 *
 * @author rjewing
 */
public class RegExpRule extends SingleColumnRule {
    private static final String NAME = "RegExp";
    private static final String GROUP_MESSAGE = "Value constraint did not pass";
    @JsonProperty
    private String pattern;
    @JsonProperty
    private boolean caseInsensitive;

    // needed for RuleTypeIdResolver to dynamically instantiate Rule implementation
    private RegExpRule() {
    }

    public RegExpRule(String column, String pattern, boolean caseInsensitive, RuleLevel level) {
        super(column, level);
        this.pattern = pattern;
        this.caseInsensitive = caseInsensitive;
    }

    public RegExpRule(String column, String pattern) {
        this(column, pattern, false, RuleLevel.WARNING);
    }

    @Override
    public boolean run(RecordSet recordSet, EntityMessages messages) {
        Assert.notNull(recordSet, "recordSet must not be null");

        if (!validConfiguration(recordSet, messages)) {
            return false;
        }

        String uri = recordSet.entity().getAttributeUri(column);
        Set<String> invalidValues = new LinkedHashSet<>();

        String p = this.pattern;
        if (!p.startsWith("^")) p = "^" + p;
        if (!p.endsWith("$")) p += "$";
        Pattern pattern = caseInsensitive ? Pattern.compile(p, Pattern.CASE_INSENSITIVE) : Pattern.compile(p);

        for (Record r : recordSet.recordsToPersist()) {

            String value = r.get(uri);

            if (value.equals("")) {
                continue;
            }

            if (!pattern.matcher(value).matches()) {
                invalidValues.add(value);
                if (level().equals(RuleLevel.ERROR)) r.setError();
            }
        }


        if (invalidValues.size() == 0) return true;

        setMessages(invalidValues, messages);
        setError();
        return false;
    }

    private void setMessages(Set<String> invalidValues, EntityMessages messages) {
        for (String value : invalidValues) {
            messages.addMessage(
                    GROUP_MESSAGE,
                    new Message("Value \"" + value + "\" in column \"" + column + "\" does not match the pattern \"" + pattern + "\""),
                    level()
            );
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public boolean validConfiguration(List<String> messages, Entity entity) {
        boolean valid = super.validConfiguration(messages, entity);

        if (StringUtils.isEmpty(pattern)) {
            messages.add("Invalid " + name() + " Rule configuration. pattern must not be blank or null.");

            return false;
        }

        return valid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RegExpRule)) return false;
        if (!super.equals(o)) return false;

        RegExpRule that = (RegExpRule) o;

        if (caseInsensitive != that.caseInsensitive) return false;
        return pattern != null ? pattern.equals(that.pattern) : that.pattern == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (pattern != null ? pattern.hashCode() : 0);
        result = 31 * result + (caseInsensitive ? 1 : 0);
        return result;
    }
}
