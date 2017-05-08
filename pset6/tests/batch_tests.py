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
num_threads = [2, 4, 8]
NUM_TRIALS = 1

def format_params(p):
    return '%d %d %d %d %d %d %d %f %f %f' % (
        p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9]
    )

def format_result(results, p):
    temp = '\\addplot coordinates {\n\t(2, 2S)\n\t(4, 4S)\n\t(8, 8S)\n};'
    for n in num_threads:
        temp = temp.replace('%dS' % n, str(results[(p, n)]))
    print temp

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

def run_test(id_num):

    results = { }

    for p in parameters:
        print '-----------------------------------------------'
        print 'Starting for params %s' % str(p)

        serial_cmd = 'java pset6.SerialFirewallTest %d %s' % (num_ms, format_params(p))
        serial_pkt_per_ms = run_cmd(serial_cmd)
        if serial_pkt_per_ms is None:
            print 'Couldn\'t parse serial output %s' % str(p)
            continue
        
        # for n in num_threads:
        #     parallel_cmd = 'java pset6.ParallelFirewallTest %d %s %d' % (num_ms, format_params(p), n)
        #     parallel_pkt_per_ms = run_cmd(parallel_cmd)
        #     results[(p, n)] = parallel_pkt_per_ms / serial_pkt_per_ms

        # format_result(results, p)
        print "THROUGHPUT %f" % serial_pkt_per_ms

    # template_path = 'template.tex'
    # with open(template_path, 'r') as f:
    #     fig_template = f.read()
    #
    # all_figs = ""
    #
    # for load in load_vals:
    #     for rho in rho_vals:
    #         config_tex = fig_template
    #         config_tex = config_tex.replace('P_ADD', str(load[0]))
    #         config_tex = config_tex.replace('P_REM', str(load[1]))
    #         config_tex = config_tex.replace('RHO', str(rho))
    #
    #         for hi in xrange(len(h_vals)):
    #             for n_workers in n_workers_vals:
    #                 config_tex = config_tex.replace('HT%d_NAME' % hi, h_vals[hi])
    #
    #                 config_tex = config_tex.replace('HT%d_W%d_N' % (hi, n_workers), str(n_workers))
    #                 config_tex = config_tex.replace('HT%d_W%d_S' % (hi, n_workers),
    #                                    str(results[ (load, rho, n_workers, hi) ]))
    #
    #         all_figs += '%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n'
    #         all_figs += '%%% P_+ = {0} , P_- = {1}, rho = {2} \n'.format(load[0], load[1], rho)
    #         all_figs += config_tex
    #         all_figs += '\n\n'
    #
    #
    # with open('results_0.tex' % (T, id_num), 'w') as f:
    #     f.write(all_figs)

##############################################

if __name__ == '__main__':
    id_num = sys.argv[1]
    run_test(id_num)
