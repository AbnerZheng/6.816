add 6.816
for RHO in 0.5 0.75 0.9
do
    cqsub python tests/batch_test.py b 0.09 0.01 $RHO
done

for RHO in 0.5 0.75 0.9
do
    cqsub python tests/batch_test.py b 0.45 0.05 $RHO
done
