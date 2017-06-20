package biocode.fims.bcid;

import biocode.fims.digester.Entity;
import biocode.fims.models.Bcid;
import biocode.fims.models.EntityIdentifier;
import biocode.fims.models.Expedition;
import biocode.fims.models.Project;
import biocode.fims.projectConfig.ProjectConfig;
import biocode.fims.service.BcidService;
import biocode.fims.settings.SettingsManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;
import java.util.Collections;

/**
 * Tests for the Resolver class
 */
@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("biocode.fims.bcid.BcidDatabase")
@PrepareForTest(BcidDatabase.class)
public class ResolverTest {
    private String RESOLVER_METADATA_PREFIX_KEY = "resolverMetadataPrefix";
    private String DIVIDER_KEY = "divider";
    private String RESOLVER_METADATA_PREFIX_VALUE = "http://biscicol.org/id/metadata/";
    private String DIVIDER_VALUE = "_";
    private String WEBADRESS = "http://biscicol.org/";

    private String IDENTIFIER = "ark:/21999/L2";
    private String SUFFIX = "MBIO52";

    @InjectMocks
    private Resolver resolver;

    @Mock
    private BcidService bcidService;
    @Mock
    private SettingsManager settingsManager;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        Mockito.when(settingsManager.retrieveValue(RESOLVER_METADATA_PREFIX_KEY)).thenReturn(RESOLVER_METADATA_PREFIX_VALUE);
        Mockito.when(settingsManager.retrieveValue(DIVIDER_KEY)).thenReturn(DIVIDER_VALUE);

    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_with_null_webAddress() throws Exception {
        Bcid bcid = new Bcid.BcidBuilder("Resource")
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, null);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_concept_with_webAddress_no_suffix() throws Exception {
        Bcid bcid = new Bcid.BcidBuilder("Resource")
                .webAddress(new URI(WEBADRESS))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, null);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_concept_with_suffix_no_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.models.Bcid.BcidBuilder("Resource")
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, new ProjectConfig());

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER + SUFFIX), location);
    }

    @Test
    public void should_return_default_conceptForwardingAddress_plus_suffix_for_concept_no_webAddress() throws Exception {
        Project project = new Project.ProjectBuilder("DEMO", "DEMO Project", null, "http://example.com/").build();
        project.setProjectId(1);
        Expedition expedition = new Expedition.ExpeditionBuilder("DEMO").build();
        expedition.setEntityIdentifiers(Collections.singletonList(new EntityIdentifier("Resource", new URI(IDENTIFIER))));
        expedition.setProject(project);

        Bcid bcid = new Bcid.BcidBuilder("Resource").build();
        bcid.setExpedition(expedition);

        Entity entity = new Entity("Resource", "someURI");
        entity.setConceptForwardingAddress(WEBADRESS + "{ark}/test/{suffix}");
        ProjectConfig config = new ProjectConfig();
        config.addEntity(entity);

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, config);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER + "/test/" + SUFFIX), location);
    }

    @Test
    public void should_return_webAddress_plus_suffix_for_concept_with_webAddress_and_suffix() throws Exception {
        Bcid bcid = new Bcid.BcidBuilder("Resource")
                .webAddress(new URI(WEBADRESS))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, null);

        Assert.assertEquals(new URI(WEBADRESS + SUFFIX), location);
    }

    @Test
    public void should_return_webAddress_for_expedition_with_webAddress_and_suffix() throws Exception {
        Bcid bcid = new Bcid.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE)
                .webAddress(new URI(WEBADRESS))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, null);

        Assert.assertEquals(new URI(WEBADRESS), location);
    }

    @Test
    public void should_return_default_expeditionForwardingAddress_plus_identifier_for_expedition_with_suffix_no_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.models.Bcid.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE).build();

        ProjectConfig config = new ProjectConfig();
        config.setExpeditionForwardingAddress(WEBADRESS + "{ark}");

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, config);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_expedition_no_expeditionForwardingAddress_no_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.models.Bcid.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE).build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, new ProjectConfig());

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER + SUFFIX), location);
    }

    @Test
    public void should_return_default_datasetForwardingAddress_plus_identifier_for_dataset_with_suffix_no_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.models.Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE).build();

        ProjectConfig config = new ProjectConfig();
        config.setDatasetForwardingAddress(WEBADRESS + "{ark}");

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, config);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_dataset_no_datasetForwardingAddress_no_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.models.Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE).build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, new ProjectConfig());

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER + SUFFIX), location);
    }

    @Test
    public void should_return_webAddress_no_suffix_for_dataset_with_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.models.Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE)
                .webAddress(new URI(WEBADRESS))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, new ProjectConfig());

        Assert.assertEquals(new URI(WEBADRESS), location);
    }
}
