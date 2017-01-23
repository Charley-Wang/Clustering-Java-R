# GLM learning for road prediction
# by Charley (Xingsheng) Wang
# 5-fold cross validation
# on April 5, 2016

# nb <- glm.nb(ind ~ offset(log(len)) + offset(log(adt + 0.01)) + ., data = train)
# to optimize the learning rate

rm(list=ls())

# valid or training
valid_route_1 <- c(12)
valid_route_2 <- c(5, 3, 129, 548, 166)
valid_route_3 <- c(2, 525, 903, 100, 308, 128)
valid_route_4 <- c(155, 23, 504, 161, 821, 501, 411, 223, 528, 193, 204, 433)
valid_route_5 <- c(26, 9, 27, 174, 11, 270, 531, 538, 906)

para_type <- 1

test_num <- "5"

if (para_type == 0) {
  work_dir <- "C:/Users/li/Desktop/WXS/Chifan/05_MyWorkingProjects/01_Road/Predict30/data400"
  input_file <- "cluster.orig.csv"
  # input_file <- "cluster.csv"
  input_file_type <- "cluster"
  valid_num <- "4"
} else {
  args <- commandArgs(trailingOnly = TRUE)
  work_dir <- args[1]
  input_file <- args[2]
  input_file_type <- args[3]
  valid_num <- args[4]
}

print(work_dir)
print(input_file)
print(input_file_type)
print(valid_num)

input_file_full = paste(c(work_dir, "/", input_file), collapse = "")

# read input data and change # of incidents

data = read.csv(file = input_file_full, header = TRUE, sep = ",")

if (1==1) {
  data$ind <- data$ind * 2
}

if (1==2) {
  good_len <- data$len >= 0.1
  data <- data[good_len, ]
}

if (valid_num == "1") {
  valid_route <- valid_route_1
} else if (valid_num == "2") {
  valid_route <- valid_route_2
} else if (valid_num == "3") {
  valid_route <- valid_route_3
} else if (valid_num == "4") {
  valid_route <- valid_route_4
} else {
  valid_route <- valid_route_5
}

if (test_num == "1") {
  test_route <- valid_route_1
} else if (test_num == "2") {
  test_route <- valid_route_2
} else if (test_num == "3") {
  test_route <- valid_route_3
} else if (test_num == "4") {
  test_route <- valid_route_4
} else {
  test_route <- valid_route_5
}


# create valid data index according to route number from valid_route
valid_true <- data$route == valid_route[1]
if (length(valid_route) >= 2) {
  for(ii in 2:length(valid_route)) {
    valid_true <- (valid_true | (data$route == valid_route[ii]))
  }
}

test_true <- data$route == test_route[1]
if (length(test_route) >= 2) {
  for(ii in 2:length(test_route)) {
    test_true <- (test_true | (data$route == test_route[ii]))
  }
}

# create train data index
train_true <- (!(valid_true | test_true))

# remove unwanted columns
ncol <- dim(data)[2]
if (input_file_type == "uni") {
  data <- data[,4:ncol]
  ncol <- ncol - 3
} else {
  data <- data[,c(5, 13:ncol)]
  ncol <- ncol - 11
}

train <- data[train_true,]
valid <- data[valid_true,]
test <- data[test_true,]

print(head(train))
print(head(valid))
print(head(test))
print(dim(train))
print(dim(valid))
print(dim(test))

rm(list=c("train_true","valid_true","test_true","data"))

Train_ROE <- NULL
Run_Time <- NULL

library(MASS)

#================ train by GBM ====================

# nb <- glm.nb(ind ~ offset(log(len)) + offset(log(adt)) + ., data = train)
nb <- glm.nb(ind ~ offset(log(len)) + offset(log(adt)) + .-adt, data=train, control=glm.control(maxit=100))

print(summary(nb))

#=================== train ========================

preData <- train[,1:(ncol - 1)]
ind <- train$ind
typ <- "GLM Train"

predictGBM <- predict(nb, preData, type="response")

e <- abs(ind - predictGBM)
sum_e <- sum(e)
sum_i <- sum(abs(ind))
print(paste(paste(typ, "ROE: "), sum_e * 100 / sum_i))

#=================== valid ========================

preData <- valid[,1:(ncol - 1)]
ind <- valid$ind
typ <- "GLM Valid"

predictGBM <- predict(nb, valid, type="response")

e <- abs(ind - predictGBM)
sum_e <- sum(e)
sum_i <- sum(abs(ind))
print(paste(paste(typ, "ROE: "), sum_e * 100 / sum_i))


#=================== test ========================

preData <- test[,1:(ncol - 1)]
ind <- test$ind
typ <- "GLM Test"

predictGBM <- predict(nb, test, type="response")

e <- abs(ind - predictGBM)
sum_e <- sum(e)
sum_i <- sum(abs(ind))
print(paste(paste(typ, "ROE: "), sum_e * 100 / sum_i))

