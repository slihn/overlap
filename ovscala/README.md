Overlap in Scala
===================

This is an implementation of portfolio overlap in scala.
EJML SimpleMatrix (not a sparse matrix) is used as the numerical back-end.

# Performance

* PC Spec: Dell, Ubuntu 64-bit, Intel Xeon W3550 @3.07GHz
* Memory: The program consumes about 570 MB for storing data in SimpleMatrix

It takes about 48 seconds to generate the overlap matrix `OV`.
With this period, 37 seconds are spent in reading CSV.
There are 837 funds. The overlap matrix has 837*836/2 ~ 350,000 elements.
Each overlap element (between two funds) takes about 1.4 milliseconds. This is very fast.
But the memory foot print is very large.

# How to Run

> sbt "run unit"

Or,

> sbt "run all"