__author__ = 'slihn'

from numpy import float32
import datetime
import time
from os import path
import csv
import zipfile
import StringIO
import cov  # this is cython, using dense matrix
import cov_packed # this is cython, using sparse vector

# Global context
use_large = True
data_file_small = path.join("..", "data", "overlap_data_small.csv")
data_file_large = path.join("..", "data", "overlap_data.zip")

use_cov_packed = True

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


def get_overlap_matrix():
    fund_list, data = get_pos_size_data(use_large)
    if not use_cov_packed:
        sp = cov.generate_overlap_matrix(fund_list, data)
    else:
        sp = cov_packed.generate_overlap_matrix(fund_list, data)
    return sp


def unit_test_2_funds(debug=False):
    fund_id1 = 178472
    fund_id2 = 216718

    sp = get_overlap_matrix()
    if not use_cov_packed:
        min_overlap, cross_left, cross_right = cov.calculate_overlap(fund_id1, fund_id2)
    else:
        min_overlap, cross_left, cross_right = cov_packed.calculate_overlap(fund_id1, fund_id2)

    if debug:
        print "security keys = %d" % sp['security_cnt']
    print "calculate overlap for fund %d vs %d" % (fund_id1, fund_id2)

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

    print "Unit test pass"


def overlap_for_all_funds(debug=False):
    print "%s: read data" % now_iso()
    tm_start = millis()

    sp = get_overlap_matrix()
    print "security keys = %d" % sp['security_cnt']
    print "%s: start" % now_iso()

    # Here we call into the high-performance cython implementation
    if not use_cov_packed:
        fund_ov = cov.overlap_for_all_funds(sp, use_large, debug)
    else:
        fund_ov = cov_packed.overlap_for_all_funds(sp, use_large, debug)

    tm_end = millis()
    print "%s: elapsed %d millis" % (now_iso(), tm_end-tm_start)
    return fund_ov


def fund_statistics(debug):
    import math

    sp = get_overlap_matrix()
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
