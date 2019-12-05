package ru.bdm.reflection;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.sf.cglib.core.Signature;
import net.sf.cglib.proxy.*;
import org.javatuples.Pair;
import org.objectweb.asm.Type;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.cache.CacheBuilder.newBuilder;
import static com.google.common.collect.ImmutableSet.copyOf;
import static org.javatuples.Pair.with;
import static org.objectweb.asm.Type.getType;
import static ru.bdm.reflection.Util.*;

/**
 * User: D.Brusentsov
 * Date: 20.06.13
 * Time: 16:17
 */
public class PropertyJoiner {

    private final PropertyExtractor extractor;

    private final Set<String> properties;

    public PropertyJoiner(final @Nonnull PropertyExtractor extractor, final @Nonnull String... properties) {
        this.extractor = extractor;
        this.properties = copyOf(properties);
    }

    public PropertyJoiner(final @Nonnull PropertyExtractor extractor, final @Nonnull Collection<String> properties) {
        this(extractor, properties.toArray(new String[properties.size()]));
    }

    public static <T> T joinProperties(final @Nonnull T t, final @Nonnull Map<String, ?> map) {
        return new PropertyJoiner(new PropertyExtractor() {
            @Override
            public Object get(Object o, String property) {
                return map.get(property);
            }
        }, map.keySet()).joinProperties(t);
    }

    public <T> T joinProperties(final @Nonnull T t) {
        try {
            final Class clazz = t instanceof Factory ? t.getClass().getSuperclass() : t.getClass();

            final Class proxyClass = getProxyClass(clazz, properties);

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

    public static interface PropertyExtractor {
        public Object get(Object o, String property);
    }

    private static final LoadingCache<Pair<Class, Set<String>>, Class> PROXY_CLASSES_CACHE = newBuilder()
            .build(new CacheLoader<Pair<Class, Set<String>>, Class>() {
                @Override
                public Class load(Pair<Class, Set<String>> classHolderTypesKey) throws Exception {
                    return createProxyClass(classHolderTypesKey);
                }
            });

    private static Class createProxyClass(Pair<Class, Set<String>> classHolderTypesKey) throws ExecutionException {
        final Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(PropertyJoiner.class.getClassLoader());
        enhancer.setInterfaces(getPropertyHolderInterfaces(classHolderTypesKey.getValue1()));
        enhancer.setSuperclass(classHolderTypesKey.getValue0());
        enhancer.setCallbackType(InvocationHandler.class);
        enhancer.setCallbackFilter(new CallbackFilter() {
            @Override
            public int accept(Method method) {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });
        return enhancer.createClass();
    }

    private static Class getProxyClass(Class clazz, Set<String> properties) throws ExecutionException {
        return PROXY_CLASSES_CACHE.get(with(clazz, properties));
    }

    private static final LoadingCache<String, Class> PROPERTY_HOLDER_INTERFACE_CACHE = newBuilder()
            .build(new CacheLoader<String, Class>() {
                @Override
                public Class load(String propertyName) throws Exception {
                    return createPropertyHolderInterface(propertyName);
                }
            });

    private static Class[] getPropertyHolderInterfaces(final Set<String> properties) throws ExecutionException {
        final Class[] result = new Class[properties.size()];
        int i = 0;
        for (final String property : properties) {
            result[i++] = PROPERTY_HOLDER_INTERFACE_CACHE.get(property);
        }
        return result;
    }

    private static final Type OBJECT_TYPE = getType(Object.class);
    private static final Type[] EMPTY_TYPES = new Type[0];

    private static Class createPropertyHolderInterface(String propertyName) {
        Signature signature = new Signature(getGetterName(propertyName, Object.class), OBJECT_TYPE, EMPTY_TYPES);
        InterfaceMaker interfaceMaker = new InterfaceMaker();
        interfaceMaker.add(signature, new Type[0]);
        interfaceMaker.setNamingPolicy((prefix, source, key, names) -> upperFirst(propertyName + "Holder"));
        return interfaceMaker.create();
    }
}