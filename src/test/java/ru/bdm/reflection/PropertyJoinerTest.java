package ru.bdm.reflection;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.apache.commons.beanutils.PropertyUtils.getProperty;
import static ru.bdm.reflection.PropertyJoiner.PropertyExtractor;
import static ru.bdm.reflection.PropertyJoiner.joinProperties;

/**
 * User: D.Brusentsov
 * Date: 20.06.13
 * Time: 17:10
 */
public class PropertyJoinerTest {

    @Test
    public void testWithPropertyExtractor() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        PropertyJoiner propertyJoiner = new PropertyJoiner(
                (obj, property) -> property + "Value", "first", "second"
        );

        AnyType src = new AnyType();

        AnyType dst = propertyJoiner.joinProperties(src);

        assertEquals("firstValue", getProperty(dst, "first"));
        assertEquals("secondValue", getProperty(dst, "second"));
        assertEquals("anyPropertyValue", getProperty(dst, "anyProperty"));
    }

    @Test
    public void testWithMap() throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Map<String, ?> properties = new HashMap<String, String>() {{
            put("first", "firstValue");
            put("second", "secondValue");
        }};

        AnyType src = new AnyType();

        AnyType dst = joinProperties(src, properties);

        assertEquals("firstValue", getProperty(dst, "first"));
        assertEquals("secondValue", getProperty(dst, "second"));
        assertEquals("anyPropertyValue", getProperty(dst, "anyProperty"));
    }

    @Test
    public void performanceTest() {
        PropertyJoiner propertyJoiner = new PropertyJoiner(
                (obj, property) -> property + "Value", "first", "second"
        );

        int count = 100000;

        long t0 = System.currentTimeMillis();

        List<AnyType> qs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            qs.add(new AnyType());
        }

        long t1 = System.currentTimeMillis();

        List<AnyType> qsWithJoinedProps = new ArrayList<>();
        for (AnyType q : qs) {
            qsWithJoinedProps.add(propertyJoiner.joinProperties(q));
        }

        long t2 = System.currentTimeMillis();

        for (AnyType q : qsWithJoinedProps) {
            doSomething(q.getAnyProperty());
        }

        long t3 = System.currentTimeMillis();

        for (AnyType q : qs) {
            doSomething(q.getAnyProperty());
        }

        long t4 = System.currentTimeMillis();

        long rawCreateTime = t1 - t0;
        long proxyCreateTime = t2 - t1;
        long proxyIterateTime = t3 - t2;
        long rawIterateTime = t4 - t3;

        System.out.println("proxyCreateTime:" + proxyCreateTime);
        System.out.println("proxyIterateTime: " + proxyIterateTime);
        System.out.println("rawCreateTime: " + rawCreateTime);
        System.out.println("rawIterateTime: " + rawIterateTime);
    }

    private void doSomething(final Object obj) {
        //to prevent optimisation
    }

    public static class AnyType {
        public Object getAnyProperty() {
            return "anyPropertyValue";
        }
    }
}
