overlap
=======

High-Performance Sparse Matrix Implementation of Portfolio Overlap Calculation

# Introduction

Portfolio overlap is a powerful tool to analyze how a portfolio compares to its peers. In the hedge funds, managers tend to flock into similar stocks, especially during a bull market. This is described as the herd behavior. But when the bear market sets in, hedge fund managers can engage in fire sale, and everybody is trying to pull out at the same time, causing unusual volatility and large drawdown. Therefore, monitoring portfolio through overlap becomes very important for fund managers to avoid the trampede of the herd behavior.

The definition of portfolio overlap is simple. However, its calculation involves comparing every position between any two portfolios in the system. The permutation grows quickly in a large database. Therefore, how to do it efficiently is an interesting topic in quantitative analysis.

# Definition of Portfolio Overlap

There are three different overlaps -
* Minimum Overlap
* Left Cross Overlap
* Right Cross Overlap

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

where `*` is matrix transport; and `.` is an abstract form of `Sum_k(OP())`.

Therefore, large-scale computation of portfolio overlap can be most efficiently implemented by a high-performance sparse matrix library. When the library offers `O(1)` matrix lookup scalability (on `S[i][k]`), then the performance of calculating `OV` will scale as `O(N^2)` where `N` is number of portfolios. (Assuming number of securities remains statisically similar.)

# Implementation

I intend to implement the calculation in various languages to compare performance -
* Python - scipy.sparse - dok_matrix
* R - TBD
* Java - TBD
* GSL Sparse - TBD

