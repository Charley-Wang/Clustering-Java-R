/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.randweight;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

/**
 *
 * @author li
 */
public class RandomUpdateWeight {
    public static final int MAX_NUM_FIELDS = 16;
    public static String fieldsName = "hcMSE,hcLen,hcR,vcLen,absPG,vcK,incLan,decLan,incWid,decWid,sdL,sdR,sdLC,sdRC,adt,ind";
    public static double[] weight = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_cluster = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_modeling = new double[MAX_NUM_FIELDS];
    
    /**
     * dir wtFile fieldCtrlFile 10 outFile
     * @param args
     * @throws IOException 
     */
    public static void main(String [] args) throws IOException {
        String dir = args[0];
        String wtFile = args[1];
        String fieldCtrlFile = args[2];
        double detPercent = Double.valueOf(args[3]);
        String outFile = args[4];
                
        readFieldCtrlFile(dir, fieldCtrlFile);
        readWeight(dir, wtFile);
        updateWeights(detPercent);
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
    
    public static void updateWeights(double detPercent) {
        int numFields = 0;
        Random rnd = new Random();
        
        for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
            if (fieldCtrl_cluster[kk] == 0 || fieldCtrl_cluster[kk] == 0.0) continue;
            numFields++;
        }
        
        // rnd.nextInt(6); return one of 0…5
        int modifyFieldNum = 0, rndNum, jj;
        jj = -1;
        rndNum = rnd.nextInt(numFields);
        for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
            if (fieldCtrl_cluster[kk] == 0 || fieldCtrl_cluster[kk] == 0.0) continue;
            jj++;
            if (jj == rndNum) {
                modifyFieldNum = kk;
                break;
            }
        }
        
        // rnd.nextDouble(); return [0.0,1.0]
        double det = rnd.nextDouble() * detPercent / 100.0;
        if (rnd.nextBoolean()) {
            weight[modifyFieldNum] = weight[modifyFieldNum] * (1.0 + det);
        }
        else {
            weight[modifyFieldNum] = weight[modifyFieldNum] * (1.0 - det);
        }
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
