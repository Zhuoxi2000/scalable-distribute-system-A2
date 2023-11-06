package Client;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.sleep;

public class albumClt {
    private static final int INITIAL_THREAD_COUNT = 10;
    private static final int INITIAL_CALLS_PER_THREAD = 100;
    protected static final AtomicInteger SUCCESSFUL_REQ = new AtomicInteger(0);
    protected static final AtomicInteger FAILED_REQ = new AtomicInteger(0);
    protected static List<Long> latenciesPost = Collections.synchronizedList(new ArrayList<>());
    protected static List<Long> latenciesGet = Collections.synchronizedList(new ArrayList<>());
    protected static CountDownLatch totalThreadsLatch;

    public static void main(String[] args) throws InterruptedException {
        long start, end;
        String currentPhase = "Loading Java Server Phase - App Load Balancer - 3 Servlets (t2.medium), 1 DB (db.t3.medium, 225 Connections)";

        // Define starting constants
        int threadGroupSize = Integer.parseInt(args[0]);
        int numThreadGroups = Integer.parseInt(args[1]);
        long delay = Long.parseLong(args[2]);
        String serverURL = args[3];

        // Thread calls and calculations
        int callsPerThread = 1000;
        int maxThreads = threadGroupSize * numThreadGroups;

        // Executor service used for thread pooling and countdown latch to track when loading is complete
        ExecutorService servicePool = Executors.newFixedThreadPool(maxThreads);
        totalThreadsLatch = new CountDownLatch(INITIAL_THREAD_COUNT);

        // Run initialization phase
        initializationPhase(servicePool, serverURL);

        // Redefine countdown latch for server loading phase
        totalThreadsLatch = new CountDownLatch(maxThreads);

        // Load Server
        start = System.currentTimeMillis();
        loadServerPhase(numThreadGroups, threadGroupSize, delay, serverURL, callsPerThread, servicePool);
        end = System.currentTimeMillis();

        printResults(numThreadGroups, threadGroupSize, callsPerThread, currentPhase, start, end);
    }

    private static void loadServerPhase(int numThreadGroups, int threadGroupSize, long delay, String serverURL, int callsPerThread, ExecutorService servicePool) throws InterruptedException {
        for (int i = 0; i < numThreadGroups; i++) {
            for (int j = 0; j < threadGroupSize; j++) {
                servicePool.execute(new albumRun(callsPerThread, serverURL, false));
            }
            sleep(delay * 1000L);
        }
        totalThreadsLatch.await();
        servicePool.shutdown();
    }

    private static void initializationPhase(ExecutorService servicePool, String serverURL) throws InterruptedException {
        for (int i = 0; i < INITIAL_THREAD_COUNT; i++) {
            servicePool.execute(new albumRun(INITIAL_CALLS_PER_THREAD, serverURL, true));
        }
        totalThreadsLatch.await();
    }

    protected static void printResults(int numThreadGroups, int threadGroupSize, int callsPerThread, String currentPhase, long start, long end) {
        Cal PostCals = new Cal(latenciesPost);
        Cal GetCals = new Cal(latenciesGet);

        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        double wallTime = (end - start) * 0.001;

        System.out.println("*************************************************************************************");
        System.out.println("***" + currentPhase + "***");
        System.out.println("GroupNum" + numThreadGroups + " Thread Num" + threadGroupSize + " Calls" + callsPerThread);
        System.out.println("Successful Num" + SUCCESSFUL_REQ.get());
        System.out.println("Failed Num" + FAILED_REQ.get());
        System.out.println("Throughput " + decimalFormat.format(SUCCESSFUL_REQ.get() / wallTime));
        System.out.println("Wall Time " + decimalFormat.format(wallTime));
        printCalculations(decimalFormat, PostCals);
        System.out.println("POST");
        printCalculations(decimalFormat, GetCals);
        System.out.println("GET");
    }

    private static void printCalculations(DecimalFormat decimalFormat, Cal cals) {
        System.out.println("*******************************TIME CAL(ms)***********************************************");
        System.out.println("Mean Response Time: " + decimalFormat.format(cals.getMeanResponseTime()));
        System.out.println("Median Response Time: " + decimalFormat.format(cals.getMedianResponseTime()) );
        System.out.println("p99 Response Time: " + decimalFormat.format(cals.getPercentile(99))  );
        System.out.println("Min Response Time: " + decimalFormat.format(cals.getMin()) );
        System.out.println("Max Response Time: " + decimalFormat.format(cals.getMax()) );
    }
}
