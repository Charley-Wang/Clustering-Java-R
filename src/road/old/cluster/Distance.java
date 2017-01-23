/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.old.cluster;

/**
 *
 * @author li
 */
public class Distance {
    // id is only used to debug
    public int id;
    public IndexSegment preSeg, nxtSeg;
    public Distance pre, nxt;
    public double dist;
    public int idxBinData;
    public Distance() {
        preSeg = nxtSeg = null;
        pre = nxt = null;
        idxBinData = -1;
    }
}
