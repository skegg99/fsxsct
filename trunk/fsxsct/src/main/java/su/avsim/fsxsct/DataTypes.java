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
        lat = 0;
        lon = 0;
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
    String type;

    public TaxiwaySegment() {
    }

    public TaxiwaySegment(int start, int end, float width, String name, String type) {
        this.start = start;
        this.end = end;
        this.width = width;
        this.name = name;
        this.type = type;
    }
}

class Runway {
    GeoCords start, end;
    double length, width, heading;
    String number, pD, sD;

    public Runway (GeoCords start, GeoCords end, double length, double width, double heading, String number, String pD, String sD) {
        this.start = start;
        this.end = end;
        this.length = length;
        this.width =  width;
        this.heading = heading;
        this.number = number;
        this.pD = pD;
        this.sD = sD;

    }

}
