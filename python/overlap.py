__author__ = 'slihn'

from scipy import sparse
from numpy import float32
import datetime
import time
from os import path
import csv
import zipfile
import StringIO
# from numba import jit

# Global context
use_large = True
data_file_small = path.join("..", "data", "overlap_data_small.csv")
data_file_large = path.join("..", "data", "overlap_data.zip")

matrix_mode = 1  # 1: dense; 2: sparse; 0: no matrix; -1: excluding for loop; -2: excluding set join


def read_small_sample():
    print "Use small sample file"
    fh = open(data_file_small, 'rb')
    rd = csv.reader(fh, delimiter=',', quotechar='"')
    return fh, rd


def read_large_sample():
    print "Use large sample file"
    fh = open(data_file_large, 'rb')
    csv_file_in_zip = "overlap_data.csv"
    data_io = StringIO.StringIO(zipfile.ZipFile(fh).read(csv_file_in_zip))
    rd = csv.reader(data_io)
    return fh, rd


def get_pos_size_data(use_large):
    fh, rd = read_large_sample() if use_large else read_small_sample()

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

    fh.close()

    fund_list = sorted(fund_dict.keys())
    print "fund_list = %d rows = %d" % (len(fund_list), len(data))

    assert fund_dict[178472] == 1
    assert len(fund_list) == (3839 if use_large else 837)

    return fund_list, data


#@jit
def calculate_overlap(fund_id1, fund_id2, fund_dict, sp_overlap):
    # Overlap(i,j) = Sum_k(Min(S_i_k, S_j_k))
    # i,j: institutions, k: security id, S_i_k: position size
    min_overlap = 0.0
    cross_left = 0.0
    cross_right = 0.0

    data1 = fund_dict[fund_id1]
    data2 = fund_dict[fund_id2]
    i1 = data1['sp_fund_id']
    s1 = data1['sp_security_set']
    i2 = data2['sp_fund_id']
    s2 = data2['sp_security_set']

    if matrix_mode == -2:
        return [min_overlap, cross_left, cross_right]

    su = s1 & s2  # This intersect is an important performance step
    if matrix_mode == -1:
        return [min_overlap, cross_left, cross_right]

    for s in su:
        if matrix_mode > 0:
            a = sp_overlap[i1, s]
            b = sp_overlap[i2, s]
            if a > 0.0 and b > 0.0:
                min_overlap += min(a, b)
                cross_left += a
                cross_right += b

    return [min_overlap, cross_left, cross_right]


#@jit
def generate_overlap_sparse_matrix(use_large):

    fund_list, rs = get_pos_size_data(use_large)
    fund_dict = dict()
    for i in range(len(fund_list)):
        fund_dict[fund_list[i]] = {'sp_fund_id': i, 'sp_security_list': []}

    security_cnt = 0
    security_keys = dict()
    for ps in rs:
        k = ps['security_key']
        sp_security_id = security_keys.get(k, -1)
        if sp_security_id == -1:
            security_keys[k] = security_cnt
            security_cnt += 1

    sp_overlap = sparse.dok_matrix((len(fund_list), len(security_keys.keys())), dtype=float32)
    for ps in rs:
        fund_id = ps['fund_id']
        sp_fund_id = fund_dict[fund_id]['sp_fund_id']
        sp_security_id = security_keys[ps['security_key']]
        sp_overlap[sp_fund_id, sp_security_id] = ps['pos_size']
        fund_dict[fund_id]['sp_security_list'].append(sp_security_id)

    for fund_id in fund_list:
        fund_dict[fund_id]['sp_security_set'] = set(fund_dict[fund_id]['sp_security_list'])

    if matrix_mode == 1:
        sp_overlap = sp_overlap.todense()
        print "Use dense matrix"

    return {
        'fund_list': fund_list,
        'fund_dict': fund_dict,
        'sp_overlap': sp_overlap,
        'security_keys': security_keys,
        'security_cnt': security_cnt,
    }


def unit_test_2_funds(debug=False):
    fund_id1 = 178472
    fund_id2 = 216718

    sp = generate_overlap_sparse_matrix(use_large)
    fund_dict = sp['fund_dict']
    sp_overlap = sp['sp_overlap']
    if debug:
        print "security keys = %d" % sp['security_cnt']

    min_overlap, cross_left, cross_right = calculate_overlap(fund_id1, fund_id2, fund_dict, sp_overlap)

    # reference data, pre-calculated from another source
    min_overlap_ref = 0.26660
    cross_left_ref = 0.44561
    cross_right_ref = 0.27654

    print "ovlp  %.5f vs %.5f for %d vs %d" % (min_overlap_ref, min_overlap, fund_id1, fund_id2)
    print "left  %.5f vs %.5f for %d vs %d" % (cross_left_ref, cross_left, fund_id1, fund_id2)
    print "right %.5f vs %.5f for %d vs %d" % (cross_right_ref, cross_right, fund_id1, fund_id2)
    assert ("%.5f" % min_overlap_ref) == ("%.5f" % min_overlap)
    assert ("%.5f" % cross_left_ref) == ("%.5f" % cross_left)
    assert ("%.5f" % cross_right_ref) == ("%.5f" % cross_right)

    print "unit test pass"


def overlap_for_all_funds(debug=False):
    print now_iso()
    tm_start = millis()
    cnt_mod = 5000 if use_large else 1000

    sp = generate_overlap_sparse_matrix(use_large)
    fund_dict = sp['fund_dict']
    sp_overlap = sp['sp_overlap']
    fund_list = sp['fund_list']
    print "security keys = %d" % sp['security_cnt']
    print "%s: start" % now_iso()
    cnt = 0
    fund_ov = dict()
    for fund_id1 in fund_list:
        fund_ov[fund_id1] = dict()
        for fund_id2 in fund_list:
            if fund_id2 <= fund_id1:
                continue
            min_overlap, cross_left, cross_right = calculate_overlap(fund_id1, fund_id2, fund_dict, sp_overlap)
            fund_ov[fund_id1][fund_id2] = [min_overlap, cross_left, cross_right]
            cnt += 1
            if debug or cnt % cnt_mod == 0 or cnt < 100:
                elapsed = ("elapsed %d sec" % ((millis()-tm_start)/1000)) if cnt % 100000 == 0 else ""
                print "ovlp %7d  %.5f %.5f %.5f for %d vs %d %s" % \
                      (cnt, min_overlap, cross_left, cross_right, fund_id1, fund_id2, elapsed)

    tm_end = millis()
    print "%s: elapsed %d millis" % (now_iso(), tm_end-tm_start)
    return fund_ov


def fund_statistics(debug):
    import math

    sp = generate_overlap_sparse_matrix(use_large)
    print "security keys = %d" % sp['security_cnt']

    factor = 4
    fund_list = sp['fund_list']
    fund_dict = sp['fund_dict']
    fund_stats = dict()
    for fund_id in fund_list:
        security_cnt = len(fund_dict[fund_id]['sp_security_list'])
        if security_cnt < 1:
            continue
        if debug:
            print "%d - %-6d" % (fund_id, security_cnt)
        bucket = math.ceil(math.log10(security_cnt)/math.log10(factor))
        if fund_stats.get(bucket) is None:
            fund_stats[bucket] = 0
        fund_stats[bucket] += 1

    print "distribution of # of positions, bucketed by a factor of %d:" % factor
    for bucket in sorted(fund_stats.keys()):
        print "nbr of positions <= %-6d - nbr of funds %-6d" % (math.pow(factor, bucket), fund_stats[bucket])


# ---------------------------------------------------------------- #
def now_iso():
    return datetime.datetime.now().isoformat(' ')


def millis():
    return int(round(time.time() * 1000))


# ---------------------------------------------------------------- #
def usage():
    print """
    -a, --all: generate overlap matrix for all funds in sample data
    -u, --unit: invoke the unit test (between two funds)
    -d, --debug: show debug messages
    -s, --stats: statistics on funds, primarily distribution of # of positions
    -h, --help: this help
    """


def main(argv):
    import getopt
    try:
        opts, args = getopt.getopt(argv,
                                   "daush",
                                   ["debug", "all", "unit", "stats", "help"])
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    debug_mode = False
    for opt, arg in opts:
        if opt in ('-d', '--debug'):
            debug_mode = True
            continue
        elif opt in ('-a', '--all'):
            overlap_for_all_funds(debug_mode)
            continue
        elif opt in ('-u', '--unit'):
            unit_test_2_funds(debug_mode)
            continue
        elif opt in ('-s', '--stats'):
            fund_statistics(debug_mode)
            continue
        elif opt in ('-h', '--help'):
            usage()
            sys.exit(0)
        else:
            print "Unknown option: %s" % opt
            usage()
            sys.exit(2)
            
            
if __name__ == "__main__":
    import sys
    main(sys.argv[1:])
