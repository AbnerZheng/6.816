add 6.816
TESTNUM=0
for RHO in 0.5 0.75 0.9
do
    cqsub python tests/batch_test.py b $TESTNUM 0.09 0.01 $RHO
    ((TESTNUM+=1))
done

for RHO in 0.5 0.75 0.9
do
    cqsub python tests/batch_test.py b $TESTNUM 0.45 0.05 $RHO
    ((TESTNUM+=1))
done
