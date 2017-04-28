import os
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


def run_test():
    max_bucket_size = 4

    num_milisseconds = 2000
    w = 4000
    init_size = 0

    n_workers = 4
    bucket_size_vals = [1, 2, 4, 8, 16, 32]

    load_vals = [(0.09, 0.01), (0.45, 0.05)]
    rho_vals = [0.5]

    h_vals = [ 'LockingHashTable',
               'LockFreeHashTable',
               'LinearProbeHashTable',
               'CuckooHashTable']
               # 'AwesomeHashTable' ]

    # h_vals = ['LockingHashTable']
    # h_vals = ['LockFreeHashTable']
    # h_vals = ['LinearProbeHashTable']
    # h_vals = ['CuckooHashTable']
    # h_vals = ['AwesomeHashTable']

    if max_bucket_size is None:
        print "Set max_bucket_size before running"
        assert(max_bucket_size is not None)

    for hi in xrange(len(h_vals)):
        h = h_vals[hi]
        winners = []
        print h
        for load in load_vals:
            for rho in rho_vals:
                print '-----------------------------------------------'
                print 'Params load=%s, rho=%s' % (str(load), str(rho))
                if not os.path.exists('%s.class' % h) and load == load_vals[0] and rho == rho_vals[0]:
                    print "Skipping %s - class does not exist" % h
                    continue

                best_bucket_size = (0, 0)
                for max_bucket_size in bucket_size_vals:
                    params = '%d %f %f %f %d %d %d' % (num_milisseconds,
                                                       load[0],
                                                       load[1],
                                                       rho,
                                                       max_bucket_size,
                                                       w,
                                                       init_size)
                    cmd = 'java ParallelHashPacket %s %d %s' % (params, n_workers, h)
                    pkts_per_ms = run_cmd(cmd)
                    if pkts_per_ms > best_bucket_size[1]:
                        best_bucket_size = (max_bucket_size, pkts_per_ms)
                    print 'max_bucket_size %d\t %f' % (max_bucket_size, pkts_per_ms)
                winners.append(best_bucket_size[0])
        print winners
        print
        print


if __name__ == '__main__':
    run_test()
