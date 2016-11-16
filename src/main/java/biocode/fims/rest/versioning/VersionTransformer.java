package biocode.fims.rest.versioning;

import biocode.fims.rest.FimsService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Spring aop Advice class to wrap all jersey Resource methods in the biocode.fims.rest.services package
 * to allow transformation of the request parameters and response for different api versions.
 * The resource methods should always deal with code in the latest api version. This class will transform
 * the request and response through a pipeline for the requested api version.
 */
@Aspect
public class VersionTransformer {
    private final static Logger logger = LoggerFactory.getLogger(VersionTransformer.class);
    private final static String TRANSFORMER_PACKAGE = "biocode.fims.rest.versioning.transformers";

    @Around("execution(* biocode.fims.rest.services..*.*(..))")
    public Object transformResource(ProceedingJoinPoint jp) throws Throwable {
        FimsService fimsService = (FimsService) jp.getTarget();

        // Set the default version, if not set by getHeaders()
        String version = APIVersion.DEFAULT_VERSION;
        if (fimsService.getHeaders() != null) {
            version = fimsService.getHeaders().getHeaderString("Api-Version");
        }
        Object returnValue;

        Object[] args = jp.getArgs();

        if (args.length == 0) {
            returnValue = jp.proceed();
        } else {
            HashMap<String, Object> argMap = new LinkedHashMap<>();
            String[] argNames = ((MethodSignature) jp.getSignature()).getParameterNames();
            for (int i = 0; i < args.length; i++) {
                argMap.put(argNames[i], args[i]);
            }

            for (APIVersion apiVersion : APIVersion.range(version)) {
                // TODO implement a request transformer
            }

            returnValue = jp.proceed(argMap.values().toArray());
        }

        if (returnValue != null) {
            List<APIVersion> versionRangeReversed = APIVersion.range(version);
            Collections.reverse(versionRangeReversed);
            // remove the latest APIVersion as that is what the returnVal is from the resource Method
            versionRangeReversed.remove(0);

            for (APIVersion apiVersion : versionRangeReversed) {
                Transformer transformer = getTransformer(apiVersion, jp.getTarget().getClass().getSimpleName());
                returnValue = transformResponse(returnValue, transformer, jp.getSignature().getName());
            }
        }

        return returnValue;
    }

    private void transformResource(LinkedHashMap<String, Object> argMap, APIVersion version, String classShortName) {

    }

    private Object transformResponse(Object returnVal, Transformer transformer, String methodName) {
        if (transformer != null) {
            Object obj = transformer.updateResponseData(returnVal, methodName);
            return obj;
        }

        return returnVal;
    }

    /**
     * lookup the {@link Transformer} implementation for the specific class and {@link APIVersion}.transformerSuffix.
     * we look for a class in the {@link VersionTransformer}.TRANSFORMER_PACKAGE with the following naming strategy:
     * {JerseyResourceClassSimpleName}Transformer{APIVersion.transformerSuffix}
     *
     * @param version
     * @param classSimpleName
     * @return
     */
    private Transformer getTransformer(APIVersion version, String classSimpleName) {
        String transformerClass = TRANSFORMER_PACKAGE + "." + classSimpleName + "Transformer" + version.getTransformerSuffix();
        Transformer transformer = null;

        try {
            transformer = (Transformer) Class.forName(transformerClass).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            logger.debug("Problem instentiating transformer class: " + transformerClass);
        }

        return transformer;
    }
}