/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package su.avsim.fsxsct;


/* basic geo coordinates class
 *
 */

class GeoCords {

    double lat, lon;

    public GeoCords() {
    }

    public GeoCords(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }
}

/**
 * tw point instance
 */
class TaxiwayPoint {

    int index;
    taxiwayPointType type;
    taxiwayPointOrientation ori;
    double lat, lon;

    // no one really need these enums for now. may need it later
    public enum taxiwayPointType {

        NORMAL, HOLD_SHORT, ILS_HOLD_SHORT
    }

    public enum taxiwayPointOrientation {

        FORWARD, REVERSE
    }

    public TaxiwayPoint(int index,
            taxiwayPointType type,
            taxiwayPointOrientation ori,
            double lat,
            double lon) {
        this.index = index;
        this.type = type;
        this.ori = ori;
        this.lat = lat;
        this.lon = lon;
    }

    public static taxiwayPointType returnType(String s) {
        if (s.equalsIgnoreCase("NORMAL")) {
            return taxiwayPointType.NORMAL;
        }
        if (s.equalsIgnoreCase("HOLD_SHORT")) {
            return taxiwayPointType.HOLD_SHORT;
        }
        if (s.equalsIgnoreCase("ILS_HOLD_SHORT")) {
            return taxiwayPointType.ILS_HOLD_SHORT;
        }
        return taxiwayPointType.NORMAL;
    }

    public static taxiwayPointOrientation returnOrientation(String s) {
        if (s.equalsIgnoreCase("FORWARD")) {
            return taxiwayPointOrientation.FORWARD;
        }
        if (s.equalsIgnoreCase("REVERSE")) {
            return taxiwayPointOrientation.REVERSE;
        }
        return taxiwayPointOrientation.FORWARD;
    }
}

class TaxiwaySegment {

    int start, end;
    float width;
    String name;

    public TaxiwaySegment() {
    }

    public TaxiwaySegment(int start, int end, float width, String name) {
        this.start = start;
        this.end = end;
        this.width = width;
        this.name = name;
    }
}
