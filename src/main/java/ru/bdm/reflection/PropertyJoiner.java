package ru.bdm.reflection;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.Factory;
import net.sf.cglib.proxy.InterfaceMaker;
import net.sf.cglib.proxy.InvocationHandler;
import org.objectweb.asm.Type;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.cache.CacheBuilder.newBuilder;
import static com.google.common.collect.ImmutableSet.copyOf;
import static org.objectweb.asm.Type.getType;
import static ru.bdm.reflection.Util.getGetterName;
import static ru.bdm.reflection.Util.getPropertyName;
import static ru.bdm.reflection.Util.upperFirst;

/**
 * User: D.Brusentsov
 * Date: 20.06.13
 * Time: 16:17
 */
public class PropertyJoiner {

    private static final Type OBJECT_TYPE = getType(Object.class);
    private static final Type[] EMPTY_TYPES = new Type[0];
    private static final LoadingCache<String, Class> PROPERTY_HOLDER_INTERFACE_CACHE = newBuilder()
            .build(new CacheLoader<String, Class>() {
                @Override
                public Class load(String propertyName) {
                    return createPropertyHolderInterface(propertyName);
                }
            });
    private static final LoadingCache<ProxyClassKey, Class> PROXY_CLASSES_CACHE = newBuilder()
            .build(new CacheLoader<ProxyClassKey, Class>() {
                @Override
                public Class load(ProxyClassKey classHolderTypesKey) throws Exception {
                    return createProxyClass(classHolderTypesKey);
                }
            });
    private final PropertyExtractor extractor;
    private final Set<String> properties;

    public PropertyJoiner(final @Nonnull PropertyExtractor extractor, final @Nonnull String... properties) {
        this.extractor = extractor;
        this.properties = copyOf(properties);
    }

    public PropertyJoiner(final @Nonnull PropertyExtractor extractor, final @Nonnull Collection<String> properties) {
        this(extractor, properties.toArray(new String[]{}));
    }

    public static <T> T joinProperties(final @Nonnull T t, final @Nonnull Map<String, ?> map) {
        return new PropertyJoiner((o, property) -> map.get(property), map.keySet()).joinProperties(t);
    }

    private static Class createProxyClass(ProxyClassKey classHolderTypesKey) throws ExecutionException {
        final Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(PropertyJoiner.class.getClassLoader());
        enhancer.setInterfaces(getPropertyHolderInterfaces(classHolderTypesKey.names));
        enhancer.setSuperclass(classHolderTypesKey.clazz);
        enhancer.setCallbackType(InvocationHandler.class);
        enhancer.setCallbackFilter(method -> {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        });
        return enhancer.createClass();
    }

    private static Class[] getPropertyHolderInterfaces(final Set<String> properties) throws ExecutionException {
        final Class[] result = new Class[properties.size()];
        int i = 0;
        for (final String property : properties) {
            result[i++] = PROPERTY_HOLDER_INTERFACE_CACHE.get(property);
        }
        return result;
    }

    private static Class<?> createPropertyHolderInterface(final String propertyName) {
        Signature signature = new Signature(getGetterName(propertyName, Object.class), OBJECT_TYPE, EMPTY_TYPES);
        InterfaceMaker interfaceMaker = new InterfaceMaker();
        interfaceMaker.add(signature, new Type[0]);
        interfaceMaker.setNamingPolicy((prefix, source, key, names) -> upperFirst(propertyName + "Holder"));
        return interfaceMaker.create();
    }

    public <T> T joinProperties(final @Nonnull T t) {
        try {
            final Class<?> clazz = t instanceof Factory ? t.getClass().getSuperclass() : t.getClass();

            final Class<?> proxyClass = PROXY_CLASSES_CACHE.get(new ProxyClassKey(clazz, properties));

            final @SuppressWarnings("unchecked") T result = (T) proxyClass.newInstance();

            ((Factory) result).setCallback(0, (InvocationHandler) (proxy, method, args) -> {
                final String propertyName = getPropertyName(method);
                if (properties.contains(propertyName)) {
                    return extractor.get(t, propertyName);
                } else {
                    return method.invoke(t, args);
                }
            });

            return result;
        } catch (Exception e) {
            throw propagate(e);
        }
    }

    public interface PropertyExtractor {
        Object get(Object obj, String property);
    }

    private static final class ProxyClassKey {
        final Class<?> clazz;
        final Set<String> names;

        ProxyClassKey(final Class<?> clazz, final Set<String> names) {
            this.clazz = clazz;
            this.names = names;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final ProxyClassKey that = (ProxyClassKey) obj;
            return Objects.equals(clazz, that.clazz) &&
                    Objects.equals(names, that.names);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, names);
        }
    }
}