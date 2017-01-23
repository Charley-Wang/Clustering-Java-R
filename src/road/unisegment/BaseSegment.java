/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.unisegment;

/**
 *
 * @author Xingsheng Wang on June 25, 2015
 *          email: charleyxswang@gmail.com
 *          based on the code of Joshua
 *
 */
public abstract class BaseSegment {
    public String beginAB, endAB;
    public int route;
    public double beginARM, endARM, beginMP, endMP;

    public void assignBase() {
        beginAB = endAB = "_";
        route = 0;
        beginARM = endARM = beginMP  = endMP = 0;
    }

    public void assignBase(String [] values) {
        route = Integer.parseInt(values[0]);
        beginARM = Double.parseDouble(values[1]);
        endARM = Double.parseDouble(values[2]);
        beginMP = Double.parseDouble(values[3]);
        beginAB = values[4];
        endMP = Double.parseDouble(values[5]);
        endAB = values[6];
    }

    public void outputBase() {
        System.out.println("Route: " + route + "\tBeginARM: " + beginARM + "\tEndARM: " + endARM + "\tBeginMP: " + 
            beginMP + "\tBeginAB: " + beginAB + "\tEndMP: " + endMP + "\tEndAB: " + endAB);		
    }

    public abstract boolean normalizeData();

    public abstract boolean restoreData();
}
