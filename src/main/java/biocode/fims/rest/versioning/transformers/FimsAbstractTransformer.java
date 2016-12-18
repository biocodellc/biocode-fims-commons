package biocode.fims.rest.versioning.transformers;

import biocode.fims.rest.versioning.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MultivaluedMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

/**
 * abstract class implementing {@link Transformer#updateRequestData(LinkedHashMap, String, MultivaluedMap)} and
 * {@link Transformer#updateResponseData(Object, String)} methods
 *
 * @author RJ Ewing
 */
public abstract class FimsAbstractTransformer implements Transformer {
    private final static Logger logger = LoggerFactory.getLogger(FimsAbstractTransformer.class);

    @Override
    public void updateRequestData(LinkedHashMap<String, Object> argMap, String methodName, MultivaluedMap<String, String> queryParameters) {
        try {
            Method transformMethod = this.getClass().getMethod(methodName + "Request", LinkedHashMap.class, MultivaluedMap.class);
            transformMethod.invoke(this, argMap, queryParameters);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            logger.debug("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response\n {}", e);
        } catch (InvocationTargetException e) {
            logger.info("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response\n {}", e);
        }
    }

    @Override
    public Object updateResponseData(Object returnVal, String methodName) {
        try {
            Method transformMethod = this.getClass().getMethod(methodName + "Response", Object.class);
            return transformMethod.invoke(this, returnVal);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            logger.debug("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response\n {}", e);
        } catch (InvocationTargetException e) {
            logger.info("Problem transforming response for class: " + this.getClass() + " and method: " + methodName + "Response\n {}", e);
        }
        return returnVal;
    }
}
