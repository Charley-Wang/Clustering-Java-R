/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.weight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import road.altersegment.AlterSegment;

/**
 *                                                                             1            2           3            4         5           6
 * @commParam C://Users//li//Desktop//WXS//Chifan//17_Road//Data//clean alter_dist.csv weight.csv alter_err.csv 0.00001 weight2.csv fieldCtrl.csv
 * @author Charley (Xingsheng) Wang on July 11, 2015
 *          xingshengw@gmail.com
 */
public class UpdateWeights {
    public static final int MAX_NUM_FIELDS = 16;
    public static String fieldsName = "hcMSE,hcLen,hcR,vcLen,absPG,vcK,incLan,decLan,incWid,decWid,sdL,sdR,sdLC,sdRC,adt,ind";
    public static String[] fieldsNameArray = {"hcMSE","hcLen","hcR","vcLen","absPG","vcK","incLan",
        "decLan","incWid","decWid","sdL","sdR","sdLC","sdRC","adt","ind"};
    public static AlterSegment[] alterSegments;
    public static double[] weight = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_cluster = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_modeling = new double[MAX_NUM_FIELDS];
    public static double[] errors;
    public static double[] avgPairSegErrors;
    public static int INDEX_INCIDENT = 15;
    
    public static void main(String [] args) throws IOException {
        String dir = args[0];
        String alterFile = args[1];
        String wtFile = args[2];
        String errFile = args[3];
        double CONSTANT_K = Double.valueOf(args[4]);
        String outFile = args[5];
        String fieldCtrlFile = args[6];
        
        readFieldCtrlFile(dir, fieldCtrlFile);
        readWeight(dir, wtFile);
        createWeightSegments(dir, alterFile);
        readErrors(dir, errFile);
                
        calClusterErros();
        updateWeights(CONSTANT_K);
        saveWeights(dir + "//" + outFile);
    }
    
    public static void saveWeights(String outFileName) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFileName, false)));
        
        String txts[] = fieldsName.split(",");
        for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
            out.print(txts[kk]);
            if (fieldCtrl_cluster[kk] == 0 || fieldCtrl_cluster[kk] == 0.0) out.println(",0.0");
            else out.println("," + weight[kk]);
        }

        out.close();
    }
    
    public static void updateWeights(double CONSTANT_K) {
        double ebar, d1, d2, e, dw;
        AlterSegment seg;
        
        for (int ii = 0; ii < alterSegments.length; ii++) {
            seg = alterSegments[ii];
            for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
                // added on August 16, 2015
                if (fieldCtrl_cluster[kk] == 0 || fieldCtrl_cluster[kk] == 0.0) continue;
                
                ebar = avgPairSegErrors[seg.idxClusterPairSeg];
                e = errors[ii];
                d1 = seg.dist1[kk];
                d2 = seg.dist2[kk];
                dw = CONSTANT_K * (d2 - d1) * (ebar - e);
                weight[kk] += dw * weight[kk];
            }
        }
    }
    
    /**
     * Tested on July 21, 2015
     */
    public static void calClusterErros() {
        avgPairSegErrors = new double[alterSegments[alterSegments.length - 1].idxClusterPairSeg + 1];
        for (int ii = 0; ii < avgPairSegErrors.length; ii++) avgPairSegErrors[ii] = 0.0;
        int lastID = - 1;
        int num = 0;
        for (int ii = 0; ii < alterSegments.length; ii++) {
            if (alterSegments[ii].idxClusterPairSeg == lastID) {
                num++;
            }
            else {
                if (lastID != -1) avgPairSegErrors[lastID] /= num;
                num = 1;
                lastID = alterSegments[ii].idxClusterPairSeg;
            }
            avgPairSegErrors[alterSegments[ii].idxClusterPairSeg] += errors[ii];
        }
        avgPairSegErrors[lastID] /= num;
    }
   
    /**
     * Tested on July 21, 2015
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
    
    /**
     * Tested on July 21, 2015
     * @param dir
     * @param errFile 
     */
    public static void readErrors(String dir, String errFile) {
        int num = 0;
        Path vcPath = Paths.get(dir + "//" + errFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) {
                num++;
            }
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + errFile);
        }
        
        errors = new double[num];
        
        num = -1;
        String[] txts;
        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) {
                num++;
                txts = entry.split(",");
                errors[num] = Double.valueOf(txts[1]);
            }
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + errFile);
        }
    }
    
    /**
     * Tested on July 21, 2015
     * @param dir
     * @param wtSegFile 
     */
    public static void createWeightSegments(String dir, String wtSegFile) {
        int numSeg = 0, jj = -1;
        Path vcPath = Paths.get(dir + "//" + wtSegFile);
        Charset charset = Charset.forName("US-ASCII");

        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) numSeg++;
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + wtSegFile);
        }
        
        alterSegments = new AlterSegment[numSeg];
        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) {
                jj++;
                AlterSegment seg = new AlterSegment(MAX_NUM_FIELDS);
                String[] txts = entry.split(",");
                
                seg.idxClusterPairSeg = Integer.valueOf(txts[0]);
                seg.clusterID1 = Integer.valueOf(txts[1]);
                seg.clusterID2 = Integer.valueOf(txts[2]);
                seg.midIdxUniSegment = Integer.valueOf(txts[3]);
                
                int kk = -1;
                for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
                    // modified on August 16, 2015
                    if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                    kk++;
                    seg.dist1[ii] = Double.valueOf(txts[3 + kk * 2 + 1]);
                    seg.dist2[ii] = Double.valueOf(txts[3 + kk * 2 + 2]);
                }

                alterSegments[jj] = seg;
            }
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + wtSegFile);
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
}
