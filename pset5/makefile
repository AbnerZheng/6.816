JFLAGS= 
JC= javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	Node.java \
	WaitFreeQueue.java \
	PaddedPrimitive.java \
	StopWatch.java \
	Fingerprint.java \
	Statistics.java \
	RandomGenerator.java \
	BucketList.java \
	HashTable.java \
	AppSpecificHashTable.java \
	AwesomeHashTable.java \
	CuckooHashTable.java \
	LinearProbeHashTable.java \
	LockFreeHashTable.java \
	LockingHashTable.java \
	HashPacketGenerator.java \
	HashPacketWorker.java \
	HashPacketTest.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
