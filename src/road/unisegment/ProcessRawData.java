/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.unisegment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * This file is coded before the discussion of July 3, 2015
 * This file is replaced by BaseSegmentation.java on July 9, 2015
 * @author Xingsheng Wang on June 25, 2015
 *          email: charleyxswang@gmail.com
 */
public class ProcessRawData {
    public static final int MAX_NUM_SEGMENTS = 6;
    public static Map dictSegments = new HashMap();
    public static LinkedList<HorizontalCurveSegment> hcSegments = new LinkedList<HorizontalCurveSegment>();
    public static LinkedList<VerticalCurveSegment> vcSegments = new LinkedList<VerticalCurveSegment>();
    public static LinkedList<ShoulderSegment> shoulderSegments = new LinkedList<ShoulderSegment>();
    public static LinkedList<LaneSegment> laneSegments = new LinkedList<LaneSegment>();
    public static LinkedList<IncidentSegment> incidents = new LinkedList<IncidentSegment>();
    public static LinkedList<ADTSegment> adtSegments = new LinkedList<ADTSegment>();
    
    // all fields, variables 
    public static final int MAX_NUM_FIELDS = 16;
    public static Field[][] allFields = new Field[MAX_NUM_FIELDS][];
    public static String[] allFieldsName = new String[MAX_NUM_FIELDS];
    public static boolean[] fieldsLnNorm = new boolean[MAX_NUM_FIELDS];
    public static double[] fieldsLnNormMultiple = new double[MAX_NUM_FIELDS];
    public static double[] fieldsLnNormAdd = new double[MAX_NUM_FIELDS];
    public static StatisticalVariables[] allFieldsStat = new StatisticalVariables[MAX_NUM_FIELDS];
    //                      0     1        2
    public static Field[] hcMSE, hcLen, hcRecR;
    //                      3     4    5
    public static Field[] vcLen, pga, pgb;
    //                      6       7       8       9
    public static Field[] incLan, decLan, incWid, decWid;
    //                     10     11    12     13
    public static Field[] lWid, lcWid, rcWid, rWid;
    //                     14
    public static Field[] adt;
    
    // for incident - the value to be predicted   15
    public static Field[] ind;
        
    public static double lengthSegmentation;
    public static int numRoute;
    public static Map dictRouteID_SeqNumMinMaxMP = new HashMap();
    public static Route[] route;
    
    /*
    parameters for running
    
    C://Users//li//Desktop//WXS//Chifan//17_Road//FilteredRawData horizontal.csv vertical.csv shoulder.csv lanes.csv incidents.csv adt.csv 0.01 fixSeg0d01Mile.csv
    
    */
    
    /**
     * 
     * @param args[0]:  the folder name of the data
     *         args[1]:  the file name of horizontal geometry information
     *         args[2]:  the file name of vertical geometry information
     *         args[3]:  the file name of shoulder geometry information
     *         args[4]:  the file name of lane geometry information
     *         args[5]:  the file name of incident information
     *         args[6]:  the file name of ADT information
     *         args[7]:  the length of fixed length segmentation
     *         args[8]:  the output file name of segmentation
     */
//    public static void main(String [] args) throws IOException {
//        String dir = args[0];
//        String hcFile = args[1];
//        String vcFile = args[2];
//        String sdFile = args[3];
//        String lnFile = args[4];
//        String idFile = args[5];
//        String adtFile = args[6];
//        lengthSegmentation = Double.valueOf(args[7]);
//        String segFile = args[8];
//                
//        // read, process, and filter the raw data from geometry and incident tables
//        assembleHorizontalSegments(dir, hcFile);
//        assembleVerticalSegments(dir, vcFile);
//        assembleShoulderSegments(dir, sdFile);
//        assembleLaneSegments(dir, lnFile);
//        assembleIncidents(dir, idFile);
//        assembleADTSegments(dir, adtFile);
//        
//        setDictSegments();
//        setRouteID_SeqNumMinMaxMPMap();
//        
//        // fill the fildes from geometry and incident classes
//        assignHorizontalFields();
//        assignVerticalFields();
//        assignLaneFields();
//        assignShoulderFields();
//        assignIncidentFields();
//        assignADTFields();
//        
//        setAllFields();
//        calStaticalVariables();
//        normalize();
//        
//        segmentation();
//        saveSegmentation(dir + "//" + segFile);
//    }
    
    public static void specialLn() {
        // This data from the real data and fitting
        
        
    }
    
    /*
    private static void saveSegmentation(String fileName) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, false)));
        
        out.print("route,begMP,endMP");
        for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
            out.print("," + allFieldsName[kk]);
        }
        out.println();
        
        for (int ii = 0; ii < route.length; ii++) {
            Route rt = route[ii];
            for (int jj = 0; jj < rt.fixSegments.length; jj++) {
                FixLengthSegment seg = rt.fixSegments[jj];
                out.print(seg.route);
                out.print("," + seg.beginMP);
                out.print("," + seg.endMP);
                double[] fd = seg.fields;
                for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
                    out.print("," + fd[kk]);
                }
                out.println();
            }
        }
        
        out.close();
    }
    */
    
    /**
     * tested
     */
    /*
    private static void segmentation() {
        int routeID, nFrag, len, nFragBeg, nFragEnd;
        double minMP, maxMP, dist;
        for (int ii = 0; ii < route.length; ii++) {
            routeID = route[ii].route;
            minMP = route[ii].minMP;
            maxMP = route[ii].maxMP;
            // for the testing
            // System.out.println(routeID + ": " + minMP + ", " + maxMP + ", ");
            
            nFrag = (int)((maxMP - minMP)/lengthSegmentation + 1.0);
            FixLengthSegment[] frags = new FixLengthSegment[nFrag];
            for (int jj = 0; jj < nFrag; jj++) {
                FixLengthSegment frag = new FixLengthSegment(MAX_NUM_FIELDS);
                frag.route = routeID;
                frag.beginMP = minMP + jj * lengthSegmentation;
                frag.endMP = minMP + (jj + 1) * lengthSegmentation;
                frags[jj] = frag;
            }
            
            for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                Field[] fds = allFields[jj];
                len = fds.length;
                for (int kk = 0; kk < len; kk++) {
                    Field fd = fds[kk]; 
                    if (routeID == fd.SR) {
                        nFragBeg = (int)((fd.begMP - minMP)/lengthSegmentation);
                        nFragEnd = (int)((fd.endMP - minMP)/lengthSegmentation);
                        for (int mm = nFragBeg; mm <= nFragEnd; mm++) {
                            if (mm == nFragBeg && mm == nFragEnd) {
                                dist = fd.endMP - fd.begMP;
                            }
                            else if (mm == nFragBeg && mm != nFragEnd) {
                                dist = (nFragBeg + 1) * lengthSegmentation + minMP - fd.begMP;
                            }
                            else if (mm != nFragBeg && mm == nFragEnd) {
                                dist = fd.endMP - (nFragEnd * lengthSegmentation + minMP);
                            }
                            else {
                                dist = lengthSegmentation;
                            }
                            if (fd.begMP == fd.endMP) {
                                frags[mm].fields[jj] += fd.value;
                            }
                            else {
                                frags[mm].fields[jj] += fd.value * dist / (fd.endMP - fd.begMP);
                            }
                        }
                    }
                }
            }
            
            route[ii].fixSegments = frags;
        }
    }
    */
    
    /**
     * tested for the fields of hcMSE, hcLen, and adt
     * only for fields 1 to 14, not including incident
     */
    public static void normalize() {
        int len;
        double det;
        
        for (int ii = 0; ii < MAX_NUM_FIELDS - 1; ii++) {
            Field[] fd = allFields[ii];
            len = fd.length;
            det = allFieldsStat[ii].max - allFieldsStat[ii].min;
            
            if (det == 0 || det == 0.0) continue;
            for (int jj = 0; jj < len; jj++) fd[jj].value1 = (fd[jj].value1 - allFieldsStat[ii].min) / det;
        }
    }
    
    /**
     * tested for horizontal and shoulder fields
     */
    private static void calStaticalVariables() {
        double min, max, mean, SD, len, val;
        //double minP, maxP, minN, maxN;
        
        for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
            StatisticalVariables sv = new StatisticalVariables();
            //sv.containZero = false;
            
            Field[] fd = allFields[ii];
            len = fd.length;
            if (len == 0) {
                min = max = 0;
                //minP = maxP = 0;
                //minN = maxN = 0;
            }
            else {
                min = fd[0].value0;
                max = fd[0].value0;
                //minP = Double.MAX_VALUE;
                //maxP = 0;
                //minN = 0;
                //maxN = -1.0 * Double.MAX_VALUE;
            }
            mean = 0;
            SD = 0;
            
            for (int jj = 0; jj < len; jj++) {
                val = fd[jj].value0;
                mean += val / len;
                if (val > max) max = val;
                if (val < min) min = val;
                //if (val == 0 || val == 0.0) sv.containZero = true;
                //else if (val > 0.0) {
                //    if (val > maxP) maxP = val;
                //    if (val < minP) minP = val;
                //} 
                //else {
                //    if (val > maxN) maxN = val;
                //   if (val < minN) minN = val;
                //}
            }
            
            for (int jj = 0; jj < len; jj++) {
                val = fd[jj].value0 - mean;
                SD += val * val;
            }
            if (len == 0) SD = 0.0;
            else if (len == 1) SD = SD;
            else SD = Math.sqrt(SD / (len - 1.0));
            
            sv.min = min;
            sv.max = max;
            sv.mean = mean;
            sv.SD = SD;
            sv.fieldName = allFieldsName[ii];
            
            allFieldsStat[ii] = sv;
        }
    }
    
    /**
     * tested
     */
    private static void setAllFields() {
        allFields[0] = hcMSE;       allFieldsName[0] = "hcMSE";
        allFields[1] = hcLen;       allFieldsName[1] = "hcLen";
        allFields[2] = hcRecR;      allFieldsName[2] = "hcRecR";
        allFields[3] = vcLen;       allFieldsName[3] = "vcLen";
        allFields[4] = pga;         allFieldsName[4] = "pga";
        allFields[5] = pgb;         allFieldsName[5] = "pgb";
        allFields[6] = incLan;      allFieldsName[6] = "incLan";
        allFields[7] = decLan;      allFieldsName[7] = "decLan";
        allFields[8] = incWid;      allFieldsName[8] = "incWid";
        allFields[9] = decWid;      allFieldsName[9] = "decWid";
        allFields[10] = lWid;       allFieldsName[10] = "lWid";
        allFields[11] = lcWid;      allFieldsName[11] = "lcWid";
        allFields[12] = rcWid;      allFieldsName[12] = "rcWid";
        allFields[13] = rWid;       allFieldsName[13] = "rWid";
        allFields[14] = adt;        allFieldsName[14] = "adt";
        allFields[15] = ind;        allFieldsName[15] = "ind";
    }
        
    /**
     * set dictionary {route id : [seqNum, minMP, maxMP, 
     *                             freq in hcSegments, freq in vcSegments, ...]} for each route
     * variable name: dictRouteID_SeqNumMinMaxMP
     * here, route also be built
     * routes of 2, 3, and 5 have been tested.
     * 
     */
    private static void setRouteID_SeqNumMinMaxMPMap() {
        int routeID;
        double begMP, endMP, temp;
        double[] SeqNumMinMaxMP;
        LinkedList<BaseSegment> bsLink;
        BaseSegment bs;
        
        numRoute = -1;
        for (int ii = 0; ii < MAX_NUM_SEGMENTS; ii++) {
            bsLink = (LinkedList<BaseSegment>)dictSegments.get(ii);
            for (int jj = 0; jj < bsLink.size(); jj++) {
                bs = (BaseSegment)bsLink.get(jj);
                routeID = bs.route;
                begMP = bs.beginMP;
                endMP = bs.endMP;
                
                if (endMP < begMP) {
                    // !!! change the raw data if it is not resonable
                    temp = begMP;
                    begMP = endMP;
                    endMP = temp;
                }
                 
                if (dictRouteID_SeqNumMinMaxMP.containsKey(routeID)) {
                    SeqNumMinMaxMP = (double[])dictRouteID_SeqNumMinMaxMP.get(routeID);
                    if (begMP < SeqNumMinMaxMP[1]) SeqNumMinMaxMP[1] = begMP;
                    if (endMP > SeqNumMinMaxMP[2]) SeqNumMinMaxMP[2] = endMP;
                    SeqNumMinMaxMP[ii + 3] = SeqNumMinMaxMP[ii + 3] + 1;
                    dictRouteID_SeqNumMinMaxMP.replace(routeID, SeqNumMinMaxMP);
                }
                else {
                    numRoute++;
                    SeqNumMinMaxMP = new double[3 + MAX_NUM_SEGMENTS];
                    SeqNumMinMaxMP[0] = numRoute;
                    SeqNumMinMaxMP[1] = begMP;
                    SeqNumMinMaxMP[2] = endMP;
                    for (int kk = 0; kk < MAX_NUM_SEGMENTS; kk++) SeqNumMinMaxMP[kk + 3] = 0;
                    SeqNumMinMaxMP[ii + 3] = SeqNumMinMaxMP[ii + 3] + 1;
                    dictRouteID_SeqNumMinMaxMP.put(routeID, SeqNumMinMaxMP);
                }
            }
        }
        
        // delete route with only one kind of field
        int rID;
        numRoute = 0;
        Iterator rt = dictRouteID_SeqNumMinMaxMP.keySet().iterator();
        while (rt.hasNext()) {
           rID = (int)rt.next();
           SeqNumMinMaxMP = (double[])dictRouteID_SeqNumMinMaxMP.get(rID);
           int zeroNum = 0;
           for (int kk = 0; kk < MAX_NUM_SEGMENTS; kk++) {
               if (SeqNumMinMaxMP[kk + 3] == 0.0 || SeqNumMinMaxMP[kk + 3] == 0.0) zeroNum++;
           }
           if (zeroNum == 0) numRoute++;
        }
        
        route = new Route[numRoute];
        
        int num = -1;
        rt = dictRouteID_SeqNumMinMaxMP.keySet().iterator();
        while (rt.hasNext()) {
           rID = (int)rt.next();
           SeqNumMinMaxMP = (double[])dictRouteID_SeqNumMinMaxMP.get(rID);
           int zeroNum = 0;
           for (int kk = 0; kk < MAX_NUM_SEGMENTS; kk++) {
               if (SeqNumMinMaxMP[kk + 3] == 0.0 || SeqNumMinMaxMP[kk + 3] == 0.0) zeroNum++;
           }
           if (zeroNum == 0) {
               num++;
               Route route1 = new Route();
               route1.freq = SeqNumMinMaxMP;
               route1.route = rID;
               route1.minMP = SeqNumMinMaxMP[1];
               route1.maxMP = SeqNumMinMaxMP[2];
               route[num] = route1;
           }
        }
    }
    
    /**
     * tested
     */
    private static void setDictSegments() {
        dictSegments.put(0, hcSegments);
        dictSegments.put(1, vcSegments);
        dictSegments.put(2, shoulderSegments);
        dictSegments.put(3, laneSegments);
        dictSegments.put(4, incidents);
        dictSegments.put(5, adtSegments);
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 2000, and the last
     */
    private static void assignHorizontalFields() {
        hcMSE = new Field[hcSegments.size()];
        hcLen = new Field[hcSegments.size()];
        hcRecR = new Field[hcSegments.size()];
        
        HorizontalCurveSegment hc;
        for (int ii = 0; ii < hcSegments.size(); ii++) {
            hc = hcSegments.get(ii);
            
            Field f = new Field();
            f.SR = hc.route;
            f.begMP = hc.beginMP;
            f.endMP = hc.endMP;
            f.value0 = hc.hcMSE;
            hcMSE[ii] = f;
            
            f = new Field();
            f.SR = hc.route;
            f.begMP = hc.beginMP;
            f.endMP = hc.endMP;
            f.value0 = hc.hcLength;
            hcLen[ii] = f;
            
            f = new Field();
            f.SR = hc.route;
            f.begMP = hc.beginMP;
            f.endMP = hc.endMP;
            //f.value0 = hc.hcRecR;
            f.value0 = hc.hcRadius;
            hcRecR[ii] = f;
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 4000, and the last
     */
    private static void assignVerticalFields() {
        vcLen = new Field[vcSegments.size()];
        pga = new Field[vcSegments.size()];
        pgb = new Field[vcSegments.size()];
        
        VerticalCurveSegment vc;
        for (int ii = 0; ii < vcSegments.size(); ii++) {
            vc = vcSegments.get(ii);
            
            Field f = new Field();
            f.SR = vc.route;
            f.begMP = vc.beginMP;
            f.endMP = vc.endMP;
            f.value0 = vc.vcLength;
            vcLen[ii] = f;
            
            f = new Field();
            f.SR = vc.route;
            f.begMP = vc.beginMP;
            f.endMP = vc.endMP;
            f.value0 = vc.pga;
            pga[ii] = f;
            
            f = new Field();
            f.SR = vc.route;
            f.begMP = vc.beginMP;
            f.endMP = vc.endMP;
            f.value0 = vc.pgb;
            pgb[ii] = f;
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 4000, and the last
     */
    private static void assignLaneFields() {
        incLan = new Field[laneSegments.size()];
        decLan = new Field[laneSegments.size()];
        incWid = new Field[laneSegments.size()];
        decWid = new Field[laneSegments.size()];
        
        LaneSegment ln;
        for (int ii = 0; ii < laneSegments.size(); ii++) {
            ln = laneSegments.get(ii);
            
            Field f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.increasingLanes;
            incLan[ii] = f;
            
            f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.decreasingLanes;
            decLan[ii] = f;
            
            f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.increasingWidth;
            incWid[ii] = f;
            
            f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.decreasingWidth;
            decWid[ii] = f;
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 3, 1000, and the last
     */
    private static void assignShoulderFields() {
        lWid = new Field[shoulderSegments.size()];
        lcWid = new Field[shoulderSegments.size()];
        rcWid = new Field[shoulderSegments.size()];
        rWid = new Field[shoulderSegments.size()];

        ShoulderSegment sd;
        for (int ii = 0; ii < shoulderSegments.size(); ii++) {
            sd = shoulderSegments.get(ii);
            
            Field f = new Field();
            f.SR = sd.route;
            f.begMP = sd.beginMP;
            f.endMP = sd.endMP;
            f.value0 = sd.leftWidth;
            lWid[ii] = f;
            
            f = new Field();
            f.SR = sd.route;
            f.begMP = sd.beginMP;
            f.endMP = sd.endMP;
            f.value0 = sd.leftCenterWidth;
            lcWid[ii] = f;
            
            f = new Field();
            f.SR = sd.route;
            f.begMP = sd.beginMP;
            f.endMP = sd.endMP;
            f.value0 = sd.rightCenterWidth;
            rcWid[ii] = f;
            
            f = new Field();
            f.SR = sd.route;
            f.begMP = sd.beginMP;
            f.endMP = sd.endMP;
            f.value0 = sd.rightWidth;
            rWid[ii] = f;
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 3, 6000, and the last
     */
    private static void assignIncidentFields() {
        ind = new Field[incidents.size()];

        IncidentSegment incident;
        for (int ii = 0; ii < incidents.size(); ii++) {
            incident = incidents.get(ii);
            
            Field f = new Field();
            f.SR = incident.route;
            f.begMP = incident.incMP;
            f.endMP = incident.incMP;
            f.value0 = 1;
            ind[ii] = f;
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 500, and the last
     */
    private static void assignADTFields() {
        adt = new Field[adtSegments.size()];

        ADTSegment adtSeg;
        for (int ii = 0; ii < adtSegments.size(); ii++) {
            adtSeg = adtSegments.get(ii);
            
            Field f = new Field();
            f.SR = adtSeg.route;
            f.begMP = adtSeg.beginMP;
            f.endMP = adtSeg.endMP;
            f.value0 = adtSeg.adt;
            adt[ii] = f;
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 2000, and the last
     * @param dir
     * @param hcFile 
     */
    public static void assembleHorizontalSegments(String dir, String hcFile) {
        Path hcPath = Paths.get(dir + "//" + hcFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader hcIn = Files.newBufferedReader(hcPath, charset);
            String entry = hcIn.readLine();

            while((entry = hcIn.readLine()) != null) {
                hcSegments.add(new HorizontalCurveSegment(entry));  
            }		

            // Hi, Joshua, it need to be deleted
            // hcSegments.add(new HorizontalCurveSegment());

            hcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading horizontal.csv");
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 2000, and the last
     * @param dir
     * @param vcFile 
     */
    public static void assembleVerticalSegments(String dir, String vcFile) {
        Path vcPath = Paths.get(dir + "//" + vcFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();

            while((entry = vcIn.readLine()) != null) {
                    vcSegments.add(new VerticalCurveSegment(entry));
            }		

            // Hi, Joshua, it need to be deleted
            // vcSegments.add(new VerticalCurveSegment());

            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading vertical.csv");
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 1000, and the last
     * @param dir
     * @param sdFile 
     */
    public static void assembleShoulderSegments(String dir, String sdFile) {
        Path shoulderPath = Paths.get(dir + "//" + sdFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader shoulderIn = Files.newBufferedReader(shoulderPath, charset);
            String entry = shoulderIn.readLine();

            while((entry = shoulderIn.readLine()) != null) {
                    shoulderSegments.add(new ShoulderSegment(entry));  
            }		
            
            // Hi, Joshua, it need to be deleted
            // shoulderSegments.add(new ShoulderSegment());

            shoulderIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading shoulder.csv");
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 4000, and the last
     * @param dir
     * @param lnFile 
     */
    public static void assembleLaneSegments(String dir, String lnFile) {
        Path lanesPath = Paths.get(dir + "//" + lnFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
                BufferedReader lanesIn = Files.newBufferedReader(lanesPath, charset);
                String entry = lanesIn.readLine();

          while((entry = lanesIn.readLine()) != null) {
                laneSegments.add(new LaneSegment(entry));  
          }

          // Hi, Joshua, it need to be deleted
          // laneSegments.add(new LaneSegment());

          lanesIn.close();

        }
        catch (IOException FNFE) {
                System.out.println("failure on reading lanes.csv");
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 3, 7000, and the last
     * @param dir
     * @param idFile 
     */
    public static void assembleIncidents(String dir, String idFile) {
        Path incidentPath = Paths.get(dir + "//" + idFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader incidentIn = Files.newBufferedReader(incidentPath, charset);
            String entry = incidentIn.readLine();

            while((entry = incidentIn.readLine()) != null) {
                incidents.add(new IncidentSegment(entry));  
            }

            incidentIn.close();

        }
        catch (IOException FNFE) {
            System.out.println("failure on reading incidents.csv");
        }
    }	
    
    /**
     * It has been tested by checking the records of 1, 2, 500, and the last
     * @param dir
     * @param adtFile 
     */
    public static void assembleADTSegments(String dir, String adtFile) {
        Path hcPath = Paths.get(dir + "//" + adtFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader hcIn = Files.newBufferedReader(hcPath, charset);
            String entry = hcIn.readLine();

            while((entry = hcIn.readLine()) != null) {
                if (entry.trim().equals(",,,,,,,,,,")) break;
                adtSegments.add(new ADTSegment(entry));  
            }		

            hcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading" + adtFile);
        }
    }
}
