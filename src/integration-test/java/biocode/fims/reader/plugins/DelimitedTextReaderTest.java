package biocode.fims.reader.plugins;

import biocode.fims.projectConfig.models.Attribute;
import biocode.fims.projectConfig.models.Entity;
import biocode.fims.models.records.Record;
import biocode.fims.models.records.RecordSet;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.reader.DataReader;
import org.junit.Before;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author rjewing
 */
abstract class DelimitedTextReaderTest {
    protected ClassLoader classLoader;

    @Before
    public void setUp() throws Exception {
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
    }

    protected void verifySingleEntityMapping(DataReader reader) {
        List<RecordSet> recordSets = reader.getRecordSets(0, null);

        assertEquals(1, recordSets.size());

        for (RecordSet set: recordSets) {
            assertEquals("samples", set.conceptAlias());
            assertEquals(4, set.records().size());

            Record r = set.records().get(0);

            assertTrue(r.has("urn:materialSampleID"));
            assertTrue(r.has("urn:principalInvestigator"));
            assertTrue(r.has("urn:locality"));
            assertTrue(r.has("urn:decimalLatitude"));
            assertTrue(r.has("urn:decimalLongitude"));
            assertTrue(r.has("urn:ObservationDate"));
            assertTrue(r.has("urn:timeCollected"));
            assertTrue(r.has("urn:collectionTimestamp"));
            assertTrue(r.has("urn:elevation"));
            assertTrue(r.has("urn:hasLegs"));
        }
    }

    protected void verifyMultiEntityMapping(DataReader reader) {
        List<RecordSet> recordSets = reader.getRecordSets(0, null);

        assertEquals(2, recordSets.size());
        for (RecordSet set: recordSets) {
            assertEquals(4, set.records().size());

            Record r = set.records().get(0);
            switch (set.conceptAlias()) {
                case "samples":
                    assertTrue(r.has("urn:materialSampleID"));
                    assertTrue(r.has("urn:principalInvestigator"));
                    assertTrue(r.has("urn:decimalLatitude"));
                    assertTrue(r.has("urn:decimalLongitude"));
                    assertTrue(r.has("urn:timeCollected"));
                    assertTrue(r.has("urn:elevation"));
                    break;
                case "events":
                    assertTrue(r.has("urn:hasLegs"));
                    assertTrue(r.has("urn:collectionTimestamp"));
                    assertTrue(r.has("urn:ObservationDate"));
                    assertTrue(r.has("urn:locality"));
                    break;
                default:
                    fail("Should only contain \"samples\" and \"events\" RecordSets");
            }
        }
    }

    protected ProjectConfig getSingleEntityConfig() {
        ProjectConfig config = new ProjectConfig();

        Entity entity = new Entity("samples", "someURI");
        entity.setWorksheet("sheet1");
        config.addEntity(entity);

        Attribute a1 = new Attribute("materialSampleID", "urn:materialSampleID");
        Attribute a2 = new Attribute("principalInvestigator", "urn:principalInvestigator");
        Attribute a3 = new Attribute("locality", "urn:locality");
        Attribute a4 = new Attribute("phylum", "urn:phylum");
        Attribute a5 = new Attribute("Latitude", "urn:decimalLatitude");
        Attribute a6 = new Attribute("decimalLongitude", "urn:decimalLongitude");
        Attribute a7 = new Attribute("Observation_Date", "urn:ObservationDate");
        Attribute a8 = new Attribute("Time Collected", "urn:timeCollected");
        Attribute a9 = new Attribute("Collection TimeStamp", "urn:collectionTimestamp");
        Attribute a10 = new Attribute("Elevation", "urn:elevation");
        Attribute a11 = new Attribute("hasLegs", "urn:hasLegs");
        entity.addAttribute(a1);
        entity.addAttribute(a2);
        entity.addAttribute(a3);
        entity.addAttribute(a4);
        entity.addAttribute(a5);
        entity.addAttribute(a6);
        entity.addAttribute(a7);
        entity.addAttribute(a8);
        entity.addAttribute(a9);
        entity.addAttribute(a10);
        entity.addAttribute(a11);

        return config;
    }

    protected ProjectConfig getMultipleEntityConfig() {
        ProjectConfig config = new ProjectConfig();

        Entity entity1 = new Entity("samples", "someURI");
        entity1.setWorksheet("sheet1");
        config.addEntity(entity1);

        Entity entity2 = new Entity("events", "someURI");
        entity2.setWorksheet("sheet1");
        config.addEntity(entity2);

        Attribute a1 = new Attribute("materialSampleID", "urn:materialSampleID");
        Attribute a2 = new Attribute("principalInvestigator", "urn:principalInvestigator");
        Attribute a3 = new Attribute("Elevation", "urn:elevation");
        Attribute a4 = new Attribute("phylum", "urn:phylum");
        Attribute a5 = new Attribute("Latitude", "urn:decimalLatitude");
        Attribute a6 = new Attribute("decimalLongitude", "urn:decimalLongitude");
        Attribute a7 = new Attribute("Time Collected", "urn:timeCollected");
        entity1.addAttribute(a1);
        entity1.addAttribute(a2);
        entity1.addAttribute(a3);
        entity1.addAttribute(a4);
        entity1.addAttribute(a5);
        entity1.addAttribute(a6);
        entity1.addAttribute(a7);

        Attribute a8 = new Attribute("hasLegs", "urn:hasLegs");
        Attribute a9 = new Attribute("Collection TimeStamp", "urn:collectionTimestamp");
        Attribute a10 = new Attribute("Observation_Date", "urn:ObservationDate");
        Attribute a11 = new Attribute("locality", "urn:locality");
        entity2.addAttribute(a8);
        entity2.addAttribute(a9);
        entity2.addAttribute(a10);
        entity2.addAttribute(a11);

        return config;
    }
}
