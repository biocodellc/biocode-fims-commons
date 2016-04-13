package biocode.fims.repository;

import biocode.fims.dao.ExpeditionDao;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Expedition;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.util.Collection;

/**
 * Repository class for {@link Expedition} domain objects
 */
@Repository
public class ExpeditionRepository {
    final static Logger logger = LoggerFactory.getLogger(BcidRepository.class);

    private ExpeditionDao expeditionDao;
    private BcidRepository bcidRepository;
    private SettingsManager settingsManager;

    @Autowired
    public ExpeditionRepository(ExpeditionDao expeditionDao, BcidRepository bcidRepository, SettingsManager settingsManager) {
        this.expeditionDao = expeditionDao;
        this.bcidRepository = bcidRepository;
        this.settingsManager = settingsManager;
    }

    public void create(Expedition expedition, URI webAddress) {
        if (!expedition.isNew())
            return;

        expeditionDao.create(expedition);
        Bcid bcid = createExpeditionBcid(expedition, webAddress);
        attachBcid(bcid, expedition);
        expedition.setBcid(bcid);
    }

    public void update(Expedition expedition) {
        expeditionDao.update(expedition);
    }

    public void attachBcid(Bcid bcid, Expedition expedition) {
        expeditionDao.attachBcid(bcid, expedition);
    }

    public Expedition findById(int id) {
        return find(
                new MapSqlParameterSource()
                    .addValue("expeditionId", id)
        );
    }

    public Expedition findByExpeditionCodeAndProjectId(String expeditionCode, int projectId) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("expeditionCode", expeditionCode)
                .addValue("projectId", projectId);

        return find(params);
    }

    private Bcid createExpeditionBcid(Expedition expedition, URI webAddress) {
        boolean ezidRequest = Boolean.parseBoolean(settingsManager.retrieveValue("ezidRequest"));

        Bcid expditionBcid = new Bcid.BcidBuilder(expedition.getUserId(), Expedition.EXPEDITION_RESOURCE_TYPE)
                .webAddress(webAddress)
                .title("Expedition: " + expedition.getExpeditionTitle())
                .ezidRequest(ezidRequest)
                .build();

        bcidRepository.save(expditionBcid);
        return expditionBcid;
    }

    private Expedition find(MapSqlParameterSource params) {
        Expedition expedition = expeditionDao.findExpedition(params);

        expedition.setBcid(
                bcidRepository.findByExpeditionAndResourceType(
                        expedition.getExpeditionId(), Expedition.EXPEDITION_RESOURCE_TYPE
                ).iterator().next()
        );

        return expedition;
    }

    public static void main(String[] args) {
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("/applicationContext.xml");
        ExpeditionRepository expeditionRepository = applicationContext.getBean(ExpeditionRepository.class);
//        BcidRepository bcidRepository = applicationContext.getBean(BcidRepository.class);

        Expedition expedition = new Expedition.ExpeditionBuilder("DEMOHIT", 8, 1).build();
        System.out.println(expedition);
//
        expeditionRepository.create(expedition, null);
        System.out.println(expedition);

        System.out.println("Finding expedition");
        System.out.println(expeditionRepository.findById(expedition.getExpeditionId()));
//        System.out.println(expeditionRepository.findById(310));

    }
}
