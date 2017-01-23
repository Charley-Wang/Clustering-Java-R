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
import java.util.Map;

/**
 *
 * Version 3: on March 23, 2016
 *            generate two sets of data 
 *            one is the original data set for the modeling
 *            another is the 0-1 normalization data set for the clustering
 *            horizontal curve radium uses 1/R, for the flat road, the value is 0 directly
 *                                              for the road with R, then hcR is 1/R
 *                                              this value is set in the 
 * 
 * 
 * @author Charley (Xingsheng) Wang on July 9, 2015
 *          email: charleyxswang@gmail.com
 */
public class UniSegmentation {
    // public static final double SHOULDERFACTOR = 0.0005;
    // public static double HORIZONTALR;
    
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

    //                      0      1     2
    public static Field[] hcMSE, hcLen, hcR;
    //                      3      4     5
    public static Field[] vcLen, absPG, vcK;
    //                      6      7        8       9
    public static Field[] incLan, decLan, incWid, decWid;
    //                     10   11   12    13
    public static Field[] sdL, sdR, sdLC, sdRC;
    //                     14
    public static Field[] adt;
    // for incident        15 (the value to be predicted)
    public static Field[] ind;
    
    public static int INDEX_ADT = 14;
    public static int INDEX_INCIDENT = 15;
            
    public static double lengthSegmentation;
    public static int numRoute;
    public static Map dictRouteID_SeqNumMinMaxMP = new HashMap();
    public static Route[] route;
   
    /**
     * parameters for running
     *                           0                                                               1      2      3        4       5      6     7         8       9
     * C:\\Users\\li\\Desktop\\WXS\\Chifan\\05_MyWorkingProjects\\01_Road\\Predict30\\data400 hc.csv vc.csv sd.csv lane.csv ind.csv adt.csv 0.01 uniSeg.csv 40000
     * @param args[0]:  the folder name of the data
     *         args[1]:  the file name of horizontal geometry information
     *         args[2]:  the file name of vertical geometry information
     *         args[3]:  the file name of shoulder geometry information
     *         args[4]:  the file name of lane geometry information
     *         args[5]:  the file name of incident information
     *         args[6]:  the file name of ADT information
     *         args[7]:  the length of fixed length segmentation
     *         args[8]:  the output file name of segmentation
     *         args[9]:  the default horizontal curve radium
     */
    public static void main(String [] args) throws IOException {
        // long tStart = System.currentTimeMillis();
        
        String dir = args[0];
        String hcFile = args[1];
        String vcFile = args[2];
        String sdFile = args[3];
        String lnFile = args[4];
        String idFile = args[5];
        String adtFile = args[6];
        lengthSegmentation = Double.valueOf(args[7]);
        String segFile = args[8];
        // HORIZONTALR = Double.valueOf(args[9]);
                
        // read, process, and filter the raw data from geometry and incident tables
        assembleHorizontalSegments(dir, hcFile);
        assembleVerticalSegments(dir, vcFile);
        assembleShoulderSegments(dir, sdFile);
        assembleLaneSegments(dir, lnFile);
        assembleIncidents(dir, idFile);
        assembleADTSegments(dir, adtFile);
        
        setDictSegments();
        setRouteID_SeqNumMinMaxMPMap();
        
        // fill the fildes from geometry and incident classes
        assignHorizontalFields();
        assignVerticalFields();
        assignLaneFields();
        assignShoulderFields();
        assignIncidentFields();
        assignADTFields();
        
        setAllFields();
        calStaticalVariables();
        normalize0to1();
        
        // segmentation(); // this function is for the old version
        uinformSegmentation();
        
        // added on June 11, 2016
        distFrequency();
        
        saveSegmentation(dir + "//" + segFile);
        
        // report time used: total 3 s
        // long tEnd = System.currentTimeMillis();
        // double tDelta = (tEnd - tStart)/1000.0;
        // System.out.println("cluster time: " + tDelta + " s");
    }
    
    /**
     * added on June 11, 2016
     * Use the distribution 60 values for the distribution (which is split evenly 30 before and 30 after):
     */
    private static void distFrequency() {
        double[] distribution1 = {  1, 1, 1, 2, 2, 
                                    3, 4, 5, 6, 8, 
                                    10, 12, 15, 17, 20, 
                                    23, 27, 30, 33, 37, 
                                    40, 42, 44, 46, 47,
                                    48,
                                    47, 46, 44, 42, 40,
                                    37, 33, 30, 27, 23,
                                    20, 17, 15, 12, 10,
                                    8, 6, 5, 4, 3,
                                    2, 2, 1, 1, 1};
        
        double[] dis = new double[distribution1.length];
        int halfDist = distribution1.length/2;
        
        double[] distribution2 = {1, 1, 1, 1, 2,
                                2, 3, 3, 4, 5,
                                6, 7, 9, 10, 12,
                                14, 16, 18, 21, 23,
                                25, 28, 30, 32, 34,
                                36, 38, 39, 39, 40,
                                40, 39, 39, 38, 36,
                                34, 32, 30, 28, 25,
                                23, 21, 18, 16, 14,
                                12, 10, 9, 7, 6,
                                5, 4, 3, 3, 2,
                                2, 1, 1, 1, 1};
        
        Route rt;
        double[] fd;
        double freq;
        int idx;
        FixLengthSegment2 seg, seg2;
        
        for (int ii = 0; ii < route.length; ii++) {
            rt = route[ii];
            for (int jj = 0; jj < rt.fixSegments.length; jj++) {
                seg = rt.fixSegments[jj];
                fd = seg.fields1;
                freq = fd[INDEX_INCIDENT];
                for (int kk = 0; kk < distribution1.length; kk++) {
                    idx = jj - halfDist + kk;
                    if (idx >= 0 && idx < rt.fixSegments.length) {
                        seg2 = rt.fixSegments[idx];
                        seg2.disFreq += freq * distribution1[kk];
                    }
                }
            }
        }
        
    }
    
    public static void specialLn() {
        // This data from the real data and fitting
    }
    
    private static void saveSegmentation(String fileName) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, false)));
        
        out.print("route,begMP,endMP");
        for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
            out.print("," + allFieldsName[kk]);
        }
        // added on March 23, 2016 for saving the original data sets
        for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
            out.print("," + "O" + allFieldsName[kk]);
        }
        // added on June 11, 2016 for the distribution
        out.print("," + "distFreq");
        out.println();
        
        for (int ii = 0; ii < route.length; ii++) {
            Route rt = route[ii];
            for (int jj = 0; jj < rt.fixSegments.length; jj++) {
                FixLengthSegment2 seg = rt.fixSegments[jj];
                double[] fd = seg.fields1;
                
                // added on March 23 to remove any record with ADT = 0
                if (fd[INDEX_ADT] == 0 || fd[INDEX_ADT] == 0.0) {
                    continue;
                }
                
                out.print(seg.route);
                out.print("," + seg.beginMP);
                out.print("," + seg.endMP);
                for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
                    out.print("," + fd[kk]);
                }
                
                // added on March 23, 2016 for saving the original data sets
                fd = seg.fields0;
                for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
                    out.print("," + fd[kk]);
                }
                
                // added on June 11, 2016
                out.print("," + seg.disFreq);
                
                out.println();
            }
        }
        
        out.close();
    }
    
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
            System.out.println(routeID + ": " + minMP + ", " + maxMP + ", ");
            
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
     * Tested on July 18, 2015
     * This program is only used for segment length with 0.01 mile which is the precise of MPs
     */
    private static void uinformSegmentation() {
        int routeID, nFrag, len, nFragBeg, nFragEnd;
        double minMP, maxMP;
        for (int ii = 0; ii < route.length; ii++) {
            routeID = route[ii].route;
            minMP = route[ii].minMP;
            maxMP = route[ii].maxMP;
            
            // for the testing
            // System.out.println(routeID + ": " + minMP + ", " + maxMP + ", ");
            
            nFrag = (int)((maxMP - minMP)/lengthSegmentation);
            FixLengthSegment2[] frags = new FixLengthSegment2[nFrag];
            for (int jj = 0; jj < nFrag; jj++) {
                FixLengthSegment2 frag = new FixLengthSegment2(MAX_NUM_FIELDS);
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
                        if (nFragBeg == nFragEnd) {
                            if (jj == INDEX_INCIDENT) {
                                // for incident
                                if (nFragBeg > 0) frags[nFragBeg - 1].fields0[jj] += 0.5;
                                if (nFragBeg < nFrag) frags[nFragBeg].fields0[jj] += 0.5;
                                
                                if (nFragBeg > 0) frags[nFragBeg - 1].fields1[jj] += 0.5;
                                if (nFragBeg < nFrag) frags[nFragBeg].fields1[jj] += 0.5;
                            }
                            else {
                                if (nFragBeg > 0) frags[nFragBeg - 1].fields0[jj] = fd.value0;
                                if (nFragBeg < nFrag) frags[nFragBeg].fields0[jj] = fd.value0;
                                
                                if (nFragBeg > 0) frags[nFragBeg - 1].fields1[jj] = fd.value1;
                                if (nFragBeg < nFrag) frags[nFragBeg].fields1[jj] = fd.value1;
                            }
                        }
                        for (int mm = nFragBeg; mm < nFragEnd; mm++) {
                            frags[mm].fields0[jj] = fd.value0;
                            frags[mm].fields1[jj] = fd.value1;
                        }
                    }
                }
            }
            
            route[ii].fixSegments = frags;
        }
    }
    
    /**
     * 0-1 normalization for all fields except for incident
     * tested for the fields of hcMSE, hcLen, and adt
     */
    public static void normalize0to1() {
        int len;
        double det;
        
        for (int ii = 0; ii < MAX_NUM_FIELDS - 1; ii++) {
            
            // new codes for shoulder width field[8...16]
            // if (ii >= 8 && ii <= 16) continue;
            
            Field[] fd = allFields[ii];
            len = fd.length;
            
            // This is new code for the straight road
            if (allFieldsStat[ii].min > 0) allFieldsStat[ii].min = 0;
            // This is sepcial for hcR 
            //                         0      1     2
            // public static Field[] hcMSE, hcLen, hcR;
            // if (allFieldsStat[2].max < HORIZONTALR) allFieldsStat[2].max = HORIZONTALR;
            
            det = allFieldsStat[ii].max - allFieldsStat[ii].min;
            // This is for the testing
            /*
            System.out.println("No: " + ii + ", " + allFieldsName[ii] + 
                    ", min = " + allFieldsStat[ii].min +
                    ", max = " + allFieldsStat[ii].max +
                    ", det = " + det);
            */
            
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
//                if (ii == 10 && jj == 2176) {
//                    System.out.println("ii = " + ii + ", jj = " + jj);
//                }
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
     * tested on July 17, 2015 again
     */
    private static void setAllFields() {
        //                         0      1     2
        // public static Field[] hcMSE, hcLen, hcR;
        // tested on July 17, 2015 for the first 3, No 2000, and the last three
        allFields[0] = hcMSE;       allFieldsName[0] = "hcMSE";
        allFields[1] = hcLen;       allFieldsName[1] = "hcLen";
        allFields[2] = hcR;         allFieldsName[2] = "hcR";
        
        //                         3      4     5
        // public static Field[] vcLen, absPG, vcK;
        // tested on July 17, 2015 for the first 3, No 2000, and the last three
        allFields[3] = vcLen;       allFieldsName[3] = "vcLen";
        allFields[4] = absPG;       allFieldsName[4] = "absPG";
        allFields[5] = vcK;         allFieldsName[5] = "vcK";
        
        //                         6      7        8       9
        // public static Field[] incLan, decLan, incWid, decWid;
        // tested on July 17, 2015 for the first 3, No 4000, and the last three
        allFields[6] = incLan;         allFieldsName[6] = "incLan";
        allFields[7] = decLan;         allFieldsName[7] = "decLan";
        allFields[8] = incWid;         allFieldsName[8] = "incWid";
        allFields[9] = decWid;         allFieldsName[9] = "decWid";
        
        //                 10   11   12    13
        // public static Field[] sdL, sdR, sdLC, sdRC;
        // tested on July 17, 2015 for the first 3, No 1000, and the last three
        allFields[10] = sdL;        allFieldsName[10] = "sdL";
        allFields[11] = sdR;        allFieldsName[11] = "sdR";
        allFields[12] = sdLC;       allFieldsName[12] = "sdLC";
        allFields[13] = sdRC;       allFieldsName[13] = "sdRC";
        
        //                       14    15
        // public static Field[] adt, ind;
        // tested on July 17, 2015 for the first 3, No 600, and the last three
        allFields[14] = adt;        allFieldsName[14] = "adt";
        // tested on July 17, 2015 for the first 3, No 6000, and the last three
        allFields[15] = ind;        allFieldsName[15] = "ind";
    }
        
    /**
     * Route ID = 19, 121, 203, 274, 522, and 702 have been checked on July 17, 2015
     * set dictionary {route id : [seqNum, minMP, maxMP, 
     *     freq in hcSegments, freq in vcSegments, ...]} for each route
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
                
                // the structure of dictRouteID_SeqNumMinMaxMP
                // key = routeID
                // value[0...8]
                // value[0]: sequential number in the program
                // value[1]: begMP
                // value[2]: endMP
                // value[3..8]: the frequencies for 4 geometry tables, 1 adt table, and 1 incident table 
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
        
        //
        String[] tableNames = new String[6];
        tableNames[0] = new String("hcSegments");
        tableNames[1] = new String("vcSegments");
        tableNames[2] = new String("shoulderSegments");
        tableNames[3] = new String("laneSegments");
        tableNames[4] = new String("incidents");
        tableNames[5] = new String("adtSegments");
        
        // delete route with only one kind of field
        int rID;
        numRoute = 0;
        Iterator rt = dictRouteID_SeqNumMinMaxMP.keySet().iterator();
        while (rt.hasNext()) {
           rID = (int)rt.next();
           SeqNumMinMaxMP = (double[])dictRouteID_SeqNumMinMaxMP.get(rID);
           int zeroNum = 0;
           int totalFreq = 0;
           for (int kk = 0; kk < MAX_NUM_SEGMENTS; kk++) {
               totalFreq += SeqNumMinMaxMP[kk + 3];
               if (SeqNumMinMaxMP[kk + 3] == 0.0 || SeqNumMinMaxMP[kk + 3] == 0.0) {
                   zeroNum++;
                   /*
                   if (zeroNum == 1) {
                       System.out.println("====================================");
                       System.out.println("Check if routeID = " + rID + " is included in all six tables.");
                   }
                   System.out.println("...is not included in the talbe of " + tableNames[kk]);
                   */
               }
           }
           if (zeroNum == 0) numRoute++;
           else {
               /*
               System.out.println("Total number = " + zeroNum);
               System.out.println("Total freq   = " + totalFreq);
               System.out.println(rID + "," + zeroNum + "," + totalFreq);
               */
           }
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
        //                         0      1     2
        // public static Field[] hcMSE, hcLen, hcR;
        hcMSE = new Field[hcSegments.size()];
        hcLen = new Field[hcSegments.size()];
        hcR = new Field[hcSegments.size()];
        
        HorizontalCurveSegment hc;
        for (int ii = 0; ii < hcSegments.size(); ii++) {
            hc = hcSegments.get(ii);
            
            Field f = new Field();
            f.SR = hc.route;
            f.begMP = hc.beginMP;
            f.endMP = hc.endMP;
            f.value0 = hc.hcMSE;
            f.value1 = f.value0;
            hcMSE[ii] = f;
            
            f = new Field();
            f.SR = hc.route;
            f.begMP = hc.beginMP;
            f.endMP = hc.endMP;
            f.value0 = hc.hcLength;
            f.value1 = f.value0;
            hcLen[ii] = f;
            
            f = new Field();
            f.SR = hc.route;
            f.begMP = hc.beginMP;
            f.endMP = hc.endMP;
            f.value0 = hc.hcRadius;
            f.value1 = f.value0;
            hcR[ii] = f;
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 4000, and the last
     */
    private static void assignVerticalFields() {
        //                         3      4     5
        // public static Field[] vcLen, absPG, vcK;
        vcLen = new Field[vcSegments.size()];
        absPG = new Field[vcSegments.size()];
        vcK = new Field[vcSegments.size()];
        
        VerticalCurveSegment vc;
        for (int ii = 0; ii < vcSegments.size(); ii++) {
            vc = vcSegments.get(ii);
            
            Field f = new Field();
            f.SR = vc.route;
            f.begMP = vc.beginMP;
            f.endMP = vc.endMP;
            f.value0 = vc.vcLength;
            f.value1 = f.value0;
            vcLen[ii] = f;
            
            f = new Field();
            f.SR = vc.route;
            f.begMP = vc.beginMP;
            f.endMP = vc.endMP;
            f.value0 = Math.abs(vc.pga - vc.pgb);
            f.value1 = f.value0;
            absPG[ii] = f;
            
            f = new Field();
            f.SR = vc.route;
            f.begMP = vc.beginMP;
            f.endMP = vc.endMP;
            // K = L/A
            if (absPG[ii].value0 == 0.0) {
                f.value0 = 0;
            }
            else f.value0 = vc.vcLength / absPG[ii].value0;
            f.value1 = f.value0;
            vcK[ii] = f;
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 4000, and the last
     */
    private static void assignLaneFields() {
        //                         6      7        8       9
        // public static Field[] incLan, decLan, incWid, decWid;
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
            f.value1 = f.value0;
            incLan[ii] = f;
            
            f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.decreasingLanes;
            f.value1 = f.value0;
            decLan[ii] = f;
            
            f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.increasingWidth;
            f.value1 = f.value0;
            incWid[ii] = f;

            f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.decreasingWidth;
            f.value1 = f.value0;
            decWid[ii] = f;
        }
    }
    
    private static int[][] calShoulderRange(String type) {
        Map dictLenFreq = new HashMap();
        int value;
        
        ShoulderSegment sd;
        int maxValue = 0;
        for (int ii = 0; ii < shoulderSegments.size(); ii++) {
            sd = shoulderSegments.get(ii);
            if (type.equals("left")) {
                value = (int)sd.leftWidth;
            }
            else if(type.equals("right")) {
                value = (int)sd.rightWidth;
            }
            else {
                value = (int)(sd.rightCenterWidth + sd.leftCenterWidth);
            }
            if (value > maxValue) maxValue = value;
            if (dictLenFreq.containsKey(value)) {
                dictLenFreq.replace(value, (int)dictLenFreq.get(value) + 1);
            }
            else {
                dictLenFreq.put(value, 1);
            }
        }
        
        int num = 2, accct, p, lastp, det1, det2;
        int[] delimiter = new int[num];
        int equalSize = shoulderSegments.size()/(num + 1);
        
        p = -1;
        lastp = -1;
        
        int[] total = new int[num];
        
        for (int ii = 0; ii < num; ii++) {
            accct = 0;
            while (accct < equalSize && p <= maxValue) {
                p++;
                if (dictLenFreq.containsKey(p)) {
                    accct += (int)dictLenFreq.get(p);
                    lastp = p;
                }
            }
            det1 = Math.abs(equalSize - accct);
            det2 = Math.abs(equalSize - (accct - (int)dictLenFreq.get(lastp)));
            if (det1 < det2) {
                delimiter[ii] = lastp + 1;
                total[ii] = accct;
            }
            else {
                delimiter[ii] = lastp;
                p--;
                total[ii] = accct - (int)dictLenFreq.get(lastp);
            }
        }
        
        int[][] rng = new int[num + 1][2];
        
        for(int ii = 0; ii < num + 1; ii++) {
            if (ii == 0) {
                rng[0][0] = 0;
                rng[0][1] = delimiter[0];
            }
            else if (ii == num) {
                rng[ii][0] = delimiter[ii - 1];
                rng[ii][1] = maxValue + 1;
            }
            else {
                rng[ii][0] = delimiter[ii - 1];
                rng[ii][1] = delimiter[ii];
            }
        }
        
        return rng;
    }
    
    private static void assignShoulderFields() {
        //                 10   11   12    13
        // public static Field[] sdL, sdR, sdLC, sdRC;
        
        sdL = new Field[shoulderSegments.size()];
        sdR = new Field[shoulderSegments.size()];
        sdLC = new Field[shoulderSegments.size()];
        sdRC = new Field[shoulderSegments.size()];
        
        ShoulderSegment ln;
        for (int ii = 0; ii < shoulderSegments.size(); ii++) {
            ln = shoulderSegments.get(ii);
            
            Field f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.leftWidth;
            f.value1 = f.value0;
            sdL[ii] = f;
            
            f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.rightWidth;
            f.value1 = f.value0;
            sdR[ii] = f;
            
            f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.leftCenterWidth;
            f.value1 = f.value0;
            sdLC[ii] = f;

            f = new Field();
            f.SR = ln.route;
            f.begMP = ln.beginMP;
            f.endMP = ln.endMP;
            f.value0 = ln.rightCenterWidth;
            f.value1 = f.value0;
            sdRC[ii] = f;
        }
    }
    
    /**
     * It has been tested by checking the records of 1, 2, 3, 1000, and the last
     */
//    private static void assignShoulderFields() {
//        double vl, vr, vc;
//        
//        int[][] rngLeft = calShoulderRange("left");
//        int[][] rngRight = calShoulderRange("right");
//        int[][] rngCenter = calShoulderRange("center");
//        
//        // ??? special for center
//        rngCenter[0][0] = 0;    rngCenter[0][1] = 1;        // 0...0    2146
//        rngCenter[1][0] = 1;    rngCenter[1][1] = 9;        // 1...8    174
//        rngCenter[2][0] = 9;    rngCenter[2][1] = 9999;     // 9...inf  138
//        
//        sdL0 = new Field[shoulderSegments.size()];
//        sdL1 = new Field[shoulderSegments.size()];
//        sdL2 = new Field[shoulderSegments.size()];
//        sdR0 = new Field[shoulderSegments.size()];
//        sdR1 = new Field[shoulderSegments.size()];
//        sdR2 = new Field[shoulderSegments.size()];
//        sdC0 = new Field[shoulderSegments.size()];
//        sdC1 = new Field[shoulderSegments.size()];
//        sdC2 = new Field[shoulderSegments.size()];
//
//        ShoulderSegment sd;
//        for (int ii = 0; ii < shoulderSegments.size(); ii++) {
//            sd = shoulderSegments.get(ii);
//            
//            Field[] f = new Field[9];
//            for (int jj = 0; jj < 9; jj++) {
//                f[jj] = new Field();
//                f[jj].SR = sd.route;
//                f[jj].begMP = sd.beginMP;
//                f[jj].endMP = sd.endMP;
//                f[jj].value = 0;
//            }
//            
//            for (int jj = 0; jj < 9; jj++) {
//                vl = sd.leftWidth;
//                vr = sd.rightWidth;
//                vc = sd.leftCenterWidth + sd.rightCenterWidth;
//                for (int kk = 0; kk < 3; kk++) {
//                    if (vl >= rngLeft[kk][0] && vl < rngLeft[kk][1]) {
//                        f[kk].value = 1 * SHOULDERFACTOR;
//                    }
//                    if (vr >= rngRight[kk][0] && vr < rngRight[kk][1]) {
//                        f[kk + 3].value = 1 * SHOULDERFACTOR;
//                    }
//                    if (vc >= rngCenter[kk][0] && vc < rngCenter[kk][1]) {
//                        f[kk + 6].value = 1 * SHOULDERFACTOR;
//                    }
//                }
//            }
//            
//            sdL0[ii] = f[0];
//            sdL1[ii] = f[1];
//            sdL2[ii] = f[2];
//            sdR0[ii] = f[3];
//            sdR1[ii] = f[4];
//            sdR2[ii] = f[5];
//            sdC0[ii] = f[6];
//            sdC1[ii] = f[7];
//            sdC2[ii] = f[8];
//        }
//    }
    
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
            f.value1 = f.value0;
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
            f.value1 = f.value0;
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
                //System.out.println(entry);
                vcSegments.add(new VerticalCurveSegment(entry));
            }		

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
