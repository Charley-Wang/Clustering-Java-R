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
public class NumericAttributePackage {
    public double min, max;

    NumericAttributePackage() {
        min = Double.MAX_VALUE;
        max = -Double.MAX_VALUE;
    }

    public void adjustRange(double value) {
        adjustMin(value);
        adjustMax(value);
    }

    public boolean adjustMin(double newMin) {
        if (newMin < min) {
            min = newMin;
            return true;
        }
        return false;
    }

    public boolean adjustMax(double newMax) {
        if (newMax > max) {
            max = newMax;
            return true;
        }
        return false;
    }

    public double normalize(double raw) {
        return (raw - min) / (max - min);
    }

    public double restore(double normalized) {
        return normalized * (max - min) + min;
    }
}
