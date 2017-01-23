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
 */
public class FixLengthSegment2 {
    public int route;
    public double beginMP, endMP;
    public double[] fields0;
    // added on March 3, 2016 for generating the second fields, the original data set
    public double[] fields1;
    public boolean mergeablePre, mergeableNext;
    public double disFreq;

    public FixLengthSegment2(int numFields) {
        route = 0;
        beginMP = endMP = 0;
        fields0 = new double[numFields];
        // added on March 3, 2016 for generating the second fields, the original data set
        fields1 = new double[numFields];
        
        for (int ii = 0; ii < numFields; ii++) fields0[ii] = 0.0;
        //                         0      1     2
        // public static Field[] hcMSE, hcLen, hcR;
        // this is speical for hcR = 40000 for the flat road
        // fields1[2] = 40000;
        // after normalization, it should be 1
        // fields1[2] = 1;
        
        // added on March 3, 2016 for generating the second fields, the original data set
        for (int ii = 0; ii < numFields; ii++) fields1[ii] = 0.0;
        // fields0[2] = horizontalMaxR;

        // added on June 11, 2016
        disFreq = 0.0;
        
        mergeablePre = true;
        mergeableNext = true;
    }
}
