package biocode.fims.run;

import biocode.fims.application.config.FimsAppConfig;
import biocode.fims.digester.Entity;
import biocode.fims.models.Project;
import biocode.fims.models.records.Record;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.repositories.RecordRepository;
import biocode.fims.service.ProjectService;
import biocode.fims.settings.FimsPrinter;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.IOException;

/**
 * @author rjewing
 */
public class FimsPostgresMigrator {
    private final static Logger logger = LoggerFactory.getLogger(FimsPostgresMigrator.class);

    private final ProjectService projectService;
    private final RecordRepository recordRepository;
    private final ProjectConfigConverter configConverter;

    FimsPostgresMigrator(ProjectService projectService, RecordRepository recordRepository, ProjectConfigConverter configConverter) {
        this.projectService = projectService;
        this.recordRepository = recordRepository;
        this.configConverter = configConverter;
    }

    void migrate() throws IOException {
//        configConverter.storeConfigs();

        // create entity tables for all the projects
        for (Project p : projectService.getProjects()) {

            ProjectConfig config = p.getProjectConfig();

            if (config != null) {

                recordRepository.createProjectSchema(p.getProjectId());

                for (Entity entity : config.getEntities()) {

                    if (entity.isChildEntity()) {

                        Entity parentEntity = config.getEntity(entity.getParentEntity());
                        String parentColumnUri = entity.getAttributeUri(parentEntity.getUniqueKey());
                        recordRepository.createChildEntityTable(p.getProjectId(), entity.getConceptAlias(), entity.getParentEntity(), parentColumnUri);

                    } else {

                        recordRepository.createEntityTable(p.getProjectId(), entity.getConceptAlias());

                    }

                }
            } else {
                logger.error("project id: " + p.getProjectId() + " project_config is null. Not creating schema and entity tables.");
            }

        }
    }

    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(FimsAppConfig.class);
        ProjectService projectService = applicationContext.getBean(ProjectService.class);
        RecordRepository repository = applicationContext.getBean(RecordRepository.class);

        ProjectConfigConverter projectConfigConverter = new ProjectConfigConverter(projectService);

        FimsPostgresMigrator migrator = new FimsPostgresMigrator(projectService, repository, projectConfigConverter);
        migrator.migrate();
    }
}
