Overlap in Scala
===================

This is an implementation of portfolio overlap in scala.
EJML SimpleMatrix and MTJ DenseMatrix (not sparse matrix) are used as the numerical back-end.

# Performance

* PC Spec: Dell, Ubuntu 64-bit, Intel Xeon W3550 @3.07GHz

After extensive tuning, scala can calculate overlap with extremely high speed.
There are two expensive computation bottlenecks -

The first bottleneck is obviously the cost of matrix lookup. 
Dense matrix is much faster than sparse matrix, but with a very large memory footprint.

The second bottleneck is more subtle. It is the intersect of security keys between two funds and the subsequent "for loop" on the intersect. 
The cost can be as significant if not more. Since this part involves array and set operation in scala collection library, its performance can be a drag if not implemented correctly.

Comparing scala to python, the static typing of scala is significantly faster than python.
Python uses dynamic typing and dictionary lookup extensively. These are slow operations.

## Small sample

It takes about 5 seconds to generate the overlap matrix `OV`.
There are 837 funds. The overlap matrix has 837*836/2 ~ 350,000 elements.
Each overlap element (between two funds) takes about 0.14 milliseconds. This is very fast.
But the memory foot print is very large.

* Memory: The program consumes about 600 MB for storing data in the dense matrix (EJML)
* No matrix lookup: The elapsed time is 4 seconds.
* Use dense matrix: The elapsed time is 5 seconds.
* Use sparse matrix: N/A

## Large sample

* Memory: 
** Dense matrix - 1.8 GB for both EJML and MTJ. This is quite large on 30MB of raw data.
** Sparse matrix - TBD, I can't find any suitable implementation yet...
* No matrix lookup: The elapsed time is 110 seconds. (Excluding for loop: 110 seconds; excluding set join: 6 seconds)
** The "set join" cost is relatively high, compared to the matrix lookup.
* Use dense matrix: The elapsed time is 158 seconds (EJML); 122 seconds (MTJ).
** Here you can see MTJ's C based library is significantly faster than EJML.
* Use sparse matrix: N/A

# How to Run

> sbt -J-Xmx4G "run unit"

Or,

> sbt -J-Xmx4G "run all"
