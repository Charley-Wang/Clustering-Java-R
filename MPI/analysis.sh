
n=$1
n2=$2
grep 'GLM Train ROE:' ../data$n/GBM?.$n2.txt > data$n.Train.ROE.txt
grep 'GLM Train ROE:' ../data$n/GBM??.$n2.txt >> data$n.Train.ROE.txt

grep 'GLM Valid ROE:' ../data$n/GBM?.$n2.txt > data$n.Valid.ROE.txt
grep 'GLM Valid ROE:' ../data$n/GBM??.$n2.txt >> data$n.Valid.ROE.txt

grep 'GLM Test ROE:' ../data$n/GBM?.$n2.txt > data$n.Test.ROE.txt
grep 'GLM Test ROE:' ../data$n/GBM??.$n2.txt >> data$n.Test.ROE.txt

paste data$n.* > _data$n.txt
sed -i -- 's/"//g' _data$n.txt

rm data*


