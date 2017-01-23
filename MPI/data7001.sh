#!/bin/bash
#PBS -m abe
#PBS -l nodes=4:ppn=1
#PBS -l walltime=300:00:00
#PBS -l pmem=16gb
#PBS -j oe

module load R
module load mpich2

# mkdir classes
# javac ./src/road/clean/*.java ./src/road/unisegment/*.java ./src/road/cluster/*.java ./src/road/weight/*.java ./src/road/altersegment/*.java -d ./classes
# jar cvf road.jar -C classes/ .

# number of nodes
np=4

# max number of jobs for paralling computation
maxJobs=10
halfJobs=5

# for cross validation
cn=1
# for test
tt=2
scriptR=roadGLM200$tt.R

# set folders
suf=7001
fold=/gpfs/home/xxw15/group/predict/Predict66
dat=$fold/data$suf
log=$fold/data$suf/data$suf.log
javaEXE=/gpfs/home/xxw15/group/predict/jdk1.8.0_51/bin/java

# number of final clusters
numFinalCluster=500

# weights controlling parameters
delta=0.5
deltamin=0.001
lammda=0.0

# fixed parameters for the clustering
mileFixSeg=0.01
autoClusterCut=0.0000001
constantK=0.00001
defaltR=40000
#scriptR=roadGLM200$tt.R
#numCycles=1000

# route and # of cluster for each route
totalRoute=33
route=(0 12 5 3 129 548 166 2 525 903 100 308 128 155 23 504 161 821 501 411 223 528 193 204 433 26 9 27 174 11 270 531 538 906)

# for total number of clusters of 3000
# clusters=(0 494 699 137 55 1 19 455 112 22 5 10 4 94 55 132 71 29 6 21 5 8 6 11 4 153 153 123 51 34 19 3 2 7)
# for total number of clusters of 400
# clusters=(0 49 232 18 1 1 5 34 5 3 1 1 1 1 1 1 1 1 2 1 1 1 1 1 1 9 14 5 1 3 1 1 1 1)
# for total number of clusters of 500
clusters=(0 69 273 19 1 1 5 59 5 3 1 1 1 1 1 1 1 1 2 1 1 1 1 4 1 17 16 5 1 3 1 1 1 1)

# generate uniform segments for all
# $javaEXE -cp $fold/road.jar road.unisegment.UniSegmentation $dat hc.csv vc.csv sd.csv lane.csv ind.csv adt.csv $mileFixSeg uniseg.csv $defaltR

# split uniseq.csv route by route
R CMD BATCH --no-save --no-restore "--args $dat uniseg.csv" $fold/splitUnisegRouteByRoute2.R

# for the first calculation with the inital weights, weight0.csv
iii=0
$javaEXE -cp $fold/road.jar road.cluster.Cluster $dat uniseg.csv weight$iii.csv $autoClusterCut autoCluster$iii.csv $numFinalCluster cluster$iii.csv fieldCtrl.csv cluster 0
k=1
while [ $k -le $totalRoute ]
do
  echo "=========================== $k ======================"
  num1=${clusters[$k]}
  echo "$num1"
  $javaEXE -cp $fold/road.jar road.cluster.Cluster $dat uniseg.csv.$k.csv weight$iii.csv $autoClusterCut autoCluster$iii.$k.csv $num1 cluster$iii.$k.csv fieldCtrl.csv cluster 0
  echo "$dat uniseg.csv.$k.csv weight$iii.csv $autoClusterCut autoCluster$iii.$k.csv ${clusters[$k]} cluster$iii.$k.csv fieldCtrl.csv cluster 0"
  k=$(($k+1))
done

# combine splitted files
echo "$dat autoCluster$iii. .csv 1 $totalRoute autoCluster$iii.csv $fold/combineNCSVFiles.R"
R CMD BATCH --no-save --no-restore "--args $dat autoCluster$iii. .csv 1 $totalRoute autoCluster$iii.csv" $fold/combineNCSVFiles.R
R CMD BATCH --no-save --no-restore "--args $dat autoCluster$iii. .csv.orig 1 $totalRoute autoCluster$iii.csv.orig" $fold/combineNCSVFiles.R
R CMD BATCH --no-save --no-restore "--args $dat cluster$iii. .csv 1 $totalRoute cluster$iii.csv" $fold/combineNCSVFiles.R
R CMD BATCH --no-save --no-restore "--args $dat cluster$iii. .csv.orig 1 $totalRoute cluster$iii.csv.orig" $fold/combineNCSVFiles.R

R CMD BATCH --no-save --no-restore "--args $dat cluster$iii.csv.orig cluster $cn" $fold/$scriptR $dat/GBM$iii.$cn.txt
sleep 1m

# get error ROE of Valid
grep -H "GLM Train ROE:" $dat/GBM$iii.$cn.txt | cut -d' ' -f6 | cut -c-6 > $dat/roe$iii.$cn.txt
sleep 1s
read e1 < $dat/roe$iii.$cn.txt

prev_err=$e1
echo "iii=$iii, err=$prev_err" > $log

rm $dat/autoCluster*
rm $dat/roe*
rm $dat/cluster$iii.*.*.csv
rm $dat/cluster$iii.*.*.csv.orig
tar -cvzf $dat/_weight$iii.tar $dat/weight*.*.csv
tar -cvzf $dat/_GBM$iii.tar $dat/GBM$iii.*.*.txt
tar -cvzf $dat/_cluster$iii.tar $dat/cluster$iii.*.csv
tar -cvzf $dat/_cluster.orig.$iii.tar $dat/cluster$iii.*.csv.orig
rm $dat/weight*.*.csv
rm $dat/GBM$iii.*.*.txt
rm $dat/cluster$iii.*.csv
rm $dat/cluster$iii.*.csv.orig
sleep 1m

# =============== main loop for updating weights =========================

d1=$(echo "$deltamin < $delta" | bc)

while [ $d1 -gt 0 ]
do

  jjj=$iii
  iii=$(($iii+1))

  # weights w --> w + delta
  $javaEXE -cp $fold/road.jar road.weight.GenerateWeightsSelFeature $dat weight $iii $delta fieldCtrl.csv
  
  mpiexec -n $np $fold/mpiRoad$cn $iii $fold $dat $autoClusterCut $numFinalCluster $maxJobs $scriptR $javaEXE

  #     1 2 3 4 5
  grad=(0 0 0 0 0)
  dir=(0 0 0 0 0)
  gsum=0

  ppp=1
  while [ $ppp -le $halfJobs ]
  do
    grep -H "GLM Train ROE:" $dat/GBM$iii.$ppp.$cn.txt | cut -d' ' -f6 | cut -c-6 > $dat/roe.$ppp.$cn.txt
    sleep 1s
    read e1 < $dat/roe.$ppp.$cn.txt

    inc_err=$e1
    echo "iii=$iii, ppp=$ppp, incerr=$inc_err" >> $log

    qqq=$(($ppp+$halfJobs))

    grep -H "GLM Train ROE:" $dat/GBM$iii.$qqq.$cn.txt | cut -d' ' -f6 | cut -c-6 > $dat/roe.$qqq.$cn.txt
    sleep 1s
    read e1 < $dat/roe.$qqq.$cn.txt

    dec_err=$e1
    echo "iii=$iii, qqq=$qqq, decerr=$dec_err" >> $log

    # return d1 = 0 or 1
    d1=$(echo "$inc_err < $dec_err" | bc)
    if [ $d1 -gt 0 ]
    then
       d2=$(echo "$inc_err < $prev_err" | bc)
       if [ $d2 -gt 0 ]
       then
          d=$(echo "$prev_err - $inc_err" | bc -l)
          dir[$ppp-1]=1
          grad[$ppp-1]=$d
          gsum=$(echo "$gsum + $d" | bc -l)
       fi
    else
       d2=$(echo "$dec_err < $prev_err" | bc)
       if [ $d2 -gt 0 ]
       then
          d=$(echo "$prev_err - $dec_err" | bc -l)
          dir[$ppp-1]=-1
          grad[$ppp-1]=$(echo "-1.0 * $d" | bc -l)
          gsum=$(echo "$gsum + $d" | bc -l)
       fi
    fi

    ppp=$(($ppp+1))
  done

  echo "iii=$iii, gsum=$gsum" >> $log

  d3=$(echo "$gsum > 0" | bc)
  if [ $d3 -gt 0 ]
  then
     
     $javaEXE -cp $fold/road.jar road.weight.UpdateWithGrad $dat weight$jjj.csv weight$iii.csv fieldCtrl.csv $delta $lammda $gsum ${grad[0]} ${grad[1]} ${grad[2]} ${grad[3]} ${grad[4]}

     # ============================
     # new code on June 4, 2016
     k=1
     while [ $k -le $totalRoute ]
     do
       echo "=========================== $k ======================"
       num1=${clusters[$k]}
       echo "$num1"
       $javaEXE -cp $fold/road.jar road.cluster.Cluster $dat uniseg.csv.$k.csv weight$iii.csv $autoClusterCut autoCluster$iii.$k.csv $num1 cluster$iii.$k.csv fieldCtrl.csv cluster 0
       echo "$dat uniseg.csv.$k.csv weight$iii.csv $autoClusterCut autoCluster$iii.$k.csv ${clusters[$k]} cluster$iii.$k.csv fieldCtrl.csv cluster 0"
       k=$(($k+1))
     done

     # combine splitted files
     echo "$dat autoCluster$iii. .csv 1 $totalRoute autoCluster$iii.csv $fold/combineNCSVFiles.R"
     R CMD BATCH --no-save --no-restore "--args $dat autoCluster$iii. .csv 1 $totalRoute autoCluster$iii.csv" $fold/combineNCSVFiles.R
     R CMD BATCH --no-save --no-restore "--args $dat autoCluster$iii. .csv.orig 1 $totalRoute autoCluster$iii.csv.orig" $fold/combineNCSVFiles.R
     R CMD BATCH --no-save --no-restore "--args $dat cluster$iii. .csv 1 $totalRoute cluster$iii.csv" $fold/combineNCSVFiles.R
     R CMD BATCH --no-save --no-restore "--args $dat cluster$iii. .csv.orig 1 $totalRoute cluster$iii.csv.orig" $fold/combineNCSVFiles.R
     # ============================

     R CMD BATCH --no-save --no-restore "--args $dat cluster$iii.csv.orig cluster $cn" $fold/$scriptR $dat/GBM$iii.$cn.txt

     sleep 1m

     # get previous error ROE of Alter
     # this is different with the data81
     grep -H "GLM Train ROE:" $dat/GBM$iii.$cn.txt | cut -d' ' -f6 | cut -c-6 > $dat/roe$iii.$cn.txt
     sleep 1s
     read e1 < $dat/roe$iii.$cn.txt

     prev_err=$e1
     echo "iii=$iii, err=$prev_err" >> $log

  else
     delta=$(echo "$delta / 2.0" | bc -l)
     iii=$(($iii-1))
  fi

  d1=$(echo "$deltamin < $delta" | bc)
  
  rm $dat/autoCluster*
  rm $dat/roe*
  rm $dat/cluster$iii.*.*.csv
  rm $dat/cluster$iii.*.*.csv.orig
  tar -cvzf $dat/_weight$iii.tar $dat/weight*.*.csv
  tar -cvzf $dat/_GBM$iii.tar $dat/GBM$iii.*.*.txt
  tar -cvzf $dat/_cluster$iii.tar $dat/cluster$iii.*.csv
  tar -cvzf $dat/_cluster.orig.$iii.tar $dat/cluster$iii.*.csv.orig
  rm $dat/weight*.*.csv
  rm $dat/GBM$iii.*.*.txt
  rm $dat/cluster$iii.*.csv
  rm $dat/cluster$iii.*.csv.orig
  sleep 1m

done



