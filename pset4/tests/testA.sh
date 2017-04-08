#!/bin/bash

# Lock Types: 0=TAS, 1=Backoff, 2=ReentrantWrapper, 4=CLH, 5=MCS
source constants.sh

echo "PART A: IDLE LOCK OVERHEAD"
# Find the throughput (increments/ms) of a serial counter
# java SerialCounter [numMilliseconds]
repeat "java SerialCounter $TIME"

# Compare to the throughputs of single-threaded counters with locks
# java ParallelCounter [numMilliseconds] [numThreads] [lockType]
for LOCK in 0 1 2 4 5
do
    repeat "java ParallelCounter $TIME 1 $LOCK"
done
