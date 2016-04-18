package biocode.fims.bcid;

import biocode.fims.digester.Mapping;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.repository.BcidRepository;
import biocode.fims.settings.SettingsManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
    public void should_return_resolverMetadataPrefix_plus_identifier() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, "Resource").build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, null);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_bcid_webAddress_with_no_suffix() throws Exception {
        String customWebAddress = "http://biscicol.org/customWebAddress/";

        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, "Resource")
                .webAddress(new URI(customWebAddress))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, null);

        Assert.assertEquals(new URI(customWebAddress), location);
    }

    @Test
    public void should_return_bcid_webAddress_with_suffix() throws Exception {
        String customWebAddress = "http://biscicol.org/customWebAddress/";

        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, "Resource")
                .webAddress(new URI(customWebAddress))
                .build();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER + SUFFIX, null);

        Assert.assertEquals(new URI(customWebAddress + SUFFIX), location);
    }

    @Test
    public void should_return_default_expedition_forward_address() throws Exception {
        String defaultExpeditionWebAddress = "http://biscicol.org/defaultExpeditionWebAddress";

        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, Expedition.EXPEDITION_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(defaultExpeditionWebAddress).when(mapping).getExpeditionForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, mapping);

        Assert.assertEquals(new URI(defaultExpeditionWebAddress), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_expedition_bcid() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, Expedition.EXPEDITION_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(null).when(mapping).getExpeditionForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }

    @Test
    public void should_return_default_dataset_forwarding_address() throws Exception {
        String defaultDatasetWebAddress = "http://biscicol.org/defaultDatasetWebAddress";

        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, ResourceTypes.DATASET_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(defaultDatasetWebAddress).when(mapping).getConceptForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, mapping);

        Assert.assertEquals(new URI(defaultDatasetWebAddress), location);
    }

    @Test
    public void should_return_resolverMetadataPrefix_plus_identifier_for_dataset_bcid() throws Exception {
        PowerMockito.mockStatic(BcidDatabase.class);
        PowerMockito.when(BcidDatabase.getUserName(8)).thenReturn("demo");
        Bcid bcid = new biocode.fims.entities.Bcid.BcidBuilder(8, ResourceTypes.DATASET_RESOURCE_TYPE).build();

        Mapping mapping = PowerMockito.spy(new Mapping());
        PowerMockito.doReturn(null).when(mapping).getExpeditionForwardingAddress();

        bcid.setIdentifier(new URI(IDENTIFIER));
        Mockito.when(bcidRepository.findByIdentifier(IDENTIFIER)).thenReturn(bcid);

        URI location = resolver.resolveIdentifier(IDENTIFIER, mapping);

        Assert.assertEquals(new URI(RESOLVER_METADATA_PREFIX_VALUE + IDENTIFIER), location);
    }
}