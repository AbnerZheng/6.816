import commands
import re
import sys

# [num_addresses_log, num_trains_log, mean_train_size, mean_trains_per_comm,
# mean_window, mean_comms_per_address, mean_work, config_fraction, png_fraction,
# accepting_fraction]
num_ms = 2000
parameters = (11, 12, 5, 1, 3, 3, 3822, 0.24, 0.04, 0.96)
num_threads = [1, 2, 4, 8]
lock_types = ["TAS", "Backoff", "ReentrantWrapper", "CLH", "MCS"]
queue_strategies = ["LockFree", "RandomQueue", "LastQueue"]
NUM_TRIALS = 1

def format_params(p):
    return '%d %d %d %d %d %d %d %f %f %f' % (
        p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9]
    )

final_results = []
def format_result(strategy, results):
    temp = '\\addplot coordinates {\n\t(1, 1S)\n\t(2, 2S)\n\t(4, 4S)\n\t(8, 8S)\n};'
    for n in num_threads:
        temp = temp.replace('%dS' % n, str(results[n]))
    final_results.append((strategy, temp))

def strategy_to_string(lock, queue):
    lock_type = lock_types[lock]
    queue_strategy = queue_strategies[queue]
    return "<%s, %s>" % (lock_type, queue_strategy)

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

def run_test():

    results = { }
    print '-----------------------------------------------'
    print 'Starting for params %s' % str(parameters)

    for lock in xrange(len(lock_types)):
        for queue in xrange(len(queue_strategies)):
            strategy = strategy_to_string(lock, queue)
            print strategy
            for n in num_threads:
                parallel_cmd = 'java pset6.ParallelFirewallTest %d %s %d %s %s' % (
                    num_ms, format_params(parameters), n, lock, queue)
                parallel_pkt_per_ms = run_cmd(parallel_cmd)
                results[n] = parallel_pkt_per_ms
            format_result(strategy, results)

    for result in final_results:
        print result[0]
    for result in final_results:
        print result[1]

##############################################

if __name__ == '__main__':
    run_test()
