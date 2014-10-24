Cython Implementation
=======

In this implementation, [Cython](http://cython.org/) is used to test performance and memory consumption of dense matrix in C.
We still use python for high-level data operations, but switch to cython for number crunching.
There is no C-based sparse matrix implementation yet. So we won't be able to test that.

# Performance

* PC Spec: Dell, Ubuntu 64-bit, Intel Xeon W3550 @3.07GHz

## Large sample

* Memory: 1.8 GB for dense matrix. This large footprint is due to dense matrix in double.
* No matrix lookup: The elapsed time is 51 seconds. (Excluding for loop: 46 seconds; excluding set join: 19 seconds)
* Use dense matrix: The elapsed time is 56 seconds.

Cython increases performance a lot. It almost eliminates the matrix lookup overhead completely, as seen in MTJ.
There is a basic overhead (~20 seconds) when calling python functions millions of times.
Also there is a performance hit in the python "set join" operation (~20 seconds). But consider this takes ~100 seconds in scala, it is not too bad here.

# How to Run

Simply execute `overlap.sh -a` with `python2.7` and `cython`, which will invoke `overlap_for_all_funds()`.
Check out `overlap.sh -h` for other usage.


