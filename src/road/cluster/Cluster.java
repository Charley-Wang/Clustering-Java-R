/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.cluster;

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
import java.util.Iterator;
import java.util.Set;

/**
 * There are 33 routes in total.
 * @author Charley (Xingsheng) Wang on July 10, 2015
 *          xingshengw@gmail.com
 */
public class Cluster {
    // MAX_NUM_FIELDS include adt and incidents
    public static final int MAX_NUM_FIELDS = 16;
    //                                   0     1    2    3     4    5    6      7      8      9     10  11  12  13   14  15
    public static String fieldsName = "hcMSE,hcLen,hcR,vcLen,absPG,vcK,incLan,decLan,incWid,decWid,sdL,sdR,sdLC,sdRC,adt,ind";
    private static final int INDEX_INCWID = 8;
    private static final int INDEX_DECWID = 9;
    private static final int INDEX_SDL = 10;
    private static final int INDEX_SDR = 11;
    private static final int INDEX_SDLC = 12;
    private static final int INDEX_SDRC = 13;
    public static String[] fieldsNameArray = {"hcMSE","hcLen","hcR","vcLen","absPG","vcK","incLan",
        "decLan","incWid","decWid","sdL","sdR","sdLC","sdRC","adt","ind"};
    public static int INDEX_INCIDENT = 15;
    public static String fieldsNameWithoutInd = "hcMSE,hcLen,hcR,vcLen,absPG,vcK,incLan,decLan,incWid,decWid,sdL,sdR,sdLC,sdRC,adt";
        
    // the min cutoff length of the clustered segment
    // public static double CUTOFF_MIN_SEGMENT = 0.015;
    // on Jan 16, 2016, this value is changed to 0.0 from prediction20
    public static double CUTOFF_MIN_SEGMENT = 0.0;
    
    private static final boolean USE_DISTRIBUTE = false;
        
    public static UniSegment[] uniSegments;
    public static double[] weight = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_cluster = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_modeling = new double[MAX_NUM_FIELDS];
    
    // Fields for the program
    
    // data         all
    // merge        all
    
    // cluster      fieldCtrl_cluster
    // dist         fieldCtrl_cluster
    // weights      fieldCtrl_cluster
    
    // output       fieldCtrl_modeling but including ind anyway
    
    public static int numSegment;
    public static ClusterSegment clusterSegmentLink;
    
    public static Distance[] distance;
    public static Distance linkDist;
    public static int numDist;
    public static SortElementDouble[] sortElements;
    
    public static int sizeBin, finalNumSegment;
    public static double[] binData;
    public static Distance[] binPt;
        
    /**
     * We can control which fields will be counted for distance calculation for clustering
     * by setting values in weight.csv as 0 or not 
     *                                                                             0              1          2          3           4           5        6            7          8    9
     * C://Users//li//Desktop//WXS//Chifan//05_MyWorkingProjects//01_Road//Predict30//test uniSeg.csv weight0.csv 0.0000001 autoCluster.csv 3000 cluster.csv fieldCtrl.csv cluster 0
     *
     * C://Users//li//Desktop//WXS//Chifan//05_MyWorkingProjects//01_Road//Predict34//data uniSeg.csv.1.csv weight0.csv 0.0000001 autoCluster.1.csv 494 cluster.1.csv fieldCtrl.csv cluster 0
     *
     * test on July 7, 2016
     * C://Users//li//Desktop//WXS//Chifan//05_MyWorkingProjects//01_Road//Predict60//data uniSeg.csv.1.csv weight0.csv 0.0000001 autoCluster.1.csv 494 cluster.1.csv fieldCtrl.csv cluster 0
     * the output file of cluster.1.csv.orig is right: checked the first, id = 7, and the last - they are all right
     * the output file of cluster.1.csv is right: checked the first, id = 6, and the last - they are all right
     * 
     * 
     * args[9] 1 the first line of weights file is the title
     *         0 w.o.
     * @param args
     * @throws IOException 
     */
    public static void main(String [] args) throws IOException {
        String dir = args[0];
        String uniSegFile = args[1];
        String wtFile = args[2];
        // the default value is 0.0000001
        double cutoffAutoCluster = Double.valueOf(args[3]);
        // the file name of clustered segments after auto clustering
        // this file is majorly used for tracking the process
        String autoClusterFileName = args[4];
        // the final number of segments after clustring
        finalNumSegment = Integer.valueOf(args[5]);
        String clusterFile = args[6];
        String fieldCtrlFile = args[7];
        String willCluster = args[8];
        int readWeightsFirstLine = Integer.valueOf(args[9]);
        
        // read and build uniform segments
        readUniSegments(dir, uniSegFile);
        
        // added on July 6, 2016
        combine();
        
        setUniSegMerageable();
        
        // read fields control file and weight file
        readFieldCtrlFile(dir, fieldCtrlFile);
        readWeight(dir, wtFile, readWeightsFirstLine);
        
        // build homogeneous segments
        buildClusterSegByCombineSameUniSeg(cutoffAutoCluster);
        
        // build distance link and order them according to the distance
        calDist();
        orderDist();
        createDistLink();
        createBinarySearchArrays();
        
        // save homogeneous segments
        saveClusterSegments(dir + "//" + autoClusterFileName, true);
        saveClusterSegments(dir + "//" + autoClusterFileName + ".orig", false);
        
        // cluster segments, if input parameter says cluster firstly
        if (willCluster.equals("cluster")) {
            long tStart = System.currentTimeMillis();
            cluster(finalNumSegment);
            long tEnd = System.currentTimeMillis();
            long tDelta = tEnd - tStart;
            System.out.println("cluster time: " + tDelta);

            // this is new codes for deleting any segment which lenght is less than CUTOFF_MIN_SEGMENT
            mergeSegments(CUTOFF_MIN_SEGMENT);
        }
        
        // assign cluster ID and save the results before partition
        assignUniSegWithClusterSegmentID();
        saveClusterSegments(dir + "//" + clusterFile, true);
        saveClusterSegments(dir + "//" + clusterFile + ".orig", false);
        
        // this function is not used from Jan 16, 2016 (prediction20)
        // partition data to training and validating data sets
        // String[] partitionFileNames = {dir + "//" + traingFileWithoutCSV, dir + "//" + validFileWithoutCSV};
        // segmentPartition(partitionFileNames);
        
        // new partition from Jan 16, 2016 (prediction20)
        //          9        10    11  12  13
        // uni|auto|cluster part_ 0.6 0.2 0.2
        /*
        String partType = args[9];
        String outputPrefix = args[10];
        double percentTrain = Double.valueOf(args[11]);
        double percentValid = Double.valueOf(args[12]);
        double percentTest  = Double.valueOf(args[13]);
        String partFile = "";
        if (partType.equals("uni")) partFile = uniSegFile;
        else if (partType.equals("auto")) partFile = autoClusterFileName;
        else partFile = clusterFile;
        
        randomPartion(dir, partType, outputPrefix, percentTrain, percentValid, percentTest, partFile);
        */
    }
    
    // added on July 5, 2016
    // combine 2 to 1 
    // private static final int INDEX_INCWID = 8;
    // private static final int INDEX_DECWID = 9;
    // private static final int INDEX_SDL = 10;
    // private static final int INDEX_SDR = 11;
    // private static final int INDEX_SDLC = 12;
    // private static final int INDEX_SDRC = 13;
    static void combine() {
        int n = uniSegments.length;
        for (int ii = 0; ii < n; ii++) {
            uniSegments[ii].fields0[INDEX_INCWID] += uniSegments[ii].fields0[INDEX_DECWID];
            uniSegments[ii].fields0[INDEX_SDL]    += uniSegments[ii].fields0[INDEX_SDR];
            uniSegments[ii].fields0[INDEX_SDLC]   += uniSegments[ii].fields0[INDEX_SDRC];
            
            uniSegments[ii].fields1[INDEX_INCWID] += uniSegments[ii].fields1[INDEX_DECWID];
            uniSegments[ii].fields1[INDEX_SDL]    += uniSegments[ii].fields1[INDEX_SDR];
            uniSegments[ii].fields1[INDEX_SDLC]   += uniSegments[ii].fields1[INDEX_SDRC];
        }
    }
    
    static void randomPartion(String dir, String partType, String outputPrefix, double percentTrain, double percentValid, double percentTest, String partFile) throws IOException {
        Path path = Paths.get(dir + "//" + partFile);
        Charset charset = Charset.forName("US-ASCII");

        BufferedReader in = Files.newBufferedReader(path, charset);
        String entry;
        int numRecords = -1;
        while((entry = in.readLine()) != null) numRecords++;
        in.close();
        
        System.out.println(numRecords);
    }
    
    /**
     * fileNames without the suffix such as .csv
     * the program will create two versions of output files
     * one for model fitting in R and its format is: ind, length, field 0, field1, ..., fieldn
     * another for alternative and output tracking， file name with _full
     * 
     * check:
     * 1. training_uniform_Full itself                  [OK for isMergable]
     * 2. training_uniform_Full vs. uniSeg              [OK for the first, middle, and last 5-10 records]
     * 3. training_cluster_Full itself                  [OK for all isMergable]
     * 4. training_cluster_Full vs. uniSeg              [OK for the No7 and the last cluster]
     * 5. training_cluster_Full vs. training_cluster    [OK for the first and the last several records]
     * 6. validation_cluster_Full itself                []
     * 7. validation_cluster_Full vs. uniSeg            []
     * 8. validation_cluster_Full vs. training_cluster  []
     * 
     * @param fileNames
     * @throws IOException 
     */
    public static void segmentPartition(String[] fileNames) throws IOException {
        int num;
        
        // get the last element of the linkDist
        Distance curr, lastDist = null;
        curr = linkDist;
        while (curr != null) {
            lastDist = curr;
            curr = curr.nxt;
        } 
        
        // get the natural number of delimters
        num = 1;
        for (int ii = 0; ii < uniSegments.length; ii++) {
            if (uniSegments[ii].nxtMerg == false && ii < uniSegments.length - 1) num++;
        }

        // addNum is the number of delimters from the big distance between two clustered segments
        int addNum = 0;
        while (addNum < numSegment) {
            if (addNum != 0) {
                uniSegments[lastDist.preSeg.uniSegIndexEnd].nxtMerg = false;
                // lastDist.preSeg.nxtMerg = false;
                if (lastDist.preSeg.uniSegIndexEnd + 1 < uniSegments.length) {
                    uniSegments[lastDist.preSeg.uniSegIndexEnd + 1].preMerg = false;
                    // lastDist.nxtSeg.preMerg = false;
                }
                lastDist = lastDist.pre;
            }
            
            SegmentGap[] gap = new SegmentGap[num + addNum];
            SegmentGap gap1 = new SegmentGap();
            gap1.idxBeg = 0;
            gap1.route = uniSegments[0].route;
            gap[0] = gap1;
            int jj = 0;
            for (int ii = 0; ii < uniSegments.length; ii++) {
                if (uniSegments[ii].nxtMerg == false) {
                    // update the previous gap
                    gap[jj].idxEnd = ii;
                    gap[jj].num = gap[jj].idxEnd - gap[jj].idxBeg + 1;
                    
                    // create a new gap
                    if (jj < gap.length - 1 && ii < uniSegments.length - 1) {
                        jj++;
                        gap1 = new SegmentGap();
                        gap1.idxBeg = ii + 1;
                        gap1.route = uniSegments[ii + 1].route;
                        gap[jj] = gap1;
                    }
                }
            }
            gap[gap.length - 1].idxEnd = uniSegments.length - 1;
            gap[gap.length - 1].num = gap[gap.length - 1].idxEnd - gap[gap.length - 1].idxBeg + 1;
        
            int minNum, minV, maxV;
            int len = fileNames.length;
            Set[] sets = new HashSet[len];
            int[] nums = new int[len];
            for (int ii = 0; ii < len; ii++) {
                nums[ii] = 0;
                sets[ii] = new HashSet();
            }
        
            for (int ii = 0; ii < gap.length - 1; ii++) {
                minNum = 0;
                minV = nums[0];
                for (jj = 1; jj < len; jj++) {
                    if (nums[jj] < minV) {
                        minNum = jj;
                        minV = nums[jj];
                    }
                }
                nums[minNum] += gap[ii].num;
                sets[minNum].add(ii);
            }
        
            minV = maxV = nums[0];
            for (jj = 1; jj < len; jj++) {
                if (nums[jj] < minV) minV = nums[jj];
                if (nums[jj] > maxV) maxV = nums[jj];
            }
            
            if (maxV - minV <= uniSegments.length * 0.1 / len) {
                // save the results
                for (int ii = 0; ii < len; ii++) {
                    int[] ranges = new int[sets[ii].size() * 2];
                    Iterator it = sets[ii].iterator();
                    jj = 0;
                    while (it.hasNext()) {
                        gap1 = gap[(int)it.next()];
                        ranges[jj] = gap1.idxBeg;
                        ranges[jj + 1] = gap1.idxEnd;
                        jj += 2;
                    }

                    if (ii == 0) saveUniSegments(fileNames[ii] + "_uniform_Full.csv", ranges, true, true);
                    saveClusterSegments(fileNames[ii] + "_cluster_Full.csv", ranges, true, true);
                    saveClusterSegments(fileNames[ii] + "_cluster.csv", ranges, false, true);
                    
                    if (ii == 0) saveUniSegments(fileNames[ii] + "_uniform_Full.csv.orig", ranges, true, false);
                    saveClusterSegments(fileNames[ii] + "_cluster_Full.csv.orig", ranges, true, false);
                    saveClusterSegments(fileNames[ii] + "_cluster.csv.orig", ranges, false, false);
                }
                    
                break;
            }
            
            addNum++;
        }
    }
    
    // checked on July 7, 2016
    // incidents saved any way
    // total weights dependent on fieldCtrl_cluster
    // saved attributes dependent on fieldCtrl_modeling
    public static void saveClusterSegments(String fileName, int[] ranges, boolean isFullFormat, boolean isNormed) throws IOException {
        ClusterSegment seg = clusterSegmentLink;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, false)));
        
        if (isFullFormat) {
            out.print("clusterID,route,begMP,endMP,len,weights,uIdxB,uIdxE,preMerg,nxtMerg,preDist,nxtDist");
            
            // modified on August 15, 2015
            // out.print(fieldsName);
            for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                // incidents are saved any way
                if (ii != INDEX_INCIDENT)
                    if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
                out.print("," + fieldsNameArray[ii]);
            }
        }
        else {
            out.print("ind,len");
            
            // modified on August 15, 2015
            // out.print(fieldsNameWithoutInd);
            for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
                if (ii == INDEX_INCIDENT) continue;
                out.print("," + fieldsNameArray[ii]);
            }
        }
        
        if (USE_DISTRIBUTE) {
            // added on June 12, 2016
            out.print(",disFreq");
        }
        
        out.println();

        boolean inRange;
        int range1 = 0, range2 = 0;
        while(seg != null) {
            // System.out.println(seg.clusterID);
            
            inRange = false;
            for (int ii = 0; ii < ranges.length; ii += 2) {
                range1 = ranges[ii];
                range2 = ranges[ii + 1];
                if (seg.uniSegIndexBeg >= range1 && seg.uniSegIndexEnd <= range2) {
                    inRange = true;
                    break;
                }
            }
            if (inRange == false) {
                seg = seg.nxt;
                continue;
            }
            
            if (isFullFormat) {
                double weights = 0.0;
                for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                    // added on August 15, 2015
                    if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                    
                    if (isNormed) weights += Math.abs(seg.fields1[ii] * weight[ii]);
                    else          weights += Math.abs(seg.fields0[ii] * weight[ii]);
                }
                
                out.print(seg.clusterID);
                out.print("," + seg.route);
                out.print("," + seg.begMP);
                out.print("," + seg.endMP);
                out.print("," + (seg.endMP - seg.begMP));
                out.print("," + weights);
                out.print("," + seg.uniSegIndexBeg);
                out.print("," + seg.uniSegIndexEnd);
                
                if (seg.uniSegIndexBeg == range1) out.print("," + false);
                else out.print("," + seg.preMerg);
                if (seg.uniSegIndexEnd == range2) out.print("," + false);
                else out.print("," + seg.nxtMerg);
                
                if (seg.preMerg == true) out.print("," + seg.preDist.dist);
                else out.print("," + (-1));
                if (seg.nxtMerg == true) out.print("," + seg.nxtDist.dist);
                else out.print("," + (-1));
                
                for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                    // added on August 15, 2015
                    if (ii != INDEX_INCIDENT)
                        if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
                    
                    if (isNormed) out.print("," + seg.fields1[ii]);
                    else          out.print("," + seg.fields0[ii]);
                }
            }
            else {
                if (isNormed) out.print(seg.fields1[INDEX_INCIDENT]);
                else          out.print(seg.fields0[INDEX_INCIDENT]);
                out.print("," + (seg.endMP - seg.begMP));
                for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                    if (ii == INDEX_INCIDENT) continue;
                    
                    // added on August 15, 2015
                    if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
                    
                    if (isNormed) out.print("," + seg.fields1[ii]);
                    else          out.print("," + seg.fields0[ii]);
                }
            }
            
            if (USE_DISTRIBUTE) {
                // added on June 12, 2016
                out.print("," + seg.distFreq);
            }
            
            out.println();
            seg = seg.nxt;
        }

        out.close();
    }
    
    // checked on July 7, 2016
    // incidents saved any way
    public static void saveUniSegments(String fileName, int[] ranges, boolean isFullFormat, boolean isNormed) throws IOException {
        int idxBeg, idxEnd;
        UniSegment seg;
        
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, false)));
        if (isFullFormat) {
            out.print("clusterID,route,begMP,endMP,used,preMerg,nxtMerg");
            
            // modified on August 15, 2015
            // out.print("," + fieldsName);
            for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                if (ii != INDEX_INCIDENT)
                    if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
                out.print("," + fieldsNameArray[ii]);
            }
        }
        else {
            out.print("ind,len");

            // modified on August 15, 2015
            // out.print(fieldsNameWithoutInd);
            for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                if (ii == INDEX_INCIDENT) continue;
                if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
                out.print("," + fieldsNameArray[ii]);
            }
        }
        
        if (USE_DISTRIBUTE) {
            // added on June 12, 2016
            out.print(",disFreq");
        }
        
        out.println();

        for (int kk = 0; kk < ranges.length; kk += 2) {
            idxBeg = ranges[kk];
            idxEnd = ranges[kk + 1];
            for (int ii = idxBeg; ii <= idxEnd; ii++) {
                seg = uniSegments[ii];
                if (isFullFormat) {
                    out.print(seg.clusterID);
                    out.print("," + seg.route);
                    out.print("," + seg.begMP);
                    out.print("," + seg.endMP);
                    out.print("," + seg.used);
                    out.print("," + seg.preMerg);
                    // ??? pay attention to
                    if (ii == idxEnd && kk == ranges.length - 1) out.print("," + false);
                    else out.print("," + seg.nxtMerg);
                    for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                        // added on August 15, 2015
                        if (jj != INDEX_INCIDENT)
                            if (fieldCtrl_modeling[jj] == 0 || fieldCtrl_modeling[jj] == 0.0) continue;
                        
                        if (isNormed) out.print("," + seg.fields1[jj]);
                        else          out.print("," + seg.fields0[jj]);
                    }
                }
                else {
                    if (isNormed) out.print(seg.fields1[INDEX_INCIDENT]);
                    else          out.print(seg.fields0[INDEX_INCIDENT]);
                    out.print("," + (seg.endMP - seg.begMP));
                    for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                        // added on August 15, 2015
                        if (fieldCtrl_modeling[jj] == 0 || fieldCtrl_modeling[jj] == 0.0) continue;
                        
                        if (jj == INDEX_INCIDENT) continue;
                        if (isNormed) out.print("," + seg.fields1[jj]);
                        else          out.print("," + seg.fields0[jj]);
                    }
                }
                
                if (USE_DISTRIBUTE) {
                    // added on June 12, 2016
                    out.print("," + seg.distFreq);
                }
                
                out.println();
            }
        }

        out.close();
    }
    
    public static void assignUniSegWithClusterSegmentID() {
        ClusterSegment curr;
        curr = clusterSegmentLink;
        int id = 0;
        while (curr != null) {
            id++;
            curr.clusterID = id;
            for (int ii = curr.uniSegIndexBeg; ii <= curr.uniSegIndexEnd; ii++) uniSegments[ii].clusterID = id;
            curr = curr.nxt;
        }
    }
    
    /**
     * combine uniform segments to its adjacent clustered segments with shorter distance
     * to insure that there is no clustered segment with length <= minSegmentLength
     * on August 5, 2015
     * tested on August 12, 2015
     * @param minSegmentLength: any segment with length <= minSegmentLength will be merged to the nearer adjacent segment
     */
    public static void mergeSegments(double minSegmentLength) {
        double len, len1, len2, v1, v2;
        int mergeDirection = 0;
        ClusterSegment currSeg, mergeSeg;
        Distance dist1, dist2;
        currSeg = clusterSegmentLink;
        
        while (currSeg != null) {
            len = currSeg.endMP - currSeg.begMP;
            // System.out.println("Road ID: " + currSeg.route + ", " + currSeg.begMP + " - " + currSeg.endMP);
            if (len <= minSegmentLength) {
                mergeSeg = null;
                if (currSeg.preMerg == true && currSeg.nxtMerg == false) {
                    mergeSeg = currSeg.pre;
                    mergeDirection = -1;
                }
                else if (currSeg.preMerg == false && currSeg.nxtMerg == true) {
                    mergeSeg = currSeg.nxt;
                    mergeDirection = 1;
                }
                else if (currSeg.preMerg == true && currSeg.nxtMerg == true) {
                    if (currSeg.preDist.dist < currSeg.nxtDist.dist) {
                        mergeSeg = currSeg.pre;
                        mergeDirection = -1;
                    }
                    else {
                        mergeSeg = currSeg.nxt;
                        mergeDirection = 1;
                    }
                }
                else currSeg = currSeg.nxt;
                
                if (mergeSeg != null) {
                    System.out.println("    @ merged  Road ID: " + currSeg.route + ", " + currSeg.begMP + " - " + currSeg.endMP);
                    
                    // merge the currSeg to the mergeSeg
                    len1 = mergeSeg.endMP - mergeSeg.begMP;
                    len2 = currSeg.endMP - currSeg.begMP;
                    if (mergeDirection == -1) {
                        // --> mergeSeg     -->  currSeg   -->  currSeg.nxt
                        mergeSeg.endMP = currSeg.endMP;
                        mergeSeg.uniSegIndexEnd = currSeg.uniSegIndexEnd;
                        mergeSeg.nxtMerg = currSeg.nxtMerg;
                    }
                    else {
                        // currSeg.pre --> currSeg --> mergeSeg -->
                        mergeSeg.begMP = currSeg.begMP;
                        mergeSeg.uniSegIndexBeg = currSeg.uniSegIndexBeg;
                        mergeSeg.preMerg = currSeg.preMerg;
                    }
                    // calculate the new fields for the mergeSeg
                    for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                        v1 = mergeSeg.fields1[ii];
                        v2 = currSeg.fields1[ii];

                        // added on August 15, 2015
                        // if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;   
                        // merge ALL fields
                        
                        if (ii == INDEX_INCIDENT) mergeSeg.fields1[ii] = v1 + v2;
                        else mergeSeg.fields1[ii] = (v1 * len1 + v2 * len2)/(len1 + len2);

                        // added on March 23, 2016
                        v1 = mergeSeg.fields0[ii];
                        v2 = currSeg.fields0[ii];
                        
                        // added on August 15, 2015
                        // if (fieldCtrl[ii] == 0 || fieldCtrl[ii] == 0.0) continue;   
                        // merge ALL fields
                        
                        if (ii == INDEX_INCIDENT) mergeSeg.fields0[ii] = v1 + v2;
                        else mergeSeg.fields0[ii] = (v1 * len1 + v2 * len2)/(len1 + len2);
                    }
                    
                    if (USE_DISTRIBUTE) {
                        // added on June 12, 2016
                        v1 = mergeSeg.distFreq;
                        v2 = currSeg.distFreq;
                        mergeSeg.distFreq = v1 + v2;
                    }
                    
                    // update distances
                    if (mergeDirection == -1) {
                        // --> mergeSeg     -->  currSeg   -->  currSeg.nxt
                        //           |           |    |         |
                        //        nxtDist   preDist  nxtDist  preDist
                        //           |           |    |         |
                        //          [   dist 1   ]   [   dist   2 ]
                        // assign dist1 and dist2
                        dist1 = mergeSeg.nxtDist;
                        dist2 = currSeg.nxtDist;
                        // delete dist1
                        deleteDistFromLinkDist(dist1);
                        // change mergeSeg.nxt pointer
                        mergeSeg.nxtDist = dist2;
                        // update dist2
                        if (dist2 != null) {
                            dist2.preSeg = mergeSeg;
                            updateDistance(dist2);
                        }
                        // update distance of the other side
                        updateDistance(mergeSeg.preDist);
                    }
                    else {
                        // currSeg.pre --> currSeg --> mergeSeg -->
                        //       |         |     |       |
                        //    nexDist  preDist  nxtDist  preDist
                        //       |        |      |       |
                        //     [   dist 1  ]   [  dist 2  ]
                        // assign dist1 and dist2
                        dist1 = currSeg.preDist;
                        dist2 = mergeSeg.preDist;
                        // delete dist2
                        deleteDistFromLinkDist(dist2);
                        // change pointer of mergeSeg
                        mergeSeg.preDist = dist1;
                        // update dist1
                        if (dist1 != null) {
                            dist1.nxtSeg = mergeSeg;
                            updateDistance(dist1);
                        }
                        // update distance of the other side
                        updateDistance(mergeSeg.nxtDist);
                    }
                    
                    // delete currSeg
                    if (mergeDirection == -1) {
                        // --> mergeSeg     -->  currSeg   -->  currSeg.nxt
                        mergeSeg.nxt = currSeg.nxt;
                        if (mergeSeg.nxt != null) mergeSeg.nxt.pre = mergeSeg;
                    }
                    else {
                        // currSeg.pre --> currSeg --> mergeSeg -->
                        if (currSeg.pre == null) clusterSegmentLink = mergeSeg;
                        else currSeg.pre.nxt = mergeSeg;
                        mergeSeg.pre = currSeg.pre;
                    }
                    
                    // update currSeg
                    currSeg = mergeSeg.nxt;
                }
            }
            else currSeg = currSeg.nxt;
        }
    }
    
    public static void deleteDistFromLinkDist(Distance d) {
        if (d == null) return;
        Distance pre, nxt;
        // delete d from linkDist
        pre = d.pre;
        nxt = d.nxt;

        if (pre == null) {
            // if pre == null, it means that d is the head of linkDist
            linkDist = linkDist.nxt;
            linkDist.pre = null;
        }
        else {
            pre.nxt = nxt;
            if (nxt != null) nxt.pre = pre; 
        }
    }
     
    public static void updateDistance(Distance d) {
        if (d == null) return;
        
        ClusterSegment pre, nxt;
        pre = d.preSeg;
        nxt = d.nxtSeg;
        double sum = 0, dist;
        for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
            // added on August 15, 2015
            if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
            
            dist = pre.fields1[ii] - nxt.fields1[ii];
            sum += weight[ii] * dist * dist;
        }
        
        // added on June 12, 2016
        // here, I do not consider to calculate weight * distFreq
        // for our model incident is not in the clustering
        
        d.dist = Math.sqrt(sum);
    }
    
    /**
     * Checked with several clustered segments with uniform segments
     * on July 18, 2015
     * @param finalClusterNum 
     */
    public static void cluster(int finalClusterNum) {
        ClusterSegment preSeg, currSeg;
        double v1, v2, len1, len2, dist, sum;
        
        Distance head = linkDist, pt, pre, nxt;
        while (numSegment > finalClusterNum && head != null) {
            // System.out.println("The number of clusters: " + numSegment + ", " + sizeBin + ", " + numDist);
            // System.out.println("              dists:    " + numDist);
            
            // merge the currSeg to the preSeg of the head of the linkDist
            // which has the mininal distance
            preSeg = head.preSeg;
            currSeg = head.nxtSeg;
            
            len1 = preSeg.endMP - preSeg.begMP;
            len2 = currSeg.endMP - currSeg.begMP;
            preSeg.endMP = currSeg.endMP;
            preSeg.uniSegIndexEnd = currSeg.uniSegIndexEnd;
            for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                v1 = preSeg.fields1[ii];
                v2 = currSeg.fields1[ii];
                
                // added on August 15, 2015
                // if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                // merge ALL fields

                if (ii == INDEX_INCIDENT) preSeg.fields1[ii] = v1 + v2; 
                else preSeg.fields1[ii] = (v1 * len1 + v2 * len2)/(len1 + len2);

                // added on March 23, 2016
                v1 = preSeg.fields0[ii];
                v2 = currSeg.fields0[ii];
                if (ii == INDEX_INCIDENT) preSeg.fields0[ii] = v1 + v2; 
                else preSeg.fields0[ii] = (v1 * len1 + v2 * len2)/(len1 + len2);
            }
            
            if (USE_DISTRIBUTE) {
                // added on June 12, 2016
                v1 = preSeg.distFreq;
                v2 = currSeg.distFreq;
                preSeg.distFreq = v1 + v2;
            }
            
            preSeg.nxt = currSeg.nxt;
            // System.out.println(numSegment + "    " + currSeg.id);
            if (currSeg.nxt != null) currSeg.nxt.pre = preSeg;
            numSegment--;

            // delete head from linkDist
            linkDist = head.nxt;
            
            // linkDist.pre = null;
            // modify on June 3, 2016
            if (linkDist != null) linkDist.pre = null;
            
            numDist--;
            adjustBinarySearchArrays(head);
                        
            // adjust previous distance
            if (preSeg.pre != null) {
                if (preSeg.pre.nxtMerg == true && preSeg.preMerg == true) {
                    Distance d = preSeg.preDist;

                    // delete d from linkDist
                    pre = d.pre;
                    nxt = d.nxt;
                    
                    if (pre == null) {
                        // if pre == null, it means that d is the head of linkDist
                        linkDist = linkDist.nxt;
                        // linkDist.pre = null;
                        // modified on June 3, 2016
                        if (linkDist != null) linkDist.pre = null;
                    }
                    else {
                        pre.nxt = nxt;
                        if (nxt != null) nxt.pre = pre; 
                    }
                    numDist--;
                    adjustBinarySearchArrays(d);

                    // adjust d for the new distance
                    // d.dist = Math.abs(preSeg.pre.index - preSeg.index);
                    sum = 0;
                    for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                        // added on August 15, 2015
                        if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                        
                        dist = preSeg.pre.fields1[ii] - preSeg.fields1[ii];
                        sum += weight[ii] * dist * dist;
                        
                        // June 12, 2016
                        // here, I do not consider disFreq
                    }
                    d.dist = Math.sqrt(sum);
                    if (linkDist == null) {
                        // added on June 3, 2016
                        createLinkDist(d);
                    }
                    else {
                        if (d.dist < linkDist.dist) {
                            d.pre = null;
                            d.nxt = linkDist;
                            linkDist.pre = d;
                            linkDist = d;
                        }
                        else {
                            pt = binarySearch(d.dist, 0, sizeBin - 1);
                            insertLinkDist(d, pt);
                        }
                    }
                    preSeg.pre.nxtDist = d;
                    preSeg.preDist = d;
                    numDist++;
                }
            }
            
            // adjust next distance
            if (currSeg.nxt != null) {
                if (currSeg.nxtMerg == true && currSeg.nxt.preMerg == true) {
                    Distance d = currSeg.nxtDist;

                    // delete d from linkDist
                    pre = d.pre;
                    nxt = d.nxt;
                    
                    // this is important
                    d.preSeg = preSeg;
                    
                    if (pre == null) {
                        // if pre == null, it means that d is the head of linkDist
                        linkDist = linkDist.nxt;
                        // linkDist.pre = null;
                        // modify on June 3, 2016
                        if (linkDist != null) linkDist.pre = null;
                    }
                    else {
                        pre.nxt = nxt;
                        if (nxt != null) nxt.pre = pre; 
                    }

                    numDist--;
                    adjustBinarySearchArrays(d);

                    // adjust d for the new distance
                    // d.dist = Math.abs(currSeg.nxt.index - preSeg.index);
                    sum = 0;
                    for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                        // added on August 15, 2015
                        if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                        
                        dist = currSeg.nxt.fields1[ii] - preSeg.fields1[ii];
                        sum += weight[ii] * dist * dist;
                        
                        // June 12, 2016
                        // here, I do not consider distFreq
                    }
                    d.dist = Math.sqrt(sum);
                    if (linkDist == null) {
                        // added on June 3, 2016
                        createLinkDist(d);
                    }
                    else {
                        if (d.dist < linkDist.dist) {
                            d.pre = null;
                            d.nxt = linkDist;
                            linkDist.pre = d;
                            linkDist = d;
                        }
                        else {
                            pt = binarySearch(d.dist, 0, sizeBin - 1);
                            insertLinkDist(d, pt);
                        }
                    }
                    currSeg.nxt.preDist = d;
                    preSeg.nxtDist = d;
                    numDist++;
                }
            }
            
            // change the head
            head = linkDist;
        }
    }
    
    // added on June 3, 2016
    private static void createLinkDist(Distance d) {
        linkDist = new Distance();
        linkDist.dist = d.dist;
        linkDist.id = 0;
        linkDist.idxBinData = 0;
        linkDist.nxt = null;
        linkDist.pre = null;
        linkDist.nxtSeg = d.nxtSeg;
        linkDist.preSeg = d.preSeg;
        
        binData = new double[1];
        binData[0] = d.dist;
        
        binPt = new Distance[1];
        Distance dist = new Distance();
        dist.dist = d.dist;
        dist.nxt = null;
        dist.pre = null;
        dist.preSeg = d.preSeg;
        dist.nxtSeg = d.nxtSeg;
        binPt[0] = dist;
    }
    
    private static void insertLinkDist(Distance d, Distance pt) {
        Distance nxt;
        while(true) {
            if (pt.nxt == null) {
                pt.nxt = d;
                d.pre = pt;
                d.nxt = null;
                break;
            }
            else {
                if (d.dist >= pt.dist && d.dist <= pt.nxt.dist) {
                    nxt = pt.nxt;
                    pt.nxt = d;
                    d.pre = pt;
                    d.nxt = nxt;
                    nxt.pre = d;
                    break;
                }
                else pt = pt.nxt;
            }
        }
    }
    
    private static Distance binarySearch(double d, int beg, int end) {
        Distance ret = null;
        double min, max, mid;
        int avg, middle;
        
        if (end - beg <= 1) {
            ret = binPt[beg];
        }
        else {
            min = binData[beg];
            max = binData[end];
            avg = (int)((end - beg)/2);
            middle = avg + beg;
            mid = binData[middle];
            
            if (min == mid && mid == max) {
                ret = binPt[beg];
            }
            else if(min == mid && mid < max) {
                if (d == min) ret = binPt[beg];
                else ret = binarySearch(d, middle, end);
            }
            else if(min < mid && mid == max) {
                if (d == max) ret = binPt[middle];
                else ret = binarySearch(d, beg, middle);
            }
            else {
                if (min <= d && d <= mid) ret = binarySearch(d, beg, middle);
                else ret = binarySearch(d, middle, end);
            }
        }     
        
        return ret;
    }
    
    /**
     * adjust binary search arrays because of the deleting of an element d
     * @param the element will be deleted from the binary search array 
     */
    private static void adjustBinarySearchArrays(Distance d) {
        int idx;
        double nxtData;
        Distance currPt, nxtPt;
        if (d.idxBinData != -1) {
            idx = d.idxBinData;
            binPt[idx].idxBinData = -1;
            if (idx == binData.length - 1) {
                createBinarySearchArrays();
            }
            else {
                nxtData = binData[idx + 1];
                currPt = binPt[idx];
                nxtPt = currPt.nxt;
                if (nxtPt != null) {
                    if (nxtData > nxtPt.dist) {
                        binPt[idx] = nxtPt;
                        binData[idx] = nxtPt.dist;
                        nxtPt.idxBinData = idx;
                    }
                    else {
                        createBinarySearchArrays();
                    }
                }
                else {
                    createBinarySearchArrays();
                }
            }
        }
    }
    
    public static void createBinarySearchArrays() {
        // if (numSegment > 1024*4) sizeBin = 2048;
        // else sizeBin = (int)Math.sqrt(numSegment);

        Distance curr, pre;
        int num, det;
       
        // numDist
        if (numDist <= 0) {
            // added on June 3, 2016
            binData = null;
            binPt = null;
            return;
        }
        if (numDist < 4) {
            // added on June 3, 2016
            sizeBin = numDist;
            num = sizeBin;
            // det = (int)(numDist/(num - 1)) - 1;
            det = 0;
        }
        else {
            // sizeBin = (int)Math.sqrt(numSegment);
            sizeBin = (int)Math.sqrt(numDist);
            num = sizeBin;
            det = (int)(numDist/(num - 1)) - 1;
        }
        
        binData = new double[num];
        binPt = new Distance[num];
        
        pre = null;
        curr = linkDist;
        
        binData[0] = curr.dist;
        binPt[0] = curr;
        curr.idxBinData = 0;
        
        int ii = -1;
        int jj = 0;
        while (curr != null) {
            ii++;
            if (ii == det) {
                jj++;
                if (jj < num - 1) { 
                    binData[jj] = curr.dist;
                    binPt[jj] = curr;
                    curr.idxBinData = jj;
                }
                ii = 0;
            }
            pre = curr;
            curr = curr.nxt;
            if (curr != null) curr.idxBinData = -1;
        }
        
        binData[num - 1] = pre.dist;
        binPt[num - 1] = pre;
        pre.idxBinData = num - 1;
    }
    
    public static void createDistLink() {
        Distance head = null, pre = null, curr = null;
        
        for (int ii = 0; ii < sortElements.length; ii++) {
            curr = (Distance)sortElements[ii].obj;
            if (head == null) {
                head = curr;
                linkDist = head;
                curr.pre = null;
                curr.nxt = null;
                pre = curr;
            }
            else {
                pre.nxt = curr;
                curr.pre = pre;
                curr.nxt = null;
                pre = curr;
            }
        }
    }
    
    public static void orderDist() {
        SortDouble sd = new SortDouble(numDist);
        
        for (int ii = 0; ii < numDist; ii++) {
            sd.appendElement(distance[ii].dist, distance[ii]);
        }
        
        sd.mergeSort(0, numDist - 1);
        sortElements = sd.getSortedElement();
    }
    
    /**
     * build distance and calculate distances with fields controlled by fieldCtrl_cluster [8/15/15]
     */
    public static void calDist() {
        ClusterSegment currSeg, preSeg;
        
        numDist = 0;
        distance = new Distance[numSegment - 1];
        
        preSeg = clusterSegmentLink;
        currSeg = preSeg.nxt;
        
        double sum, d;
        while (currSeg != null) {
            if (preSeg.nxtMerg = true && currSeg.preMerg == true) {
                Distance curr = new Distance();
                sum = 0;
                for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                    // added on August 15, 2015
                    if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                    
                    d = preSeg.fields1[ii] - currSeg.fields1[ii];
                    sum += weight[ii] * d * d;
                    
                    // June 12, 2016
                    // here I do not consider distFreq
                }
                curr.dist = Math.sqrt(sum);
                curr.preSeg = preSeg;
                curr.nxtSeg = currSeg;
                numDist++;
                // only used to debug
                curr.id = numDist - 1;
                distance[numDist - 1] = curr;
                preSeg.nxtDist = curr;
                currSeg.preDist = curr;
            }
            preSeg = currSeg;
            currSeg = currSeg.nxt;
        }
    }
    
    /**
     * Tested on July 18, 2015
     * @param fileName
     * @throws IOException 
     */
    public static void saveClusterSegments(String fileName, boolean isNormed) throws IOException {
        ClusterSegment seg = clusterSegmentLink;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, false)));
        
        out.print("id,route,begMP,endMP,len,weights,preDist,nxtDist,uIdxB,uIdxE,preMerg,nxtMerg");

        // modified on August 15, 2015
        // out.print("," + fieldsName);
        for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
            if (ii != INDEX_INCIDENT)
                if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
            out.print("," + fieldsNameArray[ii]);
        }
        
        if (USE_DISTRIBUTE) {
            // added on June 12, 2016
            out.print(",distFreq");
        }
        
        out.println();

        while(seg != null) {
            double weights = 0.0;
            for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                // added on August 15, 2015
                if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                
                if (isNormed)
                    weights += Math.abs(seg.fields1[ii] * weight[ii]);
                else
                    weights += Math.abs(seg.fields0[ii] * weight[ii]);
                
                // added on June 12, 2016
                // here I do not consider distFreq
            }
            
            out.print(seg.clusterID);
            out.print("," + seg.route);
            out.print("," + seg.begMP);
            out.print("," + seg.endMP);
            out.print("," + (seg.endMP - seg.begMP));
            out.print("," + weights);
            if (seg.preMerg == true) out.print("," + seg.preDist.dist);
            else out.print("," + (-1));
            if (seg.nxtMerg == true) out.print("," + seg.nxtDist.dist);
            else out.print("," + (-1));
            out.print("," + seg.uniSegIndexBeg);
            out.print("," + seg.uniSegIndexEnd);
            out.print("," + seg.preMerg);
            out.print("," + seg.nxtMerg);
            for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                // added on August 15, 2015
                if (ii != INDEX_INCIDENT)
                    if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
                                
                if (isNormed) out.print("," + seg.fields1[ii]);
                else          out.print("," + seg.fields0[ii]);
            }
            
            if (USE_DISTRIBUTE) {
                // added on June 12, 2016
                out.print("," + seg.distFreq);
            }
            
            out.println();
            seg = seg.nxt;
        }

        out.close();
    }
    
    /**
     * build initial clustering data [8/15/18]
     * Checked for several combined segments on July 18, 2015
     * @param detCombineVaribles 
     */
    public static void buildClusterSegByCombineSameUniSeg(double detCombineVaribles) {
        double det;
        ClusterSegment lastCSeg = null, cSeg;
        UniSegment uSeg;
        Boolean isMerge;
        
        numSegment = 0;
        
        for (int ii = 0; ii < uniSegments.length; ii++) {
            uSeg = uniSegments[ii];
            
            isMerge = true;
            if (lastCSeg == null) {
                isMerge = false;
            }
            else {
                if (lastCSeg.route == uSeg.route && lastCSeg.endMP == uSeg.begMP && lastCSeg.nxtMerg == true && uSeg.preMerg == true) {
                    for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                        if (jj == INDEX_INCIDENT) continue;
                        
                        // added on August 15, 2015 - for clustering
                        if (fieldCtrl_cluster[jj] == 0 || fieldCtrl_cluster[jj] == 0.0) continue;
                        
                        det = Math.abs(lastCSeg.fields1[jj] - uSeg.fields1[jj]);
                        if (det > detCombineVaribles) {
                            isMerge = false;
                            break;
                        }
                    }
                }
                else {
                    isMerge = false;
                }
            }
            
            if (isMerge == true) {
                double oldLen, newLen, uLen;
                oldLen = lastCSeg.endMP - lastCSeg.begMP;
                uLen = uSeg.endMP - uSeg.begMP;
                newLen = oldLen + uLen;
                lastCSeg.endMP = uSeg.endMP;
                for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                    // for building data including all fields
                    if (jj == INDEX_INCIDENT) {
                        lastCSeg.fields1[jj] += uSeg.fields1[jj];
                        lastCSeg.fields0[jj] += uSeg.fields0[jj];
                        
                        if (USE_DISTRIBUTE) {
                            // added on June 12, 2016
                            lastCSeg.distFreq += uSeg.distFreq;
                        }
                    }
                    else {
                        lastCSeg.fields1[jj] = (lastCSeg.fields1[jj] * oldLen + uSeg.fields1[jj] * uLen) / newLen;
                        lastCSeg.fields0[jj] = (lastCSeg.fields0[jj] * oldLen + uSeg.fields0[jj] * uLen) / newLen;
                    }
                }
                lastCSeg.nxtMerg = lastCSeg.nxtMerg && uSeg.nxtMerg;
                lastCSeg.uniSegIndexEnd = ii;
            }
            else {
                cSeg = new ClusterSegment(MAX_NUM_FIELDS);
                numSegment++;
                cSeg.begMP = uSeg.begMP;
                cSeg.endMP = uSeg.endMP;
                for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                    // for building data including all fields
                    cSeg.fields1[jj] = uSeg.fields1[jj];
                    cSeg.fields0[jj] = uSeg.fields0[jj];
                }
                
                if (USE_DISTRIBUTE) {
                    // added on June 12, 2016
                    cSeg.distFreq = uSeg.distFreq;
                }
                
                cSeg.nxt = null;
                cSeg.nxtMerg = uSeg.nxtMerg;
                cSeg.nxtDist = null;
                if (lastCSeg == null) {
                    cSeg.pre = null;
                    clusterSegmentLink = cSeg;
                }
                else {
                    cSeg.pre = lastCSeg;
                    lastCSeg.nxt = cSeg;
                }
                cSeg.preDist = null;
                cSeg.preMerg = uSeg.preMerg;
                cSeg.route = uSeg.route;
                cSeg.uniSegIndexBeg = ii;
                cSeg.uniSegIndexEnd = ii;
                lastCSeg = cSeg;
            }
        }
    }
    
    /**
     * Tested for the first and the last records and the route ID = 2
     * on July 18, 2015
     * @param dir
     * @param wtFile 
     * @param readWeightsFirstLine
     */
    public static void readWeight(String dir, String wtFile, int readWeightsFirstLine) {
        int ii = -1;
        Path vcPath = Paths.get(dir + "//" + wtFile);
        Charset charset = Charset.forName("US-ASCII");
        int removeComma = 0;

        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry;
            while((entry = vcIn.readLine()) != null) {
                if (readWeightsFirstLine == 1) {
                    readWeightsFirstLine = 0;
                    removeComma = 1;
                }
                else {
                    ii++;
                    String txts[] = entry.split(",");
                    if (removeComma == 0) {
                        weight[ii] = Double.valueOf(txts[1]);
                    }
                    else {
                        String txt = txts[1];
                        txt = txt.substring(1, txt.length() - 1);
                        weight[ii] = Double.valueOf(txt);
                        System.out.println(weight[ii]);
                    }
                }
            }
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + wtFile);
        }
    }
    
    public static void readFieldCtrlFile(String dir, String wtFile) {
        int ii = -1;
        Path vcPath = Paths.get(dir + "//" + wtFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) {
                ii++;
                String txts[] = entry.split(",");
                fieldCtrl_cluster[ii] = Double.valueOf(txts[1]);
                fieldCtrl_modeling[ii] = Double.valueOf(txts[2]);
            }
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + wtFile);
        }
    }
    
    /**
     * Tested for the first and the last records and the route ID = 2
     * on July 18, 2015
     */
    public static void setUniSegMerageable() {
        UniSegment seg;
        int lastRouteID = -100;
        for (int ii = 0; ii < uniSegments.length; ii++) {
            seg = uniSegments[ii];
            if (seg.route != lastRouteID) {
                if (ii != 0) {
                    uniSegments[ii - 1].nxtMerg = false;
                }
                seg.preMerg = false;
            }
            lastRouteID = seg.route;
        }
        uniSegments[uniSegments.length - 1].nxtMerg = false;
    }
    
    /**
     * Read all fields to uniSegments [checked 8/15/15]
     * Tested on July 18, 2015
     * @param dir
     * @param uniSegFile 
     */
    public static void readUniSegments(String dir, String uniSegFile) {
        int numSeg = 0, ii = -1;
        Path vcPath = Paths.get(dir + "//" + uniSegFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) numSeg++;
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + uniSegFile);
        }
        
        uniSegments = new UniSegment[numSeg];
        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) {
                ii++;
                UniSegment seg = new UniSegment(MAX_NUM_FIELDS);
                String[] txts = entry.split(",");
                seg.route = Integer.valueOf(txts[0]);
                seg.begMP = Double.valueOf(txts[1]);
                seg.endMP = Double.valueOf(txts[2]);
                // modified on March 23, 2016
                for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                    seg.fields1[jj] = Double.valueOf(txts[3 + jj]);
                    // the last is 3 + MAX_NUM_FIELDS - 1
                }
                for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                    // the first is 3 + 0 + MAX_NUM_FIELDS
                    seg.fields0[jj] = Double.valueOf(txts[3 + jj + MAX_NUM_FIELDS]);
                }
                
                if (USE_DISTRIBUTE) {
                    // added on June 12, 2016
                    seg.distFreq = Double.valueOf(txts[3 + MAX_NUM_FIELDS * 2]);
                }
                
                uniSegments[ii] = seg;
            }
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + uniSegFile);
        }
    }
}
