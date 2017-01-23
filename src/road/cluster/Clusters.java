/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.cluster;

import java.io.IOException;

/**
 *
 * @author li
 */
public class Clusters {
    public static void main(String [] args) throws IOException {
        // C://Users//li//Desktop//WXS//Chifan//05_MyWorkingProjects//01_Road//Predict60//data 
        // uniSeg.csv weight0.csv 0.0000001 autoCluster.1.csv 3000 cluster.1.csv fieldCtrl.csv cluster 0
        
//        for (int ii = 1000; ii <= 19598; ii += 1000) {
//            System.out.println(ii);
//            args[4] = "autoCluster_" + String.valueOf(ii) + ".csv";
//            args[5] = String.valueOf(ii);
//            args[6] = "cluster_" + args[5] + ".csv";
//            Cluster.main(args);
//        }
        
        for (int ii = 1000; ii <= 18600; ii += 1000) {
            System.out.println(ii);
            args[4] = "autoCluster_" + String.valueOf(ii) + ".csv";
            args[5] = String.valueOf(ii);
            args[6] = "cluster_" + args[5] + ".csv";
            Cluster.main(args);
        }
    }

}
