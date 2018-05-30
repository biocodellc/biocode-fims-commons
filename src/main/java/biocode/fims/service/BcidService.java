package biocode.fims.service;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.bcid.*;
import biocode.fims.models.*;
import biocode.fims.repositories.BcidRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BcidService {

    private final BcidRepository bcidRepository;
    protected final FimsProperties props;

    @Autowired
    public BcidService(BcidRepository bcidRepository, FimsProperties props) {
        this.bcidRepository = bcidRepository;
        this.props = props;
    }


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

}
