#include <mpi.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>

//#define MAX_NP = 4

int DEBUG = 1;

int runOneJob(int jobID, char ** argv)
{
  // parameters for the modeling and prediction
  // the step number for the modeling, argv[1]
  int stepNum;
  // folder name for java and R scripts, argv[2]
  char fold[200];
  // folder name for datasets, argv[3]
  char dat[200];
  // cutoff for auto clustering, argv[4]
  double autoClusterCut;
  // number of final clusters, argv[5]
  int numFinalCluster;
  // R script name
  char scriptR[200];
  // java exe file path and name
  char java[200];

  char cmm1[1000];
  char cmm2[1000];
  char cmm3[1000];
  char cmm4[1000];
  char cmm[1000];

  int buff2, num1, k;

  // for total number of clusters of 3000
  // int clusters[]={494,699,137,55,1,19,455,112,22,5,10,4,94,55,132,71,29,6,21,5,8,6,11,4,153,153,123,51,34,19,3,2,7};
  // for total number of clusters of 400
  // int clusters[]={49,232,18,1,1,5,34,5,3,1,1,1,1,1,1,1,1,2,1,1,1,1,1,1,9,14,5,1,3,1,1,1,1};
  // for total number of clusters of 500
  int clusters[]={69,273,19,1,1,5,59,5,3,1,1,1,1,1,1,1,1,2,1,1,1,1,4,1,17,16,5,1,3,1,1,1,1};

  // the step number for the modeling, argv[1]
  stepNum = atoi(argv[1]);
  //folder name for java and R scripts, argv[2]
  strcpy(fold, argv[2]);
  // folder name for datasets, argv[3]
  strcpy(dat, argv[3]);
  // cutoff for auto clustering, argv[4]
  autoClusterCut = atof(argv[4]);
  // number of final clusters, argv [5]
  numFinalCluster = atoi(argv[5]);
  strcpy(scriptR, argv[7]);
  strcpy(java, argv[8]);
  
  for (k = 1; k <= 33; k++) {
    num1 = clusters[k - 1];
    sprintf(cmm1, "%s -cp %s/road.jar road.cluster.Cluster %s uniseg.csv.%d.csv", java, fold, dat, k);
    sprintf(cmm2, " %s weight%d.%d.csv %f",               cmm1, stepNum, jobID, autoClusterCut);
    sprintf(cmm3, " %s autoCluster%d.%d.%d.csv %d",       cmm2, stepNum, jobID, k, num1);
    sprintf(cmm4, " %s cluster%d.%d.%d.csv",              cmm3, stepNum, jobID, k); 
    sprintf(cmm,  " %s fieldCtrl.csv cluster 0",          cmm4);
    printf("\n%s",cmm);
    system(cmm);
  }

  // combine splitted files
  sprintf(cmm1, "R CMD BATCH --no-save --no-restore \"--args %s", dat);
  sprintf(cmm,  "%s autoCluster%d.%d. .csv 1 33", cmm1, stepNum, jobID);
  sprintf(cmm,  "%s autoCluster%d.%d.csv\"",      cmm, stepNum, jobID);
  sprintf(cmm,  "%s %s/combineNCSVFiles.R",       cmm, fold);
  printf("\n%s", cmm);
  system(cmm);

  // combine splitted files
  sprintf(cmm1, "R CMD BATCH --no-save --no-restore \"--args %s", dat);
  sprintf(cmm,  "%s autoCluster%d.%d. .csv.orig 1 33", cmm1, stepNum, jobID);
  sprintf(cmm,  "%s autoCluster%d.%d.csv.orig\"",      cmm, stepNum, jobID);
  sprintf(cmm,  "%s %s/combineNCSVFiles.R",            cmm, fold);
  printf("\n%s", cmm);
  system(cmm);

  sprintf(cmm1, "R CMD BATCH --no-save --no-restore \"--args %s", dat);
  sprintf(cmm,  "%s cluster%d.%d. .csv 1 33", cmm1, stepNum, jobID);
  sprintf(cmm,  "%s cluster%d.%d.csv\"",      cmm, stepNum, jobID);
  sprintf(cmm,  "%s %s/combineNCSVFiles.R",   cmm, fold);
  printf("\n%s", cmm);
  system(cmm);

  sprintf(cmm1, "R CMD BATCH --no-save --no-restore \"--args %s", dat);
  sprintf(cmm,  "%s cluster%d.%d. .csv.orig 1 33", cmm1, stepNum, jobID);
  sprintf(cmm,  "%s cluster%d.%d.csv.orig\"",      cmm, stepNum, jobID);
  sprintf(cmm,  "%s %s/combineNCSVFiles.R",        cmm, fold);
  printf("\n%s", cmm);
  system(cmm);

  sprintf(cmm1, "R CMD BATCH --no-save --no-restore \"--args %s", dat);
  sprintf(cmm2, "%s cluster%d.%d.csv.orig",  cmm1, stepNum, jobID);
  
  sprintf(cmm3, "%s cluster 1\" %s/%s", cmm2, fold, scriptR); 
  sprintf(cmm,  "%s %s/GBM%d.%d.1.txt", cmm3, dat, stepNum, jobID);
  printf("\n%s",cmm);
  //system(cmm);

  sprintf(cmm3, "%s cluster 2\" %s/%s", cmm2, fold, scriptR);  
  sprintf(cmm,  "%s %s/GBM%d.%d.2.txt", cmm3, dat, stepNum, jobID);
  system(cmm);

  sprintf(cmm3, "%s cluster 3\" %s/%s", cmm2, fold, scriptR);  
  sprintf(cmm,  "%s %s/GBM%d.%d.3.txt", cmm3, dat, stepNum, jobID);
  //system(cmm);

  sprintf(cmm3, "%s cluster 4\" %s/%s", cmm2, fold, scriptR);  
  sprintf(cmm,  "%s %s/GBM%d.%d.4.txt", cmm3, dat, stepNum, jobID);
  //system(cmm);

  sprintf(cmm3, "%s cluster 5\" %s/%s", cmm2, fold, scriptR);
  sprintf(cmm,  "%s %s/GBM%d.%d.5.txt", cmm3, dat, stepNum, jobID);
  //system(cmm);

  system("sleep 1m"); 

  if (DEBUG > 0) {
    srand(jobID);
    buff2 = rand() % 20;
    printf("\nJob ID = %d, sleep %d s.", jobID, buff2);
    sleep(buff2);
  }

  return 0;
}

int main(int argc, char ** argv)
{
  // number of jobs per run, argv[6]
  int numJobs;

  // parameters for mpi
  int rank, size, ii;
  int MASTER = 0;
  int buff;
  int master[2];
  int var;
  int dest;
  time_t t;
  int jobID;
  MPI_Status status;

  if (argc != 9) {
    printf("\nThe number of input parameters should be 8");
    return 0;
  }   

  MPI_Init(&argc, &argv);

  MPI_Comm_rank(MPI_COMM_WORLD, &rank);
  MPI_Comm_size(MPI_COMM_WORLD, &size);

  if (DEBUG > 1) {
    printf("\n%d", argc);
    printf("\n%s", argv[0]);
    printf("\n%s", argv[1]);
    //printf("\n%s", fold);
  }

  numJobs = atoi(argv[6]);

  if (rank == MASTER) {
    for (ii = 1; ii < size; ii++) {
      master[0] = 1;   // label for run (1) or stop (0)
      master[1] = ii;  // the number of dispatched jobs so far
      printf("\n Master b message %d", ii);
      // MPI_Bcast(&master, 2, MPI_INT, 0, MPI_COMM_WORLD);
      MPI_Send(&master, 2, MPI_INT, ii, 1, MPI_COMM_WORLD);
    }
    for (ii = size; ii <= numJobs; ii++) {
      /*
      if (ii >= size) {
        master[0] = 1;
        master[1] = ii;
        printf("\n Master b message %d", ii);
        // MPI_Bcast(&master, 2, MPI_INT, 0, MPI_COMM_WORLD);
        MPI_Send(&master, 2, MPI_INT, MPI_ANY_SOURCE, 1, MPI_COMM_WORLD);
      }
      */
      MPI_Recv(&buff, 1, MPI_INT, MPI_ANY_SOURCE, 2, MPI_COMM_WORLD, &status);
      printf("\nMaster received the message from buff = %d", buff);

      master[0] = 1;     // label for run (1) or stop (0)
      master[1] = ii;    // the number of dispatched jobs so far
      printf("\n Master b message %d", ii);
      dest = buff;
      MPI_Send(&master, 2, MPI_INT, dest, 1, MPI_COMM_WORLD);
    }
    for (ii = 1; ii < size; ii++) {
      master[0] = -1;
      master[1] = ii;
      // MPI_Bcast(&master, 2, MPI_INT, 0, MPI_COMM_WORLD);
      MPI_Send(&master, 2, MPI_INT, ii, 1, MPI_COMM_WORLD);
    }
  }
  else {
    while (1) {
      // MPI_Bcast(&master, 2, MPI_INT, 0, MPI_COMM_WORLD);
      MPI_Recv(&master, 2, MPI_INT, 0, MPI_ANY_TAG, MPI_COMM_WORLD, &status);
      if (master[0] == -1) break;

      // run one job here
      jobID = master[1];
      printf("\nIn node = %d, receive b message jobID = %d", rank, jobID);
      runOneJob(jobID, argv);      

      buff = rank;
      MPI_Send(&buff, 1, MPI_INT, 0, 2, MPI_COMM_WORLD);
    }
  }

  MPI_Finalize();
  return 0;
} 


