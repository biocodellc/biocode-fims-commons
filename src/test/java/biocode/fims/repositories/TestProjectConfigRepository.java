package biocode.fims.repositories;

import biocode.fims.digester.Entity;
import biocode.fims.projectConfig.ProjectConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author rjewing
 */
public class TestProjectConfigRepository implements ProjectConfigRepository {
    private Map<Integer, ProjectConfig> configMap;
    private Map<Integer, List<Entity>> entitiesCreatedMap;
    private Map<Integer, List<Entity>> entitiesRemovedMap;

    public TestProjectConfigRepository() {
        this.configMap = new HashMap<>();
        this.entitiesCreatedMap = new HashMap<>();
        this.entitiesRemovedMap = new HashMap<>();
    }

    @Override
    public void save(ProjectConfig config, int projectId) {
        configMap.put(projectId, config);
    }

    @Override
    public void createProjectSchema(int projectId) {

    }

    @Override
    public ProjectConfig getConfig(int projectId) {
        return configMap.get(projectId);
    }

    @Override
    public void createEntityTables(List<Entity> entities, int projectId, ProjectConfig config) {
        entitiesCreatedMap.put(projectId, entities);
    }

    @Override
    public void removeEntityTables(List<Entity> entities, int projectId) {
        entitiesRemovedMap.put(projectId, entities);
    }

    public List<Entity> getEntitiesCreated(int projectId) {
        return entitiesCreatedMap.get(projectId);
    }

    public List<Entity> getEntitiesRemoved(int projectId) {
        return entitiesRemovedMap.get(projectId);
    }

    public void put(ProjectConfig config, int projectId) {
        configMap.put(projectId, config);
    }
}
