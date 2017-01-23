/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.old.cluster;

/**
 *
 * @author Charley (Xingsheng) Wang
 */
public class SortDouble {
    private SortElementDouble elements[], tempElement[];
    private int numElements;
    private int appendNum;
    
    public SortDouble(int numElements) {
        this.numElements = numElements;
        this.elements = new SortElementDouble[numElements];
        this.tempElement = new SortElementDouble[numElements];
        this.appendNum = -1;
    }
    
    public void appendElement(double num, Object obj) {
        SortElementDouble e = new SortElementDouble();
        this.appendNum++;
        elements[this.appendNum] = e;
        e.num = num;
        e.obj = obj;
    }
    
    public SortElementDouble[] getSortedElement() {
        return elements;
    }
    
    public void mergeSort(int begin, int end) {
        if(begin == end) return;
        
        int middle = (end - begin + 1)/2 + begin - 1;
        int loc1, loc2, loc3, loc4, ii, jj, kk;
        loc1 = begin;
        loc2 = middle;
        loc3 = middle + 1;
        loc4 = end;
        
        mergeSort(loc1, loc2);
        mergeSort(loc3, loc4);
        
        ii = loc1;
        jj = loc3;
        kk = loc1;
        
        while(ii <= loc2 && jj <= loc4) {
            if(elements[ii].num < elements[jj].num) {
                tempElement[kk] = elements[ii];
                ii++;
            }
            else {
                tempElement[kk] = elements[jj];
                jj++;
            }
            kk++;
        }
        
        while(ii <= loc2) {
            tempElement[kk] = elements[ii];
            ii++;
            kk++;
        }
        
        while(jj <= loc4) {
            tempElement[kk] = elements[jj];
            jj++;
            kk++;
        }
        
        for(int ll = loc1; ll <= loc4; ll++) elements[ll] = tempElement[ll];
    }        
}
