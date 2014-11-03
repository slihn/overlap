Python Implementation
=======

In this implementation, dok_matrix in scipy.sparse is used to achieve constant time matrix lookup.
Alternatively, you can change matrix_mode to compare performance and memory consumption of dense matrix.

# Performance

* PC Spec: Dell, Ubuntu 64-bit, Intel Xeon W3550 @3.07GHz

In this python implementation, dynamic typing and dictionary lookup are used extensively. These are rather slow operations.

## Small sample

It takes about 46 seconds to generate the overlap matrix `OV`.
There are 837 funds. The overlap matrix has 837*836/2 ~ 350,000 elements.
Each overlap element (between two funds) takes about 1.3 milliseconds. 

* Memory: The program consumes about 50 MB for storing data in the sparse matrix
* No matrix lookup: The elapsed time is 3 seconds.
* Use dense matrix: The elapsed time is 31 seconds.
* Use sparse matrix: The elapsed time is 46 seconds.

The major burden of this calculation is on the matrix lookup.

## Large sample

* Memory: 500 MB for storing data in the sparse matrix; 1 GB for dense matrix.
* No matrix lookup: The elapsed time is 63 seconds. (Excluding for loop: 58 seconds; excluding set join: 33 seconds)
* Use dense matrix: The elapsed time is 1400 seconds.
* Use sparse matrix: The elapsed time is 2400 seconds.

This slowness is due to the dynamic language nature of python. Check out [Cython/C integration](https://github.com/slihn/overlap/tree/master/ovpyx) for a high performance implementation.

# Unit Test

Overlap between funds: 178472 and 216718

* Minimum Overlap = 0.26660
* Left Cross Overlap = 0.44561 
* Right Cross Overlap = 0.27654

# How to Run

Simply execute `overlap.py -a` with `python2.7`, which will invoke `overlap_for_all_funds()`.
Check out `overlap.py -h` for other usage.

