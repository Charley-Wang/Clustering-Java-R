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
public class IncidentSegment extends BaseSegment {
		
    // public int route;
    public double incARM, incMP;

    public static double max = 0;

    IncidentSegment(String entry) {
        String [] values = entry.split(",");

        route = Integer.parseInt(values[0]);
        incARM = Double.parseDouble(values[1]);
        incMP = Double.parseDouble(values[2]);
        
        beginMP = incMP;
        endMP = incMP;
        
        if (incMP > max) {
            max = incMP;
        }
    }

    @Override
    public boolean normalizeData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public boolean restoreData() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}

