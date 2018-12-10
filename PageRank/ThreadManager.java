package TetrisProject.PageRank;

import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

/**
 * Manages WorkerThreads in heavy computational tasks in Page Rank computation.
 * Uses System.out..println to give visualization of the progress completed, for batch environments with no UI.
 */
public class ThreadManager {
    private static final int STARTING_ARRAY_SIZE = 24000000;
    private static final int NUM_ITERATIONS = 15;
    private static final int PRINT_INTERVAL = 100000;

    private static Logger logger = Logger.getLogger(Rank.LOGGER_NAME);

    private HashMap<GameState, ScoreTuple> table;
    private GameState[] gameStates;
    private int count;
    private int jobCount;
    private JobBlock[][] processed;

    public int getCount() {
        return count;
    }

    public void incrementCount() {
        this.count++;
        if (count % PRINT_INTERVAL == 0) {
            System.out.println(count);
            logger.info(String.format("Created %1$s vertices.", count));
        }
    }

    public int getJobCount() {
        return jobCount;
    }

    public void incrementJobCount() {
        this.jobCount++;
    }

    public GameState[] getGameStates() {
        return gameStates;
    }

    public void setGameState(int index, GameState gameState) {
        this.gameStates[index] = gameState;
    }

    public ThreadManager(HashMap<GameState, ScoreTuple> table, JobBlock[][] processed) {
        this.table = table;

        if (processed != null) {
            count = 0;
            gameStates = new GameState[STARTING_ARRAY_SIZE];
            this.processed = processed;
        }
    }

    /**
     * Updates the Page Rank table with new vertices generates by workers
     */
    public void tableAcquireNewEntries(WorkerThread[] workers) {
        jobCount = 0;

        for (WorkerThread worker : workers) {
            worker.tableProvideNewEntries(this, gameStates);
        }
        //null terminate the array
        gameStates[jobCount] = null;
    }

    /**
     * Updates the Page Rank table using currentScore single thread. Used when there are few tasks to reduce multi-thread overhead.
     */
    public void singleThreadUpdate() {
        int processedCount = 0;
        for (int i = 0; i < jobCount; i++) {
            gameStates[i].generate(table, null, processed[0], processedCount);
            processedCount += 7;
        }
        processed[0][processedCount] = null;

        jobCount = 0;
        for (int j = 0; j < processedCount; j++) {
            GameState[] tableArray = processed[0][j].array;
            int piece = processed[0][j].piece;
            for (int i = 0; i < tableArray.length; i++) {
                //check table
                if (!table.containsKey(tableArray[i])) {
                    incrementCount();

                    table.put(tableArray[i], new ScoreTuple(tableArray[i]));
                    gameStates[jobCount] = tableArray[i];
                    jobCount++;
                }

                //update table
                GameState f = table.get(tableArray[i]).gameState;
                f.incrementNumDownLinks(piece);
            }
        }
        gameStates[jobCount] = null;
    }

    /**
     * Calculates the Page Rank scores for each page, using currentScore set number of workers.
     * Total score adds up to 1. All vertices start with equal score.
     */
    public void scheduleWorkersCalculateScore(int numWorkers) {
        WorkerThread[] workers = new WorkerThread[numWorkers];
        GameState[] gameStatesForComputation = new GameState[table.keySet().size()];
        Semaphore[][] semaphores = new Semaphore[numWorkers][Rank.NUM_SEMAPHORES];
        int[] divisions = new int[numWorkers + 1];
        scheduleWorkersInitVars(gameStatesForComputation, numWorkers, divisions);
        Rank.initSemaphores(semaphores);

        scheduleWorkersStartWorkers(workers, gameStatesForComputation, numWorkers, divisions, semaphores);
        for (int i = 0; i < NUM_ITERATIONS; i++) {
            scheduleWorkersOneIteration(numWorkers, semaphores, workers, gameStatesForComputation);
            System.out.println(i);
            System.gc();
        }

        for (int i = 0; i < numWorkers; i++) {
            workers[i].interrupt();
        }
    }

    private void scheduleWorkersInitVars(GameState[] gameStatesForComputation, int numWorkers, int[] divisions) {
        int interval = gameStatesForComputation.length / numWorkers;
        table.keySet().toArray(gameStatesForComputation);
        for (int i = 0; i < numWorkers; i++) {
            divisions[i] = interval * i + 1;
        }
        divisions[numWorkers] = gameStatesForComputation.length + 1;
        for (ScoreTuple scoreTuple : table.values()) {
            scoreTuple.currentScore = (double) 1 / count;
            scoreTuple.nextIterationScore = 0;
        }
    }

    private void scheduleWorkersStartWorkers(WorkerThread[] workers, GameState[] gameStatesForComputation,
                                             int numWorkers, int[] divisions, Semaphore[][] semaphores) {
        for (int i = 0; i < numWorkers; i++) {
            assert semaphores[i].length == Rank.NUM_SEMAPHORES;
        }
        for (int i = 0; i < numWorkers; i++) {
            workers[i] = new WorkerThread(gameStatesForComputation, divisions[i], divisions[i + 1] - 1,
                    1, table, semaphores[i]);
            workers[i].setStart(divisions[i]);
            workers[i].setEnd(divisions[i + 1] - 1);
            workers[i].start();
        }
    }

    private void scheduleWorkersOneIteration(int numWorkers, Semaphore[][] semaphores, WorkerThread[] workers,
                                             GameState[] gameStatesForComputation) {
        //residualScore comes from vertices with no outgoing edges
        double residualScore = 0;
        for (int j = 0; j < numWorkers; j++) {
            semaphores[j][0].release();
        }
        for (int j = 0; j < numWorkers; j++) {
            try {
                semaphores[j][1].acquire();
            } catch (InterruptedException e) {
                logger.warning(String.format("Thread %1$s is interrupted", j));
                logger.warning(e.getMessage());
            }
            residualScore += workers[j].getResidue();
        }
        for (ScoreTuple scoreTuple : table.values()) {
            scoreTuple.currentScore = scoreTuple.nextIterationScore + residualScore / gameStatesForComputation.length;
            scoreTuple.nextIterationScore = 0;
        }
    }

}
