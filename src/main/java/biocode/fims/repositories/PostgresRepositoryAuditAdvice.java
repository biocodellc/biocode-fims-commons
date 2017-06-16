package biocode.fims.repositories;

import biocode.fims.rest.UserContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.PreparedStatement;

/**
 * @author rjewing
 */
@Aspect
public class PostgresRepositoryAuditAdvice {
    @Autowired(required = false)
    private UserContext userContext;
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Before("@annotation(SetFimsUser)")
    public void setFimsUser() {
        String user = "anon";
        if (userContext != null && userContext.getUser() != null) {
            user = userContext.getUser().getUsername();
        }

        jdbcTemplate.execute("SET LOCAL \"fims.username\" = '" + user + "';", PreparedStatement::execute);
    }
}
