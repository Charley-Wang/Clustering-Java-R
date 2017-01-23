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

/**
 *
 * @author Xingsheng Wang on Jan 14, 2016
 */
public class UpdateWithGrad {
    public static final int MAX_NUM_FIELDS = 16;
    public static double delta, lammda, gsum;
    public static double [] grad = new double[15];
    public static double[] weight = new double[MAX_NUM_FIELDS];
    public static String fieldsName = "hcMSE,hcLen,hcR,vcLen,absPG,vcK,incLan,decLan,incWid,decWid,sdL,sdR,sdLC,sdRC,adt,ind";

    // added on July 8, 2016
    public static double[] fieldCtrl_cluster = new double[MAX_NUM_FIELDS];
    public static double[] fieldCtrl_modeling = new double[MAX_NUM_FIELDS];

    public static void main(String [] args) throws IOException {
        //                                                      0        1              2            3            4      5      6    7 ...
        // java -cp $fold/road.jar road.weight.UpdateWithGrad $dat weight$jjj.csv weight$iii.csv fieldCtrl.csv $delta $lammda $gsum ${grad[0]} ${grad[1]}
        // new example on April 5, 2016, modified on July 8, 2016 for different feature selcection according to the control file
        // lammda = 0 for no regularization
        // C://Users//li//Desktop//WXS//Chifan//05_MyWorkingProjects//01_Road//Predict31//data420m 
        // weight0.csv weight1.csv fieldCtrl.csv 0.5    1.0   6.6   0.1 0.2 0.3 0.4 0.5 0.6 0.7 0.8 0.9 1.0 1.1
        //       1           2           3        4      5     6     7 ...
        //                                      delta lammda  gsum   gradidents
        String dir = args[0];
        String inFile = args[1];
        String outFile = args[2];
        String fieldCtrlFile = args[3];
        delta = Double.valueOf(args[4]);
        lammda = Double.valueOf(args[5]);
        gsum = Double.valueOf(args[6]);
        
        readWeight(dir, inFile);
        readFieldCtrlFile(dir, fieldCtrlFile);
        
        // update on July 8, 2016
        int seqNum = -1;
        for (int ii = 0; ii < MAX_NUM_FIELDS - 1; ii++) {
            if (fieldCtrl_cluster[ii] == 0 || fieldCtrl_cluster[ii] == 0.0) continue;
            seqNum++;
            grad[ii] = Double.valueOf(args[7 + seqNum]);
            weight[ii] = weight[ii] * (1.0 - lammda) + weight[ii] * grad[ii] * delta / gsum + lammda;
        }
        
        saveWeights(dir + "//" + outFile);
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
    
    public static void saveWeights(String outFileName) throws IOException {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outFileName, false)));
        
        String txts[] = fieldsName.split(",");
        for (int kk = 0; kk < MAX_NUM_FIELDS; kk++) {
            out.print(txts[kk]);
            out.println("," + weight[kk]);
        }

        out.close();
    }
}
