
cdef extern from "calc.h":
    ctypedef struct position:
        long sp_security_id
        double pos_size

    ctypedef struct fund_pointer:
        long start
        long end
        long length

    ctypedef struct ov_output:
        double min_overlap
        double cross_left
        double cross_right

    ov_output calculate_overlap_c(long sp_fund_id1, long sp_fund_id2,
                                  position *packed_overlap, fund_pointer *fund_ptr_list, int matrix_mode)

