package biocode.fims.serializers;

import biocode.fims.digester.Entity;
import biocode.fims.fimsExceptions.FimsRuntimeException;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.type.TypeFactory;
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
import java.util.List;

/**
 * @author rjewing
 */
public class EntityTypeIdResolver implements TypeIdResolver {
    private static final Logger logger = LoggerFactory.getLogger(EntityTypeIdResolver.class);
    private static final String ENTITY_PACKAGE = Entity.class.getPackage().getName();

    private MetadataReaderFactory metadataReaderFactory;

    private static List<Entity> entities;
    private JavaType mBaseType;

    @Override
    public void init(JavaType baseType) {
        mBaseType = baseType;

        if (entities == null) {
            entities = initEntityClasses();
        }
    }

    private List<Entity> initEntityClasses() {
        List<Entity> entities = new ArrayList<>();

        try {
            PathMatchingResourcePatternResolver scanner = new PathMatchingResourcePatternResolver();
            metadataReaderFactory = new CachingMetadataReaderFactory(scanner);

            Resource[] resources = scanner.getResources(ENTITY_PACKAGE.replaceAll("\\.", "/") + "/*.class");

            for (Resource r : resources) {
                Class resourceClass = getClassFromResource(r);

                if (resourceClass != null
                        && Entity.class.isAssignableFrom(resourceClass)
                        && !Modifier.isAbstract(resourceClass.getModifiers())) {

                    Constructor<Entity> constructor = resourceClass.getDeclaredConstructor();
                    constructor.setAccessible(true);
                    entities.add(constructor.newInstance());
                }
            }

        } catch (IOException e) {
            // cant find ENTITY_PACKAGE
            throw new FimsRuntimeException("Can't find package: " + ENTITY_PACKAGE, 500, e);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            logger.error("Error Instantiating Entity subclass in package: " + ENTITY_PACKAGE, e);
        }

        return entities;
    }

    @Override
    public String idFromValue(Object value) {
        return idFromValueAndType(value, value.getClass());
    }

    @Override
    public String idFromValueAndType(Object value, Class<?> suggestedType) {
        if (Entity.class.isAssignableFrom(suggestedType)) {
            String name = suggestedType.getName();

            if (name.startsWith(ENTITY_PACKAGE)) {
                return ((Entity) value).type();
            }

            throw new IllegalStateException(suggestedType + " is not in the package " + ENTITY_PACKAGE);
        }

        throw new IllegalStateException(suggestedType + " is not a subclass of the Entity class");
    }

    @Override
    public String idFromBaseType() {
        throw new UnsupportedOperationException("must be a valid Entity subclass");
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {

        for (Entity e: entities) {
            if (e.type().equals(id)) {
                return TypeFactory.defaultInstance().constructSpecializedType(mBaseType, e.getClass());
            }
        }

        throw new IllegalStateException("Could not find Entity subclass with type: \"" + id + "\" in package: " + ENTITY_PACKAGE);
    }

    @Override
    public String getDescForKnownTypeIds() {
        return "Entity";
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
