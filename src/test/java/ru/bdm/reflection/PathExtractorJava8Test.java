package ru.bdm.reflection;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static ru.bdm.reflection.PathExtractorJava8.start;
import static ru.bdm.reflection.PathExtractorTest.Q;
import static ru.bdm.reflection.PathExtractorTest.W;

/**
 * User: D.Brusentsov
 * Date: 20.05.14
 */
public class PathExtractorJava8Test {

    @Test
    public void getSimple() {
        String q = start(W.class, W::getQ).end();

        assertEquals("q", q);
    }

    @Test
    public void getComplex() {
        String qDate = start(W.class, W::getQ).then(Q::getDate).end();

        assertEquals("q.date", qDate);
    }

    @Test
    public void getComplexWithCollection() {
        String qWsBln = start(W.class, W::getQ).thenMask(Q::getWs).then(W::isBln).end();

        assertEquals("q.ws.bln", qWsBln);
    }
}
