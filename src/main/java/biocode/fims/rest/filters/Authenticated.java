package biocode.fims.rest.filters;

import javax.ws.rs.NameBinding;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Custom annotation for checking if a user is authenticated
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
public @interface Authenticated{}
