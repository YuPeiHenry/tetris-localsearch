package TetrisProject.MainProject;

public class BaselinePlayer {
	double[] coeff;
	
	public BaselinePlayer() {
		;
	}
	public BaselinePlayer(double[] weights) {
		this.coeff = weights;
	}
	
	//implement this function to have currentScore working system
	public int pickMove(State s, int[][] legalMoves) {
		//1 - O, 2 - I, 3 - L, 4 - J, 5 - T, 6 - S, 7 - Z
		int piece = s.getNextPiece();
		int[][] moves = legalMoves;
		int[] top = s.getTop();
		int[][] field = s.getField();

		int[] newtop = new int[State.COLS];
		int highest = 0;
		for (int j = 0; j < State.COLS; j++) {
			highest = Math.max(highest, top[j]);
		}

		double[] score = new double[moves.length];
		
		
		int[][] newfield = new int[State.ROWS][State.COLS];
		for (int i = 0; i < moves.length; i++) {
			if (true) {
				//init
				for (int j = 0; j < Math.min(highest + 4, State.ROWS); j++) {
					for (int k = 0; k < State.COLS; k++) {
						newfield[j][k] = field[j][k];
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
					for(int h = height+pbottom[j]; h < height+ptop[j] && h < 20; h++) {
						newfield[h][j+slot] = 1;
					}
				}
				
				//adjust top
				for(int c = 0; c < pwidth; c++) {
					newtop[slot+c]=Math.min(19, height+ptop[c]);
				}
				
				int cleared = 0;
				
				//check for full rows - starting at the top
				for(int r = height+pheight-1; r >= height; r--) {
					//check all columns in the row
					boolean full = true;
					for(int c = 0; c < State.COLS; c++) {
						if(newfield[r][c] == 0) {
							full = false;
							break;
						}
					}
					//if the row was full - remove it and slide above stuff down
					if(full) {
						cleared++;
						//slide down all bricks
						for(int j = r; j < State.ROWS - 1; j++) {
							newfield[j] = newfield[j+1];
						}
						newfield[State.ROWS - 1] = new int[State.COLS];
						//lower the top
						//for each column
						for(int c = 0; c < State.COLS; c++) {
							newtop[c]--;
							while(newtop[c]>=1 && newfield[newtop[c]-1][c]==0)	newtop[c]--;
						}
					}
				}
				
				int sum = 0;
				int max = 0;
				int min = 0;
				int empty = 0;
				int[] empties = new int[State.COLS];
				int cover = 0;
				int chunk = 0;
				for (int j = 0; j < State.COLS; j++) {
					empty = 0;
					if (max < newtop[j]){
						max = newtop[j];
					}
					if(min>newtop[j]){
						min = newtop[j];
					}
					
					sum += coeff[0] * newtop[j] * newtop[j];

					for (int k = 0; k < newtop[j]; k++) {
						if (newfield[k][j] == 0){
							empty += 1;
							if (newfield[k + 1][j] != 0)
								cover += newtop[j] - k;
								chunk ++;
						}
					}
					empties[j] = empty;
					sum += coeff[1] * empty;
				}
				sum += coeff[2] * cover;
				sum += coeff[3] * chunk;
				
				for (int j = 0; j < State.COLS - 1; j++) {
					int abs = Math.abs(newtop[j] - newtop[j + 1]);
					sum += coeff[4] * abs * abs;
				}

				sum += coeff[5] * (max-min);
				sum += coeff[6] * max;
				sum += coeff[7] * var(empties);
				sum += coeff[8] * var(newtop);
				score[i] = sum;
			}
		}

		int choice = 0;
		for (int i = 0; i < score.length; i++) {
			if (score[i] < score[choice]) {
				choice = i;
			}
		}
		return choice;
	}
	
	public int var(int[] arr) {
		float avg = 0, sum = 0;
		int len = 0;
		for (int i : arr) {
			avg += i;
			if(i!=0) len++;
		}
		avg /= len;
		for (int i : arr) {
			sum += Math.pow(avg - i, 2);
		}
		len--;
		return (int) (sum / len);
	}

	
	public int distortMove(State s, int[][] legalMoves) {
		//1 - O, 2 - I, 3 - L, 4 - J, 5 - T, 6 - S, 7 - Z
		int piece = s.getNextPiece();
		int[][] moves = legalMoves;
		int[] top = s.getTop();
		int[][] field = s.getField();

		int[] newtop = new int[State.COLS];
		int highest = 0;
		for (int j = 0; j < State.COLS; j++) {
			highest = Math.max(highest, top[j]);
		}

		double[] score = new double[moves.length];
		int cumulativeclear = 0;
		
		
		int[][] newfield = new int[State.ROWS][State.COLS];
		for (int i = 0; i < moves.length; i++) {
			if (cumulativeclear >=2 || Math.random() < 0.01) {
				break;
			}
			//init
			for (int j = 0; j < Math.min(highest + 4, State.ROWS); j++) {
				for (int k = 0; k < State.COLS; k++) {
					newfield[j][k] = field[j][k];
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
				for(int h = height+pbottom[j]; h < height+ptop[j] && h < 20; h++) {
					newfield[h][j+slot] = 1;
				}
			}
			
			//adjust top
			for(int c = 0; c < pwidth; c++) {
				newtop[slot+c]=Math.min(19, height+ptop[c]);
			}
			
			int cleared = 0;
			
			//check for full rows - starting at the top
			for(int r = height+pheight-1; r >= height; r--) {
				//check all columns in the row
				boolean full = true;
				for(int c = 0; c < State.COLS; c++) {
					if(newfield[r][c] == 0) {
						full = false;
						break;
					}
				}
				//if the row was full - remove it and slide above stuff down
				if(full) {
					cumulativeclear++;
					cleared++;
					//slide down all bricks
					for(int j = r; j < State.ROWS - 1; j++) {
						newfield[j] = newfield[j+1];
					}
					newfield[State.ROWS - 1] = new int[State.COLS];
					//lower the top
					//for each column
					for(int c = 0; c < State.COLS; c++) {
						newtop[c]--;
						while(newtop[c]>=1 && newfield[newtop[c]-1][c]==0)	newtop[c]--;
					}
				}
			}
			
			int sum = 0;
			int max = 0;
			int min = 0;
			int empty = 0;
			int[] empties = new int[State.COLS];
			int cover = 0;
			int chunk = 0;
			for (int j = 0; j < State.COLS; j++) {
				empty = 0;
				if (max < newtop[j]){
					max = newtop[j];
				}
				if(min>newtop[j]){
					min = newtop[j];
				}
				
				sum += coeff[0] * newtop[j] * newtop[j];

				for (int k = 0; k < newtop[j]; k++) {
					if (newfield[k][j] == 0){
						empty += 1;
						if (newfield[k + 1][j] != 0)
							cover += newtop[j] - k;
							chunk ++;
					}
				}
				empties[j] = empty;
				sum += coeff[1] * empty;
			}
			sum += coeff[2] * cover;
			sum += coeff[3] * chunk;
			
			for (int j = 0; j < State.COLS - 1; j++) {
				int abs = Math.abs(newtop[j] - newtop[j + 1]);
				sum += coeff[4] * abs * abs;
			}

			sum += coeff[5] * (max-min);
			sum += coeff[6] * max;
			sum += coeff[7] * var(empties);
			sum += coeff[8] * var(newtop);
			score[i] = -999999 + sum;

		}

		int choice = 0;
		for (int i = 0; i < score.length; i++) {
			if (score[i] < score[choice]) {
				choice = i;
			}
		}
		return choice;
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
		BaselinePlayer p = new BaselinePlayer();
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
