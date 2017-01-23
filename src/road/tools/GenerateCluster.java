/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.tools;

import java.io.IOException;
import road.cluster.*;

/**
 *
 * @author Xingsheng Wang on Dec 19, 2015
 */
public class GenerateCluster {
    public static void main(String [] args) throws IOException {
        for(int jj = 1; jj <= 11; jj++) {
            String stepNum = String.valueOf(jj);
            String autoClusterCut = "0.0000001";
            String numFinalCluster = "1000";
            // String dat = "C:\\Users\\li\\Desktop\\WXS\\Chifan\\05_MyWorkingProjects\\01_Road\\Predict7\\data81_weights";
            // String dat = "C:\\Users\\li\\Desktop\\WXS\\Chifan\\05_MyWorkingProjects\\01_Road\\Predict7\\data82_weights";
            // String dat = "C:\\Users\\li\\Desktop\\WXS\\Chifan\\05_MyWorkingProjects\\01_Road\\Predict7\\data83_weights";
            // String dat = "C:\\Users\\li\\Desktop\\WXS\\Chifan\\05_MyWorkingProjects\\01_Road\\Predict7\\data84_weights";
            // String dat = "C:\\Users\\li\\Desktop\\WXS\\Chifan\\05_MyWorkingProjects\\01_Road\\Predict8\\data85_weights";
            String dat = "C:\\Users\\li\\Desktop\\WXS\\Chifan\\05_MyWorkingProjects\\01_Road\\Predict8\\data86";
            String cmm[] = new String[11];

            int ii = 0;
            cmm[ii++] = dat;
            cmm[ii++] = "uniseg.csv";
            cmm[ii++] = "weight" + stepNum + ".csv";
            cmm[ii++] = autoClusterCut;
            cmm[ii++] = "autoCluster" + stepNum + ".csv";
            cmm[ii++] = numFinalCluster;
            cmm[ii++] = "cluster" + stepNum + ".csv";
            cmm[ii++] = "train" + stepNum;
            cmm[ii++] = "valid" + stepNum;
            cmm[ii++] = "fieldCtrl.csv";
            cmm[ii++] = "cluster";

            Cluster.main(cmm);
        }
    }
}
