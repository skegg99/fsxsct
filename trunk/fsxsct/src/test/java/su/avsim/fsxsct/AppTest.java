package su.avsim.fsxsct;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.*;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    
    protected void setUp () {
        
    }
    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }

    public void testValueComparator() {
        Map<Integer, Double> bearingMap = new TreeMap<Integer, Double>();
        bearingMap.put (1,10d);
        bearingMap.put (2,-10d);
        bearingMap.put (3,5d);
        ValueComparator comparatorB = new ValueComparator(bearingMap);

        TreeMap<Integer, Double> sortedBearingMap = new TreeMap<Integer, Double>(comparatorB);
        sortedBearingMap.putAll(bearingMap);

        System.out.println(sortedBearingMap);

        assertEquals(3, sortedBearingMap.size()); 
        assertEquals(1, (int)sortedBearingMap.firstKey());
        assertEquals(3, (int)sortedBearingMap.higherKey(sortedBearingMap.firstKey()));
        assertEquals(2, (int)sortedBearingMap.lastKey());
        
    }
}
