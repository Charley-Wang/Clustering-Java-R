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
public class UniSegment {
    public int clusterID;
    public int route;
    public double begMP, endMP;
    // modified on March 23, 2016
    public double[] fields0, fields1;
    // public double adt, ind;
    public boolean used;
    public boolean preMerg, nxtMerg;
    // added on June 12, 2016
    public double distFreq;
    public UniSegment(int numFields) {
        used = true;
        preMerg = true;
        nxtMerg = true;
        fields0 = new double[numFields];
        fields1 = new double[numFields];
    }
}
