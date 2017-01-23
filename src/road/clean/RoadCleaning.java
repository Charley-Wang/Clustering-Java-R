/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.clean;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Charley (Xingsheng) Wang on July 6, 2015
 *          xingshengw@gmail.com
 */
public class RoadCleaning {
    public static SimpleSegment[] segments;
    
    public static void main(String[] args) throws IOException {
        String dirData, in, out;
        dirData = "C:\\Users\\li\\Desktop\\WXS\\Chifan\\17_Road\\Data\\2015-summer-WSDOT-crash-geometry-rawData\\";
        
        in = "Geometry_Horizontal_Alignment.csv";
        out = "Horizontal.csv";
        cleanHorizontal(dirData, in, out);
        // System.out.println("================ " + out + " for MP" + "================ ");
        checkOverlappedSegments(dirData, out, 0, 3, 5);
        //System.out.println("================ " + out + " for ARM" + "================ ");
        //checkOverlappedSegments(dirData, out, 0, 1, 2);
        
        in = "Geometry_LanesWidth.csv";
        out = "LanesWidth.csv";
        cleanLanesWidth(dirData, in, out);
        // System.out.println("================ " + out + " for MP" + "================ ");
        checkOverlappedSegments(dirData, out, 0, 3, 5);
//        System.out.println("================ " + out + " for ARM" + "================ ");
//        checkOverlappedSegments(dirData, out, 0, 1, 2);
                
        in = "Geometry_ShoulderWidth.csv";
        out = "ShoulderWidth.csv";
        cleanShoulderWidth(dirData, in, out);
        // System.out.println("================ " + out + " for MP" + "================ ");
        checkOverlappedSegments(dirData, out, 0, 3, 5);
//        System.out.println("================ " + out + " for ARM" + "================ ");
//        checkOverlappedSegments(dirData, out, 0, 1, 2);
        
        in = "Geometry_Vertical_Alignment.csv";
        out = "Vertical.csv";
        cleanVertical(dirData, in, out);
        // System.out.println("================ " + out + " for MP" + "================ ");
        checkOverlappedSegments(dirData, out, 0, 3, 5);
//        System.out.println("================ " + out + " for ARM" + "================ ");
//        checkOverlappedSegments(dirData, out, 0, 1, 2);
//        System.out.println("================ " + out + " for VCARM" + "================ ");
//        checkOverlappedSegments(dirData, out, 0, 7, 9);
        
        // tested on July 17, 2015
        out = "WSDOT-ADT-2012.csv";
        // System.out.println("================ " + out + " for MP" + "================ ");
        checkOverlappedSegments(dirData, out, 0, 1, 2);

        in = "WSDOT-Crashesrawdata-2012.csv";
        out = "Incident.csv";
        cleanCrashesrawdata(dirData, in, out);
    }
    
    /**
     * Tested for Horizontal, shoulder tables
     * @param dir
     * @param inputFileName
     * @param routeIndex
     * @param begMPIndex
     * @param endMPIndex
     * @throws IOException 
     */
    public static void checkOverlappedSegments(String dir, String inputFileName, int routeIndex, int begMPIndex, int endMPIndex) throws IOException {
        Path vcPath = Paths.get(dir + inputFileName);
        Charset charset = Charset.forName("US-ASCII");
        
        BufferedReader in = Files.newBufferedReader(vcPath, charset);
        int num = 0;
        String[] txts;
        String entryFirst = in.readLine();
        String entry;
        while((entry = in.readLine()) != null) {
            txts = entry.split(",");
            if (inputFileName.equals("WSDOT-ADT-2012.csv") && txts.length < 4) continue;
            if (txts[begMPIndex].trim().length() == 0 || txts[endMPIndex].trim().length() == 0) continue;
            num++;
        }
        in.close();
        
        segments = new SimpleSegment[num];
        SimpleSegment seg;
        
        in = Files.newBufferedReader(vcPath, charset);
        int line = 1, n = 1;
        entry = in.readLine();
        while((entry = in.readLine()) != null) {
            line++;
            txts = entry.split(",");
            
            if (inputFileName.equals("WSDOT-ADT-2012.csv") && txts.length < 4) continue;
            if (txts[begMPIndex].trim().length() == 0 || txts[endMPIndex].trim().length() == 0) continue;
            
            seg = new SimpleSegment();
            seg.entry = entry;
            seg.line = line;
            seg.route = Integer.valueOf(txts[routeIndex]);
            seg.begMP = Double.valueOf(txts[begMPIndex]);
            seg.endMP = Double.valueOf(txts[endMPIndex]);
            seg.isOverlapped = false;
            n++;
            segments[n - 2] = seg;
        }
        in.close();
        
        // check if records are overlapped or not
        int i, j;
        double a, b, c, d;
        String outs;
        boolean isOverlapped;
        for (int ii = 0; ii < num - 1; ii++) {
            i = segments[ii].route;
            a = segments[ii].begMP;
            b = segments[ii].endMP;
            for (int jj = ii + 1; jj < num; jj++) {
                j = segments[jj].route;
                if (i == j) {
                    outs = "Line " + segments[ii].line + ", " + segments[jj].line + " are overlapped.";
                    c = segments[jj].begMP;
                    d = segments[jj].endMP;
                    isOverlapped = false;
                    if (a > c && a < d) isOverlapped = true;
                    if (b > c && b < d) isOverlapped = true;
                    if (c > a && c < b) isOverlapped = true;
                    if (d > a && d < b) isOverlapped = true;
                    if (a == c || b == d) isOverlapped = true;
                    if (isOverlapped) {
                        // System.out.println(outs);
                        segments[ii].isOverlapped = true;
                        segments[jj].isOverlapped = true;
                    }
                }
            }
        }
        
        // save records which are not overlapped
        txts = inputFileName.split("\\.");
        String outFile2 = txts[0] + "_clean." + txts[1];
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dir + outFile2, false)));
        out.println(entryFirst);
        for (int ii = 0; ii < segments.length; ii++) {
            if (segments[ii].isOverlapped) continue;
            out.println(segments[ii].entry);
        }
        out.close();
    }
    
    /**
     * Clean raw data of Horizontal
     * Tested on July 17, 2015 for the first 3 and the last 3 records
     * the number of records is right after checked manually
     * @param dir The input and output data directory
     * @param inputFileName Horizontal file name with .csv format
     * @param outputFileName Filtered file name
     */
    public static void cleanHorizontal(String dir, String inputFileName, String outputFileName) throws IOException {
        Path vcPath = Paths.get(dir + inputFileName);
        Charset charset = Charset.forName("US-ASCII");
        BufferedReader in = Files.newBufferedReader(vcPath, charset);

        String[] txts;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dir + outputFileName, false)));
        
        String outputFieldNum = "1,4,5,6,7,8,9,12,13,14,15,16";
        //           0    1      2      3     4     5     6     7  8 9  10    11
        out.println("SR,BegARM,EndARM,BegMP,BegAB,EndMP,EndAB,Type,R,e,len,direction");
        txts = outputFieldNum.split(",");
        int[] num = new int[txts.length];
        for (int ii = 0; ii < txts.length; ii++) num[ii] = Integer.valueOf(txts[ii]);

        String entry = in.readLine();
        while((entry = in.readLine()) != null) {
            entry = entry.replaceAll("\"", "");
            txts = entry.split(",");
            // input file format
            //  0       1+       2       3        4+      5+     6+      7+      8+       9+
            // SRID	SR	RRT	RRQ	BegARM	EndARM	BegMP	BegAB	EndMP	EndAB
            //            10                                          11                              12+
            // HorizontalCurvePointOfTangencyArm	HorizontalCurvePointOfCurvatureArm	HorizontalCurveType
            //          13+                             14+                              15+                        16+
            // HorizontalCurveRadius	HorizontalCurveMaximum(Super)Elevation	HorizontalCurveLength	HorizontalCurveDirection
            
            // output file format
            // SR	BegARM	EndARM	BegMP	BegAB	EndMP	EndAB	
            // HorizontalCurveType	HorizontalCurveRadius	HorizontalCurveMaximum(Super)Elevation	
            // HorizontalCurveLength	HorizontalCurveDirection

            if (txts[2].trim().length() > 0 || txts[3].trim().length() > 0 || txts[13].trim().equals("0") || txts[13].trim().equals("0.00")) {
                continue;
            }
            
            for (int ii = 0; ii < num.length; ii++) {
                if (ii > 0) out.print(",");
                out.print(txts[num[ii]]);
            }
            out.println();
        }	
        
        in.close();
        out.close();
    }
    
    /**
     * Tested on July 17, 2015 for the first 3 and the last 3 records
     * the number of records is right after checked manually
     * Clean raw data of LanesWidth, any data should not contain ","
     * @param dir The input and output data directory
     * @param inputFileName LanesWidth file name with .csv format
     * @param outputFileName Filtered file name
     */
    public static void cleanLanesWidth(String dir, String inputFileName, String outputFileName) throws IOException {
        Path vcPath = Paths.get(dir + inputFileName);
        Charset charset = Charset.forName("US-ASCII");
        BufferedReader in = Files.newBufferedReader(vcPath, charset);

        String[] txts;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dir + outputFileName, false)));
        
        String outputFieldNum = "1,4,5,6,7,8,9,10,11,12,13,14";
        //           0    1      2      3     4     5    6
        out.println("SR,BegARM,EndARM,BegMP,BegAB,EndMP,EndAB,Direction,LanesInc,LanesDec,WidInc,WidDec");
        
        txts = outputFieldNum.split(",");
        int[] num = new int[txts.length];
        for (int ii = 0; ii < txts.length; ii++) num[ii] = Integer.valueOf(txts[ii]);

        String entry = in.readLine();
        while((entry = in.readLine()) != null) {
            entry = entry.replaceAll("\"", "");
            txts = entry.split(",");
            // input file format
            //  0       1+       2       3       4+       5+     6+      7+      8+       9+ 
            // SRID	SR	RRT	RRQ	BegARM	EndARM	BegMP	BegAB	EndMP	EndAB
            //       10+                 11+                     12+                 13+             14+
            // RoadwayDirection	NumberOfLanesIncreasing	NumberOfLanesDecreasing	RoadwayWidthInc	RoadwayWidthDec
            
            // output file format
            // SR	BegARM	EndARM	BegMP	BegAB	EndMP	EndAB	
            // RoadwayDirection	NumberOfLanesIncreasing	NumberOfLanesDecreasing	RoadwayWidthInc	RoadwayWidthDec	

            if (txts[2].trim().length() > 0 || txts[3].trim().length() > 0) {
                continue;
            }
            
            for (int ii = 0; ii < num.length; ii++) {
                if (ii > 0) out.print(",");
                out.print(txts[num[ii]]);
            }
            out.println();
        }	
        
        in.close();
        out.close();
    }
    
    /**
     * Tested on July 17, 2015 for the first 3 and the last 3 records
     * the number of records is right after checked manually
     * Clean raw data of ShoulderWidth, any data should not contain ,
     * @param dir The input and output data directory
     * @param inputFileName ShoulderWidth file name with .csv format
     * @param outputFileName Filtered file name
     */
    public static void cleanShoulderWidth(String dir, String inputFileName, String outputFileName) throws IOException {
        Path vcPath = Paths.get(dir + inputFileName);
        Charset charset = Charset.forName("US-ASCII");
        BufferedReader in = Files.newBufferedReader(vcPath, charset);

        String[] txts;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dir + outputFileName, false)));
        
        String outputFieldNum = "1,4,5,6,7,8,9,10,11,12,13,14";
        //           0    1      2     3     4      5    6
        out.println("SR,BegARM,EndARM,BegMP,BegAB,EndMP,EndAB,Direction,Left,LeftCen,RightCen,Right");
        
        txts = outputFieldNum.split(",");
        int[] num = new int[txts.length];
        for (int ii = 0; ii < txts.length; ii++) num[ii] = Integer.valueOf(txts[ii]);

        String entry = in.readLine();
        while((entry = in.readLine()) != null) {
            entry = entry.replaceAll("\"", "");
            txts = entry.split(",");
            // input file format
            //   0      1+       2       3        4+      5+     6+      7+      8+      9+
            // SRID	SR	RRT	RRQ	BegARM	EndARM	BegMP	BegAB	EndMP	EndAB
            //       10+            11+                         12+                    13+                             14+
            // RoadwayDirection	ShoulderWidthLeft	ShoulderWidthLeftCenter	ShoulderWidthRightCenter	ShoulderWidthRight

            // output file format
            // SR	BegARM	EndARM	BegMP	BegAB	EndMP	EndAB	
            // RoadwayDirection	ShoulderWidthLeft	ShoulderWidthLeftCenter	ShoulderWidthRightCenter	ShoulderWidthRight

            if (txts[2].trim().length() > 0 || txts[3].trim().length() > 0) {
                continue;
            }
            
            for (int ii = 0; ii < num.length; ii++) {
                if (ii > 0) out.print(",");
                out.print(txts[num[ii]]);
            }
            out.println();
        }	
        
        in.close();
        out.close();
    }
    
    /**
     * Tested on July 17, 2015 for the first 3 and the last 3 records
     * the number of records is right after checked manually
     * Clean raw data of Vertical, any data should not contain ,
     * @param dir The input and output data directory
     * @param inputFileName Vertical file name with .csv format
     * @param outputFileName Filtered file name
     */
    public static void cleanVertical(String dir, String inputFileName, String outputFileName) throws IOException {
        Path vcPath = Paths.get(dir + inputFileName);
        Charset charset = Charset.forName("US-ASCII");
        BufferedReader in = Files.newBufferedReader(vcPath, charset);

        String[] txts;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dir + outputFileName, false)));
        
        String outputFieldNum = "1,4,5,6,7,8,9,15,16,17,18,19,20,21";
        //            0   1      2     3     4      5     6    7  8   9
        out.println("SR,BegARM,EndARM,BegMP,BegAB,EndMP,EndAB,Arm,Vpi,Evc,Type,Len,aGrade,bGrade");
        
        txts = outputFieldNum.split(",");
        int[] num = new int[txts.length];
        for (int ii = 0; ii < txts.length; ii++) num[ii] = Integer.valueOf(txts[ii]);

        String entry = in.readLine();
        while((entry = in.readLine()) != null) {
            //entry = entry.replaceAll("\"", "");
            txts = entry.split(",");
            // input file format
            //   0            1+                       2                          3                      
            // SRID	State Route Number	Related Route Type	Related Route Qualifier	
            //    4+              5+                 6+            7+              8+            9+
            // Begin ARM	End ARM	         Begin SRMP	Begin AB	End SRMP	End AB
            //      10             11                         12
            // Begin SRMP2	End SRMP2	Related Roadway Type Description
            //        13                  14             15+                     16+                     17+
            // State Route Description	RRT_RRQ	Vertical Curve Bvc Arm	Vertical Curve Vpi Arm	Vertical Curve Evc Arm
            //        18+                        19+                            20+                                 21+
            // Vertical Curve Type	Vertical Curve Length	Vertical Curve Percent Grade Ahead	Vertical Curve Percent Grade Back

            // output file format
            // SRID	BeginARM	EndARM	BeginMP	BeingAB	EndMP	EndAB	
            // Vertical Curve Bvc Arm	Vertical Curve Vpi Arm	Vertical Curve Evc Arm	Vertical Curve Type	
            // Vertical Curve Length	Vertical Curve Percent Grade Ahead	Vertical Curve Percent Grade Back	

            if (txts[2].trim().length() > 0 || txts[3].trim().length() > 0 || (!txts[14].trim().equals("MAINLINE"))) {
                continue;
            }
            
            for (int ii = 0; ii < num.length; ii++) {
                if (ii > 0) out.print(",");
                out.print(txts[num[ii]]);
            }
            out.println();
        }	
        
        in.close();
        out.close();
    }

    /**
     * Tested on July 17, 2015 for the first 3 and the last 3 records
     * Clean raw data of Crashesrawdata, any data should not contain ,
     * @param dir The input and output data directory
     * @param inputFileName Crashesrawdata file name with .csv format
     * @param outputFileName Filtered file name
     */
    public static void cleanCrashesrawdata(String dir, String inputFileName, String outputFileName) throws IOException {
        Path vcPath = Paths.get(dir + inputFileName);
        Charset charset = Charset.forName("US-ASCII");
        BufferedReader in = Files.newBufferedReader(vcPath, charset);

        String[] txts;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dir + outputFileName, false)));
        
        String outputFieldNum = "1,4,2";
        out.println("SR,ARM,MP");
        
        txts = outputFieldNum.split(",");
        int[] num = new int[txts.length];
        for (int ii = 0; ii < txts.length; ii++) num[ii] = Integer.valueOf(txts[ii]);

        String entry = in.readLine();
        String reportNumber;
        Set reportSet = new HashSet();

        while((entry = in.readLine()) != null) {
            //entry = entry.replaceAll("\"", "");
            txts = entry.split(",");
            // input file format
            //          0                         1+                    2+                                        3       
            // Collision Report Number      State Route ID      State Route Mile Post       State Route Mile Post Ahead_Back Indicator
            //                       4+
            // State Route Accumulated Route Milepost or ARM    State Route Number          State Route Related Roadway Type	
            // State Route Related Roadway Qualifier	State Route History_Suspense Indicator	
            // Most Severe Injury Type	Most Severe Injury Type Code	Collision Severity	
            // Collision Severity Code	Most Severe Sobriety Type	Most Severe Sobriety Type Code	First Collision Type	
            // First Collision Type Code	Roadway Type Description

            // output file format
            // SRID INCARM INCMP
            
            reportNumber = txts[0].trim();
            if (reportSet.contains(reportNumber)) {
                continue;
            }
            else {
                reportSet.add(reportNumber);
            }
            
            for (int ii = 0; ii < num.length; ii++) {
                if (ii > 0) out.print(",");
                out.print(txts[num[ii]]);
            }
            out.println();
        }	
        
        in.close();
        out.close();
    }
}



