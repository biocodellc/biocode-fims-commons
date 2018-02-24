package biocode.fims.validation.rules;

import biocode.fims.fimsExceptions.FimsRuntimeException;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.ClassUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author rjewing
 */
public class RuleTypeIdResolver implements TypeIdResolver {
    private static final Logger logger = LoggerFactory.getLogger(RuleTypeIdResolver.class);
    private static final String RULE_PACKAGE = Rule.class.getPackage().getName();

    private MetadataReaderFactory metadataReaderFactory;

    private static List<Rule> rules;
    private JavaType mBaseType;

    @Override
    public void init(JavaType baseType) {
        mBaseType = baseType;

        if (rules == null) {
            rules = initRuleClasses();
        }
    }

    private List<Rule> initRuleClasses() {
        List<Rule> rules = new ArrayList<>();

        try {
            PathMatchingResourcePatternResolver scanner = new PathMatchingResourcePatternResolver();
            metadataReaderFactory = new CachingMetadataReaderFactory(scanner);

            Resource[] resources = scanner.getResources("classpath*:" + RULE_PACKAGE.replaceAll("\\.", "/") + "/*.class");

            for (Resource r : resources) {
                Class resourceClass = getClassFromResource(r);

                if (resourceClass != null
                        && Rule.class.isAssignableFrom(resourceClass)
                        && !Modifier.isAbstract(resourceClass.getModifiers())) {

                    Constructor<Rule> constructor = resourceClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    rules.add(constructor.newInstance());
                }
            }

        } catch (IOException e) {
            // cant find RULE_PACKAGE
            throw new FimsRuntimeException("Can't find package: " + RULE_PACKAGE, 500, e);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            logger.error("Error Instantiating Rule implementation in package: " + RULE_PACKAGE, e);
        }

        return rules;
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        if (value instanceof Rule) {
            String name = suggestedType.getName();

            if (name.startsWith(RULE_PACKAGE)) {
                return ((Rule) value).name();
            }

            throw new IllegalStateException(suggestedType + " is not in the package " + RULE_PACKAGE);
        }

        throw new IllegalStateException(suggestedType + " does not implement the Rule interface");
    }

    @Override
    public String idFromBaseType() {
        throw new UnsupportedOperationException("must be a valid Rule implementation");
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {

        for (Rule r : rules) {
            if (r.name().equals(id)) {
                return TypeFactory.defaultInstance().constructSpecializedType(mBaseType, r.getClass());
            }
        }

        throw new IllegalStateException("Could not find Rule implementation with name: \"" + id + "\" in package: " + RULE_PACKAGE);
    }

    @Override
    public String getDescForKnownTypeIds() {
        return "Rule";
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CUSTOM;
    }

    private Class getClassFromResource(Resource resource) {
        try {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
            return Class.forName(metadataReader.getClassMetadata().getClassName());
        } catch (ClassNotFoundException | IOException e) {
            logger.debug("couldn't find Class for resource: " + resource.getFilename());
        }

        return null;
    }
}
