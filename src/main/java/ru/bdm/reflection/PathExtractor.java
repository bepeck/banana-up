package ru.bdm.reflection;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.base.Objects.firstNonNull;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.any;
import static java.lang.reflect.Modifier.isFinal;
import static org.apache.commons.beanutils.ConvertUtils.convert;
import static org.apache.commons.lang.ClassUtils.wrapperToPrimitive;
import static ru.bdm.reflection.Util.getInterfaceParameterType;

/**
 * User: D.Brusentsov
 * Date: 22.04.13
 * Time: 13:26
 */
public class PathExtractor {

    private static final List<Class<?>> DO_NOT_PROXY = ImmutableList.<Class<?>>of(String.class, Boolean.class, Number.class,
            Date.class, Enum.class);

    public static String getPath(final Example example) {
        return getPath(getInterfaceParameterType(example.getClass(), Example.class, 0), example);
    }

    @SuppressWarnings("unchecked")
    public static <T> String getPath(final Class<T> entityClass, final Example<T> example) {
        final Example<T> checkedExample = checkNotNull(example);

        final Path pathContainer = new Path();

        final Object proxy = createProxy(checkNotNull(entityClass), pathContainer);

        checkedExample.example((T) proxy);

        final String path = pathContainer.getValue();
        if (path == null) {
            throw new ExampleNotProvided("no path defined");
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    public static <T> T mask(final Collection<?> collection, Class<T> maskClass) {
        if (collection instanceof Masked) {
            final MaskInfo maskInfo = ((Masked) collection).getInfo();
            if (maskInfo.type == null) {
                throw new RawCollection("collection " + maskInfo.path + " is raw");
            }
            if (maskClass != null && !maskInfo.type.isAssignableFrom(maskClass)) {
                throw new IllegalArgumentException("bad mask type: " + maskClass.getName() + ", expected subclass of " + maskInfo.type);
            }
            try {
                return (T) createProxy(firstNonNull(maskClass, maskInfo.type), maskInfo.path);
            } catch (Exception e) {
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
        enhancer.setCallback(new MethodInterceptor() {
            public Object intercept(Object o, Method method, Object[] args, MethodProxy proxy) throws Throwable {
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
                } else if (isFinal(returnType.getModifiers()) || any(DO_NOT_PROXY, new Predicate<Class<?>>() {
                    @Override
                    public boolean apply(Class<?> clazz) {
                        return clazz.isAssignableFrom(returnType);
                    }
                })) {
                    return null;
                }
                return createProxy(returnType, path);
            }
        });
        return enhancer.create();
    }

    public static <T> T mask(final Collection<T> collection) {
        return mask(collection, null);
    }

    protected static Object createCollectionProxy(final Class<?> collectionType,
                                                  final Class<?> collectionItemType,
                                                  final Path path) {
        final MaskInfo maskInfo = new MaskInfo(path, collectionItemType);

        final Enhancer enhancer = new Enhancer();
        enhancer.setClassLoader(PathExtractor.class.getClassLoader());
        if (collectionType.isInterface()) {
            enhancer.setInterfaces(new Class<?>[]{collectionType, Masked.class});
        } else {
            enhancer.setInterfaces(new Class<?>[]{Masked.class});
            enhancer.setSuperclass(collectionType);
        }
        enhancer.setCallback(new MethodInterceptor() {
            public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
                if (method.getDeclaringClass() == Masked.class) {
                    return maskInfo;
                }
                throw new UnsupportedMethod("only methods of " + Masked.class.getName() + " are supported");
            }
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

    public static class Path {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }

    public interface Example<Entity> {
        void example(Entity entity);
    }

    public interface Masked {
        MaskInfo getInfo();
    }

    public static class MaskInfo {
        final Path path;
        final Class<?> type;

        MaskInfo(Path path, Class<?> type) {
            this.path = path;
            this.type = type;
        }
    }

    public static class PathExtractorException extends RuntimeException {
        protected PathExtractorException(String message) {
            super(message);
        }
    }

    public static class PropertyNotFound extends PathExtractorException {
        public PropertyNotFound(String message) {
            super(message);
        }
    }

    public static class UnsupportedMethod extends PathExtractorException {
        public UnsupportedMethod(String message) {
            super(message);
        }
    }

    public static class RawCollection extends PathExtractorException {
        public RawCollection(String message) {
            super(message);
        }
    }

    public static class ExampleNotProvided extends PathExtractorException {
        public ExampleNotProvided(String message) {
            super(message);
        }
    }
}
