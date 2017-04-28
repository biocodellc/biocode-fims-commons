package biocode.fims.rest;

import javax.persistence.NamedEntityGraph;
import java.lang.annotation.*;

/**
 * Specify a entityGraph to fetch for the current logged in user
 * @author RJ Ewing
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UserEntityGraph {
    /**
     * the name of the {@link NamedEntityGraph} to fetch for the authenticated user {@link biocode.fims.models.User}
     * @return
     */
    String value();
}
