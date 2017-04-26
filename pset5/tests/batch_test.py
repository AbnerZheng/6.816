###########################################################################

# This script is to help you batch run tests and generate latex figures

# It will call your Java code as follows:
#   java ParallelHashPacket <NUM_MILISECONDS> <P_+> <P_-> <HIT_RATIO> <MAX_BUCKET_SIZE> <W> <INIT_SIZE> <N_WORKERS> <HASH_NAME>
#   java SerialHashPacket   <NUM_MILISECONDS> <P_+> <P_-> <HIT_RATIO> <MAX_BUCKET_SIZE> <W> <INIT_SIZE>

# HASH_NAME should be one of the strings in h_vals below

# somewhere in the output of your java program there should be a line
#           PKT_PER_MS <number> PKT_PER_MS

# this script will find the pattern, extract the <number> and dump the data
# in a .tex file as a figure and in a python pickle

# if you want to see what this script generates with dummy data,
# set TEX_TEST = True and run it
#       python batch_test.py

# ------
# Feel free to modify this script as you like. If you think you have significantly
# imporved this script, feel free to share it with others.

# You will need to change parts of this code if you do not use the API above.

# Do not ask too much from this script, it's 3 hours of work.
# While I will fix bugs here if you tell me about them, I do not intend to
# add more features (error handling in particular). I do not guarantee that
# this script will work if your program crashes. If you do not follow the API above,
# do not email me asking how to change the script appropriately.

# Good luck!

###########################################################################

import os
import commands
import re
import pickle

TEX_TEST = False

def run_cmd(cmd):
    print 'Starting command: %s' % cmd

    if TEX_TEST:
        status = 0
        output = 'PKT_PER_MS ' + str(hash(cmd) % 100) + ' PKT_PER_MS'
        print 'Output from: %s\n%s' % (cmd, output)
        return hash(cmd) % 100
    
    print 'Output from: %s' % cmd

    outputs = []
    for _ in xrange(5):
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

def run_test(T):
    # T = one of 'a', 'b', 'c'
    # this script will generate
    #   test_${T}_results.pypickle - contains the results in a python pickled
    #   test_${T}_results.tex      - automatically generated plot data to include in the report

    ##############################
    # feel free to change the parameters

    # if you change the len(h_vals) or len(n_workers_vals),
    # the generated .tex file will be corrupted. See template_graph_b.tex

    # you should set these variables before running
    max_bucket_size = 4

    if T == 'a':
        print '-----------------------------------------------'
        test_a_cmd = ('java ParallelHashPacket 2000 0 0 0 0 1 0 8 -1')
        run_cmd(test_a_cmd)
        return
    elif T == 'b':
        num_milisseconds = 2000
        w = 4000
        init_size = 0

        n_workers_vals = [1, 2, 4, 8]

        load_vals = [ (0.09, 0.01) , (0.45, 0.05) ]
        rho_vals = [ 0.5, 0.75, 0.9 ]

        h_vals = [ 'LockingHashTable',
                   'LockFreeHashTable',
                   'LinearProbeHashTable']
                   'CuckooHashTable',
                   'AwesomeHashTable' ]

    elif T == 'c':
        num_milisseconds = 2000
        w = 4000
        init_size = 1000000
    
        n_workers_vals = [1, 2, 4, 8, 16, 32]
    
        load_vals = [ (0.2, 0.2) ]
        rho_vals = [ 0.5 ]
    
        h_vals = [ 'LockingHashTable',
                   'LockFreeHashTable',
                   'LinearProbeHashTable',
                   'CuckooHashTable',
                   'AwesomeHashTable',
                   'AppSpecificHashTable']
    else:
        print 'No such test: %s' % (str(T))
        return
  
    #########################################################
    # You should not need to modify anything below this in this function
    # unless you use a different API of your Java program.
    # Feel free to modify it though if needed.

    if TEX_TEST:
        max_bucket_size = 5

    if max_bucket_size is None:
        print "Set max_bucket_size before running"
        assert(max_bucket_size is not None)

    results = { }

    for load in load_vals:
        for rho in rho_vals:
            print '-----------------------------------------------'
            print 'Starting for params load=%s, rho=%s' % (str(load), str(rho))

            params = '%d %f %f %f %d %d %d' % (
                                       num_milisseconds,
                                       load[0],
                                       load[1],
                                       rho,
                                       max_bucket_size,
                                       w,
                                       init_size)
            serial_cmd = ('java SerialHashPacket %s' % params)

            serial_pkt_per_ms = run_cmd(serial_cmd)
            if serial_pkt_per_ms is None:
                print 'Couldn\'t parse serial output for params load=%s, rho=%s' % (str(load), str(rho))
                print 
                continue

            for hi in xrange(len(h_vals)):
                h = h_vals[hi]
                if not os.path.exists('%s.class' % h) and not TEX_TEST and load == load_vals[0] and rho == rho_vals[0]:
                    print "Skipping %s - class does not exist" % h
                    continue

                for n_workers in n_workers_vals:

                    cmd = 'java ParallelHashPacket %s %d %s' % (params, n_workers, h)

                    pkt_per_ms = run_cmd(cmd) 

                    results[ ( load, rho, n_workers, hi) ] = pkt_per_ms / serial_pkt_per_ms

    pickle.dump(results, open('test_%s_results.pypickle' % T, 'w'))

    # printing

    template_path = 'template_graph_%s.tex' % T
    with open(template_path, 'r') as f:
        fig_template = f.read()

    all_figs = ""

    for load in load_vals:
        for rho in rho_vals:
            config_tex = fig_template
            config_tex = config_tex.replace('P_ADD', str(load[0]))
            config_tex = config_tex.replace('P_REM', str(load[1]))
            config_tex = config_tex.replace('RHO', str(rho))

            for hi in xrange(len(h_vals)):
                for n_workers in n_workers_vals:
                    config_tex = config_tex.replace('HT%d_NAME' % hi, h_vals[hi])

                    config_tex = config_tex.replace('HT%d_W%d_N' % (hi, n_workers), str(n_workers))
                    config_tex = config_tex.replace('HT%d_W%d_S' % (hi, n_workers),
                                       str(results[ (load, rho, n_workers, hi) ]))

            all_figs += '%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%\n'
            all_figs += '%%% P_+ = {0} , P_- = {1}, rho = {2} \n'.format(load[0], load[1], rho)
            all_figs += config_tex
            all_figs += '\n\n'


    with open('test_%s_results.tex' % T, 'w') as f:
        f.write(all_figs)

##############################################

if __name__ == '__main__':
    # run_test('a')
    run_test('b')
    # run_test('c')

