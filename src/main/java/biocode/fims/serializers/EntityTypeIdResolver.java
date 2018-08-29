package biocode.fims.serializers;

import biocode.fims.config.models.Entity;
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
import java.util.Objects;

/**
 * Jackson TypIdResolver for polymorphic deserialization.
 * <p>
 * This will dynamically register all Entity implementation classes that exists in the same
 * package as the {@link Entity} class. This allows for packages to provide custom Entities
 * (ex. biocode-fims-sequences FastaEntity) and for those entities to be correctly serialized
 * & deserialized.
 *
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

            Resource[] resources = scanner.getResources("classpath*:" + ENTITY_PACKAGE.replaceAll("\\.", "/") + "/*.class");

            for (Resource r : resources) {
                Class resourceClass = getClassFromResource(r);

                if (resourceClass != null
                        && Entity.class.isAssignableFrom(resourceClass)
                        && !Modifier.isInterface(resourceClass.getModifiers())
                        && !Modifier.isAbstract(resourceClass.getModifiers())) {

                    try {
                        Constructor<Entity> constructor = resourceClass.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        entities.add(constructor.newInstance());
                    } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                        logger.error("Error Instantiating Entity implementation in package: " + ENTITY_PACKAGE, e);
                    }
                }
            }

        } catch (IOException e) {
            // cant find ENTITY_PACKAGE
            throw new FimsRuntimeException("Can't find package: " + ENTITY_PACKAGE, 500, e);
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

            String type = ((Entity) value).type();

            if (name.startsWith(ENTITY_PACKAGE)) {
                return type;
            }

            for (Entity e : entities) {
                if (Objects.equals(e.type(), type)) return type;
            }

            throw new IllegalStateException(suggestedType + " is not in the package " + ENTITY_PACKAGE);
        }

        throw new IllegalStateException(suggestedType + " is not an Entity implementation");
    }

    @Override
    public String idFromBaseType() {
        throw new UnsupportedOperationException("must be a valid Entity implementation");
    }

    @Override
    public JavaType typeFromId(DatabindContext context, String id) throws IOException {

        for (Entity e : entities) {
            if (Objects.equals(e.type(), id)) {
                return TypeFactory.defaultInstance().constructSpecializedType(mBaseType, e.getClass());
            }
        }

        throw new IllegalStateException("Could not find Entity implementation with type: \"" + id + "\" in package: " + ENTITY_PACKAGE);
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
