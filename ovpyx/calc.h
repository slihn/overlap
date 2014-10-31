
typedef struct {
    long sp_security_id;
    double pos_size;
} position;

typedef struct  {
    long start;
    long end;
    long length;
} fund_pointer;

typedef struct {
    double min_overlap;
    double cross_left;
    double cross_right;
} ov_output;

ov_output calculate_overlap_c(long sp_fund_id1, long sp_fund_id2,
                              position *packed_overlap, fund_pointer *fund_ptr_list, int matrix_mode);

