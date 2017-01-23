# Machine Learning Driven Clustering

## Introduction
<p></p>
Safety performance functions (SPFs) predict the likelihood of crashes on a roadway segment based as a function of the segment length, traffic counts, and roadway features.  These functions, derived using statistical and machine learning analyses, are an important tools to identify infrastructure improvements that can improve roadway safety. The first step in SPF modeling is roadway segmentation. Previous studies have found that segmentation approaches affect the models’ transferability, i.e. their predictive ability for future crashes or crashes on other roadways. 

<p></p>
&nbsp;&nbsp;&nbsp;&nbsp;In this thesis, we propose a novel segmentation methodology, which is driven by machine learning clustering approach. This approach was chosen both to characterize the extent to which segmentation approaches affect conclusions drawn from the models, and to determine if this approach could lead to models with better predictive capabilities. In the clustering approach, roadway segmentation is based on a weighted distance between adjacent segments. Segmented roadway data is used to build models that allow for the estimation of the gradient in error measure as a function of the segmentation weights. The weights are updated based on this gradient, and this process repeats with the performance of models guiding the weight updating and the segmentation.

<p></p>
&nbsp;&nbsp;&nbsp;&nbsp;This study presents a novel approach to investigate the effect of greatly on conclusions drawn from this modelling. Both modeling and segmentation parameters are extremely sensitive to initial conditions. It indicates that resulting models likely exhibit poor transferability, and biases that researchers bring to modelling can greatly affect the conclusions that they draw.

## Novel Contributions
<p></p>
&nbsp;&nbsp;&nbsp;&nbsp;•	It presents a novel gradient-descent approach for roadway segmentation, which allows for different initial weights that can reflect the biases that investigators bring to these studies with respect to the importance of different roadway features on safety.
<p></p>
&nbsp;&nbsp;&nbsp;&nbsp;•	It introduces a new error metric for segmentation studies, Weighted Absolute Percentage Error (WAPE). WAPE has used been used in business applications for demand forecasting.  It is particularly appropriate for segmentation studies, due to its independence of number of segments and its ability to deal with excessive zeroes present in the data.
<p></p>
&nbsp;&nbsp;&nbsp;&nbsp;•	It uses the spatial clustering algorithm to illustrate the extent to which emphasizing the importance of different features in the clustering can affect the resulting SPFs.  The significant effect suggests inherent limitations in the transferability of the resulting models.

## Table of Contents
Chapter 1. INTRODUCTION

Chapter 2. RELATED WORKS

&nbsp;&nbsp;&nbsp;&nbsp;2.1. Feature selection in roadway crash research

&nbsp;&nbsp;&nbsp;&nbsp;2.2. Challenges in the modeling of crash-frequency data

&nbsp;&nbsp;&nbsp;&nbsp;2.3. Segmentation methods

&nbsp;&nbsp;&nbsp;&nbsp;2.4. Evolution of the modeling methodologies

&nbsp;&nbsp;&nbsp;&nbsp;2.5. Influence of segmentation on resulting model

Chapter 3. METHODOLGY

&nbsp;&nbsp;&nbsp;&nbsp;3.1. Data for segmenting and modeling

&nbsp;&nbsp;&nbsp;&nbsp;3.2. Algorithm for segmenting and modeling

&nbsp;&nbsp;&nbsp;&nbsp;3.3. Parameters for segmentation and modeling

Chapter 4. RESULTS

&nbsp;&nbsp;&nbsp;&nbsp;4.1. The performance of the system on the training dataset

&nbsp;&nbsp;&nbsp;&nbsp;4.2. Validity in the models based on the initial parameters

&nbsp;&nbsp;&nbsp;&nbsp;4.3. The limit of the generalizability of models

Chapter 5. CONCLUSIONS AND FUTURE WORK

&nbsp;&nbsp;&nbsp;&nbsp;5.1. Contributions of this thesis

&nbsp;&nbsp;&nbsp;&nbsp;5.2. Future work

REFERENCES

