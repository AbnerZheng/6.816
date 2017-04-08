#!/bin/bash
add 6.816
javac -d . ../*.java

# Lock Types: 0=TAS, 1=Backoff, 2=ReentrantWrapper, 4=CLH, 5=MCS
TIME=2000
TRIALS=5

echo "PART A: IDLE LOCK OVERHEAD"
# Find the throughput (increments/ms) of a serial counter
# java SerialCounter [numMilliseconds]
for (( i=1; i<=$TRIALS; i++ ))
do
    java SerialCounter $TIME
done
echo

# Compare to the throughputs of single-threaded counters with locks
# java ParallelCounter [numMilliseconds] [numThreads] [lockType]
for LOCK in 0 1 2 4 5
do
    for (( i=1; i<=$TRIALS; i++ ))
    do
        java ParallelCounter $TIME 1 $LOCK
    done
    echo
done

rm *.class
