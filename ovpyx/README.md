Cython Implementation
=======

In this implementation, [Cython](http://cython.org/) is used to test performance and memory consumption of dense matrix in C.
We still use python for high-level data operations, but switch to cython for number crunching.
There is no C-based sparse matrix implementation yet. So we won't be able to test that.

# Performance

* PC Spec: Dell, Ubuntu 64-bit, Intel Xeon W3550 @3.07GHz

## Dense Matrix, Large sample

> Set `use_cov_packed = False` in `overlap.py`

* Memory: 1.8 GB for dense matrix. This large footprint is due to dense matrix in double.
* No matrix lookup: The elapsed time is 51 seconds. (Excluding for loop: 46 seconds; excluding set join: 19 seconds)
* Use dense matrix: The elapsed time is 56 seconds.

Cython increases performance a lot. It almost eliminates the matrix lookup overhead completely, as seen in MTJ.
There is a basic overhead (~20 seconds) when calling python functions millions of times.
Also there is a performance hit in the python "set join" operation (~20 seconds). But consider this takes ~100 seconds in scala, it is not too bad here.

## Sparse Matrix (cov_packed), Large sample

> Set `use_cov_packed = True` in `overlap.py`

* Memory: 500 MB for sparse matrix. The memory footprint is reduced by the sparse implementation.

** 90% Pure implementation in Cython
* No matrix lookup: The elapsed time is 40 seconds. 
* Use sparse lookup: The elapsed time is 260 seconds.
** Core calculation implemented in C code (Large structure still in Cython)
* No matrix lookup: The elapsed time is 6 seconds. 
* Use sparse lookup: The elapsed time is 39 seconds.

> As you can see, pyx is 10 times faster than python, and C is 6 times faster than pyx

This method is suggested by Will Meyer. It uses a one-dimensional position array to store (sorted) security id and position size.
And a pointer structure to label the start and end locations for each fund. This is a form of sparse implementation.
The majority of overhead comes from scanning through all the involved securities and comparing them between two funds.

# How to Run

Simply execute `overlap.sh -a` with `python2.7` and `cython`, which will invoke `overlap_for_all_funds()`.
Check out `overlap.sh -h` for other usage.


