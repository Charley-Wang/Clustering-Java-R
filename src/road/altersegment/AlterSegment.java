/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.altersegment;

/**
 *
 * @author Charley (Xingsheng) Wang on July 11, 2015
 *          xingshengw@gmail.com
 */
public class AlterSegment {
    public int clusterID1, clusterID2, idxClusterPairSeg, midIdxUniSegment;
    public double len;
    public double[] fields;
    // iDist is the inner distance within two clustered segments
    // public double adt, ind, iDist;
    // dist are the inner distance within two clustered segments for different fields
    public double[] dist1;
    public double[] dist2;
    public AlterSegment nxt;
    public AlterSegment(int numFields) {
        fields = new double[numFields];
        dist1 = new double[numFields];
        dist2 = new double[numFields];
    }
}
