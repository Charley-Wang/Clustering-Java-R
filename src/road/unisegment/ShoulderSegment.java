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

public class ShoulderSegment extends BaseSegment {

    public static String [] strAttributes = {"RoadDir"};
    public static String [] numAttributes = {"LeftWidth", "LeftCenterWidth", "RightCenterWidth", "RightWidth"};
    public String roadwayDirection;
    public double leftWidth, leftCenterWidth, rightCenterWidth, rightWidth;
    public static HashMap<String, NumericAttributePackage> shoulderPackage = new HashMap<String, NumericAttributePackage>();
    public boolean normalized;

    static {
        for (int it = 0; it < numAttributes.length; ++it) {
            shoulderPackage.put(numAttributes[it], new NumericAttributePackage());
        }
    }

    ShoulderSegment() {
        assignBase();

        roadwayDirection = "_";
        leftWidth = leftCenterWidth = rightCenterWidth = rightWidth = 0;
    }

    ShoulderSegment(String entry) {
        String [] values = entry.split(",");

        assignBase(values);

        roadwayDirection = values[7];
        leftWidth = Double.parseDouble(values[8]);
        leftCenterWidth = Double.parseDouble(values[9]);
        rightCenterWidth = Double.parseDouble(values[10]);
        rightWidth = Double.parseDouble(values[11]);

        shoulderPackage.get("LeftWidth").adjustRange(leftWidth);
        shoulderPackage.get("LeftCenterWidth").adjustRange(leftCenterWidth);
        shoulderPackage.get("RightCenterWidth").adjustRange(rightCenterWidth);
        shoulderPackage.get("RightWidth").adjustRange(rightWidth);
    }

    @Override
    public boolean normalizeData() {

        if (!normalized) {
                NumericAttributePackage item = shoulderPackage.get("LeftWidth");
                leftWidth = item.normalize(leftWidth);
                item = shoulderPackage.get("LeftCenterWidth");
                leftCenterWidth = item.normalize(leftCenterWidth);
                item = shoulderPackage.get("RightCenterWidth");
                rightCenterWidth = item.normalize(rightCenterWidth);
                item = shoulderPackage.get("RightWidth");
                rightWidth = item.normalize(rightWidth);

                normalized = true;

                return true;
        }

        return false;		
    }

    @Override
    public boolean restoreData() {
        if (normalized) {
                NumericAttributePackage item = shoulderPackage.get("IncLanes");
                leftWidth = item.restore(leftWidth);
                item = shoulderPackage.get("LeftCenterWidth");
                leftCenterWidth = item.restore(leftCenterWidth);
                item = shoulderPackage.get("RightCenterWidth");
                rightCenterWidth = item.restore(rightCenterWidth);
                item = shoulderPackage.get("RightWidth");
                rightWidth = item.restore(rightWidth);

                normalized = false;

                return true;
        }

        return false;
    }

    // For console debugging, functionality checking
    public void outputShoulder(int line) {
        System.out.println(line + "\tDirection: " + roadwayDirection + "\tLeftWidth: " + leftWidth + "\tLeftCenterWidth: " + leftCenterWidth + 
            "\tRightCenterWidth: " + rightCenterWidth + "\tRightWidth: " + rightWidth);
    }	
}
