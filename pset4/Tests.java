class TestA {
    public static void main(String[] args) {
        System.out.println("Part A: Idle Lock Overhead");
//        String[] args2 = new String[]{"2000"};
//        SerialCounter.main(args2);
//        args2 = new String[]{"2000", "1", "0"};
//        ParallelCounter.main(args2);
//        args2 = new String[]{"2000", "1", "1"};
//        ParallelCounter.main(args2);
        String[] args2 = new String[]{"2000", "1", "2"};
        ParallelCounter.main(args2);
        args2 = new String[]{"2000", "1", "4"};
        ParallelCounter.main(args2);
        args2 = new String[]{"2000", "1", "5"};
        ParallelCounter.main(args2);
    }
}

class TestBC {
    public static void main(String[] args) {
        System.out.println("Part B: Lock Scaling");
        System.out.println("Part C: Fairness");

        String[] locks = new String[]{"0", "1", "2", "4", "5"};
        String[] numThreads = new String[]{"1", "2", "8", "32", "64"};

        for (int i = 0; i < locks.length; i++) {
            for (int j = 0; j < numThreads.length; j++) {
                String[] args2 = new String[]{"2000", numThreads[j], locks[i]};
                System.out.println("Lock #" + args2[2] + ", " + args2[1] + " Threads");
                ParallelCounter.main(args2);
            }
        }
    }
}

class TestD {
    public static void main(String[] args) {
        System.out.println("Part D: IDLE LOCK OVERHEAD");

        String[] means = new String[]{"25", "100", "400", "800"};
        String[] locks = new String[]{"0", "1", "2", "4", "5"};
        String[] args2;

        for (int i = 0; i < means.length; i++) {
            args2 = new String[]{"2000", "1", means[i], "1", "1", "8", "0", "0"};
            System.out.println("MeanWork " + means[i] + ", LockFree");
            ParallelPacket.main(args2);
            for (int j = 0; j < locks.length; j++) {
                args2 = new String[]{"2000", "1", means[i], "1", "1", "8", locks[j], "0"};
                System.out.println("MeanWork " + means[i] + ", Lock #" + locks[j] + ", HomeQueue");
                ParallelPacket.main(args2);
            }
        }
    }
}

class TestE {
    public static void main(String[] args) {
        System.out.println("Part E: SPEEDUP WITH UNIFORM LOAD");

        String[] means = new String[]{"1000", "2000", "4000", "8000"};
        String[] numThreads = new String[]{"1", "2", "8"};
        String[] locks = new String[]{"0", "1", "2"};
        String[] args2;

        for (int i = 0; i < means.length; i++) {
            for (int j = 0; j < numThreads.length; j++) {
                args2 = new String[]{"2000", numThreads[j], means[i], "1", "1"};
                System.out.println("MeanWork " + args2[2] + ", NumThreads " + args2[1] + ", Serial");
                SerialPacket.main(args2);

                args2 = new String[]{"2000", numThreads[j], means[i], "1", "1", "8", "0", "0"};
                System.out.println("MeanWork " + args2[2] + ", NumThreads " + args2[1] + ", LockFree");
                ParallelPacket.main(args2);

                for (int k = 0; k < locks.length; k++) {
                    args2 = new String[]{"2000", numThreads[j], means[i], "1", "1", "8", locks[k], "2"};
                    System.out.println("MeanWork " + args2[2] + ", NumThreads " + args2[1] + ", Lock #" + args2[6] + ", RandomQueue");
                    ParallelPacket.main(args2);
                }

                for (int k = 0; k < locks.length; k++) {
                    args2 = new String[]{"2000", numThreads[j], means[i], "1", "1", "8", locks[k], "3"};
                    System.out.println("MeanWork " + args2[2] + ", NumThreads " + args2[1] + ", Lock #" + args2[6] + ", LastQueue");
                    ParallelPacket.main(args2);
                }
            }
        }
    }
}

class TestF {
    public static void main(String[] args) {
        System.out.println("Part F: SPEEDUP WITH EXPONENTIAL LOAD");

        String[] means = new String[]{"1000", "2000", "4000", "8000"};
        String[] numThreads = new String[]{"1", "2", "8"};
        String[] locks = new String[]{"0", "1", "2"};
        String[] args2;

        for (int i = 0; i < means.length; i++) {
            for (int j = 0; j < numThreads.length; j++) {
                args2 = new String[]{"2000", numThreads[j], means[i], "0", "1"};
                System.out.println("MeanWork " + args2[2] + ", NumThreads " + args2[1] + ", Serial");
                SerialPacket.main(args2);

                args2 = new String[]{"2000", numThreads[j], means[i], "0", "1", "8", "0", "0"};
                System.out.println("MeanWork " + args2[2] + ", NumThreads " + args2[1] + ", LockFree");
                ParallelPacket.main(args2);

                for (int k = 0; k < locks.length; k++) {
                    args2 = new String[]{"2000", numThreads[j], means[i], "0", "1", "8", locks[k], "2"};
                    System.out.println("MeanWork " + args2[2] + ", NumThreads " + args2[1] + ", Lock #" + args2[6] + ", RandomQueue");
                    ParallelPacket.main(args2);
                }

                for (int k = 0; k < locks.length; k++) {
                    args2 = new String[]{"2000", numThreads[j], means[i], "0", "1", "8", locks[k], "3"};
                    System.out.println("MeanWork " + args2[2] + ", NumThreads " + args2[1] + ", Lock #" + args2[6] + ", LastQueue");
                    ParallelPacket.main(args2);
                }
            }
        }
    }
}