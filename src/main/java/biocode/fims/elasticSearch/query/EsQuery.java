package biocode.fims.elasticSearch.query;

import biocode.fims.config.ConfigurationFileFetcher;
import biocode.fims.digester.Attribute;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Validation;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.query.QueryWriter;
import biocode.fims.run.TemplateProcessor;
import biocode.fims.settings.PathManager;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.elasticsearch.action.ActionRequestBuilder;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * class to query elasticsearch
 * TODO create a query interface for fims
 */
public class EsQuery {
    private static final Logger logger = LoggerFactory.getLogger(EsQuery.class);
    private static final TimeValue DEFAULT_TIME_VALUE = new TimeValue(1, TimeUnit.MINUTES);
    private static final String DEFAULT_SHEET = "Resources";

    private final Client client;
    private final ElasticSearchQuery query;

    public EsQuery(Client client, ElasticSearchQuery query) {
        this.client = client;
        this.query = query;
    }

    public String writeKml(List<Attribute> attributes, String outputDir) {
        SearchRequestBuilder searchRequestBuilder = getScrollableSearchRequestBuilder();

        List<JSONObject> resources = mapScrollableResults(searchRequestBuilder.get());

        QueryWriter queryWriter = getQueryWriter(attributes, resources, DEFAULT_SHEET);

        return queryWriter.writeKML(PathManager.createFile("output.kml", outputDir));
    }

    public String writeTab(List<Attribute> attributes, String outputDir) {
        SearchRequestBuilder searchRequestBuilder = getScrollableSearchRequestBuilder();

        List<JSONObject> resources = mapScrollableResults(searchRequestBuilder.get());

        QueryWriter queryWriter = getQueryWriter(attributes, resources, DEFAULT_SHEET);

        return queryWriter.writeTAB(PathManager.createFile("output.tsv", outputDir), true);
    }

    public String writeCsv(List<Attribute> attributes, String outputDir) {
        SearchRequestBuilder searchRequestBuilder = getScrollableSearchRequestBuilder();

        List<JSONObject> resources = mapScrollableResults(searchRequestBuilder.get());

        QueryWriter queryWriter = getQueryWriter(attributes, resources, DEFAULT_SHEET);

        return queryWriter.writeCSV(PathManager.createFile("output.csv", outputDir), true);
    }

    public String writeCSpace(List<Attribute> attributes, String outputDir) {
        if (query.getIndicies().length > 1) {
            throw new BadRequestException("You can only query 1 project at a time for cspace queries");
        }

        SearchRequestBuilder searchRequestBuilder = getScrollableSearchRequestBuilder();

        List<JSONObject> resources = mapScrollableResults(searchRequestBuilder.get());

        QueryWriter queryWriter = getQueryWriter(attributes, resources, DEFAULT_SHEET);

        File configFile = new ConfigurationFileFetcher(Integer.parseInt(query.getIndicies()[0]), outputDir, true).getOutputFile();

        Mapping mapping = new Mapping();
        mapping.addMappingRules(configFile);

        Validation validation = new Validation();
        validation.addValidationRules(configFile, mapping);

        return queryWriter.writeCSPACE(PathManager.createFile("output.cspace.xml", outputDir), validation);
    }

    public File writeExcel(List<Attribute> attributes, String outputDir) {
        int projectId = 0;
        SearchRequestBuilder searchRequestBuilder = getScrollableSearchRequestBuilder();

        List<JSONObject> resources = mapScrollableResults(getResponse(searchRequestBuilder));

        String sheetName = DEFAULT_SHEET;

        // if there is only 1 index, then we are only querying 1 project
        if (query.getIndicies().length == 1) {
            projectId = Integer.parseInt(query.getIndicies()[0]);
            File configFile = new ConfigurationFileFetcher(projectId, outputDir, true).getOutputFile();

            Mapping mapping = new Mapping();
            mapping.addMappingRules(configFile);

            sheetName = mapping.getDefaultSheetName();
        }

        QueryWriter queryWriter = getQueryWriter(attributes, resources, sheetName);

        String excelFile = queryWriter.writeExcel(PathManager.createFile("output.xlsx", outputDir));

        if (query.getIndicies().length == 1) {
            // Here we attach the other components of the excel sheet found with
            XSSFWorkbook justData = null;
            try {
                justData = new XSSFWorkbook(new FileInputStream(excelFile));
            } catch (IOException e) {
                logger.error("failed to open QueryWriter excelFile", e);
            }

            TemplateProcessor t = new TemplateProcessor(projectId, outputDir, justData);
            excelFile = t.createExcelFileFromExistingSources(sheetName, outputDir).getAbsolutePath();
        }

        return new File(excelFile);

    }

    public Page<JSONObject> getJSON() {
        SearchRequestBuilder searchRequestBuilder = getPageableSearchRequestBuilder();


        return mapPageableResults(getResponse(searchRequestBuilder));
    }

    private QueryWriter getQueryWriter(List<Attribute> attributes, List<JSONObject> resources, String sheetName) {
        QueryWriter queryWriter = new QueryWriter(attributes, sheetName);

        populateQueryWriter(queryWriter, resources);

        return queryWriter;
    }

    private void populateQueryWriter(QueryWriter queryWriter, List<JSONObject> resources) {
        int rowCnt = 0;

            for (JSONObject resource : resources) {
                Row row = queryWriter.createRow(rowCnt);

                Set<Map.Entry> set = resource.entrySet();
                for (Map.Entry entry: set) {
                    // TODO support arrays, and objects
                    // TODO currently passing in columnName instead of uri, querywriter will only detect dataType if passed a uri
                    // TODO write bcid last?
                    queryWriter.createCell(row, String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));

                }

                rowCnt++;
            }
    }

    private SearchResponse getResponse(ActionRequestBuilder searchRequestBuilder) {
        SearchResponse response = (SearchResponse) searchRequestBuilder.get();

        if (!(response.status() == RestStatus.OK || response.status() == RestStatus.NO_CONTENT)) {
            throw new ServerErrorException("Server Error fetching resources", response.toString());
        }

        return response;
    }

    private SearchRequestBuilder getPageableSearchRequestBuilder() {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder();

        searchRequestBuilder.setFrom(query.getPageable().getPageNumber() * query.getPageable().getPageSize());
        searchRequestBuilder.setSize(query.getPageable().getPageSize());

        return searchRequestBuilder;
    }

    private SearchRequestBuilder getScrollableSearchRequestBuilder() {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder();

        searchRequestBuilder.setScroll(DEFAULT_TIME_VALUE);
        searchRequestBuilder.setSize(1000);

        return searchRequestBuilder;
    }

    private SearchRequestBuilder getSearchRequestBuilder() {
        SearchRequestBuilder searchRequestBuilder = client.prepareSearch(query.getIndicies());

        searchRequestBuilder.setTypes(query.getTypes());

        searchRequestBuilder.setQuery(query.getQuery());
        searchRequestBuilder.setFetchSource(query.getSource(), null);

        return searchRequestBuilder;
    }

    private Page<JSONObject> mapPageableResults(SearchResponse response) {
        List<JSONObject> results = new ArrayList<>();
        long totalHits = response.getHits().getTotalHits();

        for (SearchHit hit : response.getHits()) {
            if (!StringUtils.isBlank(hit.getSourceAsString())) {
                results.add(new JSONObject(hit.getSource()));
            }
        }

        return new PageImpl<>(results, query.getPageable(), totalHits);
    }

    private List<JSONObject> mapScrollableResults(SearchResponse response) {
        List<JSONObject> resources = new ArrayList<>();

        do {
            for (SearchHit hit: response.getHits()) {
                resources.add(new JSONObject(hit.sourceAsMap()));
            }

            response = getResponse(client.prepareSearchScroll(response.getScrollId()).setScroll(DEFAULT_TIME_VALUE));
        } while (response.getHits().getHits().length != 0);

        return resources;
    }
}
