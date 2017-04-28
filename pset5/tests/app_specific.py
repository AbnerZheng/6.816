import sys
import commands
import re


def run_cmd(cmd):
    status, output = commands.getstatusoutput(cmd)

    pkt_per_ms = None
    if status != 0:
        print '    Status: %d' % status
    else:
        result = re.search('PKT_PER_MS(.*)PKT_PER_MS', output)
        try:
            pkt_per_ms_str = result.group(1)
            pkt_per_ms = float(pkt_per_ms_str)
        except:
            print '    Could not convert %s to float' % pkt_per_ms_str

    return pkt_per_ms


def run_test(h0="AppSpecificHashTable", h1="LockFreeHashTable"):
    print h0, h1
    max_bucket_size = 4

    num_milisseconds = 500
    w = 4000
    init_size = 1000000

    n_workers_vals = [1, 2, 4, 8]
    max_bucket_size_vals = [1, 2, 4, 8, 16, 32, 64, 128, 256]

    load_vals = [(0.2, 0.2)]
    rho_vals = [0.5]

    if max_bucket_size is None:
        print "Set max_bucket_size before running"
        assert(max_bucket_size is not None)

    for max_bucket_size in max_bucket_size_vals:
        speedups = []
        for load in load_vals:
            for rho in rho_vals:
                print '-----------------------------------------------'
                print 'Params load=%s, rho=%s, mbs=%d' % (str(load), str(rho), max_bucket_size)
                for n_workers in n_workers_vals:
                    params = '%d %f %f %f %d %d %d' % (num_milisseconds,
                                                       load[0],
                                                       load[1],
                                                       rho,
                                                       max_bucket_size,
                                                       w,
                                                       init_size)
                    h0_cmd = 'java ParallelHashPacket %s %d %s' % (params, n_workers, h0)
                    h1_cmd = 'java ParallelHashPacket %s %d %s' % (params, n_workers, h1)
                    # print h0_cmd
                    # print h1_cmd
                    h0_pkts_per_ms = run_cmd(h0_cmd)
                    h1_pkts_per_ms = run_cmd(h1_cmd)
                    speedup = h0_pkts_per_ms / h1_pkts_per_ms
                    speedups.append(speedup)
                    print '%d Workers\t %f' % (n_workers, speedup)
                print
            print

        print "Average speedup is %f" % (sum(speedups) / len(speedups))


if __name__ == '__main__':
    if len(sys.argv) == 3:
        h0 = sys.argv[1]
        h1 = sys.argv[2]
        run_test(h0, h1)
    else:
        run_test()
