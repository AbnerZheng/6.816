#!/bin/bash

# Lock Types: 0=TAS, 1=Backoff, 2=ReentrantWrapper, 4=CLH, 5=MCS
# Strategy: 0=LockFree, 1=HomeQueue, 2=RandomQueue, 3=LastQueue
source constants.sh
N=1
UNIFORM=1

echo "PART D: IDLE LOCK OVERHEAD"
# java ParallelPacket [numMilliseconds] [numSources] [mean] [uniformFlag]
#   [experimentNumber] [queueDepth] [lockType] [strategy]
for W in 25 100 400 800
do
    echo "MeanWork $W, LockFree"
    repeat "java ParallelPacket $TIME $N $W $UNIFORM $i $QDEPTH 0 0"

    for LOCK in 0 1 2 4 5
    do
        echo "MeanWork $W, Lock #$LOCK, HomeQueue"
        repeat "java ParallelPacket $TIME $N $W $UNIFORM $i $QDEPTH $LOCK 1"
    done
done
