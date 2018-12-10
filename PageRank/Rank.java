package TetrisProject.PageRank;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * Main program.
 * Runs the Page Rank algorithm on generated tetris game states.
 *
 * Each vertex is currentScore tetris game state.
 * Each directed edge represents currentScore state transiting to another state through placement of currentScore tetris shape.
 * States are generated with currentScore height limit, and currentScore hole limit.
 *
 */
public class Rank {
    public static final String LOGGER_NAME = "Main";

    private static final int NUM_WORKERS = 12;
    private static final int INITIAL_ARRAY_SIZE = 14000000;
    private static final int ONE_BILLION = 1000000000;
    private static final String LINE_ENDING = "\r\n";
    private static final int MIN_JOBS_PER_WORKER = 1000;
    public static final int NUM_SEMAPHORES = 2;

    private static Logger logger = Logger.getLogger(LOGGER_NAME);

    /**
     * Generates the Page Rank table. Required for computation of Page Rank scores.
     */
    private void generatePageRankTable(HashMap<GameState, ScoreTuple> table) {
        //variable initialization
        JobBlock[][] toBeProcessed = new JobBlock[NUM_WORKERS][INITIAL_ARRAY_SIZE];
        //[lower bound, upper bound]
        int[][] workerJobNumberBoundary = new int[NUM_WORKERS][2];
        Semaphore[][] semaphores = new Semaphore[NUM_WORKERS][NUM_SEMAPHORES];
        initSemaphores(semaphores);
        ThreadManager threadManager = new ThreadManager(table, toBeProcessed);
        WorkerThread[] workerThreads = new WorkerThread[NUM_WORKERS];

        startWithEmptyState(table, threadManager, workerThreads, workerJobNumberBoundary, toBeProcessed, semaphores);
        startPopulatingTable(threadManager, workerThreads, workerJobNumberBoundary, semaphores);
        wrapUpTable(threadManager, workerThreads);
    }

    private void computePageRankings(HashMap<GameState, ScoreTuple> table) {
        ThreadManager threadManager = new ThreadManager(table, null);
        threadManager.scheduleWorkersCalculateScore(NUM_WORKERS);
    }

    private void savePageRankResults(HashMap<GameState, ScoreTuple> table) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("output"));
            for (GameState gameState : table.keySet()) {
                int[][] order = gameState.order(table);
                writer.write(gameState.getIdentifiersAsString());
                for (int[] anOrder : order) {
                    writer.write(": ");
                    for (int anAnOrder : anOrder) {
                        writer.write(anAnOrder + " ");
                    }
                }
                writer.write(LINE_ENDING);
            }
            writer.close();
            logger.info("Completed saving of Page Rank results.");
        } catch (IOException e) {
            logger.severe("Unable to save output.");
            logger.severe(e.getMessage());
        }
    }

    /**
     * Initializes an array of 2-tuple semaphores with 0 permits.
     */
    public static void initSemaphores(Semaphore[][] semaphores) {
        for (int i = 0; i < NUM_WORKERS; i++) {
            assert semaphores[i].length == NUM_SEMAPHORES;
            semaphores[i][0] = new Semaphore(0, true);
            semaphores[i][1] = new Semaphore(0, true);
        }
    }

    /**
     * Start with an entirely empty game board
     */
    private void startWithEmptyState(HashMap<GameState, ScoreTuple> table, ThreadManager threadManager,
                                     WorkerThread[] workerThreads, int[][] workerJobNumberBoundary,
                                     JobBlock[][] toBeProcessed, Semaphore[][] semaphores) {
        for (int i = 0; i < NUM_WORKERS; i++) {
            assert semaphores[i].length == NUM_SEMAPHORES;
        }
        GameState emptyTetrisState = new GameState(new int[State.ROWS], new int[0]);
        table.put(emptyTetrisState, new ScoreTuple(emptyTetrisState));
        threadManager.setGameState(0, emptyTetrisState);
        threadManager.incrementCount();
        threadManager.incrementJobCount();
        for (int i = 0; i < NUM_WORKERS; i++) {
            workerThreads[i] = new WorkerThread(threadManager.getGameStates(), workerJobNumberBoundary[i], 0,
                    table, toBeProcessed[i], semaphores[i]);
            workerThreads[i].start();
        }
    }

    /**
     * Tasks the threadManager to populate the Page Rank table.
     */
    private void startPopulatingTable(ThreadManager threadManager, WorkerThread[] workerThreads,
                                      int[][] workerJobNumberBoundary, Semaphore[][] semaphores) {
        for (int i = 0; i < NUM_WORKERS; i++) {
            assert semaphores[i].length == NUM_SEMAPHORES;
        }
        while (threadManager.getJobCount() > 0) {
            int jobsPerWorker = threadManager.getJobCount() / NUM_WORKERS;
            if (jobsPerWorker > MIN_JOBS_PER_WORKER) {
                populateTableAssignJobIteration(threadManager, jobsPerWorker, workerJobNumberBoundary, semaphores);
                waitForJobCompletion(semaphores);
                threadManager.tableAcquireNewEntries(workerThreads);
            } else {
                threadManager.singleThreadUpdate();
            }
            System.gc();
        }
    }

    private void populateTableAssignJobIteration(ThreadManager threadManager, int jobsPerWorker,
                                                 int[][] workerJobNumberBoundary, Semaphore[][] semaphores) {
        for (int i = 0; i < NUM_WORKERS; i++) {
            assert semaphores[i].length == NUM_SEMAPHORES;
        }
        int[] jobNumberBoundary = new int[NUM_WORKERS + 1];
        for (int i = 0; i < NUM_WORKERS; i++) {
            jobNumberBoundary[i] = i * jobsPerWorker + 1;
        }
        jobNumberBoundary[NUM_WORKERS] = threadManager.getJobCount() + 1;
        for (int i = 0; i < NUM_WORKERS; i++) {
            workerJobNumberBoundary[i][0] = jobNumberBoundary[i];
            workerJobNumberBoundary[i][1] = jobNumberBoundary[i + 1] - 1;
            semaphores[i][0].release();
        }
    }

    private void waitForJobCompletion(Semaphore[][] semaphores) {
        for (int i = 0; i < NUM_WORKERS; i++) {
            assert semaphores[i].length == NUM_SEMAPHORES;
        }
        for (int i = 0; i < NUM_WORKERS; i++) {
            try {
                semaphores[i][1].acquire();
            } catch (InterruptedException e) {
                logger.warning(String.format("Thread %1$s is interrupted", i));
                logger.warning(e.getMessage());
            }
        }
    }

    /**
     * Notes down the total entries in the table and stops all worker threads.
     */
    private void wrapUpTable(ThreadManager threadManager, WorkerThread[] workerThreads) {
        logger.info("Total entries: " + String.valueOf(threadManager.getCount()));
        for (int i = 0; i < NUM_WORKERS; i++) {
            workerThreads[i].interrupt();
        }
    }

    /**
     * Coordinates the entire Page Rank computation process.
     */
    public static void main(String[] args) {
        logger.info("Number of processors available: " + Runtime.getRuntime().availableProcessors());
        long startTime = System.nanoTime();
        HashMap<GameState, ScoreTuple> table = new HashMap<>();
        Rank rank = new Rank();

        rank.generatePageRankTable(table);
        rank.computePageRankings(table);

        long endTime = System.nanoTime();
        logger.info("Seconds used: " + (endTime - startTime) / ONE_BILLION + LINE_ENDING);
        logger.info("Memory used: " + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())
                + LINE_ENDING);
        rank.savePageRankResults(table);
    }
}
