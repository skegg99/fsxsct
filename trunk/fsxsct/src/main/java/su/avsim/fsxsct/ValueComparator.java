/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package su.avsim.fsxsct;

import java.util.*;


/**
 * comparator for finding adjacent tw segments, which are originating form one tw point
 */
class ValueComparator implements Comparator {

    Map base;

    public ValueComparator(Map base) {
        this.base = base;
    }

    public int compare(Object a, Object b) {

        if ((Double) base.get(a) < (Double) base.get(b)) {
            return 1;
        } else if ((Double) base.get(a) == (Double) base.get(b)) {
            return 0;
        } else {
            return -1;
        }
    }
}
