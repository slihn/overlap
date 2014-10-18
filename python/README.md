Python Implementation
=======

In this implementation, dok_matrix in scipy.sparse is used to achieve constant time matrix lookup.

# Performance

* PC Spec: Dell, Ubuntu 64-bit, Intel Xeon W3550 @3.07GHz
* Memory: The program consumes about 50 MB for storing data in the sparse matrix

It takes about 52 seconds to generate the overlap matrix `OV`.
There are 837 funds. The overlap matrix has 837*836/2 ~ 350,000 elements.
Each overlap element (between two funds) takes about 1.5 milliseconds. This is reasonably fast.

If sparse matrix lookup is commented out, the elapsed time is reduced to 9 seconds.
If sparse matrix is converted to numpy matrix (dense), the elapsed time is 36 seconds.
This proves that the major burden of this calculation is on the matrix lookup.

# Unit Test

Overlap between funds: 178472 and 216718

* Minimum Overlap = 0.26660
* Left Cross Overlap = 0.44561 
* Right Cross Overlap = 0.27654

# How to Run

Simply execute `overlap.py` with `python2.7`, which will invoke `unit_test_all_funds()`.


