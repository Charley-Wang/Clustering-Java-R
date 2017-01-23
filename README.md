# Machine Learning Driven Clustering

## Introduction
<p></p>
Safety performance functions (SPFs) predict the likelihood of crashes on a roadway segment based as a function of the segment length, traffic counts, and roadway features.  These functions, derived using statistical and machine learning analyses, are an important tools to identify infrastructure improvements that can improve roadway safety. The first step in SPF modeling is roadway segmentation. Previous studies have found that segmentation approaches affect the models’ transferability, i.e. their predictive ability for future crashes or crashes on other roadways. 

<p></p>
  In this thesis, we propose a novel segmentation methodology, which is driven by machine learning clustering approach. This approach was chosen both to characterize the extent to which segmentation approaches affect conclusions drawn from the models, and to determine if this approach could lead to models with better predictive capabilities. In the clustering approach, roadway segmentation is based on a weighted distance between adjacent segments. Segmented roadway data is used to build models that allow for the estimation of the gradient in error measure as a function of the segmentation weights. The weights are updated based on this gradient, and this process repeats with the performance of models guiding the weight updating and the segmentation.

<p></p>
  This study presents a novel approach to investigate the effect of greatly on conclusions drawn from this modelling. Both modeling and segmentation parameters are extremely sensitive to initial conditions. It indicates that resulting models likely exhibit poor transferability, and biases that researchers bring to modelling can greatly affect the conclusions that they draw.
