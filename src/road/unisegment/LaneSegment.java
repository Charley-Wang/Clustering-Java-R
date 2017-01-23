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

public class LaneSegment extends BaseSegment {
	
    public final static String [] strAttributes = {"RoadDir"};
    public final static String [] numAttributes = {"IncLanes", "DecLanes", "IncWidth", "DecWidth"};
    public String roadwayDirection;
    public double increasingLanes, decreasingLanes, increasingWidth, decreasingWidth;
    public static HashMap<String, NumericAttributePackage> lanePackage = new HashMap<String, NumericAttributePackage>();
    public boolean normalized = false;
	
    static {
        for (int it = 0; it < numAttributes.length; ++it) {
            lanePackage.put(numAttributes[it], new NumericAttributePackage());
        }
    }
    
    LaneSegment() {
        assignBase();

        roadwayDirection = "_";
        increasingLanes = decreasingLanes =	increasingWidth = decreasingWidth = 0;
    }

    LaneSegment(String entry) {
        String [] values = entry.split(",");

        assignBase(values);

        roadwayDirection = values[7];
        increasingLanes = Double.parseDouble(values[8]);
        decreasingLanes = Double.parseDouble(values[9]);
        increasingWidth = Double.parseDouble(values[10]);
        decreasingWidth = Double.parseDouble(values[11]);

        lanePackage.get("IncLanes").adjustRange(increasingLanes);
        lanePackage.get("DecLanes").adjustRange(decreasingLanes);
        lanePackage.get("IncWidth").adjustRange(increasingWidth);
        lanePackage.get("DecWidth").adjustRange(decreasingWidth);
    }

    @Override
    public boolean normalizeData() {				

        if (!normalized) {
                NumericAttributePackage item = lanePackage.get("IncLanes");
                increasingLanes = item.normalize(increasingLanes);
                item = lanePackage.get("DecLanes");
                decreasingLanes = item.normalize(decreasingLanes);
                item = lanePackage.get("IncWidth");
                increasingWidth = item.normalize(increasingWidth);
                item = lanePackage.get("IncWidth");
                decreasingWidth = item.normalize(decreasingWidth);

                normalized = true;

                return true;
        }

        return false;
    }

    @Override
    public boolean restoreData() {

        if (normalized) {
            NumericAttributePackage item = lanePackage.get("IncLanes");
            increasingLanes = item.restore(increasingLanes);
            item = lanePackage.get("DecLanes");
            decreasingLanes = item.restore(decreasingLanes);
            item = lanePackage.get("IncWidth");
            increasingWidth = item.restore(increasingWidth);
            item = lanePackage.get("IncWidth");
            decreasingWidth = item.restore(decreasingWidth);

            normalized = false;

            return true;
        }

        return false;
    }

    // For console debugging, functionality checking
    public void outputLane(int line) {
        System.out.println(line + "\tDirection: " + roadwayDirection + "\tIncLanes: " + increasingLanes + "\tDecLanes: " + decreasingLanes + "\tIncWidth: " + 
            increasingWidth + "\tDecWidth: " + decreasingWidth);
    }	

}

