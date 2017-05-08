package biocode.fims.validation.rules;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.renderers.EntityMessages;
import org.junit.Before;

/**
 * @author rjewing
 */
abstract class AbstractRuleTest {
    protected EntityMessages messages;

    @Before
    public void setUpMessages() {
        this.messages = new EntityMessages("Samples");
    }

    protected Entity entity() {
        Entity entity = new Entity("Samples");

        Attribute a = new Attribute("col1", "urn:col1");
        Attribute a2 = new Attribute("col2", "urn:col2");
        entity.addAttribute(a);
        entity.addAttribute(a2);

        return entity;
    }
}
