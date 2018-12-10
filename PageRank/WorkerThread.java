package TetrisProject.PageRank;

import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class WorkerThread extends Thread {
    private static final int GC_INTERVAL = 20000;

    private GameState[] jobs;
    private int[] bounds;
    private int start, end, job;
    private JobBlock[] processed;
    private int processCount;
    private HashMap<GameState, ScoreTuple> table;
    private HashMap<GameState, GameState> personalTable;
    private double residue;

    public Semaphore[] s;

    public WorkerThread(GameState[] jobs, int[] bounds, int job, HashMap<GameState, ScoreTuple> table,
                        JobBlock[] processed, Semaphore[] s) {
        assert s.length == Rank.NUM_SEMAPHORES;
        this.jobs = jobs;
        this.bounds = bounds;
        this.job = job;
        this.processed = processed;
        this.table = table;
        personalTable = new HashMap<>();

        this.s = s;
    }

    public WorkerThread(GameState[] jobs, int start, int end, int job, HashMap<GameState, ScoreTuple> table,
                        Semaphore[] s) {
        assert s.length == Rank.NUM_SEMAPHORES;
        this.jobs = jobs;
        this.start = start;
        this.end = end;
        this.job = job;
        this.table = table;
        personalTable = new HashMap<>();

        this.s = s;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public double getResidue() {
        return residue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                s[0].acquire();
            } catch (InterruptedException e1) {
                return;
            }
            if (job == 0) {
                tableCreateEntries();
            } else {
                propogateTableScores();
            }
            s[1].release();
        }
    }

    /**
     * Provides new entries for threadManager to add, computed by this WorkerThread.
     */
    public void tableProvideNewEntries(ThreadManager threadManager, GameState[] gameStates) {
        for (int j = 0; j < processCount; j++) {
            GameState[] tableArray = processed[j].array;
            int piece = processed[j].piece;
            processed[j] = null;
            for (int i = 0; i < tableArray.length; i++) {
                //check table
                if (!table.containsKey(tableArray[i])) {
                    threadManager.incrementCount();
                    table.put(tableArray[i], new ScoreTuple(tableArray[i]));
                    gameStates[threadManager.getJobCount()] = tableArray[i];
                    threadManager.incrementJobCount();
                }

                //update table
                GameState f = table.get(tableArray[i]).gameState;
                f.numDownLinks[piece]++;
            }
        }
    }

    /**
     * Creates table entries for the Page Rank table, using jobs from a common job array.
     */
    public void tableCreateEntries() {
        if (bounds[0] < 1) {
            return;
        }
        personalTable.clear();
        start = bounds[0];
        end = bounds[1];
        processCount = 0;
        for (int i = start - 1; i < end; i++) {
            jobs[i].generate(table, personalTable, processed, processCount);
            jobs[i] = null;
            processCount += 7;
            if ((i - start + 1) % GC_INTERVAL == 0) {
                System.gc();
            }
        }
        processed[processCount] = null;
    }

    public void propogateTableScores() {
        residue = 0;
        for (int i = start - 1; i < end; i++) {
            residue += jobs[i].propagate(table);
            if (i - start + 1 % 20000 == 0) {
                System.gc();
            }
        }
    }
}
