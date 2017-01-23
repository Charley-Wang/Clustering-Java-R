/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package road.old.cluster;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import road.old.weight.Segments;
import static road.old.weight.Weight.MAX_NUM_FIELDS;
import static road.old.weight.Weight.segments;


/**
 *
 * @author li
 */

/*
parameter: C://Users//li//Desktop//WXS//Chifan//17_Road//FilteredRawData comb_index.csv cluster_index.csv
*/

public class Cluster {
    public static int numSegment;
    public static IndexSegment indexSegment;
    
    public static Distance[] distance;
    public static Distance linkDist;
    public static int numDist;
    public static SortElementDouble[] sortElements;
    
    public static int sizeBin;
    public static double[] binData;
    public static Distance[] binPt; 
    
    public static void main(String [] args) throws IOException {
        String dir = args[0];
        String idxFile = args[1];
        String outFile = args[2];
        
        readIndex(dir, idxFile);
        calDist();
        orderDist();
        createDistLink();
        
        sizeBin = 1024;
        createBinarySearchArrays();
        cluster(10000);
        
        saveIndex(dir + "//" + outFile);
    }
    
    public static void saveIndex(String fileName) throws IOException {
        IndexSegment seg = indexSegment;
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, false)));
        
        out.print("id,route,begMP,endMP,index,incFreq,preMerg,nxtMerg");
        out.println();

        while(seg != null) {
            out.print(seg.id);
            out.print("," + seg.route);
            out.print("," + seg.begMP);
            out.print("," + seg.endMP);
            out.print("," + seg.index);
            out.print("," + seg.incFreq);
            out.print("," + seg.preMerg);
            out.print("," + seg.nxtMerg);
            out.println();
            seg = seg.nxt;
        }

        out.close();
    }
    
    public static void cluster(int finalClusterNum) {
        IndexSegment preSeg, currSeg;
        double index1, index2;
        double len1, len2;
        
        Distance head = linkDist, pt, pre, nxt;
        while (numSegment > finalClusterNum && head != null) {
            // System.out.println("The number of clusters: " + numSegment);
            // System.out.println("              dists:    " + numDist);
            
            // merge the currSeg to the preSeg
            preSeg = head.preSeg;
            currSeg = head.nxtSeg;
            
            index1 = preSeg.index;
            index2 = currSeg.index;
            len1 = preSeg.endMP - preSeg.begMP;
            len2 = currSeg.endMP - currSeg.begMP;
            preSeg.endMP = currSeg.endMP;
            preSeg.incFreq += currSeg.incFreq;
            preSeg.index = (index1 * len1 + index2 * len2) / (len1 + len2);
            preSeg.nxt = currSeg.nxt;
            // System.out.println(numSegment + "    " + currSeg.id);
            if (currSeg.nxt != null) currSeg.nxt.pre = preSeg;
            numSegment--;

            // delete head from linkDist
            linkDist = head.nxt;
            linkDist.pre = null;
            numDist--;
            adjustBinarySearchArrays(head);
                        
            // adjust previous distance
            if (preSeg.pre != null) {
                if (preSeg.pre.nxtMerg == true && preSeg.preMerg == true) {
                    Distance d = preSeg.preDist;

                    // delete d from linkDist
                    pre = d.pre;
                    nxt = d.nxt;
                    
                    if (pre == null) {
                        // if pre == null, it means that d is the head of linkDist
                        linkDist = linkDist.nxt;
                        linkDist.pre = null;
                    }
                    else {
                        pre.nxt = nxt;
                        if (nxt != null) nxt.pre = pre; 
                    }
                    numDist--;
                    adjustBinarySearchArrays(d);

                    // adjust d for the new distance
                    d.dist = Math.abs(preSeg.pre.index - preSeg.index);
                    if (d.dist < linkDist.dist) {
                        d.pre = null;
                        d.nxt = linkDist;
                        linkDist.pre = d;
                        linkDist = d;
                    }
                    else {
                        pt = binarySearch(d.dist, 0, sizeBin - 1);
                        insertLinkDist(d, pt);
                    }
                    preSeg.pre.nxtDist = d;
                    preSeg.preDist = d;
                    numDist++;
                }
            }
            
            // adjust next distance
            if (currSeg.nxt != null) {
                if (currSeg.nxtMerg == true && currSeg.nxt.preMerg == true) {
                    Distance d = currSeg.nxtDist;

                    // delete d from linkDist
                    pre = d.pre;
                    nxt = d.nxt;
                    
                    // this is important
                    d.preSeg = preSeg;
                    
                    if (pre == null) {
                        // if pre == null, it means that d is the head of linkDist
                        linkDist = linkDist.nxt;
                        linkDist.pre = null;
                    }
                    else {
                        pre.nxt = nxt;
                        if (nxt != null) nxt.pre = pre; 
                    }

                    numDist--;
                    adjustBinarySearchArrays(d);

                    // adjust d for the new distance
                    d.dist = Math.abs(currSeg.nxt.index - preSeg.index);
                    if (d.dist < linkDist.dist) {
                        d.pre = null;
                        d.nxt = linkDist;
                        linkDist.pre = d;
                        linkDist = d;
                    }
                    else {
                        pt = binarySearch(d.dist, 0, sizeBin - 1);
                        insertLinkDist(d, pt);
                    }
                    currSeg.nxt.preDist = d;
                    preSeg.nxtDist = d;
                    numDist++;
                }
            }
            
            // change the head
            head = linkDist;
        }
    }
    
    private static void insertLinkDist(Distance d, Distance pt) {
        Distance nxt;
        while(true) {
            if (pt.nxt == null) {
                pt.nxt = d;
                d.pre = pt;
                d.nxt = null;
                break;
            }
            else {
                if (d.dist >= pt.dist && d.dist <= pt.nxt.dist) {
                    nxt = pt.nxt;
                    pt.nxt = d;
                    d.pre = pt;
                    d.nxt = nxt;
                    nxt.pre = d;
                    break;
                }
                else pt = pt.nxt;
            }
        }
    }
    
    private static Distance binarySearch(double d, int beg, int end) {
        Distance ret = null;
        double min, max, mid;
        int avg, middle;
        
        if (end - beg <= 1) {
            ret = binPt[beg];
        }
        else {
            min = binData[beg];
            max = binData[end];
            avg = (int)((end - beg)/2);
            middle = avg + beg;
            mid = binData[middle];
            
            if (min == mid && mid == max) {
                ret = binPt[beg];
            }
            else if(min == mid && mid < max) {
                if (d == min) ret = binPt[beg];
                else ret = binarySearch(d, middle, end);
            }
            else if(min < mid && mid == max) {
                if (d == max) ret = binPt[middle];
                else ret = binarySearch(d, beg, middle);
            }
            else {
                if (min <= d && d <= mid) ret = binarySearch(d, beg, middle);
                else ret = binarySearch(d, middle, end);
            }
        }     
        
        return ret;
    }
    
    private static void adjustBinarySearchArrays(Distance d) {
        int idx;
        double nxtData;
        Distance currPt, nxtPt;
        if (d.idxBinData != -1) {
            idx = d.idxBinData;
            binPt[idx].idxBinData = -1;
            if (idx == binData.length - 1) {
                createBinarySearchArrays();
            }
            else {
                nxtData = binData[idx + 1];
                currPt = binPt[idx];
                nxtPt = currPt.nxt;
                if (nxtPt != null) {
                    if (nxtData > nxtPt.dist) {
                        binPt[idx] = nxtPt;
                        binData[idx] = nxtPt.dist;
                        nxtPt.idxBinData = idx;
                    }
                    else {
                        createBinarySearchArrays();
                    }
                }
                else {
                    createBinarySearchArrays();
                }
            }
        }
    }
    
    public static void createBinarySearchArrays() {
        int num = sizeBin;
        Distance curr, pre;
        int det = (int)(numDist / (num - 1)) - 1;
        
        binData = new double[num];
        binPt = new Distance[num];
        
        pre = null;
        curr = linkDist;
        
        binData[0] = curr.dist;
        binPt[0] = curr;
        curr.idxBinData = 0;
        
        int ii = -1;
        int jj = 0;
        while (curr != null) {
            ii++;
            if (ii == det) {
                jj++;
                if (jj < num - 1) { 
                    binData[jj] = curr.dist;
                    binPt[jj] = curr;
                    curr.idxBinData = jj;
                }
                ii = 0;
            }
            pre = curr;
            curr = curr.nxt;
            if (curr != null) curr.idxBinData = -1;
        }
        
        binData[num - 1] = pre.dist;
        binPt[num - 1] = pre;
        pre.idxBinData = num - 1;
    }
    
    public static void createDistLink() {
        Distance head = null, pre = null, curr = null;
        
        for (int ii = 0; ii < sortElements.length; ii++) {
            curr = (Distance)sortElements[ii].obj;
            if (head == null) {
                head = curr;
                linkDist = head;
                curr.pre = null;
                curr.nxt = null;
                pre = curr;
            }
            else {
                pre.nxt = curr;
                curr.pre = pre;
                curr.nxt = null;
                pre = curr;
            }
        }
    }
    
    public static void orderDist() {
        SortDouble sd = new SortDouble(numDist);
        
        for (int ii = 0; ii < numDist; ii++) {
            sd.appendElement(distance[ii].dist, distance[ii]);
        }
        
        sd.mergeSort(0, numDist - 1);
        sortElements = sd.getSortedElement();
    }
    
    public static void calDist() {
        //Distance head = null, pre = null, curr = null;
        IndexSegment currSeg, preSeg;
        
        numDist = 0;
        distance = new Distance[numSegment - 1];
        
        preSeg = indexSegment;
        currSeg = preSeg.nxt;
        while (currSeg != null) {
            if (preSeg.nxtMerg = true && currSeg.preMerg == true) {
                Distance curr = new Distance();
                curr.dist = Math.abs(preSeg.index - currSeg.index);
                curr.preSeg = preSeg;
                curr.nxtSeg = currSeg;
                numDist++;
                // only used to debug
                curr.id = numDist - 1;
                distance[numDist - 1] = curr;
                preSeg.nxtDist = curr;
                currSeg.preDist = curr;
            }
            preSeg = currSeg;
            currSeg = currSeg.nxt;
        }
    }
    
    public static void readIndex(String dir, String idxFile) {
        Path vcPath = Paths.get(dir + "//" + idxFile);
        Charset charset = Charset.forName("US-ASCII");

        numSegment = 0;
        IndexSegment head = null, pre = null, curr = null;
        
        try {
            BufferedReader vcIn = Files.newBufferedReader(vcPath, charset);
            String entry = vcIn.readLine();
            while((entry = vcIn.readLine()) != null) {
                numSegment++;
                curr = new IndexSegment();
                String[] txts = entry.split(",");
                curr.route = Integer.valueOf(txts[0]);
                curr.begMP = Double.valueOf(txts[1]);
                curr.endMP = Double.valueOf(txts[2]);
                curr.index = Double.valueOf(txts[3]);
                curr.incFreq = Double.valueOf(txts[4]);
                curr.preMerg = Boolean.valueOf(txts[5]);
                curr.nxtMerg = Boolean.valueOf(txts[6]);
                // only used to debug
                curr.id = numSegment;
                if (head == null) {
                    head = curr;
                    curr.pre = null;
                    curr.nxt = null;
                    pre = curr;
                }
                else {
                    pre.nxt = curr;
                    curr.pre = pre;
                    curr.nxt = null;
                    pre = curr;
                }
            }
            indexSegment = head;
            vcIn.close();
        }
        catch (IOException IOE) {
            System.out.println("failure on reading " + dir + "//" + idxFile);
        }
    }
}
