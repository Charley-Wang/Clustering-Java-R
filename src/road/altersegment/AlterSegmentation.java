/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.altersegment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import road.cluster.UniSegment;

/**
 * @commParam C://Users//li//Desktop//WXS//Chifan//17_Road//Data//clean train_uniform_Full.csv weight.csv alter fieldCtrl.csv
 * @author Charley (Xingsheng) Wang on July 11, 2015
 *          xingshengw@gmail.com
 * 
 */
public class AlterSegmentation {
    public static final int MAX_NUM_FIELDS = 16;
    //                                   0     1    2    3     4    5    6      7      8      9     10  11  12  13   14  15
    public static String fieldsName = "hcMSE,hcLen,hcR,vcLen,absPG,vcK,incLan,decLan,incWid,decWid,sdL,sdR,sdLC,sdRC,adt,ind";
    public static String[] fieldsNameArray = {"hcMSE","hcLen","hcR","vcLen","absPG","vcK","incLan",
        "decLan","incWid","decWid","sdL","sdR","sdLC","sdRC","adt","ind"};
    public static String fieldsNameWithoutInd = "hcMSE,hcLen,hcR,vcLen,absPG,vcK,incLan,decLan,incWid,decWid,sdL,sdR,sdLC,sdRC,adt";
    public static int INDEX_INCIDENT = 15;
    
    public static UniSegment[] uniSegments;
    public static double[] weight = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_cluster = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_modeling = new double[MAX_NUM_FIELDS];
    public static SimpleClusterSegment[] clusterSegments;
    public static AlterSegment alterSegmentLink;
    
    public static void main(String [] args) throws IOException {
        String dir = args[0];
        String uniSegFile = args[1];
        String wtFile = args[2];
        String alterFile = args[3];
        String fieldCtrlFile = args[4];
        
        readFieldCtrlFile(dir, fieldCtrlFile);
        readWeight(dir, wtFile);
        readUniSegments(dir, uniSegFile);
        
        createClusterSegments();
        createAlterSegments();
        
        saveAlterSegments(dir + "//" + alterFile + ".csv", false, false);
        saveAlterSegments(dir + "//" + alterFile + "_Full.csv", true, false);
        saveAlterSegments(dir + "//" + alterFile + "_dist.csv", false, true);
    }
    
    /**
     * Tested!
     * @param fileName
     * @param isFullFormat
     * @param isDist
     * @throws IOException 
     */
    public static void saveAlterSegments(String fileName, boolean isFullFormat, boolean isDist) throws IOException {
        AlterSegment seg = alterSegmentLink;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, false)));
        
        if (isDist || isFullFormat) {
            out.print("idxClusterPairSeg,clusterID1,clusterID2,midIdxUniSegment");
        }
        if (isDist) {
            // modified on 8/15/15
            // out.print("hcMSE1,hcMSE2,hcLen1,hcLen2,hcR1,hcR2,vcLen1,vcLen2,absPG1,absPG2,"
            //        + "vcK1,vcK2,incLan1,incLan2,decLan1,decLan2,incWid1,incWid2,"
            //        + "decWid1,decWid2,sdL1,sdL2,sdR1,sdR2,sdLC1,sdLC2,sdRC1,sdRC2,adt1,adt2,ind1,ind2");
            for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                out.print("," + fieldsNameArray[ii] + "1," + fieldsNameArray[ii] + "2");
            }
        }
        else {
            if (isFullFormat) out.print(",");
            out.print("ind,len");
            // out.print(fieldsNameWithoutInd);
            for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
                if (ii == INDEX_INCIDENT) continue;
                out.print("," + fieldsNameArray[ii]);
            }
        }
        out.println();
        
        while(seg != null) {
           
            if (isDist || isFullFormat) {
                out.print(seg.idxClusterPairSeg + ",");
                out.print(seg.clusterID1 + ",");
                out.print(seg.clusterID2 + ",");
                out.print(seg.midIdxUniSegment);
            }
            if (isDist) {
                for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                    if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                    out.print("," + seg.dist1[ii]);
                    out.print("," + seg.dist2[ii]);
                }
            }
            else {
                if (isFullFormat) out.print(",");
                out.print(seg.fields[INDEX_INCIDENT]);
                out.print("," + seg.len);
                for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                    if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
                    if (ii != INDEX_INCIDENT) out.print("," + seg.fields[ii]);
                }
            }
            out.println();

            seg = seg.nxt;
        }

        out.close();
    }
    
    /**
     * Checked valid segments [OK]
     * Checked cluster ID between 3 and 4 [OK for distances and segments]
     */
    public static void createAlterSegments() {
        int k = clusterSegments.length;
        AlterSegment seg1, seg2, lastSeg = null;
        double[] d1, d2;
        
        int numClusterPairSeg = 0, k1, k2, m1, m2, len1, len2;
        SimpleClusterSegment cSeg1, cSeg2;
        for (int ii = 0; ii < k - 1; ii++) {
            cSeg1 = clusterSegments[ii];
            len1 = cSeg1.uniSegIndexEnd - cSeg1.uniSegIndexBeg + 1;
            if (cSeg1.nxtMerg == false || len1 < 2) continue;
            
            cSeg2 = clusterSegments[ii + 1];
            len2 = cSeg2.uniSegIndexEnd - cSeg2.uniSegIndexBeg + 1;
            if (len2 < 2) continue;
            
            numClusterPairSeg++;
            
            k1 = cSeg1.uniSegIndexBeg;
            m1 = (int)(cSeg1.uniSegIndexBeg + len1 / 2.0);
            m2 = (int)(cSeg2.uniSegIndexBeg + len2 / 2.0 - 1);
            k2 = cSeg2.uniSegIndexEnd;
            
            //      Segment 1                   segment 2
            // (a_1, a_2, a_3, a_4) - (b_1, b_2, b_3, b_4, b_5, b_6)
            // a_1, a_2, …, b_6 are uniform segments which are fixed length of 0.01 mile.

            // [a_1 .. a_2] d_1 = distance between [a_2] and [a_1] for all attributes
            //		and d_2 = distance between [a_2] and [a_3..b_6]
            //          Calculate e_1 based on observation [a_1..a_2] for all attributes from NB model
            
            //[a_3 .. b_6] d_1 = distance between [a_3] and [a_4..b_6] for all attributes
            //		and d_2 = distance between [a_3] and [a_1..a_2]
            //  	Calculate e_2 based on observation [a_3..b_6]
            
            for (int kk = m1; kk <= m2; kk++) {
                // create the first alternative segment, [k1 ... kk]
                seg1 = mergeUniSegment(k1, kk - 1);
                seg2 = mergeUniSegment(kk + 1, k2);
                d1 = calDistance(uniSegments[kk], seg1);
                d2 = calDistance(uniSegments[kk], seg2);
                seg1 = merge(seg1, kk);
                seg2 = merge(seg2, kk);
                
                seg1.dist1 = d1;
                seg1.dist2 = d2;
                seg1.idxClusterPairSeg = numClusterPairSeg - 1;
                seg1.clusterID1 = uniSegments[k1].clusterID;
                seg1.clusterID2 = uniSegments[k2].clusterID;
                seg1.midIdxUniSegment = kk;
                
                if (lastSeg == null) alterSegmentLink = seg1;
                else lastSeg.nxt = seg1;
                lastSeg = seg1;
                
                // create the second alternative segment, [kk+1 ... k2]
                seg2.dist1 = d2;
                seg2.dist2 = d1;
                seg2.idxClusterPairSeg = numClusterPairSeg - 1;
                seg2.clusterID1 = uniSegments[k1].clusterID;
                seg2.clusterID2 = uniSegments[k2].clusterID;
                seg2.midIdxUniSegment = kk;
               
                lastSeg.nxt = seg2;
                lastSeg = seg2;
            }
        }
    }
        
    public static double[] calDistance(UniSegment seg1, AlterSegment seg2) {
        double[] d = new double[MAX_NUM_FIELDS];
        for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
            if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
            d[ii] = Math.abs(seg1.fields1[ii] - seg2.fields[ii]);
        }
        return d;
    }
   
    public static AlterSegment merge(AlterSegment seg1, int idx) {
        UniSegment seg2 = uniSegments[idx];
        
        double len1 = seg1.len;
        double len2 = seg2.endMP - seg2.begMP;
        double len = len1 + len2;
        double v1, v2;
        
        seg1.len = len;
        for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
            if (ii != INDEX_INCIDENT)
                if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
            v1 = seg1.fields[ii];
            v2 = seg2.fields1[ii];
            if (ii == INDEX_INCIDENT) seg1.fields[ii] = v1 + v2;
            else seg1.fields[ii] = (v1 * len1 + v2 * len2)/len;
        }
        
        return seg1;
    }
    
    public static AlterSegment mergeUniSegment(int idxBeg, int idxEnd) {
        UniSegment uSeg;
        AlterSegment seg = new AlterSegment(MAX_NUM_FIELDS);
        
        for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
            // if (fieldCtrl_modeling[ii] == 0 || fieldCtrl_modeling[ii] == 0.0) continue;
            seg.fields[ii] = 0;
        }
        
        for (int ii = idxBeg; ii <= idxEnd; ii++) {
            uSeg = uniSegments[ii];
            for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                if (jj != INDEX_INCIDENT)
                  if (fieldCtrl_modeling[jj] == 0 || fieldCtrl_modeling[jj] == 0.0) continue;
                seg.fields[jj] += uSeg.fields1[jj];
            }
        }

        double n = idxEnd - idxBeg + 1;
        for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
            if (fieldCtrl_modeling[jj] == 0 || fieldCtrl_modeling[jj] == 0.0) continue;
            seg.fields[jj] /= n;
        }
        seg.nxt = null;
        seg.len = uniSegments[idxEnd].endMP - uniSegments[idxBeg].begMP;

        return seg;
    }
    
    /**
     * Tested on July 20, 2015
     */
    public static void createClusterSegments() {
        int lastClusterID = -1;
        int numClusters = 0;
        for (int ii = 0; ii < uniSegments.length; ii++) {
            if (uniSegments[ii].clusterID != lastClusterID) {
                numClusters++;
                lastClusterID = uniSegments[ii].clusterID;
            }
        }
        
        clusterSegments = new SimpleClusterSegment[numClusters];
        int clusterJJ = -1;
        lastClusterID = -1;
        for (int ii = 0; ii < uniSegments.length; ii++) {
            if (uniSegments[ii].clusterID != lastClusterID) {
                clusterJJ++;
                
                SimpleClusterSegment cSeg = new SimpleClusterSegment();
                cSeg.clusterID = uniSegments[ii].clusterID;
                cSeg.uniSegIndexBeg = ii;
                clusterSegments[clusterJJ] = cSeg;
                
                if (clusterJJ > 0) {
                    clusterSegments[clusterJJ - 1].nxtMerg = uniSegments[ii - 1].nxtMerg;
                    clusterSegments[clusterJJ - 1].uniSegIndexEnd = ii - 1;
                }
                
                lastClusterID = uniSegments[ii].clusterID;
            }
        }
        
        clusterSegments[numClusters - 1].nxtMerg = false;
        clusterSegments[numClusters - 1].uniSegIndexEnd = uniSegments.length - 1;
    }
    
    /**
     * Tested on July 18, 2015
     * @param dir
     * @param wtFile 
     */
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
                
                seg.clusterID = Integer.valueOf(txts[0]);
                seg.route = Integer.valueOf(txts[1]);
                seg.begMP = Double.valueOf(txts[2]);
                seg.endMP = Double.valueOf(txts[3]);
                seg.used = Boolean.valueOf(txts[4]);
                seg.preMerg = Boolean.valueOf(txts[5]);
                seg.nxtMerg = Boolean.valueOf(txts[6]);
                
                int kk = -1;
                for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                    // added on August 15, 2015
                    if (jj != INDEX_INCIDENT)
                      if (fieldCtrl_modeling[jj] == 0 || fieldCtrl_modeling[jj] == 0.0) continue;
                    kk++;
                    seg.fields1[jj] = Double.valueOf(txts[7 + kk]);
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
