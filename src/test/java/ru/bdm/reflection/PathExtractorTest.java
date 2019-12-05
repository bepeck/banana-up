package ru.bdm.reflection;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static ru.bdm.reflection.PathExtractor.*;

/**
 * User: D.Brusentsov
 * Date: 22.04.13
 * Time: 20:21
 */
public class PathExtractorTest {

    public static class Q {
        String string;
        Date date;
        List<W> ws;
        ArrayList<W> wsArrayList;
        List list;

        public ArrayList<W> getWsArrayList() {
            return wsArrayList;
        }

        public String getString() {
            return string;
        }

        public Date getDate() {
            return date;
        }

        public List<W> getWs() {
            return ws;
        }

        public List getList() {
            return list;
        }
    }

    public static class W {
        Q q;
        long lng;

        boolean bln;

        Q getQ() {
            return q;
        }

        public long getLng() {
            return lng;
        }

        public boolean isBln() {
            return bln;
        }

        public boolean q() {
            return false;
        }

        public void setLng(long lng) {
            this.lng = lng;
        }
    }

    @Test(expected = RawCollection.class)
    public void failOnRawCollectionTest() {
        getPath(Q.class, new Example<Q>() {
            @Override
            public void example(Q q) {
                mask(q.getList(), Object.class);
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void failOnNullTypeTest() {
        getPath(null, new Example<Q>() {
            @Override
            public void example(Q q) {
                q.getList();
            }
        });
    }

    @Test(expected = NullPointerException.class)
    public void failOnNullExampleTest() {
        getPath(Q.class, null);
    }

    @Test(expected = PropertyNotFound.class)
    public void failOnCallNotGetter0() {
        getPath(Q.class, new Example<Q>() {
            @Override
            public void example(Q q) {
                q.toString();
            }
        });
    }

    @Test(expected = PropertyNotFound.class)
    public void failOnCallNotGetter1() {
        getPath(W.class, new Example<W>() {
            @Override
            public void example(W q) {
                q.q();
            }
        });
    }

    @Test
    public void accessToPrimitiveTest() {
        assertEquals("bln", getPath(W.class, new Example<W>() {
            @Override
            public void example(W q) {
                q.isBln();
            }
        }));
    }

    @Test
    public void accessToDateTest() {
        assertEquals("date", getPath(Q.class, new Example<Q>() {
            @Override
            public void example(Q q) {
                q.getDate();
            }
        }));
    }

    @Test
    public void accessToStringTest() {
        assertEquals("string", getPath(Q.class, new Example<Q>() {
            @Override
            public void example(Q q) {
                q.getString();
            }
        }));
    }

    @Test
    public void accessToPojoTest() {
        assertEquals("q", getPath(W.class, new Example<W>() {
            @Override
            public void example(W q) {
                q.getQ();
            }
        }));
    }

    @Test
    public void accessToPojoThroughCollection0Test() {
        assertEquals("ws.lng", getPath(Q.class, new Example<Q>() {
            @Override
            public void example(Q q) {
                mask(q.getWs()).getLng();
            }
        }));
    }

    @Test
    public void accessToPojoThroughCollection1Test() {
        assertEquals("wsArrayList.lng", getPath(Q.class, new Example<Q>() {
            @Override
            public void example(Q q) {
                mask(q.getWsArrayList()).getLng();
            }
        }));
    }

    @Test(expected = UnsupportedMethod.class)
    public void failOnAccessToNotMaskedCollectionTest() {
        getPath(Q.class, new Example<Q>() {
            @Override
            public void example(Q q) {
                q.getWsArrayList().toString();
            }
        });
    }

    @Test(expected = ExampleNotProvided.class)
    public void failOnEmptyExampleTest() {
        getPath(Q.class, new Example<Q>() {
            @Override
            public void example(Q q) {
            }
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void maskFailTest() {
        mask(new LinkedList<Object>());
    }

    @Test
    public void maskTest() throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        final Class<String> maskType = String.class;
        final Path path = new Path();

        final List sss = (List) createCollectionProxy(List.class, maskType, path);

        Masked masked = (Masked) sss;

        MaskInfo info = masked.getInfo();

        assertEquals(info.path, path);
        assertEquals(info.type, maskType);
    }

    @Test
    public void testInterface() {
        final String empty = getPath(Collection.class, new Example<Collection>() {
            @Override
            public void example(Collection collection) {
                collection.isEmpty();
            }
        });
        assertEquals("empty", empty);
    }

    @Test
    public void testSetter() {
        final String lng = getPath(W.class, new Example<W>() {
            @Override
            public void example(W w) {
                w.setLng(0);
            }
        });
        assertEquals("lng", lng);
    }

    @Test
    public void testSkipType() {
        final String lng = getPath(new Example<W>() {
            @Override
            public void example(W o) {
                o.getLng();
            }
        });
        assertEquals("lng", lng);
    }
}
