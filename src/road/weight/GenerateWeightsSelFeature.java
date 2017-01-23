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
import java.util.Random;

/**
 *
 * @author Xingsheng Wang
 */
public class GenerateWeightsSelFeature {
    public static final int MAX_NUM_FIELDS = 16;
    public static double[] weight = new double[MAX_NUM_FIELDS];
    public static double[] weight2 = new double[MAX_NUM_FIELDS];
    //                                  0      1    2    3    4     5    6     7       8      9    10   11  12  13   14  15
    public static String fieldsName = "hcMSE,hcLen,hcR,vcLen,absPG,vcK,incLan,decLan,incWid,decWid,sdL,sdR,sdLC,sdRC,adt,ind";
    
    // added on July 9, 2016
    public static double[] fieldCtrl_cluster = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_modeling = new double[MAX_NUM_FIELDS];
    
    // example
    // C://Users//li//Desktop//WXS//Chifan//05_MyWorkingProjects//01_Road//Predict31//data420m weight 1 0.5 fieldCtrl.csv
    
    /**
     * 
     * @param args ./data75 weight 1 percent
     * @throws IOException 
     */
    public static void main(String [] args) throws IOException {
        String dir = args[0];
        String preWeightFile = args[1];
        int currStepNum = Integer.valueOf(args[2]);
        double percent = Double.valueOf(args[3]);
        boolean isrand = false;
        double rand;

        Random rnd = new Random();
        if (args.length == 6) isrand = true;
        
        readWeight(dir, preWeightFile + String.valueOf(currStepNum - 1) + ".csv");
        readFieldCtrlFile(dir, args[4]);
        
        int seqNum = 0;
        for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
            if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
            for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) weight2[jj] = weight[jj];
            for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                if (ii != jj) continue;
                if (isrand == true) rand = rnd.nextDouble();
                else rand = 1.0;
                weight2[jj] = weight[jj] * (1.0 + percent * rand);
            }
            seqNum++;
            saveWeights(dir + "//" + preWeightFile + currStepNum + "." + seqNum + ".csv");
        }
        
        for (int ii = 0; ii < MAX_NUM_FIELDS; ii++) {
            if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
            for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) weight2[jj] = weight[jj];
            for (int jj = 0; jj < MAX_NUM_FIELDS; jj++) {
                if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
                if (ii != jj) continue;
                if (isrand == true) rand = rnd.nextDouble();
                else rand = 1.0;
                weight2[jj] = weight[jj] * (1.0 - percent * rand);
            }
            seqNum++;
            saveWeights(dir + "//" + preWeightFile + currStepNum + "." + seqNum + ".csv");
        }
        
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
    
    public static void saveWeights(String outFileName) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFileName, false)));
        
        String txts[] = fieldsName.split(",");
        for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
            out.print(txts[kk]);
            out.println("," + weight2[kk]);
        }

        out.close();
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
