__author__ = 'slihn'

# Cython implementation of calculating portfolio overlap
# This implementation uses a native sparse vector method from Will Meyer

import datetime
import time
from libc.stdlib cimport malloc, free

matrix_mode = 1  # 1: dense; 0: no matrix; -1: excluding for loop; -2: excluding set join

cdef long fund_len
cdef long security_len
cdef long mat_len

cdef struct position:
    long sp_security_id
    double pos_size

cdef struct fund_pointer:
    long start
    long end
    long length

cdef position *packed_overlap
cdef fund_pointer *fund_ptr_list

fund_dict = dict()
fund_dict_tup = dict()  # to remove expensive dict lookup in calculate_overlap()


def free_memory():
    free(packed_overlap)
    free(fund_ptr_list)


def calculate_overlap(long fund_id1, long fund_id2):
    # Overlap(i,j) = Sum_k(Min(S_i_k, S_j_k))
    # i,j: institutions, k: security id, S_i_k: position size
    cdef double min_overlap = 0.0
    cdef double cross_left = 0.0
    cdef double cross_right = 0.0
    cdef double a, b
    cdef long p1, p2
    cdef fund_pointer ptr1, ptr2
    cdef position d1, d2

    # print "calc %d %d" % (fund_id1, fund_id2)

    i1, s1 = fund_dict_tup[fund_id1]
    i2, s2 = fund_dict_tup[fund_id2]
    ptr1 = fund_ptr_list[i1]
    ptr2 = fund_ptr_list[i2]

    if matrix_mode == -2:
        return [min_overlap, cross_left, cross_right]

    p1 = ptr1.start
    p2 = ptr2.start
    # TODO how do we use set to reduce while-loop scope? su = s1 & s2
    while (p1 < ptr1.end and p2 < ptr2.end):

        if matrix_mode == -1:
            p1 += 1
            p2 += 1
            continue

        d1 = packed_overlap[p1]
        d2 = packed_overlap[p2]
        if matrix_mode <= 0:
            if d1.sp_security_id <= d2.sp_security_id:
                p1 += 1
            if d1.sp_security_id >= d2.sp_security_id:
                p2 += 1
            continue

        # TODO the sp_security_id comparison appears to be quite slow
        if d1.sp_security_id == d2.sp_security_id:
            a = d1.pos_size
            b = d2.pos_size
            # print "%d %d | %d = %.6f %.6f" % (i1, i2, d1.sp_security_id, a, b)
            if a > 0.0 and b > 0.0:
                min_overlap += min(a, b)
                cross_left += a
                cross_right += b
            p1 += 1
            p2 += 1
        elif d1.sp_security_id < d2.sp_security_id:
            p1 += 1
        elif d1.sp_security_id > d2.sp_security_id:
            p2 += 1
        else:
            print "Index misaligned for sp_funds: %d x %d" % (i1, i2)
            raise MemoryError()

    return [min_overlap, cross_left, cross_right]


def generate_overlap_matrix(fund_list, data):
    global packed_overlap, fund_ptr_list
    global fund_len, security_len, mat_len
    global fund_dict, fund_dict_tup
    cdef position pos

    fund_len = len(fund_list)
    fund_ptr_list = <fund_pointer *>malloc(fund_len * sizeof(fund_pointer))
    sp_fund_dict = dict()
    for i in range(fund_len):
        fund_id = fund_list[i]
        fund_dict[fund_id] = {'sp_fund_id': i, 'sp_security_list': [], 'sp_security_data': []}
        sp_fund_dict[i] = fund_id

    security_cnt = 0
    security_keys = dict()
    for ps in data:
        k = ps['security_key']
        sp_security_id = security_keys.get(k, -1)
        if sp_security_id == -1:
            security_keys[k] = security_cnt
            security_cnt += 1

    for ps in data:
        fund_id = ps['fund_id']
        sp_fund_id = fund_dict[fund_id]['sp_fund_id']
        sp_security_id = security_keys[ps['security_key']]
        fund_dict[fund_id]['sp_security_list'].append(sp_security_id)
        d = [sp_security_id, ps['pos_size']]
        fund_dict[fund_id]['sp_security_data'].append(d)

    security_len = len(security_keys.keys())
    mat_len = 0
    for sp_fund_id in range(fund_len):
        fund_id = sp_fund_dict[sp_fund_id]
        s = fund_dict[fund_id]['sp_security_list']
        fund_dict[fund_id]['sp_security_list'] = sorted(s)
        ds = fund_dict[fund_id]['sp_security_data']
        fund_dict[fund_id]['sp_security_data'] = sorted(ds, key=lambda x: x[0])
        assert len(s) == len(ds)

        fund_ptr_list[sp_fund_id].start = mat_len
        fund_ptr_list[sp_fund_id].length = len(ds)
        mat_len += len(ds)
        fund_ptr_list[sp_fund_id].end = mat_len # exclusive

    packed_overlap = <position *>malloc(mat_len * sizeof(position))
    print "packed overlap matrix: allocate %d" % mat_len

    if not packed_overlap:
        raise MemoryError()
    for i in range(mat_len):
        pos.sp_security_id = 0
        pos.pos_size = 0.0
        packed_overlap[i] = pos

    print "filling up overlap"
    for sp_fund_id in range(fund_len):
        fund_id = sp_fund_dict[sp_fund_id]
        ds = fund_dict[fund_id]['sp_security_data']
        for i in range(len(ds)):
            d = ds[i]
            pos.sp_security_id = d[0]
            pos.pos_size = d[1]
            j = fund_ptr_list[sp_fund_id].start + i
            if j >= mat_len:
                print "Index too large: %d x %d vs %d" % (sp_fund_id, d[0], mat_len)
                raise MemoryError()
            packed_overlap[j] = pos
        fund_dict[fund_id]['sp_security_data'] = []  # clean up to save memory

    print "filled up overlap"

    for fund_id in fund_list:
        fnd = fund_dict[fund_id]
        s = fnd['sp_security_list']
        fund_dict_tup[fund_id] = (fnd['sp_fund_id'], set(s))

    return {
        'fund_list': fund_list,
        'fund_dict': fund_dict,
        'security_keys': security_keys,
        'security_cnt': security_cnt,
    }

# ---------------------------------------------------------------- #
def overlap_for_all_funds(sp, use_large, debug=False):
    tm_start = millis()
    cnt_mod = 20000 if use_large else 1000
    fund_list = sp['fund_list']
    cnt = 0
    fund_ov = dict()
    for fund_id1 in fund_list:
        fund_ov[fund_id1] = dict()
        for fund_id2 in fund_list:
            if fund_id2 <= fund_id1:
                continue
            min_overlap, cross_left, cross_right = calculate_overlap(fund_id1, fund_id2)
            # min_overlap, cross_left, cross_right = [0,0,0]
            fund_ov[fund_id1][fund_id2] = [min_overlap, cross_left, cross_right]
            cnt += 1
            if debug or cnt % cnt_mod == 0 or cnt < 100:
                elapsed = ("elapsed %d sec" % ((millis()-tm_start)/1000)) if cnt % 500000 == 0 else ""
                print "ovlp %7d  %.5f %.5f %.5f for %d vs %d %s" % \
                      (cnt, min_overlap, cross_left, cross_right, fund_id1, fund_id2, elapsed)

    return fund_ov

# ---------------------------------------------------------------- #
def now_iso():
    return datetime.datetime.now().isoformat(' ')


def millis():
    return int(round(time.time() * 1000))
