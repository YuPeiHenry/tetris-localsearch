package TetrisProject.PageRank;

/**
 * Tracks score for a given GameState. Public fields reduce overhead of computations.
 */
public class ScoreTuple implements Comparable<ScoreTuple>{
	public double currentScore;
	public double nextIterationScore;
	public GameState gameState;
	public ScoreTuple(GameState gameState) {
		assert gameState != null;
		this.gameState = gameState;
	}
	@Override
	public int compareTo(ScoreTuple other) {
		if (currentScore > other.currentScore) {
			return 1;
		} else if (currentScore == other.currentScore) {
			return 0;
		}
		return -1;
	}
}
