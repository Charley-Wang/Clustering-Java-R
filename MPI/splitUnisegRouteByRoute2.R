# Split Uniseg.csv Route by Route

rm(list=ls())

# 0 for my PC version and 1 for MPI version
para_type <- 1

if (para_type == 0) {
  work_dir <- "C:/Users/li/Desktop/WXS/Chifan/05_MyWorkingProjects/01_Road/Predict34/data"
  input_file <- "uniseg.csv"
} else {
  args <- commandArgs(trailingOnly = TRUE)
  work_dir <- args[1]
  input_file <- args[2]
}

# full path of the input file
input_file_full = paste(c(work_dir, "/", input_file), collapse = "")
# read input data and change # of incidents
data = read.csv(file = input_file_full, header = TRUE, sep = ",")

# route number
route <- c(12, 5, 3, 129, 548, 166, 2, 525, 903, 100, 308, 128, 155, 23, 504, 
           161, 821, 501, 411, 223, 528, 193, 204, 433, 26, 9, 27, 174, 11,
           270, 531, 538, 906)

# numbers of cluster for route when the total final number of cluster of 3000
clusters <- c(494, 699, 137, 55, 1, 19, 455, 112, 22, 5, 10, 4, 94, 55, 132,
              71, 29, 6, 21, 5, 8, 6, 11, 4, 153, 153, 123, 51, 34,
              19, 3, 2, 7)

# numbers of cluster for route when the total final number of cluster of 500 clustering without adt
clusters <- c(69, 273, 19, 1, 1,  5,   59, 5,  3,   1,   1,   1,   1,   1,  1,
              1,   1,   2,   1,   1,   1,   1,   4,   1,   17,  16, 5, 1,  3,
              1, 1, 1, 1)

# split data route by route with titles
for(ii in seq(1, length(clusters), 1)) {
  output_file_full = paste(c(input_file_full, ".", ii, ".csv"), collapse = "")
  data1_true <- (data["route"] == route[ii])
  data1 <- data[data1_true,]
  write.csv(data1, file = output_file_full, row.names = FALSE)
}
