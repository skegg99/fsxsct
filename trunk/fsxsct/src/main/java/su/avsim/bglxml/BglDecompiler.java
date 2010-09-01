/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package su.avsim.bglxml;

import java.io.*;
import java.util.*;

class BglSection {
    int sectionType;
    int sectionSubs;
    int sectionOffset;
    int sectionLength;
    
    public BglSection(int st, int ss, int so, int sl) {
        sectionType = st;
        sectionSubs = ss;
        sectionOffset = so;
        sectionLength = sl;
    }
    
}
/**
 *
 * @author dennis begun
 */
public class BglDecompiler {

    RandomAccessFile fin;

    protected short inv(short i) {
        return Short.reverseBytes(i);
    }

    protected int inv(int i) {
        return Integer.reverseBytes(i);
    }

    protected long inv(long i) {
        return Long.reverseBytes(i);
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

            List <BglSection> sections = new ArrayList<BglSection>();
            for (int i =0; i<numberOfSections ; i++) {
                int type = inv(fin.readInt());
                fin.skipBytes(4);
                int subs = inv(fin.readInt());
                int offset = inv(fin.readInt());
                int length = inv(fin.readInt());
                sections.add (new BglSection (type, subs, offset, length));
            }

            return new String();
        } catch (Exception e) {
            System.err.println("Error while parsing bgl file: " + e);
        }
        return "";
    }

    public BglDecompiler(String bglFileName) throws Exception {
        //this.bglFileName = bglFileName;
        fin = new RandomAccessFile(bglFileName, "r");


    }
}
