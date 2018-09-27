package biocode.fims.config.network;

import biocode.fims.config.Config;
import biocode.fims.config.models.Entity;
import biocode.fims.config.models.NetworkEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * @author rjewing
 */
public class NetworkConfig extends Config {

    // contains all available network entities which projects can toggle on/off
    // contains network validation lists TODO maybe projects can extend?
    // minimal required expedition metadata which projects can extend


    @Override
    public void addEntity(Entity entity) {
        super.addEntity(new NetworkEntity(entity));
    }

    // jackson setter
    private void setEntities(List<Entity> entities) {
        entities.forEach(this::addEntity);
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
}

