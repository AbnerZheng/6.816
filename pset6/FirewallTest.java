package pset6;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.atomic.AtomicInteger;

class SerialFirewallTest {
    public static void main(String[] args) {
        // Validate arguments
        if (args.length != 11) {
            System.out.println("ERROR: Expected 11 arguments, got " + args.length + ".");
            System.out.println("java SerialFirewallTest [numMilliseconds] [numAddressesLog] [numTrainsLog] " +
                    "[meanTrainSize] [meanTrainsPerComm] [meanWindow] [meanCommsPerAddress] [meanWork] " +
                    "[configFraction] [pngFraction] [acceptingFraction]");
            return;
        }

        // Parse arguments
        final int numMilliseconds = Integer.parseInt(args[0]);
        final int numAddressesLog = Integer.parseInt(args[1]);
        final int numTrainsLog = Integer.parseInt(args[2]);
        final double meanTrainSize = Float.parseFloat(args[3]);
        final double meanTrainsPerComm = Float.parseFloat(args[4]);
        final int meanWindow = Integer.parseInt(args[5]);
        final int meanCommsPerAddress = Integer.parseInt(args[6]);
        final int meanWork = Integer.parseInt(args[7]);
        final double configFrac = Float.parseFloat(args[8]);
        final double pngFrac = Float.parseFloat(args[9]);
        final double acceptingFrac = Float.parseFloat(args[10]);

        // Initialize values
        StopWatch timer = new StopWatch();
        PacketGenerator packetGenerator = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize,
                meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFrac, pngFrac, acceptingFrac);
        PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
        PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);

        // Create the worker and make sure the worker's permission tables are in a steady state
        SerialWorker workerData = new SerialWorker(done, packetGenerator, numAddressesLog);
        workerData.initConfig();
        Thread workerThread = new Thread(workerData);

        // Start the experiment
        workerThread.start();
        timer.startTimer();

        try {
            Thread.sleep(numMilliseconds);
        } catch (InterruptedException ignore) {;}

        // Stop the experiment and enforce a memory barrier
        done.value = true;
        memFence.value = true;
        try {
            workerThread.join();
        } catch (InterruptedException ignore) {;}
        timer.stopTimer();

        // Print statistics
        final double time = timer.getElapsedTime();
        final long totalPackets = workerData.totalPackets;
//        System.out.println("-----------------------------------------");
//        System.out.println("SERIAL FIREWALL TEST");
//        System.out.println("Total Time: " + time);
//        System.out.println("Packets: " + totalPackets);
        System.out.println("PKT_PER_MS " + (double) totalPackets / time + " PKT_PER_MS");
//        System.out.println("Histogram:");
//        workerData.printHistogram();
//        System.out.println("-----------------------------------------");
    }
}

class ParallelFirewallTest {

    public static final int MAX_PKTS_IN_FLIGHT = 256;

    public static void main(String[] args) {
        // Validate arguments
        if (args.length != 12) {
            System.out.println("ERROR: Expected 12 arguments, got " + args.length + ".");
            System.out.println("java SerialFirewallTest [numMilliseconds] [numAddressesLog] [numTrainsLog] " +
                    "[meanTrainSize] [meanTrainsPerComm] [meanWindow] [meanCommsPerAddress] [meanWork] " +
                    "[configFraction] [pngFraction] [acceptingFraction] [numWorkers]");
            return;
        }

        // Parse arguments
        final int numMilliseconds = Integer.parseInt(args[0]);
        final int numAddressesLog = Integer.parseInt(args[1]);
        final int numTrainsLog = Integer.parseInt(args[2]);
        final double meanTrainSize = Float.parseFloat(args[3]);
        final double meanTrainsPerComm = Float.parseFloat(args[4]);
        final int meanWindow = Integer.parseInt(args[5]);
        final int meanCommsPerAddress = Integer.parseInt(args[6]);
        final int meanWork = Integer.parseInt(args[7]);
        final double configFrac = Float.parseFloat(args[8]);
        final double pngFrac = Float.parseFloat(args[9]);
        final double acceptingFrac = Float.parseFloat(args[10]);
        final int numWorkers = Integer.parseInt(args[11]);
        final int queueDepth = MAX_PKTS_IN_FLIGHT / numWorkers;

        // Initialize values
        StopWatch timer = new StopWatch();
        PacketGenerator packetGenerator = new PacketGenerator(numAddressesLog, numTrainsLog, meanTrainSize,
                meanTrainsPerComm, meanWindow, meanCommsPerAddress, meanWork, configFrac, pngFrac, acceptingFrac);
        PaddedPrimitiveNonVolatile<Boolean> done = new PaddedPrimitiveNonVolatile<Boolean>(false);
        PaddedPrimitiveNonVolatile<AtomicInteger> initDone = new PaddedPrimitiveNonVolatile<AtomicInteger>(new AtomicInteger(numWorkers));
        PaddedPrimitive<Boolean> memFence = new PaddedPrimitive<Boolean>(false);

        // Dispatcher-worker communication initialization
        List<WaitFreeQueue<Packet>> queues = new ArrayList<>();
        List<ReentrantLock> locks = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            queues.add(new WaitFreeQueue<Packet>(queueDepth));
            locks.add(new ReentrantLock(false));
        }

        // Packet processing objects
        PSource png = new PSource(numAddressesLog);
        PDestination r = new PDestination(numAddressesLog);
        Histogram histogram = new Histogram();

        // Allocate and initialize Dispatcher and Worker threads
        Dispatcher dispatchData = new Dispatcher(done, queues, packetGenerator, numWorkers);
        Thread dispatchThread = new Thread(dispatchData);
        List<ParallelWorker> workers = new ArrayList<>();
        List<Thread> workerThreads = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            ParallelWorker workerData = new ParallelWorker(i, numWorkers, numAddressesLog, packetGenerator, done, initDone, queues, locks, png, r, histogram);
            Thread workerThread = new Thread(workerData);
            workers.add(workerData);
            workerThreads.add(workerThread);
        }


        // Make sure the permission tables are in a steady state
        System.out.printf("Initializing permissions table");
        for (Thread workerThread : workerThreads)
            workerThread.start();
        while (initDone.value.get() != 0) {;}
        System.out.println("DONE");

        // Start the experiment
        timer.startTimer();
        dispatchThread.start();

        try {
            Thread.sleep(numMilliseconds);
        } catch (InterruptedException ignore) {;}

        // Stop the experiment and enforce a memory barrier
        done.value = true;
        memFence.value = true;
        try {
            dispatchThread.join();
            for (Thread workerThread : workerThreads)
                workerThread.join();
        } catch (InterruptedException ignore) {
            ;
        }

        timer.stopTimer();

        // Print statistics
        final double time = timer.getElapsedTime();
        final long totalPackets = dispatchData.totalPackets;
        System.out.println("-----------------------------------------");
//        System.out.println("PARALLEL FIREWALL TEST");
//        System.out.println("Total Time: " + time);


        final long exp = (long)(totalPackets * configFrac + totalPackets * (1 - configFrac) * (1 - pngFrac) * acceptingFrac);
        final double acc = 100.0 * (1.0 - (float) Math.abs(exp - histogram.getTotalPackets()) / exp);
        final String accStr = String.format("%.2f", acc);
        System.out.println("Expected " + exp + " / " + totalPackets + " packets, " + accStr + "% accuracy");
        System.out.println("PKT_PER_MS " + (double) totalPackets / time + " PKT_PER_MS");
        System.out.println(png);
        System.out.println(r);
        System.out.println("Total packets processed: " + histogram.getTotalPackets());
        System.out.println("-----------------------------------------");
    }
}