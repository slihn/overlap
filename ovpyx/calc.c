

#include <stdlib.h>
#include <stdio.h>
#include "calc.h"

ov_output calculate_overlap_c(long sp_fund_id1, long sp_fund_id2,
                              position *packed_overlap, fund_pointer *fund_ptr_list, int matrix_mode)
{
    ov_output ov;
    double a, b;
    long p1, p2;
    fund_pointer *ptr1, *ptr2;
    position *d1, *d2;


    ov.min_overlap = 0.0;
    ov.cross_left = 0.0;
    ov.cross_right = 0.0;

    ptr1 = &fund_ptr_list[sp_fund_id1];
    ptr2 = &fund_ptr_list[sp_fund_id2];

    if (matrix_mode == -2) return ov;

    p1 = ptr1->start;
    p2 = ptr2->start;
    while (p1 < ptr1->end && p2 < ptr2->end) {

        if (matrix_mode == -1) {
            p1++;
            p2++;
            continue;
        }
        d1 = &packed_overlap[p1];
        d2 = &packed_overlap[p2];
        if (matrix_mode <= 0) {
            if (d1->sp_security_id <= d2->sp_security_id) p1++;
            if (d1->sp_security_id >= d2->sp_security_id) p2++;
            continue;
        }
        // TODO the sp_security_id comparison appears to be quite slow
        if (d1->sp_security_id == d2->sp_security_id) {
            a = d1->pos_size;
            b = d2->pos_size;
            // printf("%ld %ld | %ld = %.6f %.6f\n", sp_fund_id1, sp_fund_id2, d1->sp_security_id, a, b);
            if (a > 0.0 && b > 0.0) {
                ov.min_overlap += (a < b ? a : b);
                ov.cross_left += a;
                ov.cross_right += b;
            }
            p1++;
            p2++;
        } else if (d1->sp_security_id < d2->sp_security_id) {
            p1++;
        } else if (d1->sp_security_id > d2->sp_security_id) {
            p2++;
        } else {
            printf("Index misaligned for sp_funds: %ld x %ld\n", sp_fund_id1, sp_fund_id2);
            exit(EXIT_FAILURE);
        }
    }
    return ov;
}

