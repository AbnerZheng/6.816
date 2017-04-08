#!/bin/bash

# Lock Types: 0=TAS, 1=Backoff, 2=ReentrantWrapper, 4=CLH, 5=MCS
# Strategy: 0=LockFree, 1=HomeQueue, 2=RandomQueue, 3=LastQueue
source constants.sh

echo "PART B: LOCK SCALING"
echo "PART C: FAIRNESS"
# NOTE: BackoffLock must be optimized
# Run parallel counter on all locks and various numThreads
for LOCK in 0 1 2 4 5
do
    for NUMTHREADS in 1 2 8 32 64
    do
        echo "Lock #$LOCK, $NUMTHREADS Threads"
        repeat "java ParallelCounter $TIME $NUMTHREADS $LOCK"
    done
done
