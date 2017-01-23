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
 */
import java.util.*;

public class HorizontalCurveSegment extends BaseSegment {
    public double MAX_ADJUST_RADIUS = 100000;
    	
    public static String [] strAttributes = {"HCType", "HCDir"};
    public static String [] numAttributes = {"HCRadius", "HCMSE", "HCLength"};
    public String hcType, hcDirection;
    public double hcRadius, hcMSE, hcLength;
    public static HashMap<String, NumericAttributePackage> horizontalPackage = new HashMap<String, NumericAttributePackage>();
    public boolean normalized = false;
    
    // added by Xingsheng Wang on June 26, 2015
    // hcRecR = 1.0 / hcRadius
    // public double hcRecR;

    static {
        for (int it = 0; it < numAttributes.length; ++it) {
            horizontalPackage.put(numAttributes[it], new NumericAttributePackage());
        }
    }

    /*
    HorizontalCurveSegment(double HORIZONTAL_R){
        assignBase();

        hcType = hcDirection = "_";
        hcRadius = hcMSE = hcLength = 0;
        
        // added by Xingsheng Wang on June 26, 2015
        // hcRecR = 0.0;
        
        // added by Xingsheng Wang on July 17, 2015
        // the first default is 40000
        hcRadius = HORIZONTAL_R;
    }
    */

    HorizontalCurveSegment(String entry) {
        String [] values = entry.split(",");

        assignBase(values);

        hcType = values[7];
        hcRadius = Double.parseDouble(values[8]);
        hcMSE = Double.parseDouble(values[9]);
        hcLength = Double.parseDouble(values[10]);
        hcDirection = values[11];
        
        // added by Xingsheng Wang on June 26, 2015
        /*
        if (hcRadius == 0 || hcRadius == 0.0) {
            hcRecR = Double.MAX_VALUE;
        }
        else {
            hcRecR = MAX_ADJUST_RADIUS * 1.0 / hcRadius;
        }
        */
        
        // modified on March 23, 2016
        double hcR;
        if (hcRadius == 0 || hcRadius == 0.0) {
            hcR = 0.0;
        }
        else {
            hcR = 1.0 / hcRadius;
        }
        hcRadius = hcR;

        horizontalPackage.get("HCRadius").adjustRange(hcRadius);
        horizontalPackage.get("HCMSE").adjustRange(hcMSE);
        horizontalPackage.get("HCLength").adjustRange(hcLength);
    }

    @Override
    public boolean normalizeData() {

        if (!normalized) {
            NumericAttributePackage item = horizontalPackage.get("HCRadius");
            hcRadius = (hcRadius - item.min) / (item.max - item.min);
            item = horizontalPackage.get("HCMSE");
            hcMSE = (hcMSE - item.min) / (item.max - item.min);
            item = horizontalPackage.get("HCLength");
            hcLength = (hcLength - item.min) / (item.max - item.min);

            normalized = true;

            return true;
        }

        return false;
    }

    @Override
    public boolean restoreData() {

        if (normalized) {
            NumericAttributePackage item = horizontalPackage.get("HCRadius");
            hcRadius = item.restore(hcRadius);
            item = horizontalPackage.get("HCMSE");
            hcMSE = item.restore(hcMSE);
            item = horizontalPackage.get("HCLength");
            hcLength = item.restore(hcLength);

            normalized = false;

            return true;
        }

        return false;
    }

    public void outputHorizontal(int line) {
        System.out.println(line + "\tHCType: " + hcType + "\tHCRadius: " + hcRadius + "\tHCMSE: " + hcMSE + 
            "\tHCLength: " + hcLength + "\tHCDir: " + hcDirection);
    }	

}

