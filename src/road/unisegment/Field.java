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
public class Field {
    public int SR;
    // modify on March 23, 2016
    // value0 for original value
    // value1 for the normalized value
    // public double begMP, endMP, value;
    public double begMP, endMP, value0, value1;
    public Field() {
        SR = 0;
        begMP = 0;
        endMP = 0;
        // modify on March 23, 2016
        // value = 0;
        value1 = 0;
        value0 = 0;
    }
}
