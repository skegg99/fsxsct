package su.avsim.fsxsct;

import java.util.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class App
        extends DefaultHandler {

    List<TaxiwayPoint> taxiwayPointsList = new ArrayList<TaxiwayPoint>();
    List<TaxiwaySegment> taxiwaySegmentsList = new ArrayList<TaxiwaySegment>();
    int taxiwaySegmentCounter = 0;

    
    // tw segments index indexed by poin
    Map<Integer, Set<Integer>> taxiwayPointIndex = new HashMap<Integer, Set<Integer>>(); 
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

    protected String splitDegrees(double coord) { // разложим градусы с дес€тыми на гр/мин/c
        String coordString = new String();
        try {
            double degrees = Math.floor(coord);
            double tempMinutesLeft = (coord - degrees) * 60.0d;
            double minutes = Math.floor(tempMinutesLeft);
            double seconds = (tempMinutesLeft - minutes) * 60.0d; // с дол€ми
            // используем американскую локаль, чтобы дес€тые отдел€лись точкой
            coordString = String.format(Locale.US, "%1$03.0f.%2$02.0f.%3$06.3f", degrees, minutes, seconds);
        } catch (Exception e) {
            System.err.println("ќшибка при переводе координаты " + coordString + " " + e);
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

    protected Double[] getBorderRadialOffset(int thisPoint, int otherPoint) {
        // need to know what other segments are originating or ending in this segment's point
        Set<Integer> otherSegments = taxiwayPointIndex.get(thisPoint);
        int thisSegment = -1;
        boolean isDirect;
        try {
            // this treemap cointains bearings from this point for all crossing segments, indexed by segment number
            // it is sorted by bearing by comparator
            Map<Integer, Double> bearingMap = new TreeMap<Integer, Double>();
            ValueComparator comparatorB = new ValueComparator(bearingMap);
            // iterating through index numbers of all segments which are connected to this point

            Iterator<Integer> it2 = otherSegments.iterator();
            while (it2.hasNext()) {
                int segmentCounter = it2.next(); //segment number
                int segmentStart = taxiwaySegmentsList.get(segmentCounter).start;
                int segmentEnd = taxiwaySegmentsList.get(segmentCounter).end;
                // determining current segment
                if ((segmentStart == thisPoint && segmentEnd == otherPoint)
                        || (segmentStart == otherPoint && segmentEnd == thisPoint)) {
                    thisSegment = segmentCounter;
                }

                if ((segmentStart == thisPoint && segmentEnd == otherPoint)) {
                    isDirect = true;
                }

                if ((segmentStart == otherPoint && segmentEnd == thisPoint)) {
                    isDirect = false;
                }

                //System.out.print(segmentCounter + " ");
                // determine the point index on the outer end of segment
                int outerPoint = thisPoint == segmentStart ? segmentEnd : segmentStart;
                // get the bearing from this point to outer point
                double segmentBearing = Trig.getBearing(
                        taxiwayPointsList.get(thisPoint).lat,
                        taxiwayPointsList.get(thisPoint).lon,
                        taxiwayPointsList.get(outerPoint).lat,
                        taxiwayPointsList.get(outerPoint).lon);
                // put it to bearingMap
                bearingMap.put(segmentCounter, Trig.to360(segmentBearing));
            }
            TreeMap<Integer, Double> sortedBearingMap = new TreeMap<Integer, Double>(comparatorB);
            sortedBearingMap.putAll(bearingMap);

            // check if thisSegment was found
            if (thisSegment == -1) {
                System.out.println("ERROR: thisSegment not found for segment "
                        + thisPoint + "---->" + otherPoint);
            }
            // processing the treemap
            if (sortedBearingMap.size() >= 2) { // at least two adjacent segments
                // counterclockwize and clockwize adjacent segments RAD offset
                double ccwSegment = sortedBearingMap.lowerKey(thisSegment) != null ? sortedBearingMap.lowerEntry(thisSegment).getValue() : sortedBearingMap.lastEntry().getValue();
                double cwSegment = sortedBearingMap.higherKey(thisSegment) != null ? sortedBearingMap.higherEntry(thisSegment).getValue() : sortedBearingMap.firstEntry().getValue();
                double thSegment = sortedBearingMap.get(thisSegment);

                // corresponding segment id and width as we need to adjust angle if segments are of different width
                TaxiwaySegment ccwSegmentId = new TaxiwaySegment();
                TaxiwaySegment cwSegmentId = new TaxiwaySegment();
                TaxiwaySegment thSegmentId = new TaxiwaySegment();
                try {
                    ccwSegmentId = sortedBearingMap.lowerKey(thisSegment) != null
                            ? taxiwaySegmentsList.get(sortedBearingMap.lowerEntry(thisSegment).getKey())
                            : taxiwaySegmentsList.get(sortedBearingMap.lastEntry().getKey());

                    cwSegmentId = sortedBearingMap.higherKey(thisSegment) != null
                            ? taxiwaySegmentsList.get(sortedBearingMap.higherEntry(thisSegment).getKey())
                            : taxiwaySegmentsList.get(sortedBearingMap.firstEntry().getKey());

                    thSegmentId = taxiwaySegmentsList.get(thisSegment);
                } catch (Exception e) {

                    System.err.println("Error while getting segmentID: " + e);
                }

                float ccwSegmentWidth = ccwSegmentId.width;
                float cwSegmentWidth = cwSegmentId.width;
                float thSegmentWidth = thSegmentId.width;

                // so if segments are of same width we just use a bisection, that will do
                if (ccwSegmentWidth == thSegmentWidth && cwSegmentWidth == thSegmentWidth) {
                    return new Double[]{(Trig.to360(thSegment - ccwSegment)) / 2, Trig.to360((cwSegment - thSegment)) / 2};
                }

                // on the other hand if width is different we need a workaround
                // we will be finding coords or crosspoint for tw borders, then calculate the bearing to return

                // first we will determine each segments outer point
                int ccwSegmentOuterPoint = 0;
                int cwSegmentOuterPoint = 0;
                int thSegmentOuterPoint = 0;
                try {
                    ccwSegmentOuterPoint = thisPoint == ccwSegmentId.start ? ccwSegmentId.end : ccwSegmentId.start;
                    cwSegmentOuterPoint = thisPoint == cwSegmentId.start ? cwSegmentId.end : cwSegmentId.start;
                    thSegmentOuterPoint = thisPoint == thSegmentId.start ? thSegmentId.end : thSegmentId.start;
                } catch (Exception e) {

                    System.err.println("Error while calculating outerpoint: " + e);
                }
                // next for each segment we will find a point to start border path
                // for thisSegment there will be 2 points -
                // one adjasent to ccw and one - to cw segments
                GeoCords thSegmentOuterBorderStartForCcw = new GeoCords();
                GeoCords thSegmentOuterBorderStartForCw = new GeoCords();
                GeoCords ccwSegmentOuterBorderStart = new GeoCords();
                GeoCords cwSegmentOuterBorderStart = new GeoCords();

                thSegmentOuterBorderStartForCcw = Trig.getPoint(
                        taxiwayPointsList.get(thSegmentOuterPoint).lat,
                        taxiwayPointsList.get(thSegmentOuterPoint).lon,
                        thSegmentWidth,
                        thSegment - Math.PI / 2);

                thSegmentOuterBorderStartForCw = Trig.getPoint(
                        taxiwayPointsList.get(thSegmentOuterPoint).lat,
                        taxiwayPointsList.get(thSegmentOuterPoint).lon,
                        thSegmentWidth,
                        thSegment + Math.PI / 2);

                ccwSegmentOuterBorderStart = Trig.getPoint(
                        taxiwayPointsList.get(ccwSegmentOuterPoint).lat,
                        taxiwayPointsList.get(ccwSegmentOuterPoint).lon,
                        ccwSegmentWidth,
                        ccwSegment + Math.PI / 2);

                cwSegmentOuterBorderStart = Trig.getPoint(
                        taxiwayPointsList.get(cwSegmentOuterPoint).lat,
                        taxiwayPointsList.get(cwSegmentOuterPoint).lon,
                        cwSegmentWidth,
                        cwSegment - Math.PI / 2);

                // now we will calculate intersection points for tw border
                // via getIntersectionCoords method

                GeoCords ccwIntersection = Trig.getIntersectionCoords(
                        thSegmentOuterBorderStartForCcw,
                        Trig.getBackBearing(thSegment),
                        ccwSegmentOuterBorderStart,
                        Trig.getBackBearing(ccwSegment));
                GeoCords cwIntersection = Trig.getIntersectionCoords(
                        thSegmentOuterBorderStartForCw,
                        Trig.getBackBearing(thSegment),
                        cwSegmentOuterBorderStart,
                        Trig.getBackBearing(cwSegment));

                try {
                    if (cwIntersection != null && ccwIntersection != null) {
                        Double[] toReturn = new Double[]{thSegment - Double.valueOf(Trig.getBearing(
                            taxiwayPointsList.get(thisPoint).lat,
                            taxiwayPointsList.get(thisPoint).lon,
                            ccwIntersection.lat,
                            ccwIntersection.lon)).floatValue(),
                            Double.valueOf(Trig.getBearing(
                            taxiwayPointsList.get(thisPoint).lat,
                            taxiwayPointsList.get(thisPoint).lon,
                            cwIntersection.lat,
                            cwIntersection.lon)).floatValue() - thSegment};

                        return toReturn;
                    }
                } catch (Exception e) {

                    System.err.println("Error while trying to calculate bearing and return it: " + e);
                    System.err.println(cwIntersection.lat
                            + cwIntersection.lon);
                }
            }

            /*
            if (sortedBearingMap.size() == 2) {
            // other segment number
            double otherSegment = sortedBearingMap.lowerKey(thisSegment) != null ? sortedBearingMap.lowerEntry(thisSegment).getValue() : sortedBearingMap.lastEntry().getValue();
            double thSegment = sortedBearingMap.get(thisSegment);
            return new Double[]{(Trig.to360(thSegment - otherSegment)) / 2,
            (Trig.to360(otherSegment - thSegment)) / 2};
            //return new Double[]{Math.PI/6, Math.PI/6};
            }
             */

            //System.out.println(sortedBearingMap);
        } catch (Exception e) {

            System.err.println("Error while trying to calculate border RAD offset: " + e);
        }
        // if it's a only segment to this point = 90deg
        return new Double[]{-Math.PI / 2, Math.PI / 2};
    }

    protected String drawTaxiwayBorders(TaxiwaySegment thisSegment) {
        double startLat = taxiwayPointsList.get(thisSegment.start).lat;
        double startLon = taxiwayPointsList.get(thisSegment.start).lon;
        double endLat = taxiwayPointsList.get(thisSegment.end).lat;
        double endLon = taxiwayPointsList.get(thisSegment.end).lon;
        float width = thisSegment.width;
        double brng = Trig.getBearing(
                taxiwayPointsList.get(thisSegment.start).lat,
                taxiwayPointsList.get(thisSegment.start).lon,
                taxiwayPointsList.get(thisSegment.end).lat,
                taxiwayPointsList.get(thisSegment.end).lon);

        Double[] offsetStart = getBorderRadialOffset(thisSegment.start, thisSegment.end);
        Double[] offsetEnd = getBorderRadialOffset(thisSegment.end, thisSegment.start);

        double ang1 = Trig.getBackBearing(brng) - offsetStart[0];
        double ang2 = brng + offsetEnd[1];
        double ang3 = Trig.getBackBearing(brng) + offsetStart[1];
        double ang4 = brng - offsetEnd[0];

        float dist1 = Double.valueOf((width / 2) / Math.sin(offsetStart[0])).floatValue();
        float dist2 = Double.valueOf((width / 2) / Math.sin(offsetEnd[1])).floatValue();
        float dist3 = Double.valueOf((width / 2) / Math.sin(offsetStart[1])).floatValue();
        float dist4 = Double.valueOf((width / 2) / Math.sin(offsetEnd[0])).floatValue();

        //System.out.println(offsetStart[0] + " " + offsetStart[1]);
        String returnString = new String();
        try {
            returnString =
                    "UUDD "
                    + getBoth(Trig.getPoint(startLat, startLon, dist1, ang1)) + " "
                    + getBoth(Trig.getPoint(endLat, endLon, dist2, ang2)) + " taxiway" + "\n"
                    + "UUDD "
                    + getBoth(Trig.getPoint(startLat, startLon, dist3, ang3)) + " "
                    + getBoth(Trig.getPoint(endLat, endLon, dist4, ang4)) + " taxiway";

        } catch (Exception e) {
            System.err.println("Error while trying to calculate taxyway borders " + e);
        }
        return returnString;

    }

    /** End document. */
    public void endDocument() {
        // will now iterate through tw segments
        Iterator<TaxiwaySegment> it = taxiwaySegmentsList.iterator();

        while (it.hasNext()) {
            // thisTWS.start and thisTWS.end will contain this tw segment points
            TaxiwaySegment thisTWS = it.next();

            System.out.println(drawTaxiwayBorders(thisTWS));
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
        if (rawName.equals("TaxiwayPath")
                && (attrs.getValue("type").equalsIgnoreCase("TAXI"))) {

            System.out.println("UUDD " + getLat(taxiwayPointsList.get(getAsInt(attrs.getValue("start"))).lat) + " "
                    + getLon(taxiwayPointsList.get(getAsInt(attrs.getValue("start"))).lon) + " "
                    + getLat(taxiwayPointsList.get(getAsInt(attrs.getValue("end"))).lat) + " "
                    + getLon(taxiwayPointsList.get(getAsInt(attrs.getValue("end"))).lon) + " taxi_center");
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
        System.err.println("[Warning] "
                + getLocationString(ex) + ": "
                + ex.getMessage());
    }

    /** Error. */
    public void error(SAXParseException ex) {
        System.err.println("[Error] "
                + getLocationString(ex) + ": "
                + ex.getMessage());
    }

    /** Fatal error. */
    public void fatalError(SAXParseException ex)
            throws SAXException {
        System.err.println("[Fatal Error] "
                + getLocationString(ex) + ": "
                + ex.getMessage());
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
        if (argv.length == 0
                || (argv.length == 1 && argv[0].equals("-help"))) {
            System.out.println("\nfsxsct");
            System.exit(1);
        }
        App s1 = new App();
        s1.parseURI(argv[0]);
    } // main(String[])
}
