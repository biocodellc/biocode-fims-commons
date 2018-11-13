package biocode.fims.rest.services.subResources;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.authorizers.ProjectAuthorizer;
import biocode.fims.config.models.Attribute;
import biocode.fims.config.project.ColumnComparator;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.GenericErrorCode;
import biocode.fims.fimsExceptions.errorCodes.ProjectCode;
import biocode.fims.fimsExceptions.errorCodes.ProjectTemplateCode;
import biocode.fims.models.Project;
import biocode.fims.models.WorksheetTemplate;
import biocode.fims.query.writers.WriterWorksheet;
import biocode.fims.rest.responses.FileResponse;
import biocode.fims.rest.FimsController;
import biocode.fims.rest.filters.Authenticated;
import biocode.fims.run.ExcelWorkbookWriter;
import biocode.fims.serializers.Views;
import biocode.fims.service.ProjectService;
import biocode.fims.service.ProjectTemplateService;
import biocode.fims.tools.FileCache;
import com.fasterxml.jackson.annotation.JsonView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static biocode.fims.fimsExceptions.errorCodes.ProjectTemplateCode.UNKNOWN_COLUMN;

/**
 * @author RJ Ewing
 */
@Controller
@Produces(MediaType.APPLICATION_JSON)
public class ProjectTemplatesResource extends FimsController {

    private final ProjectTemplateService projectTemplateService;
    private final ProjectService projectService;
    private final ProjectAuthorizer projectAuthorizer;
    private final FileCache fileCache;

    @Autowired
    public ProjectTemplatesResource(ProjectService projectService, ProjectAuthorizer projectAuthorizer,
                                    FileCache fileCache, FimsProperties props, ProjectTemplateService projectTemplateService) {
        super(props);
        this.projectService = projectService;
        this.projectAuthorizer = projectAuthorizer;
        this.fileCache = fileCache;
        this.projectTemplateService = projectTemplateService;
    }

    /**
     * fetch all templates for a project
     *
     * @param projectId
     * @return
     */
    @JsonView(Views.Detailed.class)
    @GET
    public Set<WorksheetTemplate> getTemplates(@PathParam("projectId") Integer projectId) {

        Project project = projectService.getProjectWithTemplates(projectId);

        if (!projectAuthorizer.userHasAccess(userContext.getUser(), project)) {
            throw new FimsRuntimeException(ProjectCode.UNAUTHORIZED, 400);
        }

        Set<WorksheetTemplate> templates = project.getTemplates();

        for (WorksheetTemplate template : templates) {
            List<Attribute> attributes = project.getProjectConfig().attributesForSheet(template.getWorksheet());

            List<String> columns = new ArrayList<>();

            for (String col : template.getColumns()) {
                for (Attribute a : attributes) {
                    if (a.getUri().equals(col)) {
                        columns.add(a.getColumn());
                        break;
                    }
                }
            }

            template.setColumns(columns);
        }

        return templates;
    }

    /**
     * create a fims template generator configuration
     *
     * @param columns
     * @param worksheet
     * @param configName
     * @param projectId
     * @return
     */
    @Path("{configName}")
    @POST
    @Authenticated
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public WorksheetTemplate create(@FormParam("columns") List<String> columns,
                                    @FormParam("worksheet") String worksheet,
                                    @PathParam("configName") String configName,
                                    @PathParam("projectId") Integer projectId) {


        Project project = projectService.getProject(projectId);
        List<Attribute> attributes = project.getProjectConfig().attributesForSheet(worksheet);

        if (!projectAuthorizer.userHasAccess(userContext.getUser(), project)) {
            throw new FimsRuntimeException(ProjectCode.UNAUTHORIZED, 400, project.getProjectTitle());
        }

        List<String> uris = new ArrayList<>();

        for (String col : columns) {
            boolean found = false;
            for (Attribute a : attributes) {
                if (a.getColumn().equals(col)) {
                    uris.add(a.getUri());
                    found = true;
                    break;
                }
            }
            if (!found) throw new FimsRuntimeException(ProjectTemplateCode.UNKNOWN_COLUMN, 400, col, worksheet);
        }

        WorksheetTemplate worksheetTemplate = new WorksheetTemplate(configName, uris, worksheet, project, userContext.getUser());

        worksheetTemplate = projectTemplateService.save(worksheetTemplate);

        worksheetTemplate.setColumns(columns);
        return worksheetTemplate;
    }

    /**
     * update a template configuration
     *
     * @param columns
     * @param configName
     * @param projectId
     * @return
     */
    @Path("{configName}")
    @PUT
    @Authenticated
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public WorksheetTemplate update(@FormParam("columns") List<String> columns,
                                    @PathParam("configName") String configName,
                                    @PathParam("projectId") Integer projectId) {


        WorksheetTemplate template = projectTemplateService.get(configName, projectId);

        if (template == null) throw new NotFoundException();

        if (!template.getUser().equals(userContext.getUser())) {
            throw new FimsRuntimeException(GenericErrorCode.UNAUTHORIZED, 403);
        }

        Project project = projectService.getProject(projectId);
        List<Attribute> attributes = project.getProjectConfig().attributesForSheet(template.getWorksheet());


        List<String> uris = new ArrayList<>();

        for (String col : columns) {
            for (Attribute a : attributes) {
                if (a.getColumn().equals(col)) {
                    uris.add(a.getUri());
                    break;
                }
            }
        }

        template.setColumns(uris);

        return projectTemplateService.save(template);
    }

    /**
     * delete a template configuration
     *
     * @param configName
     * @param projectId
     */
    @Path("{configName}")
    @DELETE
    @Authenticated
    public void delete(@PathParam("configName") String configName,
                       @PathParam("projectId") Integer projectId) {
        WorksheetTemplate template = projectTemplateService.get(configName, projectId);

        if (template == null) throw new NotFoundException();

        if (!template.getUser().equals(userContext.getUser())) {
            throw new FimsRuntimeException(GenericErrorCode.UNAUTHORIZED, 403);
        }

        projectTemplateService.delete(configName, projectId);
    }


    /**
     * generate an excel template workbook
     *
     * @param worksheetTemplates list of {@link WorksheetTemplate}s to include in the workbook
     * @param projectId
     * @return
     */
    @Path("generate")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public FileResponse createExcel(List<WorksheetTemplate> worksheetTemplates,
                                    @PathParam("projectId") Integer projectId) {

        Project project = projectService.getProject(projectId);

        if (!projectAuthorizer.userHasAccess(userContext.getUser(), project)) {
            throw new FimsRuntimeException(GenericErrorCode.UNAUTHORIZED, 403);
        }

        for (WorksheetTemplate template : worksheetTemplates) {
            List<Attribute> attributes = project.getProjectConfig().attributesForSheet(template.getWorksheet());
            for (String col : template.getColumns()) {
                boolean found = false;
                for (Attribute a : attributes) {
                    if (a.getColumn().equals(col)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    throw new FimsRuntimeException(UNKNOWN_COLUMN, 400, col, template.getWorksheet());
                }
            }
        }


        // Create the template processor which handles all functions related to the template, reading, generation
        ExcelWorkbookWriter workbookWriter = new ExcelWorkbookWriter(project, props.naan(), userContext.getUser());

        File file = workbookWriter.write(
                worksheetTemplates.stream().map(t -> {
                    List<String> columns = t.getColumns();
                    columns.sort(new ColumnComparator(project.getProjectConfig(), t.getWorksheet()));

                    return new WriterWorksheet(t.getWorksheet(), columns);
                })
                        .collect(Collectors.toList())
        );

        // Catch a null file and return 204
        if (file == null) {
            return null;
        }

        String fileId = fileCache.cacheFileForUser(file, userContext.getUser());

        return new FileResponse(uriInfo.getBaseUriBuilder(), fileId);
    }
}
