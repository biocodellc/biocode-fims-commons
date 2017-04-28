package biocode.fims.run;

import biocode.fims.application.config.FimsAppConfig;
import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.fimsExceptions.FimsAbstractException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.models.Project;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.rest.services.rest.FimsAbstractProjectsController;
import biocode.fims.service.ProjectService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.File;
import java.io.IOException;

/**
 * class to convert the Project configuration from xml files to storing them as json in the projects table
 *
 * @author RJ Ewing
 */
public class ProjectConfigConverter {
    private ProjectService projectService;

    public ProjectConfigConverter(ProjectService projectService) {
        this.projectService = projectService;
    }

    public void storeConfigs() throws IOException {

        for (Project p: projectService.getProjects()) {

            try {
                // TODO don't use cache for production
                File configFile = new ConfigurationFileFetcher(p.getProjectId(), System.getProperty("java.io.tmpdir"), true).getOutputFile();

                Mapping mapping = new Mapping();
                mapping.addMappingRules(configFile);

                Validation validation = new Validation();
                validation.addValidationRules(configFile, mapping);

                ProjectConfig tmpPC = new ProjectConfig(mapping, validation, mapping.getMetadata());

                // convert to and from json to compare values
                ObjectMapper mapper = new ObjectMapper();
                String pcString = mapper.writeValueAsString(tmpPC);

                ProjectConfig pc = mapper.readValue(pcString, ProjectConfig.class);

                // compare ProjectConfig before and after serialization to make sure nothing was lost
                if (!pc.getMetadata().equals(mapping.getMetadata())) {
                    throw new FimsRuntimeException("project metadata is different for projectId: " + p.getProjectId(), 500);
                }
                if (!pc.getMapping().equals(mapping)) {
                    throw new FimsRuntimeException("project mapping is different for projectId: " + p.getProjectId(), 500);
                }
                if (!pc.getValidation().equals(validation)) {
                    throw new FimsRuntimeException("project validation is different for projectId: " + p.getProjectId(), 500);
                }

                p.setProjectConfig(pc);
                projectService.update(p);
            } catch (FimsAbstractException e) {
                e.printStackTrace();
            }

        }

    }

    public static void main(String[] args) throws IOException {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(FimsAppConfig.class);
        ProjectService projectService = applicationContext.getBean(ProjectService.class);

        ProjectConfigConverter projectConfigConverter = new ProjectConfigConverter(projectService);
        projectConfigConverter.storeConfigs();
    }
}
