package TetrisProject.MainProject;

public class BasePlayer {
	double[] coeff = {2.2035360980223144, -3.471755328723769, 1.3676685021788642,
			3.305503210896087, 9.731417590309876, 4.595010379399024
	};
	
	int[][] copy = new int[State.ROWS][State.COLS];
	public BasePlayer() {
		;
	}
	public BasePlayer(double[] weights) {
		this.coeff = weights;
	}
	
	public void set(double[] weights) {
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
				float avgDiff = 0;
				int maxHeight = 0;
				for (int j = 0; j < State.COLS; j++) {
					sum += newtop[j];
					maxHeight = Math.max(maxHeight, newtop[j]);
				}

				float meanHeight = (float) sum / State.COLS;
				for (int j = 0; j < State.COLS; j++) {
					avgDiff += Math.abs(meanHeight - height);
				}

				for (int column = 0; column < State.COLS; ++column) {
					if (newtop[column] > maxHeight) {
						maxHeight = height;
					}
				}
				
				int numFaults = 0;
				for (int x = 0; x < State.COLS; ++x) {
					for (int y = newtop[x] - 1; y >= 0; --y) {
						if (newfield[y][x] == 0) {
							++numFaults;
						}
					}
				}

				int sumOfPitDepths = 0;
				int pitHeight;
				int leftOfPitHeight;
				int rightOfPitHeight;

				// pit depth of first column
				pitHeight = newtop[0];
				rightOfPitHeight = newtop[1];
				int diff = rightOfPitHeight - pitHeight;
				if (diff > 2) {
					sumOfPitDepths += diff;
				}
				for (int col = 0; col < State.COLS - 2; col++) {
					leftOfPitHeight = newtop[col];
					pitHeight = newtop[col + 1];
					rightOfPitHeight = newtop[col + 2];

					int leftDiff = leftOfPitHeight - pitHeight;
					int rightDiff = rightOfPitHeight - pitHeight;
					int minDiff = leftDiff < rightDiff ? leftDiff : rightDiff;

					if (minDiff > 2) {
						sumOfPitDepths += minDiff;
					}
				}
				// pit depth of last column
				pitHeight = newtop[State.COLS - 1];
				leftOfPitHeight = newtop[State.COLS - 2];
				diff = leftOfPitHeight - pitHeight;
				if (diff > 2) {
					sumOfPitDepths += diff;
				}

				int roughness = 0;
				for (int j = 0; j < State.COLS - 1; j++) {
					roughness += Math.abs(newtop[j] - newtop[j + 1]);
				}
				
				score[i] = coeff[0] * maxHeight + coeff[1] * avgDiff / State.COLS -
						coeff[2] * cleared * cleared + coeff[3] * numFaults * numFaults +
						coeff[4] * sumOfPitDepths + coeff[5] * roughness;
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
			
			int rowtransitions = 0;
			int rowholes = 0;
			for (int j = 0; j < State.ROWS; j++) {
				boolean havehole = false;
				for (int k = 0; k < State.COLS - 1; k++) {
					if (newfield[j][k] == 0 && newfield[j][k + 1] != 0 ||
							newfield[j][k] != 0 && newfield[j][k + 1] == 0) {
						rowtransitions++;
					}
					havehole = havehole || newfield[j][k] == 0;
				}
				havehole = havehole || newfield[j][State.COLS - 1] == 0;
				if (newfield[j][0] == 0) {
					rowtransitions++;
				}
				if (newfield[j][State.COLS - 1] == 0) {
					rowtransitions++;
				}
				if (havehole) {
					rowholes++;
				}
			}
			
			int columntransitions = 0;
			int holes = 0;
			for (int j = 0; j < State.COLS; j++) {
				for (int k = 0; k < newtop[j]; k++) {
					if (newfield[k][j] == 0 && newfield[k + 1][j] != 0 ||
							newfield[k][j] != 0 && newfield[k + 1][j] == 0) {
						columntransitions++;
					}
					if (newfield[k][j] == 0) {
						holes++;
					}
				}
			}
			
			int wells = 0;
			for (int j = 1; j < State.COLS - 1; j++) {
				int x = newtop[j - 1] - newtop[j];
				int y = newtop[j + 1] - newtop[j];
				if (x > 0 && y > 0) {
					wells += Math.min(x, y);
				}
			}
			if (newtop[0] < newtop[1]) {
				wells += newtop[1] - newtop[0];
			}
			if (newtop[State.COLS - 1] < newtop[State.COLS - 2]) {
				wells += newtop[State.COLS - 2] - newtop[State.COLS - 1];
			}
			
			score[i] = -999999 + coeff[0] * (height + 1) - coeff[1] * cleared + coeff[2] * rowtransitions +
					coeff[3] * columntransitions + coeff[4] * holes + coeff[5] * wells + coeff[6] * rowholes;

		}

		int choice = 0;
		for (int i = 0; i < score.length; i++) {
			if (score[i] < score[choice]) {
				choice = i;
			}
		}
		return choice;
	}
	
	public int acquireheight(int[] top, int piece, int move) {
		int[] moves = State.legalMoves[piece][move];
		int slot = moves[State.SLOT];
		int orient = moves[State.ORIENT];
		int[] pbottom = State.getpBottom()[piece][orient];
		int pwidth = State.getpWidth()[piece][orient];
		//height if the first column makes contact
		int height = top[slot]-pbottom[0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pwidth;c++) {
			height = Math.max(height,top[slot+c]-pbottom[c]);
		}

		return height;
	}
	
	public int fillboard(int[] top, int piece, int move) {
		int[] moves = State.legalMoves[piece][move];
		int slot = moves[State.SLOT];
		int orient = moves[State.ORIENT];
		int[] pbottom = State.getpBottom()[piece][orient];
		int pwidth = State.getpWidth()[piece][orient];
		int[] ptop = State.getpTop()[piece][orient];
		//height if the first column makes contact
		int height = top[slot]-pbottom[0];
		//for each column beyond the first in the piece
		for(int c = 1; c < pwidth;c++) {
			height = Math.max(height,top[slot+c]-pbottom[c]);
		}

		//for each column in the piece - fill in the appropriate blocks
		for(int j = 0; j < pwidth; j++) {
			
			//from bottom to top of brick
			for(int h = height+pbottom[j]; h < height+ptop[j] && h < 20; h++) {
				copy[h][j+slot] += 1;
			}
		}
		return height;
	}
	
	public void setboard(State s) {
		for (int j = State.ROWS - 5; j >= 0; j--) {
			for (int k = 1; k < State.COLS - 1; k++) {
				copy[j + (int) (Math.random() * 4)][k - 1] += copy[j][k] / 4;
				copy[j + (int) (Math.random() * 4)][k] += copy[j][k] / 2;
				copy[j + (int) (Math.random() * 4)][k + 1] += copy[j][k] / 4;
				copy[j][k] = 0;
			}
		}
		int[][] destination = s.getField();
		int[] top = s.getTop();
		for (int j = 0; j < State.ROWS; j++) {
			for (int k = 0; k < State.COLS; k++) {
				destination[j][k] = copy[j][k] > 50 ? 1 : 0;
				copy[j][k] = 0;
			}
		}
		for(int r = State.ROWS - 1; r >= 0; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < State.COLS; c++) {
				if(destination[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				//slide down all bricks
				for(int j = r; j < State.ROWS - 1; j++) {
					destination[j] = destination[j+1];
				}
				destination[State.ROWS - 1] = new int[State.COLS];
			}
		}
		for (int k = 0; k < State.COLS; k++) {
			top[k] = State.ROWS - 1;
			while (top[k] >= 1 && destination[top[k] - 1][k] <= 0) top[k]--;
		}
	}
	
	public double accel(int workers) {
		double total = 0;
		int move;
		for (int i = 0; i < 24 / workers; i++) {
			for (int j = 0; j < State.ROWS; j++) {
				for (int k = 1; k < State.COLS - 1; k++) {
					copy[j][k] = 0;
				}
			}
			State s = new State();
			while(!s.hasLost()) {
				move = pickMove(s,s.legalMoves());
				//move = distortMove(s,s.legalMoves());
				s.makeMove(move);
				if (s.getTurnNumber() % 500 == 0) {
					setboard(s);
				}
			}
			total += s.getRowsCleared() ;
		}
		total /= (24 / workers);
		return total;
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

	public int singleperformance() {
		State s = new State();
		while(!s.hasLost()) {
			s.makeMove(pickMove(s,s.legalMoves()));
		}
		return s.getRowsCleared() ;
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
				s.makeMove(pickMove(s,s.legalMoves()));
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
				s.makeMove(pickMove(s,s.legalMoves()));
			}
			if (s.hasLost()) {
				total -= 1000;
			}
		}
		total /= (24 / workers);
		return total;
	}
	
/*	public int[] distribution(int workers) {
		int[] results = new int[TetrisProject.MainProject.State.ROWS];
		int move;
		for (int i = 0; i < 24 / workers; i++) {
			TetrisProject.MainProject.State s = new TetrisProject.MainProject.State();
			while(!s.hasLost() && s.getTurnNumber() < 1000) {
				move = pickMove(s,s.legalMoves());
				results[acquireheight(s.getTop(), s.nextPiece, move)] += 1;
				s.makeMove(move);
			}
		}
		return results;
	}
*/
	public double distribution(int workers) {
		int[] results = new int[State.ROWS];
		int move;
		for (int i = 0; i < 24 / workers; i++) {
			State s = new State();
			while(!s.hasLost() && s.getTurnNumber() < 8000) {
				move = pickMove(s,s.legalMoves());
				results[acquireheight(s.getTop(), s.nextPiece, move)] += 1;
				s.makeMove(move);
			}
		}
		int total = 0;
		double x = 0;
		double x2 = 0;
		for (int l = 0; l < State.ROWS; l++) {
			total += results[l];
			x += l * results[l];
			x2 += l * l * results[l];
		}
		x /= total;
		x2 /= total;
		return 1 / Math.sqrt(Math.abs(x * x - x2));
	}

	public static void main(String[] args) {
		State s = new State();
		new TFrame(s);
		BasePlayer p = new BasePlayer();
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
