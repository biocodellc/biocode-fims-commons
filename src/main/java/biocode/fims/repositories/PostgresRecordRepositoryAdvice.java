package biocode.fims.repositories;

import biocode.fims.rest.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;

/**
 * @author rjewing
 */
@Aspect
public class PostgresRecordRepositoryAdvice {
    @Autowired(required = false)
    UserContext userContext;
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Before("execution(* biocode.fims.repositories.PostgresRecordRepository.*(..))")
    public void setFimsUser() {
        String user = "anon";
        if (userContext != null && userContext.getUser() != null) {
            user = userContext.getUser().getUsername();
        }

        jdbcTemplate.execute("SET LOCAL \"fims.username\" = '" + user + "';", PreparedStatement::execute);
    }
}
