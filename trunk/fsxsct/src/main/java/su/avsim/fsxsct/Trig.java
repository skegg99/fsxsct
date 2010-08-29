/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package su.avsim.fsxsct;

/**
 * Class, responsible for all kinds of geographocal calculations
 *
 */
class Trig {

    /**
     * loxodromy bearing calculation
     * distance calculation is commented out - useless for now
     */
    public static float getBearing(double startLat, double startLon, double endLat, double endLon) {
        //double q;
        //double dLat = Math.toRadians(endLat - startLat);
        double dLon = Math.toRadians(endLon - startLon);
        startLat = Math.toRadians(startLat);
        endLat = Math.toRadians(endLat);
        double dPhi = Math.log(Math.tan(endLat / 2 + Math.PI / 4) / Math.tan(startLat / 2 + Math.PI / 4));
        return Double.valueOf(Math.atan2(dLon, dPhi)).floatValue();
    }

    /**
     * coordinates calculation by start coordinates, bearing and distanse
     */
    public static GeoCords getPoint(double lat, double lon, float dist, double brng) {
        brng = to360(brng);
        double q, dLon;
        lat = Math.toRadians(lat);
        lon = Math.toRadians(lon);
        dist = dist / 6371000; // angilar distanse (6371 km - Earth radius)

        double lat2 = lat + dist * Math.cos(brng);
        double dLat = lat2 - lat;
        double dPhi = Math.log(Math.tan(lat2 / 2 + Math.PI / 4) / Math.tan(lat / 2 + Math.PI / 4));
        if (dPhi != 0) {
            q = dLat / dPhi;
        } else {
            q = Math.cos(lat);
        }
        dLon = dist * Math.sin(brng) / q;
        if (Math.abs(lat2) > Math.PI / 2) {
            lat2 = lat2 > 0 ? Math.PI - lat2 : -(Math.PI - lat2);
        }
        double lon2 = (lon + dLon + Math.PI) % (2 * Math.PI) - Math.PI;
        return new GeoCords(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }
    // and an overloaded version with geocords
    public static GeoCords getPoint(GeoCords geo, float dist, double brng) {
        return getPoint(geo.lat, geo.lon, dist, brng);
    }

    //backtrack bearing
    public static double getBackBearing(double brng) {
        brng = brng >= Math.PI ? brng - Math.PI : brng + Math.PI;
        return brng;
    }

    //backtrack rw number
    public static String getBacktrackRW(String brng) {
        return Integer.toString(
                Double.valueOf(Math.toDegrees(getBackBearing(Math.toRadians(Double.parseDouble(brng) * 10)) / 10)).intValue());
    }

    public static double to360(double brng) {
        brng = brng >= Math.PI * 2 ? brng - Math.PI * 2 : brng;
        brng = brng < 0 ? brng + Math.PI * 2 : brng;
        return brng;
    }

    /**
     *  calculates intersection (coords) of two paths given coords on bearing
     */
    public static GeoCords getIntersectionCoords(GeoCords p1, double brng1, GeoCords p2, double brng2) {
        double lat1 = Math.toRadians(p1.lat);
        double lon1 = Math.toRadians(p1.lon);
        double lat2 = Math.toRadians(p2.lat);
        double lon2 = Math.toRadians(p2.lon);
        double brng13 = brng1;
        double brng23 = brng2;
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double dist12 = 2 * Math.asin(Math.sqrt(Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2)));
        if (dist12 == 0) {
            return null;
        }

        // initial/final bearings between points
        double brngA = Math.acos((Math.sin(lat2) - Math.sin(lat1) * Math.cos(dist12))
                / (Math.sin(dist12) * Math.cos(lat1)));
        if (Double.isNaN(brngA)) {
            brngA = 0;  // protect against rounding
        }
        double brngB = Math.acos((Math.sin(lat1) - Math.sin(lat2) * Math.cos(dist12))
                / (Math.sin(dist12) * Math.cos(lat2)));

        double brng12, brng21;
        if (Math.sin(lon2 - lon1) > 0) {
            brng12 = brngA;
            brng21 = 2 * Math.PI - brngB;
        } else {
            brng12 = 2 * Math.PI - brngA;
            brng21 = brngB;
        }

        double alpha1 = (brng13 - brng12 + Math.PI) % (2 * Math.PI) - Math.PI;  // angle 2-1-3
        double alpha2 = (brng21 - brng23 + Math.PI) % (2 * Math.PI) - Math.PI;  // angle 1-2-3

        if (Math.sin(alpha1) == 0 && Math.sin(alpha2) == 0) {

            return null;  // infinite intersections
        }
        if (Math.sin(alpha1) * Math.sin(alpha2) < 0) {

            return null;       // ambiguous intersection
        }
        //alpha1 = Math.abs(alpha1);
        //alpha2 = Math.abs(alpha2);
        // ... Ed Williams takes abs of alpha1/alpha2, but seems to break calculation?

        double alpha3 = Math.acos(-Math.cos(alpha1) * Math.cos(alpha2)
                + Math.sin(alpha1) * Math.sin(alpha2) * Math.cos(dist12));
        double dist13 = Math.atan2(Math.sin(dist12) * Math.sin(alpha1) * Math.sin(alpha2),
                Math.cos(alpha2) + Math.cos(alpha1) * Math.cos(alpha3));
        double lat3 = Math.asin(Math.sin(lat1) * Math.cos(dist13)
                + Math.cos(lat1) * Math.sin(dist13) * Math.cos(brng13));
        double dLon13 = Math.atan2(Math.sin(brng13) * Math.sin(dist13) * Math.cos(lat1),
                Math.cos(dist13) - Math.sin(lat1) * Math.sin(lat3));
        double lon3 = lon1 + dLon13;
        lon3 = (lon3 + Math.PI) % (2 * Math.PI) - Math.PI;  // normalise to -180..180?

        return new GeoCords(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }
}
