JFLAGS= 
JC= javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
	PaddedPrimitive.java \
	StopWatch.java \
	Fingerprint.java \
	Statistics.java \
	RandomGenerator.java \
	BucketList.java \
	HashTable.java \
	HashPacketGenerator.java \
	HashPacketWorker.java \
	HashPacketTest.java \

default: classes

classes: $(CLASSES:.java=.class)

clean:
	$(RM) *.class
