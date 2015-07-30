overlap
=======

High-Performance Sparse Matrix Implementation of Portfolio Overlap Calculation

# Introduction

Portfolio overlap attempts to estimate the correlation between two portfolios based on simple sum of position weights, instead of statistical properties of prices. 
The math is much simpler, yet it is a powerful tool to analyze how a portfolio compares to its peers.
The higher the overlap is, the more likely the portfolios will move in the same direction and magnitude. 
In the hedge funds, managers tend to flock into similar stocks, especially during a bull market. 
This is described as the herd behavior. But when the bear market sets in, hedge fund managers can engage in fire sale, and everybody is trying to pull out at the same time, causing unusual volatility and large drawdown. 
Therefore, monitoring portfolio through overlap becomes very important for fund managers to avoid the trampede of the herd behavior.

The definition of portfolio overlap is simple. However, its calculation involves comparing every position between any two portfolios in the system. The permutation grows quickly in a large database. Therefore, how to do it efficiently is an interesting topic in quantitative analysis.

# Definition of Portfolio Overlap

There are three different overlaps -
* Minimum Overlap - intersection between the two portfolios
* Left Cross Overlap - measures the affected percentage in the first portfolio 
* Right Cross Overlap - measures the affected percentage in the second portfolio

Let's say `i,j` are two portfolios; `k` represents the security id -
* `OV[i][j]` is the overlap between `i` and `j`; 
* `k` is the security id;
* `S[i][k]` is the position size (or weight) of security `k` in portfolio `i`;

then 

> `OV[i][j] = Sum_k( OP(S[i][k], S[j][k]) )`

where the operator `OP` can be defined as
* `OP(a,b) = min(a,b)` for Minimum Overlap
* `OP(a,b) = a if a > 0 and b > 0` for Left Cross Overlap
* `OP(a,b) = b if a > 0 and b > 0` for Right Cross Overlap

# Why Sparse Matrix

In a big-data database where you have thousands or 10s of thousands portfolios, the matrix `S[i][k]` is best understood as a sparse matrix. The overlap is simply a re-defined matrix dot operator -

> `OV = S . S*` 

where `*` is matrix transpose operator; and `.` is the abstract notation of `Sum_k(OP(...))`. 

(Note: `.` is reduced to the normal matrix dot operator when `OP` is just the multiplication, `a x b`)

Therefore, large-scale computation of portfolio overlap `OV` can be most efficiently implemented by a high-performance sparse matrix library. When the library offers `O(1)` matrix lookup scalability (that is, lookup of `S[i][k]`), then the performance of calculating `OV` will scale as `O(N^2)` where `N` is number of portfolios. (Assuming number of securities remains statisically similar.)
However, lookup in sparse matrix is usually slower than `O(1)` and is `O(<k>)` where `<k>` is average number of positions in these portfolios. if sparse matrix lookup is much slower than that of dense matrix and the memory overhead of dense matrix is not much larger, then you would still want to use the faster dense matrix implementation.

The other performance driver is the intersect of security keys, `{k in i} U {k in j}`. How this is calculated can affect the elapsed time as much as matrix lookup. The sparse implementation in Scala and Cython/C combines the two aspects together to deliver high performance.

The choice of software library will be heavily dependent on your computing environment.

# Implementation

The calculation is implemented in various languages to compare performance -
* [Python Impl](https://github.com/slihn/overlap/blob/master/python/README.md) - [scipy.sparse](http://docs.scipy.org/doc/scipy-0.14.0/reference/sparse.html) - dok_matrix. It is very easy to implement this in python. But python's dynamic type is slow for number crunching.
* [Scala Impl](https://github.com/slihn/overlap/tree/master/ovscala) -  Dense matrix using [EJML](https://code.google.com/p/efficient-java-matrix-library/) and [MTJ](https://github.com/fommil/matrix-toolkits-java). Scala's static type offers very good performance after careful tuning.
* [Cython Impl](https://github.com/slihn/overlap/tree/master/ovpyx) - Testing C performance with [Cython](http://cython.org/). Both dense matrix and sparse matrix (with C extension) are implemented and both are extremely fast.

# Data

Two sample files are attached in data/ folder. Each implementation should be benchmarked against the sample data, preferably the large sample.

## Small sample

* CSV file: `overlap_data_small.csv`
* About 118,000 rows, 4 MB in size
* 837 funds, ranging from a few positions to thousands of positions in each fund
* 15,162 security keys
* Dense matrix size = 837 * 15,162 = 12,690,594
* The data matrix is sparse, only 0.9% of cells is non-zero: 118,000 / 12,690,594 = 0.93% 

## Large sample
 
* CSV file: `overlap_data.zip` (contains `overlap_data.csv`)
* About 897,000 rows, 30 MB in size after unzip. This is 7 times of the small sample.
* 3839 funds, ranging from a few positions to thousands of positions in each fund
* 37,826 security keys
* Dense matrix size = 3839 * 37,826 = 145,214,014
* The data matrix is sparse, only 0.6% of cells is non-zero: 897,000 / 145,214,014 = 0.62% 
