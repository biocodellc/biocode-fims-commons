package biocode.fims.validation.rules;

import biocode.fims.renderers.MessagesGroup;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;

/**
 * @author rjewing
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "name")
@JsonTypeIdResolver(RuleTypeIdResolver.class)
public interface Rule {

    String name();

    void setColumn(String column);

    void setLevel(RuleLevel level);

    RuleLevel level();

    MessagesGroup messages();
}
