__author__ = 'slihn'

from scipy import sparse
from numpy import float32
import datetime
import time
from os import path
import csv

data_file = path.join("..", "data", "overlap_data.csv")


def get_pos_size_data():
    csvfile = open(data_file, 'rb')
    rd = csv.reader(csvfile, delimiter=',', quotechar='"')
    header = None
    data = []
    fund_dict = dict()
    for row in rd:
        if header is None:
            header = row
            assert header == ['fund_id', 'security_key', 'pos_size']
            continue

        d = dict()
        row = [int(row[0]), row[1], float32(row[2])]
        for i in range(len(header)):
            d[header[i]] = row[i]
        data.append(d)
        fund_dict[d['fund_id']] = 1

    csvfile.close()
    fund_list = sorted(fund_dict.keys())
    print "fund_list = %d rows = %d" % (len(fund_list), len(data))

    assert fund_dict[178472] == 1
    assert len(fund_list) == 837

    return fund_list, data


def calculate_overlap(sp, fund_id1, fund_id2):
    # Overlap(i,j) = Sum_k(Min(S_i_k, S_j_k))
    # i,j: institutions, k: security id, S_i_k: position size
    min_overlap = 0.0
    cross_left = 0.0
    cross_right = 0.0

    data1 = sp['fund_dict'][fund_id1]
    data2 = sp['fund_dict'][fund_id2]
    i1 = data1['sp_fund_id']
    s1 = data1['sp_security_list']
    i2 = data2['sp_fund_id']
    s2 = data2['sp_security_list']

    su = list(set(s1) & set(s2))  # This intersect is an important performance step
    sp_overlap = sp['sp_overlap']
    for s in su:
        a = sp_overlap[i1, s]
        b = sp_overlap[i2, s]
        if a is not None and b is not None and a > 0.0 and b > 0.0:
            min_overlap += min(a, b)
            cross_left += a
            cross_right += b

    return [min_overlap, cross_left, cross_right]


def generate_overlap_sparse_matrix():

    fund_list, rs = get_pos_size_data()
    fund_dict = dict()
    for i in range(len(fund_list)):
        fund_dict[fund_list[i]] = {'sp_fund_id': i, 'sp_security_list': []}

    security_cnt = 0
    security_keys = dict()
    for ps in rs:
        k = ps['security_key']
        sp_security_id = security_keys.get(k)
        if sp_security_id is None:
            security_keys[k] = security_cnt
            security_cnt += 1

    sp_overlap = sparse.dok_matrix((len(fund_list), len(security_keys.keys())), dtype=float32)
    for ps in rs:
        fund_id = ps['fund_id']
        sp_fund_id = fund_dict[fund_id]['sp_fund_id']
        sp_security_id = security_keys[ps['security_key']]
        sp_overlap[sp_fund_id, sp_security_id] = ps['pos_size']
        fund_dict[fund_id]['sp_security_list'].append(sp_security_id)

    return {
        'fund_list': fund_list,
        'fund_dict': fund_dict,
        'sp_overlap': sp_overlap,
        'security_keys': security_keys,
        'security_cnt': security_cnt,
    }


def unit_test_2_funds():
    fund_id1 = 178472
    fund_id2 = 216718

    min_ov_ref = 0.26660
    left_ov_ref = 0.44561
    right_ov_ref = 0.27654

    sp = generate_overlap_sparse_matrix()
    print "security keys = %d" % sp['security_cnt']

    min_ov, left_ov, right_ov = calculate_overlap(sp, fund_id1, fund_id2)
    print "ovlp  %.5f vs %.5f for %d vs %d" % (min_ov_ref, min_ov, fund_id1, fund_id2)
    print "left  %.5f vs %.5f for %d vs %d" % (left_ov_ref, left_ov, fund_id1, fund_id2)
    print "right %.5f vs %.5f for %d vs %d" % (right_ov_ref, right_ov, fund_id1, fund_id2)
    assert ("%.5f" % min_ov_ref) == ("%.5f" % min_ov)
    assert ("%.5f" % left_ov_ref) == ("%.5f" % left_ov)
    assert ("%.5f" % right_ov_ref) == ("%.5f" % right_ov)


def unit_test_all_funds(debug=False):
    print now_iso()
    tm_start = millis()

    sp = generate_overlap_sparse_matrix()
    fund_list = sp['fund_list']
    print "security keys = %d" % sp['security_cnt']
    print "%s: start" % now_iso()
    cnt = 0
    for fund_id1 in fund_list:
        for fund_id2 in fund_list:
            if fund_id2 <= fund_id1:
                continue
            min_ov, left_ov, right_ov = calculate_overlap(sp, fund_id1, fund_id2)
            cnt += 1
            if debug or cnt % 1000 == 0 or cnt < 100:
                print "ovlp %7d  %.5f %.5f %.5f for %d vs %d" % (cnt, min_ov, left_ov, right_ov, fund_id1, fund_id2)

    tm_end = millis()
    print "%s: elapsed %d millis" % (now_iso(), tm_end-tm_start)


def now_iso():
    return datetime.datetime.now().isoformat(' ')

def millis():
    return int(round(time.time() * 1000))

if __name__ == "__main__":
    # unit_test_2_funds()
    unit_test_all_funds(False)