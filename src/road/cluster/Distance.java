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
public class Distance {
    // id is only used to debug
    public int id;
    public ClusterSegment preSeg, nxtSeg;
    public Distance pre, nxt;
    public double dist;
    public int idxBinData;
    public Distance() {
        preSeg = nxtSeg = null;
        pre = nxt = null;
        idxBinData = -1;
    }
}
