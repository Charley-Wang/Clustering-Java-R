# Combine several csv files into a file

rm(list=ls())

# 0 for my PC version and 1 for MPI version
para_type <- 1

if (para_type == 0) {
  work_dir <- "C:/Users/li/Desktop/WXS/Chifan/05_MyWorkingProjects/01_Road/Predict34/data"
  prefix <- "uniseg.csv."
  suffix <- ".csv"
  beg_num <- 1
  end_num <- 33
  output <- "uniseq_all.csv"
} else {
  args <- commandArgs(trailingOnly = TRUE)
  work_dir <- args[1]
  print(work_dir)
  prefix <- args[2]
  print(prefix)
  suffix <- args[3]
  print(suffix)
  beg_num <- args[4]
  print(beg_num)
  end_num <- args[5]
  print(end_num)
  output <- args[6]
  print(output)
}

ii = beg_num
# full path of the input file
input_file_full = paste(c(work_dir, "/", prefix, ii, suffix), collapse = "")
# read input data and change # of incidents
data = read.csv(file = input_file_full, header = TRUE, sep = ",")

for (ii in seq(2, as.numeric(end_num), 1)) {
  print(ii)
  # full path of the input file
  input_file_full = paste(c(work_dir, "/", prefix, ii, suffix), collapse = "")
  # read input data and change # of incidents
  data1 = read.csv(file = input_file_full, header = TRUE, sep = ",")
  data <- rbind(data, data1)
}

output_file_full = paste(c(work_dir, "/", output), collapse = "")
write.csv(data, file = output_file_full, row.names = FALSE)

