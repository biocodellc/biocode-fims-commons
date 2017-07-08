package biocode.fims.bcid;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.digester.Mapping;
import biocode.fims.digester.Metadata;
import biocode.fims.entities.BcidTmp;
import biocode.fims.entities.Expedition;
import biocode.fims.entities.Project;
import biocode.fims.service.BcidService;
import biocode.fims.service.ExpeditionService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.mock.env.MockEnvironment;

import java.net.URI;

/**
 * Tests for the Resolver class
 */
@Ignore
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
    private FimsProperties props;
    @Mock
    private ExpeditionService expeditionService;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        MockEnvironment env = new MockEnvironment();
        env.setProperty(RESOLVER_METADATA_PREFIX_KEY, RESOLVER_METADATA_PREFIX_VALUE);
        env.setProperty(DIVIDER_KEY, DIVIDER_VALUE);
        this.props = new FimsProperties(env);

    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_with_null_webAddress() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder("Resource")
                .build();

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER, null);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_concept_with_webAddress_no_suffix() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder("Resource")
                .webAddress(new URI(WEBADRESS))
                .build();

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER, null);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_concept_with_suffix_no_webAddress() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder("Resource")
                .build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(null).when(metadata).getExpeditionForwardingAddress();

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER + SUFFIX), location);
    }

    @Test
    public void should_return_default_conceptForwardingAddress_plus_suffix_for_concept_no_webAddress() throws Exception {
        Project project = new Project.ProjectBuilder("DEMO", "DEMO Project", "http://example.com", "http://example.com/").build();
        project.setProjectId(1);
        Expedition expedition = new Expedition.ExpeditionBuilder("DEMO").build();
        expedition.setProject(project);
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder("Resource").build();
        bcidTmp.setExpedition(expedition);


        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(WEBADRESS + "{ark}/test/{suffix}").when(mapping).getConceptForwardingAddress(IDENTIFIER);

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);
        Mockito.doNothing().when(expeditionService).setEntityIdentifiers(mapping, expedition.getExpeditionCode(), project.getProjectId());

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER + "/test/" + SUFFIX), location);
    }

    @Test
    public void should_return_webAddress_plus_suffix_for_concept_with_webAddress_and_suffix() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder("Resource")
                .webAddress(new URI(WEBADRESS))
                .build();

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, null);

        Assert.assertEquals(new URI(WEBADRESS + SUFFIX), location);
    }

    @Test
    public void should_return_webAddress_for_expedition_with_webAddress_and_suffix() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE)
                .webAddress(new URI(WEBADRESS))
                .build();

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, null);

        Assert.assertEquals(new URI(WEBADRESS), location);
    }

    @Test
    public void should_return_default_expeditionForwardingAddress_plus_identifier_for_expedition_with_suffix_no_webAddress() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(WEBADRESS + "{ark}").when(metadata).getExpeditionForwardingAddress();

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_expedition_no_expeditionForwardingAddress_no_webAddress() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder(Expedition.EXPEDITION_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(null).when(metadata).getExpeditionForwardingAddress();

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER + SUFFIX), location);
    }

    @Test
    public void should_return_default_datasetForwardingAddress_plus_identifier_for_dataset_with_suffix_no_webAddress() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(WEBADRESS + "{ark}").when(metadata).getDatasetForwardingAddress();

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_dataset_no_datasetForwardingAddress_no_webAddress() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        Metadata metadata = PowerMockito.spy(new Metadata());
        mapping.addMetadata(metadata);
        PowerMockito.doReturn(null).when(metadata).getDatasetForwardingAddress();

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER + SUFFIX), location);
    }

    @Test
    public void should_return_webAddress_no_suffix_for_dataset_with_webAddress() throws Exception {
        BcidTmp bcidTmp = new BcidTmp.BcidBuilder(ResourceTypes.DATASET_RESOURCE_TYPE)
                .webAddress(new URI(WEBADRESS))
                .build();

        Mapping mapping = PowerMockito.spy(new Mapping());

        bcidTmp.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidService.getBcid(IDENTIFIER)).thenReturn(bcidTmp);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS), location);
    }
}
