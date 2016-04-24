package biocode.fims.repository;

import biocode.fims.bcid.ManageEZID;
import biocode.fims.dao.ProjectDao;
import biocode.fims.entities.Bcid;
import biocode.fims.entities.Project;
import biocode.fims.ezid.EzidException;
import biocode.fims.ezid.EzidService;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.settings.SettingsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository class for {@link Bcid} domain objects
 */
@Repository
public class ProjectRepository {
    final static Logger logger = LoggerFactory.getLogger(ProjectRepository.class);

    private ProjectDao projectDao;

    @Autowired
    public ProjectRepository(ProjectDao projectDao) {
        this.projectDao = projectDao;
    }

    public void save(Project project) {
        if (project.isNew()) {
            projectDao.create(project);
        } else {
            projectDao.update(project);
        }

    }

//    /**
//     * @param identifier the identifier of the {@link Bcid} to fetch
//     * @return the {@link Bcid} with the provided identifier
//     */
//    public Bcid findByIdentifier(String identifier) {
//        Map<String, String> params = new HashMap<>();
//        params.put("identifier", identifier);
//        try {
//            return projectDao.findBcid(params);
//        } catch (EmptyResultDataAccessException e) {
//            throw new BadRequestException("Invalid Identifier");
//        }
//    }

}
