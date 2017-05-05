package pset6;

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
        System.out.println("-----------------------------------------");
        System.out.println("SERIAL FIREWALL TEST");
        System.out.println("Total Time: " + time);
        System.out.println("Packets: " + totalPackets);
        System.out.println("Throughput: " + (double) totalPackets / time);
        System.out.println("Histogram:");
        workerData.printHistogram();
        System.out.println("-----------------------------------------");
    }
}

class ParallelFirewallTest {
    public static void main(String[] args) {

    }
}