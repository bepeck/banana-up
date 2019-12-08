package ru.bdm.reflection;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static java.lang.reflect.Modifier.isFinal;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.beanutils.ConvertUtils.convert;
import static org.apache.commons.lang.ClassUtils.wrapperToPrimitive;
import static ru.bdm.reflection.Util.firstNotNull;
import static ru.bdm.reflection.Util.getInterfaceParameterType;
import static ru.bdm.reflection.Util.propagate;

/**
 * User: D.Brusentsov
 * Date: 22.04.13
 * Time: 13:26
 */
public class PathExtractor {

    private static final List<Class<?>> DO_NOT_PROXY = unmodifiableList(asList(
            String.class,
            Boolean.class,
            Number.class,
            Date.class,
            Enum.class
    ));

    public static String getPath(final Example example) {
        return getPath(getInterfaceParameterType(example.getClass(), Example.class, 0), example);
    }

    @SuppressWarnings("unchecked")
    public static <T> String getPath(final Class<T> entityClass, final Example<T> example) {
        final Example<T> checkedExample = requireNonNull(example);

        final Path pathContainer = new Path();

        final Object proxy = createProxy(requireNonNull(entityClass), pathContainer);

        checkedExample.example((T) proxy);

        final String path = pathContainer.getValue();
        if (path == null) {
            throw new ExampleNotProvided("no path defined");
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    public static <T> T mask(final Collection<?> collection, final Class<T> maskClass) {
        if (collection instanceof Masked) {
            final MaskInfo maskInfo = ((Masked) collection).getInfo();
            if (maskInfo.type == null) {
                throw new RawCollection("collection " + maskInfo.path + " is raw");
            }
            if (maskClass != null && !maskInfo.type.isAssignableFrom(maskClass)) {
                throw new IllegalArgumentException("bad mask type: " + maskClass.getName() + ", expected subclass of " + maskInfo.type);
            }
            try {
                return (T) createProxy(firstNotNull(maskClass, maskInfo.type), maskInfo.path);
            } catch (final Exception e) {
                throw propagate(e);
            }
        }
        throw new IllegalArgumentException("collection is not instance of " + Masked.class.getName());
    }

    protected static Object createProxy(final Class<?> entityClass, final Path path) {
        final String currentPath = path.getValue();

        final Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(PathExtractor.class.getClassLoader());
        if (entityClass.isInterface()) {
            enhancer.setInterfaces(new Class<?>[]{entityClass});
        } else {
            enhancer.setSuperclass(entityClass);
        }
        enhancer.setCallback((MethodInterceptor) (o, method, args, proxy) -> {
            final String pathValue = getPath(method, currentPath);
            path.setValue(pathValue);
            final Class<?> returnType = method.getReturnType();
            if (returnType == void.class) {
                return null;
            }
            if (Collection.class.isAssignableFrom(returnType)) {
                return createCollectionProxy(returnType, Util.getCollectionItemType(method), path);
            }
            final Class<?> primitive = returnType.isPrimitive() ? returnType : wrapperToPrimitive(returnType);
            if (primitive != null) {
                return convert((Object) null, primitive);
            } else if (isFinal(returnType.getModifiers()) || DO_NOT_PROXY.stream().anyMatch(clazz -> clazz.isAssignableFrom(returnType))) {
                return null;
            }
            return createProxy(returnType, path);
        });
        return enhancer.create();
    }

    public static <T> T mask(final Collection<T> collection) {
        return mask(collection, null);
    }

    static Object createCollectionProxy(
            final Class<?> collectionType,
            final Class<?> collectionItemType,
            final Path path
    ) {
        final MaskInfo maskInfo = new MaskInfo(path, collectionItemType);

        final Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(PathExtractor.class.getClassLoader());
        if (collectionType.isInterface()) {
            enhancer.setInterfaces(new Class<?>[]{collectionType, Masked.class});
        } else {
            enhancer.setInterfaces(new Class<?>[]{Masked.class});
            enhancer.setSuperclass(collectionType);
        }
        enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
            if (method.getDeclaringClass() == Masked.class) {
                return maskInfo;
            }
            throw new UnsupportedMethod("only methods of " + Masked.class.getName() + " are supported");
        });
        return enhancer.create();
    }

    private static String getPath(final Method method, final String pathPrefix) {
        return (pathPrefix == null ? "" : pathPrefix + ".") + getPropertyName(method);
    }

    private static String getPropertyName(final Method accessor) {
        final String res = Util.getPropertyName(accessor);
        if (res == null) {
            throw new PropertyNotFound("can't get property name by method: " + accessor);
        }
        return res;
    }

    @FunctionalInterface
    public interface Example<Entity> {
        void example(Entity entity);
    }

    public interface Masked {
        MaskInfo getInfo();
    }

    //TODO: what?
    public static class Path {
        private String value;

        String getValue() {
            return value;
        }

        void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static class MaskInfo {
        final Path path;
        final Class<?> type;

        MaskInfo(Path path, Class<?> type) {
            this.path = path;
            this.type = type;
        }
    }

    static class PathExtractorException extends RuntimeException {
        PathExtractorException(String message) {
            super(message);
        }
    }

    static class PropertyNotFound extends PathExtractorException {
        PropertyNotFound(String message) {
            super(message);
        }
    }

    static class UnsupportedMethod extends PathExtractorException {
        UnsupportedMethod(String message) {
            super(message);
        }
    }

    static class RawCollection extends PathExtractorException {
        RawCollection(String message) {
            super(message);
        }
    }

    static class ExampleNotProvided extends PathExtractorException {
        ExampleNotProvided(String message) {
            super(message);
        }
    }
}
