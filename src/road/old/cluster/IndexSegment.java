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
public class IndexSegment {
    // id is only used to debug
    public int id;
    public int route;
    public double begMP, endMP;
    public double index;
    public double incFreq;
    public boolean preMerg, nxtMerg;
    public IndexSegment pre, nxt;
    public Distance preDist, nxtDist;
    public IndexSegment() {
        preDist = null;
        nxtDist = null;
    }
}
