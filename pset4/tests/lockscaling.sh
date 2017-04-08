#!/bin/bash
javac -d . ../*.java

# Parameters
TIME=2000
NUMTHREADS=4

# Optimize minDelay and maxDelay for BackoffLock
#
# Try minDelay from 10^1 to 10^8 and maxDelay from 10^1 to 10^8
# Total 36 trials - identify the parameters with the maximum throughput
#
# java ParallelCounter [numMilliseconds] [numThreads] [lockType]
java LockScalingTest $TIME $NUMTHREADS

rm *.class
