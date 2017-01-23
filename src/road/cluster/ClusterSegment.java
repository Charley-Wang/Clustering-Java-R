/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.cluster;

/**
 *
 * @author Charley (Xingsheng) Wang on July 10, 2015
 *          xingshengw@gmail.com
 */
public class ClusterSegment {
    // id is only used to debug
    public int id;
    public int clusterID;
    public int route;
    public double begMP, endMP;
    public int uniSegIndexBeg, uniSegIndexEnd;
    // modified on March 23, 2016
    public double[] fields0, fields1;
    // public double adt, ind;
    public boolean preMerg, nxtMerg;
    public ClusterSegment pre, nxt;
    public Distance preDist, nxtDist;
    // added on June 12, 2016
    public double distFreq;
    public ClusterSegment(int numFields) {
        preDist = null;
        nxtDist = null;
        fields0 = new double[numFields];
        fields1 = new double[numFields];
    }
}
