import os
import commands
import re
import pickle
import sys

# [num_addresses_log, num_trains_log, mean_train_size, mean_trains_per_comm,
# mean_window, mean_comms_per_address, mean_work, config_fraction, png_fraction,
# accepting_fraction]
num_ms = 2000
parameters = [
    (11, 12, 5,  1,  3, 3,  3822, 0.24, 0.04, 0.96),
    (12, 10, 1,  3,  3, 1,  2644, 0.11, 0.09, 0.92),
    (12, 10, 4,  3,  6, 2,  1304, 0.10, 0.03, 0.90),
    (14, 10, 5,  5,  6, 2,  315,  0.08, 0.05, 0.90),
    (15, 14, 9,  16, 7, 10, 4007, 0.02, 0.10, 0.84),
    (15, 15, 9,  10, 9, 9,  7125, 0.01, 0.20, 0.77),
    (15, 15, 10, 13, 8, 10, 5328, 0.04, 0.18, 0.80),
    (16, 14, 15, 12, 9, 5,  8840, 0.04, 0.19, 0.76)
]
num_threads = [1, 2, 4, 8]
NUM_TRIALS = 3
SERIAL_THRPTS = [142.44243067068575, 186.08678236439695, 317.9357743872752, 837.9293651313956, 119.69607795373032, 78.8442641526119, 104.81228754741342, 64.61940371348805]

def format_params(p):
    return '%d %d %d %d %d %d %d %f %f %f' % (
        p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9]
    )

def format_result(results, p):
    temp = '\\addplot coordinates {\n\t(1, 1S)\n\t(2, 2S)\n\t(4, 4S)\n\t(8, 8S)\n};'
    for n in num_threads:
        temp = temp.replace('%dS' % n, str(results[(p, n)]))
    return temp

def run_cmd(cmd):
    print 'Starting command: %s' % cmd

    outputs = []
    for _ in xrange(NUM_TRIALS):
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

        outputs.append(pkt_per_ms)
        print pkt_per_ms
    print

    outputs.sort()
    return outputs[int(len(outputs) / 2)]

def run_test(range_start, range_end):

    results = { }
    strs = []

    for i in xrange(range_start, range_end):
        p = parameters[i]
        print '-----------------------------------------------'
        print 'Starting for params %s' % str(p)

        # serial_cmd = 'java pset6.SerialFirewallTest %d %s' % (num_ms, format_params(p))
        # serial_pkt_per_ms = run_cmd(serial_cmd)
        # if serial_pkt_per_ms is None:
        #     print 'Couldn\'t parse serial output %s' % str(p)
        #     continue
        # strs.append(serial_pkt_per_ms)
        serial_pkt_per_ms = SERIAL_THRPTS[i]

        for n in num_threads:
            parallel_cmd = 'java pset6.ParallelFirewallTest %d %s %d' % (num_ms, format_params(p), n)
            parallel_pkt_per_ms = run_cmd(parallel_cmd)
            results[(p, n)] = parallel_pkt_per_ms / serial_pkt_per_ms
        strs.append(format_result(results, p))

    for string in strs:
        print string

##############################################

if __name__ == '__main__':
    range_start = int(sys.argv[1])
    range_end = int(sys.argv[2])
    if range_start != 7 or range_end != 8:
        run_test(range_start, range_end)
    else:
        marker = int(sys.argv[3])
        if marker == 0:
            num_threads = [1, 2]
            run_test(range_start, range_end)
        else:
            num_threads = [4, 8]
            run_test(range_start, range_end)
