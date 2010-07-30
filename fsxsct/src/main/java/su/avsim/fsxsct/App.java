package su.avsim.fsxsct;

import java.util.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

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
/* basic geo coordinates class
 *
 */

class GeoCords {

    double lat, lon;

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

    public TaxiwaySegment(int start, int end, float width, String name) {
        this.start = start;
        this.end = end;
        this.width = width;
        this.name = name;
    }
}

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
}

public class App
        extends DefaultHandler {

    List<TaxiwayPoint> taxiwayPointsList = new ArrayList<TaxiwayPoint>();
    List<TaxiwaySegment> taxiwaySegmentsList = new ArrayList<TaxiwaySegment>();
    int taxiwaySegmentCounter = 0;
    Map<Integer, Set<Integer>> taxiwayPointIndex = new HashMap<Integer, Set<Integer>>(); //индекс отрезков для каждой точки

    public void parseURI(String uri) {
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            sp.parse(uri, this);
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    /** Start document. */
    public void startDocument() {
        //System.out.println("<?xml version=\"1.0\"?>");
        } // startDocument()

    protected int getAsInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            System.err.println("Error while converting string to int" + e);
            return 0;
        }
    }

    protected String splitDegrees(double coord) { // разложим градусы с десятыми на гр/мин/c
        String coordString = new String();
        try {
            double degrees = Math.floor(coord);
            double tempMinutesLeft = (coord - degrees) * 60.0d;
            double minutes = Math.floor(tempMinutesLeft);
            double seconds = (tempMinutesLeft - minutes) * 60.0d; // с долями
            // используем американскую локаль, чтобы десятые отделялись точкой
            coordString = String.format(Locale.US, "%1$03.0f.%2$02.0f.%3$06.3f", degrees, minutes, seconds);
        } catch (Exception e) {
            System.err.println("Ошибка при переводе координаты " + coordString + " " + e);
        }

        return coordString;
    }

    // translating degrees to deg/min/sec (lat)
    protected String getLat(double lat) {
        String latString = new String();
        if (lat >= 0) {
            latString = "N";
        }
        if (lat <= 0) {
            latString = "S";
            lat = -lat;
        }
        latString += splitDegrees(lat);
        return latString;
    }

    // translating degrees to deg/min/sec (lon)
    protected String getLon(double lon) {
        String lonString = new String();
        if (lon >= 0) {
            lonString = "E";
        }
        if (lon <= 0) {
            lonString = "W";
            lon = -lon;
        }
        lonString += splitDegrees(lon);

        return lonString;
    }
    // outputt for coords string formatted for sct file

    protected String getBoth(GeoCords coord) {
        return getLat(coord.lat) + " " + getLon(coord.lon);
    }

    protected void getBorderRadialOffset(int thisPoint, int otherPoint) {
        // need to know what other segments are originating or ending in this segment's point
        Set<Integer> otherSegments = taxiwayPointIndex.get(thisPoint);


        try {
            // this treemap cointains bearings from this point for all crossing segments, indexed by segment number
            // it is sorted by bearing by comparator
            Map<Integer, Double> bearingMap = new TreeMap<Integer, Double>();
            ValueComparator comparatorB =  new ValueComparator(bearingMap);
            // iterating through index numbers of all segments which are connected to this point

            Iterator<Integer> it2 = otherSegments.iterator();
            while (it2.hasNext()) {
                int segmentCounter = it2.next(); //segment number
                int segmentStart = taxiwaySegmentsList.get(segmentCounter).start;
                int segmentEnd = taxiwaySegmentsList.get(segmentCounter).end;
                System.out.print(segmentCounter + " ");
                // determine the point index on the outer end of segment
                int outerPoint = thisPoint == segmentStart ? segmentEnd : segmentStart;
                // get the bearing from this point to outer point
                double segmentBearing = Trig.getBearing(
                        taxiwayPointsList.get(thisPoint).lat,
                        taxiwayPointsList.get(thisPoint).lon,
                        taxiwayPointsList.get(outerPoint).lat,
                        taxiwayPointsList.get(outerPoint).lon);
                // put it to bearingMap
                bearingMap.put(segmentCounter, segmentBearing);
            }
            Map<Integer, Double> sortedBearingMap = new TreeMap<Integer, Double>(comparatorB);
            sortedBearingMap.putAll(bearingMap);

            // some test to make sure our inintial segment is listed


            System.out.println(sortedBearingMap);
        } catch (Exception e) {
            System.err.println(e);
        }

        //return new GeoCords[2];
        }

    protected String drawTaxiwayBorders(double startLat, double startLon, double endLat,
            double endLon, float width) {
        double brng = Trig.getBearing(startLat, startLon, endLat, endLon);
        // need to know what other segments are originating or ending in this segment's points

        /*
        String returnString = new String();
        try {
        returnString = "UUDD " + getBoth(Trig.getPoint(startLat, startLon, width / 2, (brng + Math.PI / 2))) + " " +
        getBoth(Trig.getPoint(endLat, endLon, width / 2, (brng + Math.PI / 2))) + " taxiway" + "\n" +
        "UUDD " + getBoth(Trig.getPoint(startLat, startLon, width / 2, (brng - Math.PI / 2))) + " " +
        getBoth(Trig.getPoint(endLat, endLon, width / 2, (brng - Math.PI / 2))) + " taxiway";

        } catch (Exception e) {
        System.err.println("Error while tying to calculate taxyfay borders " + e);
        }
         */
        return "";

    }

    /** End document. */
    public void endDocument() {
        // will now iterate through tw segments
        Iterator<TaxiwaySegment> it = taxiwaySegmentsList.iterator();

        while (it.hasNext()) {
            // thisTWS.start and thisTWS.end will contain this tw segment points
            TaxiwaySegment thisTWS = it.next();

            getBorderRadialOffset(thisTWS.start, thisTWS.end);
        }

    } // endDocument()

    /** Start element. */
    public void startElement(String namespaceURI, String localName,
            String rawName, Attributes attrs) {

        // populating taxiwayPointsList arraylist with tw points data
        if (rawName.equals("TaxiwayPoint")) {
            try {
                taxiwayPointsList.add(Integer.parseInt(attrs.getValue("index")),
                        new TaxiwayPoint(Integer.parseInt(attrs.getValue("index")),
                        TaxiwayPoint.returnType(attrs.getValue("type")),
                        TaxiwayPoint.returnOrientation(attrs.getValue("orientation")),
                        Double.valueOf(attrs.getValue("lat")).doubleValue(),
                        Double.valueOf(attrs.getValue("lon")).doubleValue()));
            } catch (Exception e) {
                System.err.println(e);
            }
        }
        // drawing tw centerline - this will be prbably rewritten later
        if (rawName.equals("TaxiwayPath") &&
                (attrs.getValue("type").equalsIgnoreCase("TAXI"))) {

            System.out.println("UUDD " + getLat(taxiwayPointsList.get(getAsInt(attrs.getValue("start"))).lat) + " " +
                    getLon(taxiwayPointsList.get(getAsInt(attrs.getValue("start"))).lon) + " " +
                    getLat(taxiwayPointsList.get(getAsInt(attrs.getValue("end"))).lat) + " " +
                    getLon(taxiwayPointsList.get(getAsInt(attrs.getValue("end"))).lon) + " taxi_center");
        }

        // collecting tw segments data
        if (rawName.equals("TaxiwayPath") && //
                (attrs.getValue("type").equalsIgnoreCase("TAXI"))) {

            // populating taxiwaySegmentsList arraylist with TW segments data
            taxiwaySegmentsList.add(taxiwaySegmentCounter,
                    new TaxiwaySegment(getAsInt(attrs.getValue("start")),
                    getAsInt(attrs.getValue("end")),
                    Float.valueOf(attrs.getValue("width")),
                    attrs.getValue("name")));

            // creating segment index by tw point for reference
            Set l = taxiwayPointIndex.get(getAsInt(attrs.getValue("start")));
            if (l == null) {
                taxiwayPointIndex.put(getAsInt(attrs.getValue("start")), l = new HashSet<Integer>());
            }
            l.add(taxiwaySegmentCounter);
            taxiwayPointIndex.put(getAsInt(attrs.getValue("start")), l);

            Set l2 = taxiwayPointIndex.get(getAsInt(attrs.getValue("end")));
            if (l2 == null) {
                taxiwayPointIndex.put(getAsInt(attrs.getValue("end")), l2 = new HashSet<Integer>());
            }
            l2.add(taxiwaySegmentCounter);
            taxiwayPointIndex.put(getAsInt(attrs.getValue("end")), l2);


            // incrementing counter
            taxiwaySegmentCounter++;
        }

        /* simple tw border drawing - obsolete
        if (rawName.equals("TaxiwayPath") && //
        (attrs.getValue("type").equalsIgnoreCase("TAXI"))) {
        System.out.println(drawTaxiwayBorders(
        TaxiwayPointsList.get(getAsInt(attrs.getValue("start"))).lat,
        TaxiwayPointsList.get(getAsInt(attrs.getValue("start"))).lon,
        TaxiwayPointsList.get(getAsInt(attrs.getValue("end"))).lat,
        TaxiwayPointsList.get(getAsInt(attrs.getValue("end"))).lon,
        Float.valueOf(attrs.getValue("width")).floatValue()));
        }
         */

    }

    /** Characters. */
    public void characters(char ch[], int start, int length) {
        //System.out.print(new String(ch, start, length));
        } // characters(char[],int,int);

    /** Ignorable whitespace. */
    public void ignorableWhitespace(char ch[], int start, int length) {
        //characters(ch, start, length);
        } // ignorableWhitespace(char[],int,int);

    /** End element. */
    public void endElement(String namespaceURI, String localName,
            String rawName) {
        //System.out.print("</");
        //System.out.print(rawName);
        //System.out.print(">");
        } // endElement(String)

    /** Processing instruction. */
    public void processingInstruction(String target, String data) {
        System.out.print("<?");
        System.out.print(target);
        if (data != null && data.length() > 0) {
            System.out.print(' ');
            System.out.print(data);
        }
        System.out.print("?>");

    } // processingInstruction(String,String)

    //
    // ErrorHandler methods
    //
    /** Warning. */
    public void warning(SAXParseException ex) {
        System.err.println("[Warning] " +
                getLocationString(ex) + ": " +
                ex.getMessage());
    }

    /** Error. */
    public void error(SAXParseException ex) {
        System.err.println("[Error] " +
                getLocationString(ex) + ": " +
                ex.getMessage());
    }

    /** Fatal error. */
    public void fatalError(SAXParseException ex)
            throws SAXException {
        System.err.println("[Fatal Error] " +
                getLocationString(ex) + ": " +
                ex.getMessage());
        throw ex;
    }

    /** Returns a string of the location. */
    private String getLocationString(SAXParseException ex) {
        StringBuffer str = new StringBuffer();

        String systemId = ex.getSystemId();
        if (systemId != null) {
            int index = systemId.lastIndexOf('/');
            if (index != -1) {
                systemId = systemId.substring(index + 1);
            }
            str.append(systemId);
        }
        str.append(':');
        str.append(ex.getLineNumber());
        str.append(':');
        str.append(ex.getColumnNumber());

        return str.toString();
    } // getLocationString(SAXParseException):String

    /** Main program entry point. */
    public static void main(String argv[]) {
        System.out.println(argv[0]);
        if (argv.length == 0 ||
                (argv.length == 1 && argv[0].equals("-help"))) {
            System.out.println("\nfsxsct");
            System.exit(1);
        }
        App s1 = new App();
        s1.parseURI(argv[0]);
    } // main(String[])
    }
