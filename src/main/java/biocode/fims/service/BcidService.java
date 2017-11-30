package biocode.fims.service;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.*;
import biocode.fims.entities.BcidTmp;
import biocode.fims.models.*;
import biocode.fims.repositories.BcidRepository;
import biocode.fims.repositories.BcidTmpRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BcidService {

    private final BcidRepository bcidRepository;
    protected final FimsProperties props;
    private final UserService userService;
    private BcidTmpRepository bcidTmpRepository;

    @Autowired
    public BcidService(BcidRepository bcidRepository, FimsProperties props,
                       UserService userService, BcidTmpRepository bcidTmpRepository) {
        this.bcidRepository = bcidRepository;
        this.props = props;
        this.userService = userService;
        this.bcidTmpRepository = bcidTmpRepository;
    }

//    @Transactional
//    public BcidTmp create(BcidTmp bcidTmp, User user) {
//
//        // if the user is demo, never create ezid's
//        if (bcidTmp.isEzidRequest() && user.getUsername().equals("demo"))
//            bcidTmp.setEzidRequest(false);
//
//        String creator = (StringUtils.isEmpty(props.creator())) ? user.getFullName() + " <" + user.getEmail() + ">" : props.creator();
//        Bcid bcid = Bcid.fromBcidTmp(bcidTmp, creator, props.publisher());
//
//        bcid = bcidRepository.create(bcid);
//
//        bcidTmp.setUser(user);
//        bcidTmp.setIdentifier(bcid.identifier());
//        bcidTmpRepository.save(bcidTmp);
//
//        return bcidTmp;
//
//    }

    @Transactional
    public Bcid create(Bcid bcid, User user) {

        // if the user is demo, never create ezid's
        if (bcid.ezidRequest() && user.getUsername().equals("demo"))
            bcid.setEzidRequest(false);

        return bcidRepository.create(bcid);
    }

    @Transactional(readOnly = true)
    public Bcid getBcid(String identifier) {
        return bcidRepository.get(identifier);
    }

    /**
     * Entity Bcids are all Bcids with resourceType != Dataset or Expedition resourceType
     * TODO delete this after running ProjectConfigMigrator script for postgres
     *
     * @param expeditionId
     * @return
     */
    @Deprecated
    @Transactional(readOnly = true)
    public List<BcidTmp> getEntityBcids(int expeditionId) {
        return bcidTmpRepository.findByExpeditionExpeditionIdAndResourceTypeNotIn(expeditionId,
                ResourceTypes.DATASET_RESOURCE_TYPE, Expedition.EXPEDITION_RESOURCE_TYPE);
    }
}
