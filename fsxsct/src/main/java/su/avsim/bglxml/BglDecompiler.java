/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package su.avsim.bglxml;

import java.io.*;
import java.util.*;

class BglSection {

    int type;
    int subsectionsN; //number of records
    int offset;
    int size; // should always be subsectionsN x 0x10

    public BglSection(int type, int subsectionsN, int offset, int size) {
        this.type = type;
        this.subsectionsN = subsectionsN;
        this.offset = offset;
        this.size = size;
    }
}

class BglSubSection {

    int sectionId;
    int id;
    int recordsN;
    int offset;
    int size;

    public BglSubSection(int sectionId, int id, int recordsN, int offset, int size) {
        this.sectionId = sectionId;
        this.id = id;
        this.recordsN = recordsN;
        this.offset = offset;
        this.size = size;
    }
}

/**
 *
 * @author dennis begun
 */
public class BglDecompiler {

    RandomAccessFile fin;
    List<BglSection> sections = new ArrayList<BglSection>();
    List<BglSubSection> subSections = new ArrayList<BglSubSection>();
    Map<Integer, Set<Integer>> subSectionsIndex = new HashMap<Integer, Set<Integer>>();

    // inv methods corrects endianess
    protected short inv(short i) {
        return Short.reverseBytes(i);
    }

    protected int inv(int i) {
        return Integer.reverseBytes(i);
    }

    protected long inv(long i) {
        return Long.reverseBytes(i);
    }

    //public final float readFloat() throws IOException {
    //    return Float.intBitsToFloat(readInt());
    //}

    //this one does nothing really. added for consistency
    protected byte inv(byte i) {
        return i;
    }

    /**
     * this will decompile airportSubSection
     * @param subSection
     */
    public void decompileAirportSubSection(int subSectionIndex) throws IOException {
        BglSubSection subSection = subSections.get(subSectionIndex);
        fin.seek(subSection.offset); //move to subsection
        short ssid = inv (fin.readShort());
        int sssize = inv(fin.readInt());
    }

    /**
     * this will iterate airport section subsections (hopefully only one per file)
     * and pass subsection id to airport subsection decompiler
     * @param section section index
     */
    public void decompileAirportSection(int section) throws IOException {
        // iteration through subsections
        Iterator<Integer> it = subSectionsIndex.get(section).iterator();
        while (it.hasNext()) {
            int subSection = it.next();
            decompileAirportSubSection(subSection);
        }
    }

    public String decompile() {
        try {
            //bgl check 1
            if (inv(fin.readShort()) != 0x0201) { // short bgl id
                System.err.println("This is not bgl file");
                System.exit(1);
            }
            fin.skipBytes(2); //skip 2 B
            //bgl check 2
            if (inv(fin.readInt()) != 0x0038) { // size of header = 0x0038
                System.err.println("This is not bgl file 2");
                System.exit(1);
            }
            fin.skipBytes(12); //skip 12 B

            int numberOfSections = inv(fin.readInt()); //number of section pointers

            fin.seek(0x0038); //moving to section pointers block

            // collecting an ArrayList for BGL Sections
            for (int i = 0; i < numberOfSections; i++) {
                int type = inv(fin.readInt());
                fin.skipBytes(4);
                int subsectionsN = inv(fin.readInt());
                int offset = inv(fin.readInt());
                int size = inv(fin.readInt());
                sections.add(new BglSection(type, subsectionsN, offset, size));
            }

            // collecting an ArrayList for BGL Subsections
            for (int i = 0; i < sections.size(); i++) {
                fin.seek(sections.get(i).offset); // moving to the start of BGL section header
                if (sections.get(i).subsectionsN * 0x10 != sections.get(i).size) {
                    System.err.println("Section record size not equal to number of subsection * 0x10");
                }
                // interating inside section header - may contain several subsections
                for (int j = 0; j < sections.get(i).subsectionsN; j++) {
                    int id = inv(fin.readInt());
                    int recordsN = inv(fin.readInt());
                    int offset = inv(fin.readInt());
                    int size = inv(fin.readInt());
                    subSections.add(new BglSubSection(i, id, recordsN, offset, size));

                    // add subsection index by section
                    Set l = subSectionsIndex.get(i);
                    if (l == null) {
                        l = new HashSet<Integer>();
                    }
                    l.add(j);
                    subSectionsIndex.put(i, l);
                }
            }

            // the following part of decompiler will now only focus
            // on aiport section's subsections (hopefully only one per file)
            // which is identified by 0x0003 BglSection.type
            for (int i = 0; i < sections.size(); i++) {

                BglSection section = sections.get(i); //segment number
                switch (section.type) {
                    case 0x0003:
                        decompileAirportSection(i);
                        break;
                }
            }
            return new String();

        } catch (Exception e) {
            System.err.println("Error while parsing bgl file: " + e);
        }
        return "";
    }

    public BglDecompiler(String bglFileName) throws IOException {
        //this.bglFileName = bglFileName;
        fin = new RandomAccessFile(bglFileName, "r");


    }
}
