# echo -n "numMilliseconds="
# read numMs
echo -n "fractionAdd="
read fAdd
echo -n "fractionRemove="
read fRemove
echo -n "hitRate="
read hitRate
echo -n "maxBucketSize="
read maxBucketSize
echo -n "mean="
read mean
echo -n "initSize="
read initSize
echo -n "numWorkers="
read numWorkers
echo "-1=None, 0=Locking, 1=LockFree, 2=LinearProbe, 3=Cuckoo, 4=Awesome, 5=AppSpecific"
echo -n "tableType="
read tableType

java ParallelHashPacket 2000 $fAdd $fRemove $hitRate $maxBucketSize $mean $initSize $numWorkers $tableType
