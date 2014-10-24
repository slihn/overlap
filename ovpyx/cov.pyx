__author__ = 'slihn'

# Cython implementation of calculating portfolio overlap

import datetime
import time
from libc.stdlib cimport malloc, free

matrix_mode = 1  # 1: dense; 0: no matrix; -1: excluding for loop; -2: excluding set join

cdef double *sp_overlap
cdef long fund_len
cdef long security_len
cdef long mat_len

fund_dict = dict()
fund_dict_tup = dict()  # to remove expensive dict lookup in calculate_overlap()


def free_memory():
    free(sp_overlap)


def calculate_overlap(long fund_id1, long fund_id2):
    # Overlap(i,j) = Sum_k(Min(S_i_k, S_j_k))
    # i,j: institutions, k: security id, S_i_k: position size
    cdef double min_overlap = 0.0
    cdef double cross_left = 0.0
    cdef double cross_right = 0.0
    cdef double a, b

    i1, s1 = fund_dict_tup[fund_id1]
    i2, s2 = fund_dict_tup[fund_id2]

    if matrix_mode == -2:
        return [min_overlap, cross_left, cross_right]

    su = s1 & s2
    if matrix_mode == -1:
        return [min_overlap, cross_left, cross_right]

    for s in su:
        if matrix_mode <= 0:
            continue
        a = get_sp(i1, s)
        b = get_sp(i2, s)
        # print "%d %d | %d = %.6f %.6f" % (i1, i2, s, a, b)
        if a > 0.0 and b > 0.0:
            min_overlap += min(a, b)
            cross_left += a
            cross_right += b

    return [min_overlap, cross_left, cross_right]


cdef double get_sp(long sp_fund_id, long sp_security_id):
    cdef long i = sp_fund_id * fund_len + sp_security_id
    if i < mat_len:
        return sp_overlap[i]
    else:
        print "Index too large: %d x %d vs %d" % (sp_fund_id, sp_security_id, mat_len)
        raise MemoryError()


def generate_overlap_matrix(fund_list, data):
    global sp_overlap, fund_dict, fund_dict_tup, fund_len, security_len, mat_len

    for i in range(len(fund_list)):
        fund_dict[fund_list[i]] = {'sp_fund_id': i, 'sp_security_list': []}

    security_cnt = 0
    security_keys = dict()
    for ps in data:
        k = ps['security_key']
        sp_security_id = security_keys.get(k, -1)
        if sp_security_id == -1:
            security_keys[k] = security_cnt
            security_cnt += 1

    fund_len = len(fund_list)
    security_len = len(security_keys.keys())
    mat_len = fund_len * security_len
    sp_overlap = <double *>malloc(mat_len * sizeof(double))
    print "overlap matrix: allocate %d" % mat_len

    if not sp_overlap:
        raise MemoryError()
    for i in range(mat_len):
        sp_overlap[i] = 0.0

    for ps in data:
        fund_id = ps['fund_id']
        sp_fund_id = fund_dict[fund_id]['sp_fund_id']
        sp_security_id = security_keys[ps['security_key']]
        sp_overlap[sp_fund_id * fund_len + sp_security_id] = ps['pos_size']
        fund_dict[fund_id]['sp_security_list'].append(sp_security_id)

    for fund_id in fund_list:
        fnd = fund_dict[fund_id]
        s = set(fnd['sp_security_list'])
        fund_dict_tup[fund_id] = (fnd['sp_fund_id'], s)

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
