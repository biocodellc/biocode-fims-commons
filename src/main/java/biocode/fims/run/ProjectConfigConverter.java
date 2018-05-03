package biocode.fims.run;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.*;
import biocode.fims.digester.Rule;
import biocode.fims.fimsExceptions.FimsAbstractException;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import biocode.fims.fimsExceptions.errorCodes.ConfigCode;
import biocode.fims.models.Project;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.service.ProjectService;
import biocode.fims.validation.rules.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;

/**
 * class to convert the Project configuration from xml files to storing them as json in the projects table
 *
 * @author RJ Ewing
 */
public class ProjectConfigConverter {
    private final static Logger logger = LoggerFactory.getLogger(ProjectConfigConverter.class);

    private ProjectService projectService;
    private final String projectUrl;

    public ProjectConfigConverter(ProjectService projectService, String projectUrl) {
        this.projectService = projectService;
        this.projectUrl = projectUrl;
    }

    public void storeConfigs() throws IOException {

        for (Project p : projectService.getProjects(projectUrl)) {

            try {
                logger.info("\nStoring project config: " + p.getProjectId());
                File configFile = new ConfigurationFileFetcher(p, System.getProperty("java.io.tmpdir"), false).getOutputFile();

                Mapping mapping = new Mapping();
                mapping.addMappingRules(configFile);

                Validation validation = new Validation();
                validation.addValidationRules(configFile, mapping);

                ProjectConfig config = new ProjectConfig();

                validation.getLists().forEach(config::addList);
                mapping.getEntities().forEach(config::addEntity);
                config.setDatasetForwardingAddress(mapping.getMetadata().getDatasetForwardingAddress());
                config.setExpeditionForwardingAddress(mapping.getMetadata().getDatasetForwardingAddress());
                p.setDescription(mapping.getMetadata().getTextAbstract());

                mapping.getRelations().forEach(r -> {
                    Entity childEntity = config.entity(r.getSubject());
                    childEntity.setParentEntity(r.getObject());
                });

                for (Worksheet w : validation.getWorksheets()) {

                    java.util.List<Entity> entitiesForSheet = config.entitiesForSheet(w.getSheetname());

                    for (Rule r : w.getRules()) {

                        RuleLevel level;
                        if (r.getLevel().equalsIgnoreCase("error")) {
                            level = RuleLevel.ERROR;
                        } else if (r.getLevel().equalsIgnoreCase("warning")) {
                            level = RuleLevel.WARNING;
                        } else {
                            throw new IllegalStateException("unknown rule level: " + r.getLevel() + " for project: " + p.getProjectId());
                        }

                        String column = r.getColumn();

                        switch (r.getType()) {
                            case "BoundingBox":
                                // Parse the BOX3D well known text box
                                String field = r.getFields().get(0);
                                field = field.replace("BOX3D(", "").replace(")", "");
                                field = field.replace("BOX3D (", "");
                                String[] points = field.split(",");
                                try {
                                    double minLat = Double.parseDouble(points[0].split(" ")[0]);
                                    double maxLat = Double.parseDouble(points[1].split(" ")[0]);
                                    double minLng = Double.parseDouble(points[0].split(" ")[1]);
                                    double maxLng = Double.parseDouble(points[1].split(" ")[1]);

                                    String latRange = ">" + minLat + "|<" + maxLat;
                                    String lngRange = ">" + minLng + "|<" + maxLng;

                                    NumericRangeRule rule1 = new NumericRangeRule(r.getDecimalLatitude(), latRange, level);
                                    NumericRangeRule rule2 = new NumericRangeRule(r.getDecimalLongitude(), lngRange, level);

                                    entitiesForSheet.forEach(e -> {
                                        e.addRule(rule1);
                                        e.addRule(rule2);
                                    });
                                } catch (NumberFormatException e) {
                                    System.out.println("ProjectId: " + p.getProjectId() + " " + e.getMessage());
                                }
                                break;
                            case "RequiredColumn":
                                RequiredValueRule requiredValueRule = new RequiredValueRule(new LinkedHashSet<>(Collections.singletonList(column)), level);
                                entitiesForSheet.forEach(e -> e.addRule(requiredValueRule));
                                break;
                            case "RequiredColumns":
                                RequiredValueRule requiredValueRule1 = new RequiredValueRule(new LinkedHashSet<>(r.getFields()), level);
                                entitiesForSheet.forEach(e -> e.addRule(requiredValueRule1));
                                break;
                            case "checkInXMLFields":
                            case "controlledVocabulary":
                                ControlledVocabularyRule controlledVocabularyRule = new ControlledVocabularyRule(column, r.getList(), config, level);
                                entitiesForSheet.forEach(e -> e.addRule(controlledVocabularyRule));
                                break;
                            case "duplicateColumnNames":
                                // no longer needed
                                break;
                            case "isNumber":
                                for (Entity entity : entitiesForSheet) {
                                    Attribute a = entity.getAttribute(column);

                                    DataType dt = a.getDatatype();
                                    if (!dt.equals(DataType.INTEGER) && !dt.equals(DataType.FLOAT)) {

                                        if (!dt.equals(DataType.STRING)) {
                                            throw new IllegalStateException("isNumber rule set for non-numeric dataType attribute: " + column + " in projectId: " + p.getProjectId());
                                        }

                                        a.setDatatype(DataType.FLOAT);
                                    }
                                }
                                break;
                            case "isValidUrl":
                                ValidURLRule validURLRule = new ValidURLRule(column, level);
                                entitiesForSheet.forEach(e -> e.addRule(validURLRule));
                                break;
                            case "latLngChecker":
                                String lat = ">=-90|<=90";
                                String lng = ">=-180|<=-180";

                                NumericRangeRule numericRangeRule = new NumericRangeRule(r.getDecimalLatitude(), lat, level);
                                NumericRangeRule numericRangeRule1 = new NumericRangeRule(r.getDecimalLongitude(), lng, level);

                                entitiesForSheet.forEach(e -> {
                                    e.addRule(numericRangeRule);
                                    e.addRule(numericRangeRule1);
                                });
                                break;
                            case "minimumMaximumNumberCheck":
                                String minimum = column.split(",")[0];
                                String maximum = column.split(",")[1];
                                MinMaxNumberRule minMaxNumberRule = new MinMaxNumberRule(minimum, maximum, level);
                                entitiesForSheet.forEach(e -> e.addRule(minMaxNumberRule));
                                break;
                            case "requiredColumnInGroup":
                                RequiredValueInGroupRule requiredValueInGroupRule = new RequiredValueInGroupRule(new LinkedHashSet<>(r.getFields()), level);
                                entitiesForSheet.forEach(e -> e.addRule(requiredValueInGroupRule));
                                break;
                            case "requiredValueFromOtherColumn":
                                RequireValueIfOtherColumnRule requireValueIfOtherColumnRule = new RequireValueIfOtherColumnRule(column, r.getOtherColumn(), level);
                                entitiesForSheet.forEach(e -> e.addRule(requireValueIfOtherColumnRule));
                                break;
                            case "uniqueValue":
                                UniqueValueRule uniqueValueRule = new UniqueValueRule(column, level);
                                entitiesForSheet.forEach(e -> e.addRule(uniqueValueRule));
                                break;
                            case "validForURI":
                                ValidForURIRule validForURIRule = new ValidForURIRule(column, level);
                                entitiesForSheet.forEach(e -> e.addRule(validForURIRule));
                                break;
                            case "validateNumeric":
                                String[] values = r.getValue().split("=|and"); // need to check this
                                String range = URLDecoder.decode(String.join("|", values));

                                NumericRangeRule numericRangeRule2 = new NumericRangeRule(column, range, level);
                                entitiesForSheet.forEach(e -> e.addRule(numericRangeRule2));
                                break;
                            case "validDataTypeFormat":
                                // this is a default rule now
                            case "datasetContainsExtraColumns":
                                // no longer needed
                                break;
                            case "compositeUniqueValue":
                                // no usages?
                            case "checkTissueColumnsSI":
                            case "checkTissueColumns":
                            case "checkLowestTaxonLevel":
                            case "checkVoucherSI":
                            case "DwCLatLngChecker":
                            default:
                                throw new IllegalStateException("un-mapped rule: " + r.getType() + " in project: " + p.getProjectId());
                        }
                    }

                }

                try {
                    projectService.createProjectSchema(p.getProjectId());
                    projectService.saveConfig(config, p.getProjectId(), true);
                } catch (FimsRuntimeException e) {
                    if (e.getErrorCode().equals(ConfigCode.INVALID)) {
                        logger.error("\n\nInvalid project config: " + p.getProjectId());
                        for (String s : config.errors()) {
                            logger.error(s);
                        }
                    } else {
                        throw e;
                    }
                }
            } catch (FimsAbstractException e) {
                logger.error("\n\nInvalid project config: " + p.getProjectId());
                e.printStackTrace();
            }

        }

    }
}
