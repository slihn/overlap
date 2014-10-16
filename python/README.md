Python Implementation
=======

In this implementation, dok_matrix in scipy.sparse is used to achieve constant time matrix lookup.

# Performance

Data: CSV file of about 118 rows, 4 MB
PC Spec: Dell, Windows 7 64-bit, Intel Xeon W3550 @3.07GHz
Memory: The program consumes about 50 MB for storing data in the sparse matrix

It takes about 95-110 seconds to generate the overlap matrix `OV`.
There are 837 funds. The overlap matrix has 837*836/2 ~ 350,000 elements.
Each overlap element (between two funds) takes about 3.5 milliseconds. This is very fast.

# Unit Test

Overlap between funds: 178472 and 216718

* Minimum Overlap = 0.26660
* Left Cross Overlap = 0.44561 
* Right Cross Overlap = 0.27654



