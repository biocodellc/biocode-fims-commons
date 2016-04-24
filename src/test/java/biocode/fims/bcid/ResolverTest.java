package biocode.fims.bcid;

import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.repository.BcidRepository;
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
    private BcidRepository bcidRepository;
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
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, "Resource")
                .webAddress(null)
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, null);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_with_empty_webAddress() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, "Resource")
                .webAddress(new URI(""))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, null);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_concept_with_webAddress_no_suffix() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new Bcid.BcidBuilder(8, "Resource")
                .webAddress(new URI(WEBADRESS))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, null);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_concept_with_suffix_no_webAddress() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, "Resource")
                .build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(null).when(mapping).getExpeditionForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_default_conceptForwardingAddress_plus_suffix_for_concept_no_webAddress() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new Bcid.BcidBuilder(8, "Resource").build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(WEBADRESS + "{ark}").when(mapping).getConceptForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER + "/" + SUFFIX), location);
    }

    @Test
    public void should_return_webAddress_plus_suffix_for_concept_with_webAddress_and_suffix() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new Bcid.BcidBuilder(8, "Resource")
                .webAddress(new URI(WEBADRESS))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, null);

        Assert.assertEquals(new URI(WEBADRESS + SUFFIX), location);
    }

    @Test
    public void should_return_webAdress_plus_identifier_for_expedition_with_webAddress_and_suffix() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new Bcid.BcidBuilder(8, Expedition.EXPEDITION_RESOURCE_TYPE)
                .webAddress(new URI(WEBADRESS))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, null);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER), location);
    }

    @Test
    public void should_return_default_expeditionForwardingAddress_plus_identifier_for_expedition_with_suffix_no_webAddress() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, Expedition.EXPEDITION_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(WEBADRESS + "{ark}").when(mapping).getExpeditionForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_expedition_no_expeditionForwardingAddress_no_webAddress() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, Expedition.EXPEDITION_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(null).when(mapping).getExpeditionForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_dataset_no_webAddress() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, ResourceTypes.DATASET_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_webAddress_no_suffix_for_dataset_with_webAddress() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, ResourceTypes.DATASET_RESOURCE_TYPE)
                .webAddress(new URI(WEBADRESS))
                .build();

        Mapping mapping = PowerMockito.spy(new Mapping());

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, mapping);

        Assert.assertEquals(new URI(WEBADRESS), location);
    }
}