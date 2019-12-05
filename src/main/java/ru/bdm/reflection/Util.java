package ru.bdm.reflection;

import com.google.common.collect.ImmutableList;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.reverse;
import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;
import static org.apache.commons.lang.ArrayUtils.contains;
import static org.apache.commons.lang.ArrayUtils.indexOf;

/**
 * User: D.Brusentsov
 * Date: 26.04.13
 * Time: 11:28
 */
public class Util {

    /**
     * @param member getter or field that represents the property
     * @return collection generic parameter or null if it there is not
     * @throws IllegalArgumentException if method returns not a collection
     */
    public static Class getCollectionItemType(Member member) {
        if (member instanceof Method) {
            return getCollectionItemType((Method) member);
        } else if (member instanceof Field) {
            return getCollectionItemType((Field) member);
        } else {
            throw new IllegalArgumentException("illegal member type: " + member);
        }
    }

    private static Class<?> getCollectionItemType(Method getter) {
        if (!Collection.class.isAssignableFrom(getter.getReturnType())) {
            throw new IllegalArgumentException("not a collection");
        }
        return getCollectionItemType(getter.getGenericReturnType());
    }

    private static Class getCollectionItemType(Field field) {
        if (!Collection.class.isAssignableFrom(field.getType())) {
            throw new IllegalArgumentException("not a collection");
        }
        return getCollectionItemType(field.getGenericType());
    }

    private static Class getCollectionItemType(Type genericReturnType) {
        if (!(genericReturnType instanceof ParameterizedType)) {
            return null;
        }
        final Type type = ((ParameterizedType) genericReturnType).getActualTypeArguments()[0];
        if (!(type instanceof Class)) {
            return null;
        }
        return (Class<?>) type;
    }

    public static Class<?> getInterfaceParameterType(Class<?> implemented, Class<?> interfaceType, int parameterIndex) {
        checkState(interfaceType.isAssignableFrom(implemented));

        final List<Class<?>> inheritanceChain = inheritanceChain(implemented, interfaceType);

        final Class superClass = inheritanceChain.get(0);

        final Type typeParameter = findGenericInterfaceParameter(superClass.getGenericInterfaces(), interfaceType, parameterIndex);

        if (typeParameter == null || typeParameter instanceof Class<?>) {
            return (Class<?>) typeParameter;
        }

        return resolveGenericClassParameter(inheritanceChain, typeParameter);
    }

    private static Class resolveGenericClassParameter(List<Class<?>> inheritanceChain, Type typeParameter) {
        for (int i = 0, typeParameterIndex = -1; i < inheritanceChain.size(); i++) {
            final Class<?> clazz = inheritanceChain.get(i);
            if (i != 0) {
                typeParameter = ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[typeParameterIndex];
                if (typeParameter instanceof Class) {
                    return (Class<?>) typeParameter;
                }
            }
            if (typeParameter instanceof TypeVariable) {
                typeParameterIndex = indexOf(clazz.getTypeParameters(), typeParameter);
                if (typeParameterIndex == -1) {
                    throw new RuntimeException(typeParameter + " in not presented in " + clazz.toString());
                }
            } else {
                return null;
            }
        }
        return null;
    }

    private static Type findGenericInterfaceParameter(Type[] genericInterfaces, Class<?> interfaceType, int parameterIndex) {
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

    private static List<Class<?>> inheritanceChain(Class<?> implemented, Class<?> interfaceType) {
        final List<Class<?>> res = new ArrayList<Class<?>>();
        do {
            res.add(implemented);
            if (contains(implemented.getInterfaces(), interfaceType)) {
                break;
            }
            implemented = implemented.getSuperclass();
        } while (implemented != null);
        return reverse(res);
    }

    private static final String SETTER_PREFIX = "set";
    private static final String GETTER_PREFIX = "get";
    private static final String GETTER_PREFIX_BOOLEAN = "is";
    private static final List<String> PROPERTY_ACCESSOR_PREFIXES = ImmutableList.of(GETTER_PREFIX_BOOLEAN, GETTER_PREFIX, SETTER_PREFIX);

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

    public static String lowerFirst(String string) {
        final char[] chars = string.toCharArray();
        chars[0] = toLowerCase(chars[0]);
        return new String(chars);
    }

    public static String getGetterName(final String name, final Class returnType) {
        return ((returnType == boolean.class || returnType == Boolean.class) ? GETTER_PREFIX_BOOLEAN : GETTER_PREFIX) + upperFirst(name);
    }

    public static String upperFirst(String name) {
        char[] nameChars = name.toCharArray();
        nameChars[0] = toUpperCase(nameChars[0]);
        return new String(nameChars);
    }
}
