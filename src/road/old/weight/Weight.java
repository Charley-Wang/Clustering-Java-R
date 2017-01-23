/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.old.weight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/*
parameters: C://Users//li//Desktop//WXS//Chifan//17_Road//FilteredRawData fixSeg0d01Mile.csv weight.csv index.csv
*/

/**
 *
 * @author li
 */
public class Weight {
    public static final int MAX_NUM_FIELDS = 16;
    public static Segments[] segments;
    public static double[] weight = new double[MAX_NUM_FIELDS];
    
    public static void main(String [] args) throws IOException {
        String dir = args[0];
        String sgFile = args[1];
        String wtFile = args[2];
        String idxFile = args[3];
        
        readSegments(dir, sgFile);
        readWeight(dir, wtFile);
        setMerageable();
        calIndex();
        saveIndex(dir + "//" + idxFile);
        
        combineSameIndex(0.0);
        saveIndex(dir + "//comb_" + idxFile);
    }
    
    public static void saveIndex(String fileName) throws IOException {
        Segments seg;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, false)));
        
        out.print("route,begMP,endMP,index,incFreq,preMerg,nxtMerg");
        out.println();
        
        for (int ii = 0; ii < segments.length; ii++) {
            seg = segments[ii];
            if (seg.used == false) continue;
            out.print(seg.route);
            out.print("," + seg.begMP);
            out.print("," + seg.endMP);
            out.print("," + seg.index);
            out.print("," + seg.fields[MAX_NUM_FIELDS - 1]);
            out.print("," + seg.preMerg);
            out.print("," + seg.nxtMerg);
            out.println();
        }

        out.close();
    }
    
    public static void combineSameIndex(double minDetIndex) {
        int lastRouteID = -100, lastII = -100;
        double lastIndex = Double.MIN_VALUE, detIndex;
        double lastEndMP = -100;       
        boolean update = false;
        double accFreq = 0;
        boolean preMerg = true;
        boolean nxtMerg = true;
        for (int ii = 0; ii < segments.length; ii++) {
            detIndex = Math.abs(segments[ii].index - lastIndex);
            if (segments[ii].route != lastRouteID || segments[ii].begMP != lastEndMP || detIndex > minDetIndex || ii == segments.length - 1) {
            //if (segments[ii].route != lastRouteID || segments[ii].begMP != lastEndMP || segments[ii].index != lastIndex || ii == segments.length - 1) {
                if (update) {
                    segments[lastII].preMerg = preMerg;
                    segments[lastII].nxtMerg = nxtMerg;
                    segments[lastII].endMP = lastEndMP;
                    segments[lastII].fields[MAX_NUM_FIELDS - 1] = accFreq;
                }
                lastRouteID = segments[ii].route;
                lastII = ii;
                lastIndex = segments[ii].index;
                lastEndMP = segments[ii].endMP;
                update = false;
                accFreq = segments[ii].fields[MAX_NUM_FIELDS - 1];
                preMerg = segments[ii].preMerg;
                nxtMerg = segments[ii].nxtMerg;
            }
            else {
                update = true;
                segments[ii].used = false;
                accFreq += segments[ii].fields[MAX_NUM_FIELDS - 1];
                if (segments[ii].preMerg == false) preMerg = false;
                if (segments[ii].nxtMerg == false) nxtMerg = false;
                lastRouteID = segments[ii].route;
                lastEndMP = segments[ii].endMP;
            }
        }
    }
    
    public static void calIndex() {
        for (int ii = 0; ii < segments.length; ii++) {
            for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                segments[ii].index += segments[ii].fields[jj] * weight[jj];
            }
        }
    }
    
    public static void setMerageable() {
        Segments seg;
        int lastRouteID = -100;
        for (int ii = 0; ii < segments.length; ii++) {
            seg = segments[ii];
            if (seg.route != lastRouteID) {
                if (ii != 0) {
                    segments[ii - 1].nxtMerg = false;
                }
                seg.preMerg = false;
            }
            lastRouteID = seg.route;
        }
        segments[segments.length - 1].nxtMerg = false;
    }
    
    public static void readWeight(String dir, String wtFile) {
        int ii = -1;
        Path vcPath = Paths.get(dir + "//" + wtFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry;
            while((entry = vcIn.readLine()) != null) {
                ii++;
                String txts[] = entry.split(",");
                weight[ii] = Double.valueOf(txts[1]);
            }
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + wtFile);
        }
    }
    
    public static void readSegments(String dir, String sgFile) {
        int numSeg = 0, ii = -1;
        Path vcPath = Paths.get(dir + "//" + sgFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) numSeg++;
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + sgFile);
        }
        
        segments = new Segments[numSeg];
        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) {
                ii++;
                Segments seg = new Segments(MAX_NUM_FIELDS);
                String[] txts = entry.split(",");
                seg.route = Integer.valueOf(txts[0]);
                seg.begMP = Double.valueOf(txts[1]);
                seg.endMP = Double.valueOf(txts[2]);
                for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) seg.fields[jj] = Double.valueOf(txts[3 + jj]);
                segments[ii] = seg;
            }
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + sgFile);
        }
    }
}
