package ru.bdm.reflection;

import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static ru.bdm.reflection.Util.getCollectionItemType;
import static ru.bdm.reflection.Util.getInterfaceParameterType;

/**
 * User: D.Brusentsov
 * Date: 26.04.13
 * Time: 16:05
 */
public class UtilTest {

    public static class Q {
        List rawList;

        public List getRawList() {
            return rawList;
        }

        public void setRawList(List rawList) {
            this.rawList = rawList;
        }
    }

    public static class W extends Q {
        List<Q> qs;

        public List<Q> getQs() {
            return qs;
        }

        public void setQs(List<Q> qs) {
            this.qs = qs;
        }
    }

    @Test
    public void getCollectionItemTypeTest() throws NoSuchMethodException, NoSuchFieldException {
        assertEquals(Q.class, getCollectionItemType(W.class.getMethod("getQs")));
        assertNull(getCollectionItemType(W.class.getMethod("getRawList")));

        assertEquals(Q.class, getCollectionItemType(W.class.getDeclaredField("qs")));
        assertNull(getCollectionItemType(Q.class.getDeclaredField("rawList")));
    }

    public static interface Ifc<T0, T1> {
    }

    public static class IfcImpl implements Ifc<String, Double> {
    }

    public static class IfcSubImpl extends IfcImpl {
    }

    public static abstract class IfcBase<T0, T1> implements Ifc<T0, T1> {
    }

    public static class IfcSubBase<T1> extends IfcBase<String, T1> {
    }

    public static class IfcSubSubBase extends IfcSubBase<Double> {
    }

    @Test
    public void getInterfaceParameterTypeTest() {
        assertEquals(String.class, getInterfaceParameterType(IfcImpl.class, Ifc.class, 0));
        assertEquals(Double.class, getInterfaceParameterType(IfcImpl.class, Ifc.class, 1));

        assertEquals(String.class, getInterfaceParameterType(IfcSubImpl.class, Ifc.class, 0));
        assertEquals(Double.class, getInterfaceParameterType(IfcSubImpl.class, Ifc.class, 1));

        assertEquals(String.class, getInterfaceParameterType(IfcSubSubBase.class, Ifc.class, 0));
        assertEquals(Double.class, getInterfaceParameterType(IfcSubSubBase.class, Ifc.class, 1));

        assertEquals(String.class, getInterfaceParameterType(IfcSubBase.class, Ifc.class, 0));
        assertEquals(null, getInterfaceParameterType(IfcSubBase.class, Ifc.class, 1));

        assertEquals(null, getInterfaceParameterType(IfcBase.class, Ifc.class, 0));
        assertEquals(null, getInterfaceParameterType(IfcBase.class, Ifc.class, 1));
    }
}
