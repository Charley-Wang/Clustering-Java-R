/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.unisegment;

/**
 *
 * @author li
 */
public class Route {
    public int route;
    public int numSegments;
    public double minMP, maxMP;
    // freq is used only to debug and test
    public double[] freq;
    public FixLengthSegment2[] fixSegments;
    public Route() {
        freq = new double[9];
    }
}
