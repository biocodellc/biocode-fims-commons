package biocode.fims.config.network;

import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.NetworkEntity;
import biocode.fims.validation.rules.RequiredValueRule;
import biocode.fims.validation.rules.RuleLevel;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author rjewing
 */
public class NetworkConfig extends Config {

    // contains all available network entities which projects can toggle on/off
    // contains network defined validation lists
    // minimal required expedition metadata which projects can extend


    @Override
    public void addEntity(Entity entity) {
        super.addEntity(new NetworkEntity(entity));
    }

    // jackson setter
    private void setEntities(List<Entity> entities) {
        entities.forEach(this::addEntity);
    }

    @Override
    public void addList(biocode.fims.config.models.List list) {
        super.addList(list);
        list.setNetworkList();
    }

    // jackson setter
    private void setLists(List<biocode.fims.config.models.List> lists) {
        lists.forEach(this::addList);
    }

    @JsonIgnore
    public boolean isValid() {
        NetworkConfigValidator validator = new NetworkConfigValidator(this);

        validated = true;
        if (!validator.isValid()) {
            this.errors = validator.errors();
            return false;
        }

        return true;
    }


    /**
     * Find the required columns for Entity
     *
     * @param e
     * @param level
     * @return
     */
    public Set<String> getRequiredColumnsForEntity(Entity e, RuleLevel level) {
        Set<String> columns = new HashSet<>();

        e.getRules().stream()
                .filter(r -> r.level().equals(level))
                .filter(r -> r.getClass().isAssignableFrom(RequiredValueRule.class))
                .forEach(r -> columns.addAll(((RequiredValueRule) r).columns()));

        return columns;
    }
}

