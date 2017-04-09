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

class TestB {
    public static void main(String[] args) {
        System.out.println("Part B: Lock Scaling");
        System.out.println("Part C: Fairness");

        String[] locks = new String[]{"0", "1", "2", "4", "5"};
        String[] numThreads = new String[]{"1", "2", "8", "32", "64"};

        for (int i = 0; i < locks.length; i++) {
            for (int j = 0; j < numThreads.length; j++) {
                String lock = locks[i];
                String num = numThreads[j];
                String[] args2 = new String[]{"2000", numThreads[j], locks[i]};
                System.out.println("Lock #" + args2[2] + ", " + args2[1] + " Threads");
                ParallelCounter.main(args2);
            }
        }
    }
}