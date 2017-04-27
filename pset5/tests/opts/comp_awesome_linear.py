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

    n_workers_vals = [1, 2, 4, 8]
    max_bucket_size = 4

    load_vals = [(0.09, 0.01), (0.45, 0.05)]
    rho_vals = [0.5, 0.75, 0.9]

    if max_bucket_size is None:
        print "Set max_bucket_size before running"
        assert(max_bucket_size is not None)

    for load in load_vals:
        for rho in rho_vals:
            print '-----------------------------------------------'
            print 'Params load=%s, rho=%s' % (str(load), str(rho))
            for n_workers in n_workers_vals:
                params = '%d %f %f %f %d %d %d' % (num_milisseconds,
                                                   load[0],
                                                   load[1],
                                                   rho,
                                                   max_bucket_size,
                                                   w,
                                                   init_size)
                linear_cmd = 'java ParallelHashPacket %s %d %s' % (params, n_workers, 'LinearProbeHashTable')
                awesome_cmd = 'java ParallelHashPacket %s %d %s' % (params, n_workers, 'AwesomeHashTable')
                linear_pkts_per_ms = run_cmd(linear_cmd)
                awesome_pkts_per_ms = run_cmd(awesome_cmd)
                print '%d Workers\t %f' % (n_workers, awesome_pkts_per_ms / linear_pkts_per_ms)
            print
        print


if __name__ == '__main__':
    run_test()
