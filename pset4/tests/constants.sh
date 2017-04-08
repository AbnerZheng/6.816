#!/bin/bash
javac -d . ../*.java

TIME=2000
TRIALS=1
QDEPTH=8

repeat () {
    tmp=$1"; "
    cmd=$tmp
    for (( i=1; i<$TRIALS; i++ ))
    do
        cmd=$cmd$tmp
    done
    eval $cmd
    echo
}
