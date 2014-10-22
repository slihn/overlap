Python Implementation
=======

In this implementation, dok_matrix in scipy.sparse is used to achieve constant time matrix lookup.
Alternatively, you can change use_dense_matrix to True to compare performance and memory consumption of dense matrix.

# Performance

* PC Spec: Dell, Ubuntu 64-bit, Intel Xeon W3550 @3.07GHz

## Small sample

It takes about 52 seconds to generate the overlap matrix `OV`.
There are 837 funds. The overlap matrix has 837*836/2 ~ 350,000 elements.
Each overlap element (between two funds) takes about 1.5 milliseconds. This is reasonably fast.

* Memory: The program consumes about 50 MB for storing data in the sparse matrix
* No matrix lookup: The elapsed time is 9 seconds.
* Use dense matrix: The elapsed time is 36 seconds.
* Use sparse matrix: The elapsed time is 52 seconds.

This proves that the major burden of this calculation is on the matrix lookup.

## Large sample

* Memory: 500 MB for storing data in the sparse matrix; 1 GB for dense matrix (This needs to be measured more precisely.)
* No matrix lookup: The elapsed time is 250 seconds.
* Use dense matrix: The elapsed time is 1500-1700 seconds.
* Use sparse matrix: The elapsed time is 2600 seconds.

# Unit Test

Overlap between funds: 178472 and 216718

* Minimum Overlap = 0.26660
* Left Cross Overlap = 0.44561 
* Right Cross Overlap = 0.27654

# How to Run

Simply execute `overlap.py` with `python2.7`, which will invoke `overlap_for_all_funds()`.


