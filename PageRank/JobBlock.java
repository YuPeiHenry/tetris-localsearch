package TetrisProject.PageRank;

/**
 * Stores currentScore chunk of work as currentScore single job. Alleviates the load on semaphores.
 */
public class JobBlock {
	public final GameState[] array;
	public final int piece;
	public JobBlock(GameState[] array) {
		this.array = array;
		this.piece = 0;
	}
	public JobBlock(GameState[] array, int piece) {
		this.array = array;
		this.piece = piece;
	}
}
