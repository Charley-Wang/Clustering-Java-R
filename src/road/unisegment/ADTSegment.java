/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.unisegment;

import java.util.HashMap;

/**
 *
 * @author Xingsheng Wang on June 25, 2015
 *          email: charleyxswang@gmail.com
 */
public class ADTSegment extends BaseSegment {
    public static String [] numAttributes = {"ADT"};
    public double adt;
    public static HashMap<String, NumericAttributePackage> adtPackage = new HashMap<String, NumericAttributePackage>();
    public boolean normalized = false;

    static {
        for (int it = 0; it < numAttributes.length; ++it) {
            adtPackage.put(numAttributes[it], new NumericAttributePackage());
        }
    }

    ADTSegment(){
        assignBase();

        adt = 0;
    }

    ADTSegment(String entry) {
        String [] values = entry.split(",");

        route = Integer.parseInt(values[0]);
        beginMP = Double.parseDouble(values[1]);
        endMP = Double.parseDouble(values[2]);
        adt = Double.parseDouble(values[3]);

        adtPackage.get("ADT").adjustRange(adt);
    }

    @Override
    public boolean normalizeData() {

        if (!normalized) {
            NumericAttributePackage item = adtPackage.get("ADT");
            adt = (adt - item.min) / (item.max - item.min);

            normalized = true;

            return true;
        }

        return false;
    }

    @Override
    public boolean restoreData() {

        if (normalized) {
            NumericAttributePackage item = adtPackage.get("ADT");
            adt = item.restore(adt);

            normalized = false;

            return true;
        }

        return false;
    }

    public void outputHorizontal(int line) {
        System.out.println(line + "\tADT: " + adt);
    }	
}
