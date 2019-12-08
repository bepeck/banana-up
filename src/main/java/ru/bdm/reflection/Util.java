package ru.bdm.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static java.util.Arrays.asList;
import static java.util.Collections.reverse;
import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.ArrayUtils.contains;
import static org.apache.commons.lang3.ArrayUtils.indexOf;

/**
 * User: D.Brusentsov
 * Date: 26.04.13
 * Time: 11:28
 */
public final class Util {

    private static final String SETTER_PREFIX = "set";
    private static final String GETTER_PREFIX = "get";
    private static final String GETTER_PREFIX_BOOLEAN = "is";
    private static final List<String> PROPERTY_ACCESSOR_PREFIXES = unmodifiableList(asList(
            GETTER_PREFIX_BOOLEAN,
            GETTER_PREFIX,
            SETTER_PREFIX
    ));

    private Util() {
    }

    /**
     * @param member getter or field that represents the property
     * @return collection generic parameter or null if it there is not
     * @throws IllegalArgumentException if method returns not a collection
     */
    public static Class getCollectionItemType(final Member member) {
        if (member instanceof Method) {
            return getCollectionItemType((Method) member);
        } else if (member instanceof Field) {
            return getCollectionItemType((Field) member);
        } else {
            throw new IllegalArgumentException("illegal member type: " + member);
        }
    }

    private static Class<?> getCollectionItemType(final Method getter) {
        if (!Collection.class.isAssignableFrom(getter.getReturnType())) {
            throw new IllegalArgumentException("not a collection");
        }
        return getCollectionItemType(getter.getGenericReturnType());
    }

    private static Class getCollectionItemType(final Field field) {
        if (!Collection.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("not a collection");
        }
        return getCollectionItemType(field.getGenericType());
    }

    private static Class getCollectionItemType(final Type genericReturnType) {
        if (!(genericReturnType instanceof ParameterizedType)) {
            return null;
        }
        final Type type = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        if (!(type instanceof Class)) {
            return null;
        }
        return (Class<?>) type;
    }

    public static Class<?> getInterfaceParameterType(
            final Class<?> implemented,
            final Class<?> interfaceType,
            final int parameterIndex
    ) {
        if (!interfaceType.isAssignableFrom(implemented)) {
            throw new IllegalStateException();
        }

        final List<Class<?>> inheritanceChain = inheritanceChain(implemented, interfaceType);

        final Class superClass = inheritanceChain.get(0);

        final Type typeParameter = findGenericInterfaceParameter(superClass.getGenericInterfaces(), interfaceType, parameterIndex);

        if (typeParameter == null || typeParameter instanceof Class<?>) {
            return (Class<?>) typeParameter;
        }

        return resolveGenericClassParameter(inheritanceChain, typeParameter);
    }

    private static Class resolveGenericClassParameter(
            final List<Class<?>> inheritanceChain,
            final Type typeParameter
    ) {
        Type actualTypeParameter = typeParameter;
        for (int i = 0, typeParameterIndex = -1; i < inheritanceChain.size(); i++) {
            final Class<?> clazz = inheritanceChain.get(i);
            if (i != 0) {
                actualTypeParameter = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[typeParameterIndex];
                if (actualTypeParameter instanceof Class) {
                    return (Class<?>) actualTypeParameter;
                }
            }
            if (actualTypeParameter instanceof TypeVariable) {
                typeParameterIndex = indexOf(clazz.getTypeParameters(), actualTypeParameter);
                if (typeParameterIndex == -1) {
                    throw new RuntimeException(actualTypeParameter + " in not presented in " + clazz.toString());
                }
            } else {
                return null;
            }
        }
        return null;
    }

    private static Type findGenericInterfaceParameter(
            final Type[] genericInterfaces,
            final Class<?> interfaceType,
            final int parameterIndex
    ) {
        for (final Type genericInterface : genericInterfaces) {
            if (!(genericInterface instanceof ParameterizedType)) {
                continue;
            }
            final ParameterizedType genericInterfaceParametrizedType = (ParameterizedType) genericInterface;
            if (genericInterfaceParametrizedType.getRawType() != interfaceType) {
                continue;
            }
            return genericInterfaceParametrizedType.getActualTypeArguments()[parameterIndex];
        }
        return null;
    }

    private static List<Class<?>> inheritanceChain(final Class<?> clazz, final Class<?> interfaceType) {
        final List<Class<?>> res = new ArrayList<>();
        Class<?> clazzToCheck = clazz;
        do {
            res.add(clazzToCheck);
            if (contains(clazzToCheck.getInterfaces(), interfaceType)) {
                break;
            }
            clazzToCheck = clazzToCheck.getSuperclass();
        } while (clazzToCheck != null);
        reverse(res);
        return res;
    }

    public static String getPropertyName(final Method accessor) {
        final String accessorName = accessor.getName();
        for (String accessorNamePrefix : PROPERTY_ACCESSOR_PREFIXES) {
            if (!accessorName.startsWith(accessorNamePrefix)) {
                continue;
            }
            return lowerFirst(accessorName.substring(accessorNamePrefix.length()));
        }
        return null;
    }

    public static String lowerFirst(final String string) {
        final char[] chars = string.toCharArray();
        chars[0] = toLowerCase(chars[0]);
        return new String(chars);
    }

    public static String getGetterName(final String name, final Class returnType) {
        return ((returnType == boolean.class || returnType == Boolean.class)
                ? GETTER_PREFIX_BOOLEAN
                : GETTER_PREFIX
        ) + upperFirst(name);
    }

    public static String upperFirst(final String name) {
        char[] nameChars = name.toCharArray();
        nameChars[0] = toUpperCase(nameChars[0]);
        return new String(nameChars);
    }

    static RuntimeException propagate(final Exception e) {
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        throw new RuntimeException(e);
    }
}
