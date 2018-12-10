package TetrisProject.MainProject;

public class GenericPlayer {
	double[] coeff;
	
	public GenericPlayer() {
		;
	}
	
	public GenericPlayer(double[] coeff) {
		this.coeff = coeff;
	}

	//implement this function to have currentScore working system
	public int pickMove(State s, int[][] legalMoves) {
		int maxrecurse = -1;
		int bestsample = 3;
		
		int[][] moves;
		int piece;
		int[] top;
		int[][] field;
		if (legalMoves[0][0] < maxrecurse) {
			//error
			return 0;
		} else if (legalMoves[0][0] < 0) {
			piece = legalMoves[0][1];
			moves = State.legalMoves[piece];
			top = legalMoves[1];
			field = new int[State.ROWS][State.COLS];
			for (int i = 0; i < State.ROWS; i++) {
				for (int j = 0; j < State.COLS; j++) {
					field[i][j] = legalMoves[i + 2][j];
				}
			}
			field = s.getField();
		} else {
			//1 - O, 2 - I, 3 - L, 4 - J, 5 - T, 6 - S, 7 - Z
			piece = s.getNextPiece();
			moves = legalMoves;
			top = s.getTop();
			field = s.getField();
		}

		int[] newtop = new int[State.COLS];
		int[][][] fields = new int[moves.length][][];
		
		
		double[] score = new double[moves.length];
		
		for (int i = 0; i < moves.length; i++) {
			//init
			int[][] newfield = new int[State.ROWS + 2][State.COLS];
			fields[i] = newfield;
			
			for (int j = 0; j < State.ROWS; j++) {
				for (int k = 0; k < State.COLS; k++) {
					newfield[j + 2][k] = field[j][k];
				}
			}
			for (int j = 0; j < top.length; j++) {
				newtop[j] = top[j];
			}

			int orientation = moves[i][State.ORIENT];
			int slot = moves[i][State.SLOT];
			
			int pwidth = State.getpWidth()[piece][orientation];
			int[] pbottom = State.getpBottom()[piece][orientation];
			int[] ptop = State.getpTop()[piece][orientation];
			
			int pheight = State.getpHeight()[piece][orientation];

			//height if the first column makes contact
			int height = top[slot]-pbottom[0];
			//for each column beyond the first in the piece
			for(int c = 1; c < pwidth;c++) {
				height = Math.max(height,top[slot+c]-pbottom[c]);
			}

			//check if game ended
			if(height+pheight >= State.ROWS) {
				score[i] = 999999;
				continue;
			}

			
			//for each column in the piece - fill in the appropriate blocks
			for(int j = 0; j < pwidth; j++) {
				
				//from bottom to top of brick
				for(int h = height+pbottom[j]; h < height+ptop[j]; h++) {
					newfield[h + 2][j+slot] = 1;
				}
			}
			
			//adjust top
			for(int c = 0; c < pwidth; c++) {
				newtop[slot+c]=height+ptop[c];
			}
			
			int cleared = 0;
			
			//check for full rows - starting at the top
			for(int r = height+pheight-1; r >= height; r--) {
				//check all columns in the row
				boolean full = true;
				for(int c = 0; c < State.COLS; c++) {
					if(newfield[r + 2][c] == 0) {
						full = false;
						break;
					}
				}
				//if the row was full - remove it and slide above stuff down
				if(full) {
					cleared++;
					//slide down all bricks
					for(int j = r; j < State.ROWS - 1; j++) {
						newfield[j + 2] = newfield[j+3];
					}
					newfield[State.ROWS - 1 + 2] = new int[State.COLS];
					//lower the top
					//for each column
					for(int c = 0; c < State.COLS; c++) {
						newtop[c]--;
						while(newtop[c]>=1 && newfield[newtop[c]-1 + 2][c]==0)	newtop[c]--;
					}
				}
			}
			
			//calculate height of highest block
			int newheight = 0;
			for (int j = 0; j < State.COLS; j++) {
				newheight = Math.max(newheight, newtop[j]);
			}
			
			int holecreated = 0;
			int holecovered = 0;
			int holecleared = 0;
			for (int j = 0; j < State.COLS; j++) {
				for (int k = top[j] - 2; k >= newtop[j]; k--) {
					if (newfield[k + 2][j] < 0 ) {
						newfield[k + 2][j] = 0;
						holecleared += 1;
					}
				}
			}
			for (int j = slot; j < slot + pwidth; j++) {
				for (int k = newtop[j] - 2; k >= 0; k--) {
					if (newfield[k + 2][j] == 0 ) {
						newfield[k + 2][j] = -1;
						holecreated += 1;
					} else if (newfield[k + 2][j] < 0) {
						holecovered += 1;
					}
				}
			}

			
			int perimeter = -(newtop[0] + newtop[9]) / 2;
			int cliff = 0;
			for (int j = 0; j < State.COLS - 1; j++) {
				perimeter += Math.abs(newtop[j] - newtop[j + 1]);
				if (Math.abs(newtop[j] - newtop[j + 1]) > 2) {
					cliff += Math.abs(newtop[j] - newtop[j + 1]) - 2;
				}
			}
					
			
			score[i] = coeff[0] / 1000 * newheight * newheight + coeff[1] * holecreated
					+ coeff[2] * holecovered + coeff[3] * perimeter + coeff[4] * cliff - coeff[5] * cleared
					- coeff[6] * holecleared;
		}

		int choice = 0;
		
		if (legalMoves[0][0] > maxrecurse) {
			int[] best = new int[bestsample];
			for (int i = 0; i < score.length; i++) {
				int j;
				for (j = bestsample; j > 0 && score[i] < score[best[j - 1]]; j--) {
					;
				}
				if (j < bestsample) {					
					//insert
					for (int k = bestsample - 1; k > j; k--) {
						best[k] = best[k - 1];
					}
					best[j] = i;
				}
			}
			for (int i = 0; i < bestsample; i++) {
				int worstscore = Integer.MIN_VALUE;
				for (int j = 0; j < State.N_PIECES; j++) {
					int[][] newfield = fields[best[i]];
					//recursions done, new next piece;
					newfield[0] = new int[2];
					newfield[0][0] = legalMoves[0][0] - 1;
					newfield[0][1] = j;
					
					//Shallow copy top
					newfield[1] = newtop;
					worstscore = Math.max(worstscore, pickMove(s, newfield));
				}
				score[best[i]] += coeff[7] / 500 * worstscore;
//				score[best[i]] += h * worstscore;
			}
			for (int i = 0; i < bestsample; i++) {
				if (score[best[i]] < score[choice]) {
					choice = best[i];
				}
			}
		} else {
			for (int i = 1; i < score.length; i++) {
				if (score[i] < score[choice]) {
					choice = i;
				}
			}
		}

		if (legalMoves[0][0] < 0) {
			return (int)score[choice];
		} else {
			return choice;
		}
	}
	
	//implement this function to have currentScore working system
	public int distortMove(State s, int[][] legalMoves) {
		int maxrecurse = -1;
		int bestsample = 3;
		
		int[][] moves;
		int piece;
		int[] top;
		int[][] field;
		if (legalMoves[0][0] < maxrecurse) {
			//error
			return 0;
		} else if (legalMoves[0][0] < 0) {
			piece = legalMoves[0][1];
			moves = State.legalMoves[piece];
			top = legalMoves[1];
			field = new int[State.ROWS][State.COLS];
			for (int i = 0; i < State.ROWS; i++) {
				for (int j = 0; j < State.COLS; j++) {
					field[i][j] = legalMoves[i + 2][j];
				}
			}
			field = s.getField();
		} else {
			//1 - O, 2 - I, 3 - L, 4 - J, 5 - T, 6 - S, 7 - Z
			piece = s.getNextPiece();
			moves = legalMoves;
			top = s.getTop();
			field = s.getField();
		}

		int[] newtop = new int[State.COLS];
		int[][][] fields = new int[moves.length][][];
		
		
		double[] score = new double[moves.length];
		int cumulativeclear = 0;
		
		for (int i = 0; i < moves.length; i++) {
			if (i >= bestsample && (cumulativeclear >=2 || Math.random() < 0.01)) {
				break;
			}
			//init
			int[][] newfield = new int[State.ROWS + 2][State.COLS];
			fields[i] = newfield;
			
			for (int j = 0; j < State.ROWS; j++) {
				for (int k = 0; k < State.COLS; k++) {
					newfield[j + 2][k] = field[j][k];
				}
			}
			for (int j = 0; j < top.length; j++) {
				newtop[j] = top[j];
			}

			int orientation = moves[i][State.ORIENT];
			int slot = moves[i][State.SLOT];
			
			int pwidth = State.getpWidth()[piece][orientation];
			int[] pbottom = State.getpBottom()[piece][orientation];
			int[] ptop = State.getpTop()[piece][orientation];
			
			int pheight = State.getpHeight()[piece][orientation];

			//height if the first column makes contact
			int height = top[slot]-pbottom[0];
			//for each column beyond the first in the piece
			for(int c = 1; c < pwidth;c++) {
				height = Math.max(height,top[slot+c]-pbottom[c]);
			}

			//check if game ended
			if(height+pheight >= State.ROWS) {
				score[i] = -1;
				continue;
			}

			
			//for each column in the piece - fill in the appropriate blocks
			for(int j = 0; j < pwidth; j++) {
				
				//from bottom to top of brick
				for(int h = height+pbottom[j]; h < height+ptop[j]; h++) {
					newfield[h + 2][j+slot] = 1;
				}
			}
			
			//adjust top
			for(int c = 0; c < pwidth; c++) {
				newtop[slot+c]=height+ptop[c];
			}
			
			int cleared = 0;
			
			//check for full rows - starting at the top
			for(int r = height+pheight-1; r >= height; r--) {
				//check all columns in the row
				boolean full = true;
				for(int c = 0; c < State.COLS; c++) {
					if(newfield[r + 2][c] == 0) {
						full = false;
						break;
					}
				}
				//if the row was full - remove it and slide above stuff down
				if(full) {
					cleared++;
					cumulativeclear++;
					//slide down all bricks
					for(int j = r; j < State.ROWS - 1; j++) {
						newfield[j + 2] = newfield[j+3];
					}
					newfield[State.ROWS - 1 + 2] = new int[State.COLS];
					//lower the top
					//for each column
					for(int c = 0; c < State.COLS; c++) {
						newtop[c]--;
						while(newtop[c]>=1 && newfield[newtop[c]-1 + 2][c]==0)	newtop[c]--;
					}
				}
			}
			
			//calculate height of highest block
			int newheight = 0;
			for (int j = 0; j < State.COLS; j++) {
				newheight = Math.max(newheight, newtop[j]);
			}
			
			int holecreated = 0;
			int holecovered = 0;
			int holecleared = 0;
			for (int j = 0; j < State.COLS; j++) {
				for (int k = top[j] - 2; k >= newtop[j]; k--) {
					if (k + 2 < 0) {
						System.out.println(newtop[j]);
					}
					if (newfield[k + 2][j] < 0 ) {
						newfield[k + 2][j] = 0;
						holecleared += 1;
					}
				}
			}
			for (int j = slot; j < slot + pwidth; j++) {
				for (int k = newtop[j] - 2; k >= 0; k--) {
					if (newfield[k + 2][j] == 0 ) {
						newfield[k + 2][j] = -1;
						holecreated += 1;
					} else if (newfield[k + 2][j] < 0) {
						holecovered += 1;
					}
				}
			}

			
			int perimeter = -(newtop[0] + newtop[9]) / 2;
			int cliff = 0;
			for (int j = 0; j < State.COLS - 1; j++) {
				perimeter += Math.abs(newtop[j] - newtop[j + 1]);
				if (Math.abs(newtop[j] - newtop[j + 1]) > 2) {
					cliff += Math.abs(newtop[j] - newtop[j + 1]) - 2;
				}
			}
					
			
			score[i] = -999999 + coeff[0] / 1000 * newheight * newheight + coeff[1] * holecreated
					+ coeff[2] * holecovered + coeff[3] * perimeter + coeff[4] * cliff - coeff[5] * cleared
					- coeff[6] * holecleared;
		}

		int choice = 0;
		
		if (legalMoves[0][0] > maxrecurse) {
			int[] best = new int[bestsample];
			for (int i = 0; i < score.length; i++) {
				int j;
				for (j = bestsample; j > 0 && score[i] < score[best[j - 1]]; j--) {
					;
				}
				if (j < bestsample) {					
					//insert
					for (int k = bestsample - 1; k > j; k--) {
						best[k] = best[k - 1];
					}
					best[j] = i;
				}
			}
			for (int i = 0; i < bestsample; i++) {
				int worstscore = Integer.MIN_VALUE;
				for (int j = 0; j < State.N_PIECES; j++) {
					int[][] newfield = fields[best[i]];
					//recursions done, new next piece;
					newfield[0] = new int[2];
					newfield[0][0] = legalMoves[0][0] - 1;
					newfield[0][1] = j;
					
					//Shallow copy top
					newfield[1] = newtop;
					worstscore = Math.max(worstscore, distortMove(s, newfield));
				}
				score[best[i]] += coeff[7] / 500 * worstscore;
//				score[best[i]] += h * worstscore;
			}
			for (int i = 0; i < bestsample; i++) {
				if (score[best[i]] < score[choice]) {
					choice = best[i];
				}
			}
		} else {
			for (int i = 1; i < score.length; i++) {
				if (score[i] < score[choice]) {
					choice = i;
				}
			}
		}

		if (legalMoves[0][0] < 0) {
			return (int)score[choice];
		} else {
			return choice;
		}
	}
	
	public double performance(int workers) {
		double total = 0;
		for (int i = 0; i < 24 / workers; i++) {
			State s = new State();
			while(!s.hasLost()) {
				s.makeMove(pickMove(s,s.legalMoves()));
			}
			total += s.getRowsCleared() ;
		}
		total /= (24 / workers);
		return total;
	}
	
	public double distortedperformance(int workers) {
		double total = 0;
		for (int i = 0; i < 24 / workers; i++) {
			State s = new State();
			while(!s.hasLost()) {
				s.makeMove(pickMove(s,s.legalMoves()));
			}
			total += s.getRowsCleared() ;
		}
		total /= (24 / workers);
		return total;
	}
	
	public double limitperformance(int workers) {
		double total = 0;
		for (int i = 0; i < 24 / workers; i++) {
//			TetrisProject.MainProject.State s = new TetrisProject.MainProject.State(i);
			State s = new State();
			while(!s.hasLost() && s.getTurnNumber() < 1000) {
				s.makeMove(distortMove(s,s.legalMoves()));
			}
			total += s.getRowsCleared();
			if (s.hasLost()) {
				total -= 1000;
			}
			while (!s.hasLost() && s.getTurnNumber() < 1500) {
				int[] top = s.getTop();
				int highest = 0;
				for (int j = 0; j < State.COLS; j++) {
					highest = Math.max(highest, top[j]);
				}
				if (highest < 8) {
					break;
				}
				total--;
				s.makeMove(distortMove(s,s.legalMoves()));
			}
		}
		total /= (24 / workers);
		return total;
	}
	
	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		NewPlayer p = new NewPlayer();
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
			if (s.getTurnNumber() % 1000 == 0) {
				System.out.println(s.getTurnNumber());
				s.draw();
			}
			//s.draw();
			//s.drawNext(0,0);
			/*
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}*/
			
		}
		s.draw();
		System.out.println("You have completed "+s.getRowsCleared()+" rows.");
	}
	
}
