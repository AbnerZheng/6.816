#!/bin/bash
javac -d . ../*.java

# Lock Types: 0=TAS, 1=Backoff, 2=ReentrantWrapper, 4=CLH, 5=MCS
# Strategy: 0=LockFree, 1=HomeQueue, 2=RandomQueue, 3=LastQueue
TIME=2000
QDEPTH=8
TRIALS=5
UNIFORM=0


echo "PART F: SPEEDUP WITH EXPONENTIAL LOAD"
# java SerialPacket [numMilliseconds] [numSources] [mean] [uniformFlag] [experimentNumber]
# java ParallelPacket [numMilliseconds] [numSources] [mean] [uniformFlag]
#   [experimentNumber] [queueDepth] [lockType] [strategy]
for MEAN in 1000 2000 4000 8000
do
    for NUMTHREADS in 1 2 8
    do
        echo "MeanWork $MEAN, NumThreads $NUMTHREADS, Serial"
        for (( i=1; i<=$TRIALS; i++ ))
        do
            java SerialPacket $TIME $NUMTHREADS $MEAN $UNIFORM $i
        done
        echo

        echo "MeanWork $MEAN, NumThreads $NUMTHREADS, LockFree"
        for (( i=1; i<=$TRIALS; i++ ))
        do
            java ParallelPacket $TIME $NUMTHREADS $MEAN $UNIFORM $i $QDEPTH 0 0
        done
        echo

        for LOCK in 0 1 2
        do
            echo "MeanWork $MEAN, NumThreads $NUMTHREADS, Lock #$LOCK, RandomQueue"
            for (( i=1; i<=$TRIALS; i++ ))
            do
                java ParallelPacket $TIME $NUMTHREADS $MEAN $UNIFORM $i $QDEPTH $LOCK 2
            done
            echo
        done

        for LOCK in 0 1 2
        do
            echo "MeanWork $MEAN, NumThreads $NUMTHREADS, Lock #$LOCK, LastQueue"
            for (( i=1; i<=$TRIALS; i++ ))
            do
                java ParallelPacket $TIME $NUMTHREADS $MEAN $UNIFORM $i $QDEPTH $LOCK 3
            done
            echo
        done
    done
done

rm *.class
