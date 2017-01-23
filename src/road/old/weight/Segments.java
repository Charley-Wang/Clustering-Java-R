/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.old.weight;

/**
 *
 * @author li
 */
public class Segments {
    public int route;
    public double begMP, endMP;
    public double[] fields;
    public double index;
    public boolean used;
    public boolean preMerg, nxtMerg;
    public Segments(int numFields) {
        index = 0.0;
        used = true;
        preMerg = true;
        nxtMerg = true;
        fields = new double[numFields];
    }
}
