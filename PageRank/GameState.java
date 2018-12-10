package TetrisProject.PageRank;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Represents a game state in tetris. Each state is identified using 3 unique integer identifiers.
 * Symmetrical states are computed as the same.
 */
public class GameState {
	private static final int HEIGHT_LIMIT = 4;
	private static final int HOLE_LIMIT = 1;
	private static final int HOLE_FACTOR = Math.max(HEIGHT_LIMIT + 1, State.COLS);
	//probability of each tetris piece appearing, total of 7 pieces
	private static final int[] WEIGHTS = {1, 1, 1, 1, 1, 1, 1};
	private static final int WEIGHTS_TOTAL = 7;
	private static final int ENTRIES_PER_HOLE = 2;
	private static final int SYSTEM_ERROR_STATUS = 1;

    private static Logger logger = Logger.getLogger(Rank.LOGGER_NAME);

    //identifier for height of 5 columns, split into left and right half. identiferOne is always <= identiferTwo
	private int identifierOne;
    private int identifierTwo;
    //identifer for holes
    private int identifierThree;

    private int[] numDownLinks;

	public GameState(int[] heights, int[] holes) {
	    computeHeightIdentifiers(heights);
		boolean isFlipped = isIdentifierOneTwoFlipped();
        computeHoleIdentifier(isFlipped, holes);
		numDownLinks = new int[State.N_PIECES];
	}

    /**
     * Generates an list of game states from the current list of game states, by putting a single tetris piece.
     */
    public void generate(HashMap<GameState, ScoreTuple> table, HashMap<GameState, GameState> personalTable,
                         JobBlock[] processed, int processCount) {
        int[] heights = getHeightsFromIdentifiers();
        int[] holes = getHolesFromIdentifiers();
        int holeCount = holes[0];
        holes[0] = holes[holeCount * ENTRIES_PER_HOLE];

        for (int piece = 0; piece < State.N_PIECES; piece++) {
            HashMap<GameState, Integer> upLinks = links(table, personalTable, heights, holes, holeCount, piece);
            GameState[] array = new GameState[upLinks.keySet().size()];
            upLinks.keySet().toArray(array);
            processed[processCount + piece] = new JobBlock(array, piece);
        }
    }

    public String getIdentifiersAsString() {
        return identifierOne + " " + identifierTwo + " " + identifierThree + " ";
    }

    public void incrementNumDownLinks(int piece) {
        numDownLinks[piece]++;
    }

    @Override
    public int hashCode() {
        return (identifierOne * (int)Math.pow(HEIGHT_LIMIT + 1, State.COLS / 2) + identifierTwo)
                * (int)Math.pow(State.COLS * (HEIGHT_LIMIT + 1), HOLE_LIMIT)
                + identifierThree;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !GameState.class.isAssignableFrom(other.getClass())) {
            return false;
        }
        GameState otherGameState = (GameState) other;
        return otherGameState.identifierOne == identifierOne &&
                otherGameState.identifierTwo == identifierTwo && otherGameState.identifierThree == identifierThree;
    }

	private void computeHeightIdentifiers(int[] heights) {
        identifierOne = 0;
        for (int i = 0; i < State.COLS / 2; i++) {
            identifierOne *= (HEIGHT_LIMIT + 1);
            identifierOne += heights[i];
        }
        identifierTwo = 0;
        for (int i = State.COLS - 1; i >= State.COLS / 2; i--) {
            identifierTwo *= (HEIGHT_LIMIT + 1);
            identifierTwo += heights[i];
        }
    }

    private boolean isIdentifierOneTwoFlipped() {
        if (identifierOne > identifierTwo) {
            int temp = identifierTwo;
            identifierTwo = identifierOne;
            identifierOne = temp;
            return true;
        }
        return false;
    }

    private void computeHoleIdentifier(boolean isFlipped, int[] holes) {
        identifierThree = 0;
        for (int i = 0; i < holes.length / ENTRIES_PER_HOLE; i++) {
            identifierThree *= HOLE_FACTOR;
            if (isFlipped) {
                identifierThree += State.COLS - 1 - holes[ENTRIES_PER_HOLE * i + 1];
            } else {
                identifierThree += holes[ENTRIES_PER_HOLE * i + 1];
            }
            identifierThree *= HOLE_FACTOR;
            identifierThree += holes[ENTRIES_PER_HOLE * i];
        }
    }

    private int[] getHeightsFromIdentifiers() {
        int[] heights = new int[State.COLS];

        int temp = identifierOne;
        for (int i = State.COLS / 2 - 1; i >= 0; i--) {
            heights[i] = temp % (HEIGHT_LIMIT + 1);
            temp /= (HEIGHT_LIMIT + 1);
        }
        temp = identifierTwo;
        for (int i = State.COLS / 2; i < State.COLS; i++) {
            heights[i] = temp % (HEIGHT_LIMIT + 1);
            temp /= (HEIGHT_LIMIT + 1);
        }
        return heights;
    }

    private int[] getHolesFromIdentifiers() {
        int[] holes = new int[HOLE_LIMIT * ENTRIES_PER_HOLE + 1];
        int holecount = 0;

        int temp = identifierThree;
        while (temp > 0) {
            holes[holecount * ENTRIES_PER_HOLE] = temp % HOLE_FACTOR;
            temp /= HOLE_FACTOR;
            holes[holecount * ENTRIES_PER_HOLE + 1] = temp % HOLE_FACTOR;
            temp /= HOLE_FACTOR;
            holecount++;
        }
        holes[holecount * ENTRIES_PER_HOLE] = holes[0];
        holes[0] = holecount;
        return holes;
    }

    /**
     * Returns all possible resultant game states from the current game state, by putting a single tetris piece.
     * upLinks flow to downLinks.
     */
	private HashMap<GameState, Integer> links(HashMap<GameState, ScoreTuple> table,
                                              HashMap<GameState, GameState> personalTable, int[] heights, int[] holes,
                                              int holeCount, int piece) {

		HashMap<GameState, Integer> upLinks = new HashMap<>();
		//for each shape orientation
		for (int orientation = 0; orientation < State.getpOrients()[piece]; orientation++) {
			int pWidth = State.getpWidth()[piece][orientation];
            int[] pBottom = State.getpBottom()[piece][orientation];
            int[] pTop = State.getpTop()[piece][orientation];

            //for each slot (column)
			for (int slot = 0; slot <= State.COLS - pWidth; slot++) {
			    //Calculate existing information
                int[] newHeights = new int[State.COLS];
                ArrayList<Integer> newHoles = new ArrayList<>();
                System.arraycopy(heights, 0, newHeights, 0, State.COLS);
				for (int k = 0; k < ENTRIES_PER_HOLE * holeCount; k++) {
					newHoles.add(holes[k]);
				}

                dropPieceIntoField(slot, pWidth, pTop, pBottom, heights, newHeights, newHoles);
                updateFieldAfterLinesCleared(newHoles, newHeights);
                exitIfNegativeHeight(newHeights);
                addGameStateIfValid(table, newHeights, orientation, slot, newHoles, upLinks, personalTable);
			}
		}
		return upLinks;
	}

	private void dropPieceIntoField(int slot, int pWidth, int[] pTop, int[] pBottom, int[] heights, int[] newHeights,
                                    ArrayList<Integer> newHoles) {
        int pieceElevation = 0;
        for (int i = 0; i < pWidth; i++) {
            pieceElevation = Math.max(pieceElevation, heights[slot + i] - pBottom[i]);
        }

        for (int i = 0; i < pWidth; i++) {
            newHeights[slot + i] = pieceElevation + pTop[i];
            for (int row = pBottom[i] + pieceElevation - 1; row > heights[slot + i]; row--) {
                newHoles.add(row);
                newHoles.add(slot + i);
            }
        }
    }

    private int getLinesCleared(int minimumHeight, ArrayList<Integer> newHoles) {
        int linesCleared = 0;
        for (int row = minimumHeight - 1; row >= 0; row--) {
            boolean hasCleared = true;
            for (int col = 0; hasCleared && col < newHoles.size() / ENTRIES_PER_HOLE; col++) {
                hasCleared = hasCleared && newHoles.get(col * ENTRIES_PER_HOLE) != row;
            }
            if (!hasCleared) {
                continue;
            }
            linesCleared++;
            for (int i = 0; i < newHoles.size() / ENTRIES_PER_HOLE; i++) {
                if (newHoles.get(i * ENTRIES_PER_HOLE) >= row) {
                    newHoles.set(i * ENTRIES_PER_HOLE, newHoles.get(i * ENTRIES_PER_HOLE) - 1);
                }
            }
        }
        return linesCleared;
    }

    private void updateFieldAfterLinesCleared(ArrayList<Integer> newHoles, int[] newHeights) {
        int minimumHeight = State.ROWS;
        for (int i = 0; i < State.COLS; i++) {
            minimumHeight = Math.min(minimumHeight, newHeights[i]);
        }
        int linesCleared = getLinesCleared(minimumHeight, newHoles);
        if (linesCleared > 0) {
            for (int i = 0; i < State.COLS; i++) {
                newHeights[i] -= linesCleared;
            }
        }

        boolean changed = true;
        while (changed) {
            changed = false;
            for (int i = newHoles.size() / ENTRIES_PER_HOLE - 1; i >= 0; i--) {
                if (newHoles.get(i * ENTRIES_PER_HOLE) >= newHeights[newHoles.get(i * ENTRIES_PER_HOLE + 1)]) {
                    changed = true;
                    newHeights[newHoles.get(i * ENTRIES_PER_HOLE + 1)] -= 1;
                    if (minimumHeight >= 0 && newHeights[newHoles.get(i * ENTRIES_PER_HOLE + 1)] < 0) {
                        logger.severe("Height has went negative while computing holes. Exiting...");
                        logger.severe(newHeights[newHoles.get(i * ENTRIES_PER_HOLE + 1)] + " "
                                + newHoles.get(i * ENTRIES_PER_HOLE + 1));
                        System.exit(SYSTEM_ERROR_STATUS);
                    }
                    newHoles.remove(i * ENTRIES_PER_HOLE + 1);
                    newHoles.remove(i * ENTRIES_PER_HOLE);
                }
            }
        }
    }

    private void exitIfNegativeHeight(int[] newHeights) {
        for (int x = 0; x < State.COLS; x++) {
            if (newHeights[x] < 0) {
                logger.severe("Height has went negative after clearing rows. Exiting...");
                System.exit(SYSTEM_ERROR_STATUS);
            }
        }
	}

	private void addGameStateIfValid(HashMap<GameState, ScoreTuple> table, int[] newHeights, int orientation, int slot,
                                     ArrayList<Integer> newHoles, HashMap<GameState, Integer> upLinks,
                                     HashMap<GameState, GameState> personalTable) {
        int maximumHeight = 0;
        for (int k = 0; k < State.COLS; k++) {
            maximumHeight = Math.max(maximumHeight, newHeights[k]);
        }

        if (maximumHeight <= HEIGHT_LIMIT && newHoles.size() <= HOLE_LIMIT * ENTRIES_PER_HOLE) {
            int[] holeArray = new int[newHoles.size()];
            for (int k = 0; k < holeArray.length; k++) {
                holeArray[k] = newHoles.get(k);
            }
            GameState newfield = new GameState(newHeights, holeArray);
            if (table.containsKey(newfield)) {
                newfield = table.get(newfield).gameState;
            }

            if (!upLinks.containsKey(newfield)) {
                if (personalTable != null) {
                    if (personalTable.containsKey(newfield)) {
                        newfield = personalTable.get(newfield);
                    } else {
                        personalTable.put(newfield, newfield);
                    }
                }
                upLinks.put(newfield, orientation * State.COLS + slot);
            }
        }
    }

    /**
     * Propogates score using the Page Rank algorithm. Score of each vertex is distributed among its downLinks.
     */
	public double propagate(HashMap<GameState, ScoreTuple> table) {
		double residue = 0;
		int[] heights = getHeightsFromIdentifiers();
		int[] holes = getHolesFromIdentifiers();
		int holeCount = holes[0];
		holes[0] = holes[holeCount * ENTRIES_PER_HOLE];

		ScoreTuple value = table.get(this);
		for (int piece = 0; piece < State.N_PIECES; piece++) {
			HashMap<GameState, Integer> upLinks = links(table, null, heights, holes, holeCount, piece);
			for (GameState newField : upLinks.keySet()) {
				//add incoming
                ScoreTuple getField = table.get(newField);
                double multiplier = 1.0 * WEIGHTS[piece] / WEIGHTS_TOTAL / getField.gameState.numDownLinks[piece];
				value.nextIterationScore += getField.currentScore * multiplier;
			}
			if (numDownLinks[piece] == 0) {
				residue += value.currentScore * WEIGHTS[piece] / WEIGHTS_TOTAL;
			}
		}

		return residue;
	}

    /**
     * Gets the game states sorted in descending order of their score from the Page Rank algorithm.
     * The most popular game state will be the first entry.
     */
	public int[][] order(HashMap<GameState, ScoreTuple> table) {
		int[] heights = getHeightsFromIdentifiers();
		int[] holes = getHolesFromIdentifiers();
		int holeCount = holes[0];
		holes[0] = holes[holeCount * ENTRIES_PER_HOLE];
		
		int[][] result = new int[State.N_PIECES][];
		for (int piece = 0; piece < State.N_PIECES; piece++) {
			HashMap<GameState, Integer> upLinks = links(table, null, heights, holes, holeCount, piece);
			result[piece] = new int[upLinks.size()];
			ArrayList<ScoreTuple> list = new ArrayList<>();
			for (GameState newField : upLinks.keySet()) {
				list.add(table.get(newField));
			}
			Collections.sort(list);
			for (int j = 0; j < result[piece].length; j++) {
				result[piece][j] = upLinks.get(list.get(j).gameState);
			}
		}
		return result;
	}
}
