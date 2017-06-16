package biocode.fims.service;

import biocode.fims.digester.Attribute;
import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.repositories.TestProjectConfigRepository;
import biocode.fims.settings.SettingsManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.Assert.*;

/**
 * @author rjewing
 */
public class ProjectServiceTest {

    private TestProjectConfigRepository configRepository;
    private ProjectService projectService;

    @Before
    public void setUp() {
        // TODO refactor this so we don't need to use Mockito

        SettingsManager settingsManager = Mockito.mock(SettingsManager.class);
        Mockito.when(settingsManager.retrieveValue("ezidRequests")).thenReturn("false");

        ExpeditionService expeditionService = Mockito.mock(ExpeditionService.class);
        Mockito.when(expeditionService.getExpeditions(Mockito.anyInt(), Mockito.anyBoolean())).thenReturn(Collections.emptyList());

        configRepository = new TestProjectConfigRepository();
        projectService = new ProjectService(null, expeditionService, null, configRepository, settingsManager);
    }

    @Test
    public void saveConfig_should_throw_exception_for_invalid_config() {
        try {
            ProjectConfig config = new ProjectConfig();
            config.addEntity(new Entity());

            projectService.saveConfig(config, 1);
            fail();
        } catch (FimsRuntimeException e) {
            assertEquals(ConfigCode.INVALID, e.getErrorCode());
        }
    }

    @Test
    public void saveConfig_should_generate_attribute_uris_and_create_entity_tables_for_new_config() {
        ProjectConfig config = new ProjectConfig();
        Entity e = new Entity("entity1", "entity1URI");
        e.addAttribute(new Attribute("column1", ""));
        Entity e2 = new Entity("entity2", "entity2URI");
        e2.addAttribute(new Attribute("column1", ""));
        config.addEntity(e);
        config.addEntity(e2);

        projectService.saveConfig(config, 1);

        ProjectConfig savedConfig = configRepository.getConfig(1);

        assertEquals(config, savedConfig);

        assertNotSame("", config.entity("entity1").getAttribute("column1").getUri());
        assertNotSame("", config.entity("entity2").getAttribute("column1").getUri());

        assertEquals(config.entities(), configRepository.getEntitiesCreated(1));
    }

    @Test
    public void saveConfig_should_create_and_remove_entity_tables_for_updated_config() {
        Entity e = new Entity("entity1", "entity1URI");
        e.addAttribute(new Attribute("column1", ""));
        Entity e2 = new Entity("entity2", "entity2URI");
        e2.addAttribute(new Attribute("column1", ""));
        Entity e3 = new Entity("entity3", "entity3URI");
        e3.addAttribute(new Attribute("column1", ""));

        ProjectConfig config = new ProjectConfig();
        config.addEntity(e);
        config.addEntity(e2);

        // load repository with config so we can test updating
        configRepository.save(config, 1);

        // update the config now
        ProjectConfig updatedConfig = new ProjectConfig();
        updatedConfig.addEntity(e2);
        updatedConfig.addEntity(e3);

        projectService.saveConfig(updatedConfig, 1);

        ProjectConfig savedConfig = configRepository.getConfig(1);

        assertEquals(updatedConfig, savedConfig);

        assertEquals(Collections.singletonList(e), configRepository.getEntitiesRemoved(1));
        assertEquals(Collections.singletonList(e3), configRepository.getEntitiesCreated(1));
    }

}