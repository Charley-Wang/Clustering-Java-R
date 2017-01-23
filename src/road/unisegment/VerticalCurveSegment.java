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

public class VerticalCurveSegment extends BaseSegment {

    public static String [] strAttributes = {"VCType"};
    public static String [] numAttributes = {"VCBVCARM", "VCVPIARM", "VCEVCARM",
            "VCLength", "VCPGA", "VCPGB"};
    public String vcType;
    public double bvcARM, vpiARM, evcARM, vcLength, pga, pgb;
    public static HashMap<String, NumericAttributePackage> verticalPackage = new HashMap<String, NumericAttributePackage>();
    public boolean normalized = false;

    static {
        for (int it = 0; it < numAttributes.length; ++it) {
            verticalPackage.put(numAttributes[it], new NumericAttributePackage());
        }
    }

    VerticalCurveSegment(){
        assignBase();

        vcType = "_";
        bvcARM = vpiARM = evcARM = vcLength = pga = pgb = 0;
    }

    VerticalCurveSegment(String entry) {
        String [] values = entry.split(",");

        assignBase(values);

        if (values[7].trim().length() > 0) bvcARM = Double.parseDouble(values[7]);
        if (values[8].trim().length() > 0) vpiARM = Double.parseDouble(values[8]);
        if (values[9].trim().length() > 0) evcARM = Double.parseDouble(values[9]);
        vcType = values[10];
        vcLength = Double.parseDouble(values[11]);
        pga = Double.parseDouble(values[12]);
        pgb = Double.parseDouble(values[13]);

        verticalPackage.get("VCBVCARM").adjustRange(bvcARM);
        verticalPackage.get("VCVPIARM").adjustRange(vpiARM);
        verticalPackage.get("VCEVCARM").adjustRange(evcARM);
        verticalPackage.get("VCLength").adjustRange(vcLength);
        verticalPackage.get("VCPGA").adjustRange(pga);
        verticalPackage.get("VCPGB").adjustRange(pgb);
    }

    @Override
    public boolean normalizeData() {

        if (!normalized) {
                NumericAttributePackage item = verticalPackage.get("VCBVCARM");
                bvcARM = item.normalize(bvcARM);
                item = verticalPackage.get("VCVPIARM");
                vpiARM = item.normalize(vpiARM);
                item = verticalPackage.get("VCEVCARM");
                evcARM = item.normalize(evcARM);
                item = verticalPackage.get("VCLength");
                vcLength = item.normalize(vcLength);
                item = verticalPackage.get("VCPGA");
                pga = item.normalize(pga);
                item = verticalPackage.get("VCPGB");
                pgb = item.normalize(pgb);

                normalized = true;

                return true;
        }

        return false;
    }

    @Override
    public boolean restoreData() {

        if (normalized) {
                NumericAttributePackage item = verticalPackage.get("VCBVCARM");
                bvcARM = item.restore(bvcARM);
                item = verticalPackage.get("VCVPIARM");
                vpiARM = item.restore(vpiARM);
                item = verticalPackage.get("VCEVCARM");
                evcARM = item.restore(evcARM);
                item = verticalPackage.get("VCLength");
                vcLength = item.restore(vcLength);
                item = verticalPackage.get("VCPGA");
                pga = item.restore(pga);
                item = verticalPackage.get("VCPGB");
                pgb = item.restore(pgb);

                normalized = false;

                return true;
        }

        return false;
    }

    public void outputVertical(int line) {
        System.out.println(line + "\tVCBVCARM: " + bvcARM + "\tVCVPIARM: " + vpiARM + "\tVCEVCARM: " + evcARM +
                "\tVCType: " + vcType + "\tVCLength: " + vcLength + "\tVCPGA: " + pga + "\tVCPGB: " + pgb);
    }	
	
}

