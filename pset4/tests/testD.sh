#!/bin/bash
javac -d . ../*.java

# Lock Types: 0=TAS, 1=Backoff, 2=ReentrantWrapper, 4=CLH, 5=MCS
# Strategy: 0=LockFree, 1=HomeQueue, 2=RandomQueue, 3=LastQueue
TIME=2000
N=1
UNIFORM=1
QDEPTH=8
TRIALS=5

echo "PART D: IDLE LOCK OVERHEAD"
# java ParallelPacket [numMilliseconds] [numSources] [mean] [uniformFlag]
#   [experimentNumber] [queueDepth] [lockType] [strategy]
for W in 25 100 400 800
do
    echo "MeanWork $W, LockFree"
    for (( i=1; i<=$TRIALS; i++ ))
    do
        java ParallelPacket $TIME $N $W $UNIFORM $i $QDEPTH 0 0
    done
    echo

    for LOCK in 0 1 2 4 5
    do
        echo "MeanWork $W, Lock #$LOCK, HomeQueue"
        for (( i=1; i<=$TRIALS; i++ ))
        do
            java ParallelPacket $TIME $N $W $UNIFORM $i $QDEPTH $LOCK 1
        done
        echo
    done
done

rm *.class
