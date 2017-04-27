package biocode.fims.bcid;

import biocode.fims.digester.Mapping;
import biocode.fims.digester.Metadata;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import biocode.fims.settings.SettingsManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.URI;

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
    @Mock
    private ExpeditionService expeditionService;

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
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder("Resource")
                .build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(null).when(metadata).getExpeditionForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_default_conceptForwardingAddress_plus_suffix_for_concept_no_webAddress() throws Exception {
        Project project = new Project.ProjectBuilder("DEMO", "DEMO Project", "http://example.com", "http://example.com/").build();
        project.setId(1);
        Expedition expedition = new Expedition.ExpeditionBuilder("DEMO").build();
        expedition.setProject(project);
        Bcid bcid = new Bcid.BcidBuilder("Resource").build();
        bcid.setExpedition(expedition);


        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(WEBADRESS + "{ark}/test/{suffix}").when(mapping).getConceptForwardingAddress(IDENTIFIER);

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);
        Mockito.doNothing().when(expeditionService).setEntityIdentifiers(mapping, expedition.getExpeditionCode(), project.getId());

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

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
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(WEBADRESS + "{ark}").when(metadata).getExpeditionForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_expedition_no_expeditionForwardingAddress_no_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(null).when(metadata).getExpeditionForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_default_datasetForwardingAddress_plus_identifier_for_dataset_with_suffix_no_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(WEBADRESS + "{ark}").when(metadata).getDatasetForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_dataset_no_datasetForwardingAddress_no_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(null).when(metadata).getDatasetForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_webAddress_no_suffix_for_dataset_with_webAddress() throws Exception {
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE)
                .webAddress(new URI(WEBADRESS))
                .build();

        Mapping mapping = PowerMockito.spy(new Mapping());

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS), location);
    }
}
