package TetrisProject.MainProject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class NewPlayer {
	double[] coeff = {0.7001550227403642, 0.9196415066719058, 0.6051464900374413, 1.487336707115174, 1.4355850100517278, 0.7337301284074784, 1.152634054422379};

	int[][] copy = new int[State.ROWS][State.COLS];
	int[] copytop = new int[State.COLS];
	double[] prob = {0.5, 0.5};
	int expert = 0;
	public NewPlayer() {
		;
	}
	public NewPlayer(double[] weights) {
		this.coeff = weights;
	}
	
	public void set(double[] weights) {
		this.coeff = weights;
	}
	public void reset() {
		prob[0] = 0.5;
		prob[1] = 0.5;
		expert = 0;
		copy = new int[State.ROWS][State.COLS];
		copytop = new int[State.COLS];
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
			
			score[i] = coeff[0] * (height + 1) - coeff[1] * cleared + coeff[2] * rowtransitions +
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
	
	public int expertPickMove(State s, int[][] legalMoves) {
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
		double[] score2 = new double[moves.length];
		
		double[] regretscore = new double[moves.length];
		
		int[][] newfield = new int[State.ROWS][State.COLS];
		int[][] regretfield = new int[State.ROWS][State.COLS];
		for (int i = 0; i < moves.length; i++) {
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
				score2[i] = 999999;
				regretscore[i] = 999999;
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
			
			score[i] = coeff[0] * (height + 1) - coeff[1] * cleared + coeff[2] * rowtransitions +
					coeff[3] * columntransitions + coeff[4] * holes + coeff[5] * wells + coeff[6] * rowholes;
			
			score2[i] = coeff[7] * (height + 1) - coeff[8] * cleared + coeff[9] * rowtransitions +
					coeff[10] * columntransitions + coeff[11] * holes + coeff[12] * wells + coeff[13] * rowholes;
			
			
			
			
			
			for (int j = 0; j < State.ROWS; j++) {
				for (int k = 0; k < State.COLS; k++) {
					regretfield[j][k] = copy[j][k];
				}
			}
			for (int j = 0; j < top.length; j++) {
				newtop[j] = copytop[j];
			}

			//height if the first column makes contact
			height = top[slot]-pbottom[0];
			//for each column beyond the first in the piece
			for(int c = 1; c < pwidth;c++) {
				height = Math.max(height,top[slot+c]-pbottom[c]);
			}

			//check if game ended
			if(height+pheight >= State.ROWS) {
				regretscore[i] = 999999;
				continue;
			}

			
			//for each column in the piece - fill in the appropriate blocks
			for(int j = 0; j < pwidth; j++) {
				
				//from bottom to top of brick
				for(int h = height+pbottom[j]; h < height+ptop[j] && h < 20; h++) {
					regretfield[h][j+slot] = 1;
				}
			}
			
			//adjust top
			for(int c = 0; c < pwidth; c++) {
				newtop[slot+c]=Math.min(19, height+ptop[c]);
			}
			
			cleared = 0;
			
			//check for full rows - starting at the top
			for(int r = height+pheight-1; r >= height; r--) {
				//check all columns in the row
				boolean full = true;
				for(int c = 0; c < State.COLS; c++) {
					if(regretfield[r][c] == 0) {
						full = false;
						break;
					}
				}
				//if the row was full - remove it and slide above stuff down
				if(full) {
					cleared++;
					//slide down all bricks
					for(int j = r; j < State.ROWS - 1; j++) {
						regretfield[j] = regretfield[j+1];
					}
					regretfield[State.ROWS - 1] = new int[State.COLS];
					//lower the top
					//for each column
					for(int c = 0; c < State.COLS; c++) {
						newtop[c]--;
						while(newtop[c]>=1 && regretfield[newtop[c]-1][c]==0)	newtop[c]--;
					}
				}
			}
			
			rowtransitions = 0;
			rowholes = 0;
			for (int j = 0; j < State.ROWS; j++) {
				boolean havehole = false;
				for (int k = 0; k < State.COLS - 1; k++) {
					if (regretfield[j][k] == 0 && regretfield[j][k + 1] != 0 ||
							regretfield[j][k] != 0 && regretfield[j][k + 1] == 0) {
						rowtransitions++;
					}
					havehole = havehole || regretfield[j][k] == 0;
				}
				havehole = havehole || regretfield[j][State.COLS - 1] == 0;
				if (regretfield[j][0] == 0) {
					rowtransitions++;
				}
				if (regretfield[j][State.COLS - 1] == 0) {
					rowtransitions++;
				}
				if (havehole) {
					rowholes++;
				}
			}
			
			columntransitions = 0;
			holes = 0;
			for (int j = 0; j < State.COLS; j++) {
				for (int k = 0; k < newtop[j]; k++) {
					if (regretfield[k][j] == 0 && regretfield[k + 1][j] != 0 ||
							regretfield[k][j] != 0 && regretfield[k + 1][j] == 0) {
						columntransitions++;
					}
					if (regretfield[k][j] == 0) {
						holes++;
					}
				}
			}
			
			wells = 0;
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
			
			if (expert == 0) {
				regretscore[i] = coeff[0] * (height + 1) - coeff[1] * cleared + coeff[2] * rowtransitions +
						coeff[3] * columntransitions + coeff[4] * holes + coeff[5] * wells + coeff[6] * rowholes;
			} else {
				regretscore[i] = coeff[7] * (height + 1) - coeff[8] * cleared + coeff[9] * rowtransitions +
						coeff[10] * columntransitions + coeff[11] * holes + coeff[12] * wells + coeff[13] * rowholes;
			}
		}

		int choice = 0;
		int choice2 = 0;
		int regretchoice = 0;
		for (int i = 0; i < score.length; i++) {
			if (score[i] < score[choice]) {
				choice = i;
			}
			if (score2[i] < score2[choice2]) {
				choice2 = i;
			}
			if (regretscore[i] < regretscore[regretchoice]) {
				regretchoice = i;
			}
		}
		if (expert == 0) {
			if (regretscore[regretchoice] < score[choice]) {
				prob[expert] /= 2;
				double total = prob[0] + prob[1];
				prob[0] /= total;
				prob[1] /= total;
			}
		} else {
			if (regretscore[regretchoice] < score2[choice2]) {
				prob[expert] /= 2;
				double total = prob[0] + prob[1];
				prob[0] /= total;
				prob[1] /= total;
			}
		}

		expert = (Math.random() < prob[0]) ? 0 : 1;
		int shape = (expert == 0) ? choice : choice2;
		
		for (int i = 0; i < State.ROWS; i++) {
			for (int j = 0; j < State.COLS; j++) {
				copy[i][j] = field[i][j];
			}
		}
		for (int j = 0; j < State.ROWS; j++) {
			for (int k = 0; k < State.COLS; k++) {
				copy[j][k] = field[j][k];
			}
		}
		for (int j = 0; j < top.length; j++) {
			copytop[j] = top[j];
		}

		int orientation = moves[shape][State.ORIENT];
		int slot = moves[shape][State.SLOT];
		
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
			return (expert == 1) ? choice : choice2;
		}

		
		//for each column in the piece - fill in the appropriate blocks
		for(int j = 0; j < pwidth; j++) {
			//from bottom to top of brick
			for(int h = height+pbottom[j]; h < height+ptop[j] && h < 20; h++) {
				copy[h][j+slot] = 1;
			}
		}
		
		//adjust top
		for(int c = 0; c < pwidth; c++) {
			copytop[slot+c]=Math.min(19, height+ptop[c]);
		}
				
		//check for full rows - starting at the top
		for(int r = height+pheight-1; r >= height; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < State.COLS; c++) {
				if(copy[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				//slide down all bricks
				for(int j = r; j < State.ROWS - 1; j++) {
					copy[j] = copy[j+1];
				}
				copy[State.ROWS - 1] = new int[State.COLS];
				//lower the top
				//for each column
				for(int c = 0; c < State.COLS; c++) {
					copytop[c]--;
					while(copytop[c]>=1 && copy[copytop[c]-1][c]==0)	copytop[c]--;
				}
			}
		}

		if (expert == 0) {
			return choice;
		} else {
			return choice2;
		}
	}

	/*
	public int distortMove(TetrisProject.MainProject.State s, int[][] legalMoves) {
		//1 - O, 2 - I, 3 - L, 4 - J, 5 - T, 6 - S, 7 - Z
		int piece = s.getNextPiece();
		int[][] moves = legalMoves;
		int[] top = s.getTop();
		int[][] gameState = s.getField();

		int[] newtop = new int[TetrisProject.MainProject.State.COLS];
		int highest = 0;
		for (int j = 0; j < TetrisProject.MainProject.State.COLS; j++) {
			highest = Math.max(highest, top[j]);
		}

		double[] score = new double[moves.length];
		int cumulativeclear = 0;
		
		
		int[][] newfield = new int[TetrisProject.MainProject.State.ROWS][TetrisProject.MainProject.State.COLS];
		for (int i = 0; i < moves.length; i++) {
			if (cumulativeclear >=2 || Math.random() < 0.01) {
				break;
			}
			//init
			for (int j = 0; j < Math.min(highest + 4, TetrisProject.MainProject.State.ROWS); j++) {
				for (int k = 0; k < TetrisProject.MainProject.State.COLS; k++) {
					newfield[j][k] = gameState[j][k];
				}
			}
			for (int j = 0; j < top.length; j++) {
				newtop[j] = top[j];
			}

			int orientation = moves[i][TetrisProject.MainProject.State.ORIENT];
			int slot = moves[i][TetrisProject.MainProject.State.SLOT];
			
			int pwidth = TetrisProject.MainProject.State.getpWidth()[piece][orientation];
			int[] pbottom = TetrisProject.MainProject.State.getpBottom()[piece][orientation];
			int[] ptop = TetrisProject.MainProject.State.getpTop()[piece][orientation];
			
			int pheight = TetrisProject.MainProject.State.getpHeight()[piece][orientation];

			//height if the first column makes contact
			int height = top[slot]-pbottom[0];
			//for each column beyond the first in the piece
			for(int c = 1; c < pwidth;c++) {
				height = Math.max(height,top[slot+c]-pbottom[c]);
			}

			//check if game ended
			if(height+pheight >= TetrisProject.MainProject.State.ROWS) {
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
				for(int c = 0; c < TetrisProject.MainProject.State.COLS; c++) {
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
					for(int j = r; j < TetrisProject.MainProject.State.ROWS - 1; j++) {
						newfield[j] = newfield[j+1];
					}
					newfield[TetrisProject.MainProject.State.ROWS - 1] = new int[TetrisProject.MainProject.State.COLS];
					//lower the top
					//for each column
					for(int c = 0; c < TetrisProject.MainProject.State.COLS; c++) {
						newtop[c]--;
						while(newtop[c]>=1 && newfield[newtop[c]-1][c]==0)	newtop[c]--;
					}
				}
			}
			
			int rowtransitions = 0;
			int rowholes = 0;
			for (int j = 0; j < TetrisProject.MainProject.State.ROWS; j++) {
				boolean havehole = false;
				for (int k = 0; k < TetrisProject.MainProject.State.COLS - 1; k++) {
					if (newfield[j][k] == 0 && newfield[j][k + 1] != 0 ||
							newfield[j][k] != 0 && newfield[j][k + 1] == 0) {
						rowtransitions++;
					}
					havehole = havehole || newfield[j][k] == 0;
				}
				havehole = havehole || newfield[j][TetrisProject.MainProject.State.COLS - 1] == 0;
				if (newfield[j][0] == 0) {
					rowtransitions++;
				}
				if (newfield[j][TetrisProject.MainProject.State.COLS - 1] == 0) {
					rowtransitions++;
				}
				if (havehole) {
					rowholes++;
				}
			}
			
			int columntransitions = 0;
			int holes = 0;
			for (int j = 0; j < TetrisProject.MainProject.State.COLS; j++) {
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
			for (int j = 1; j < TetrisProject.MainProject.State.COLS - 1; j++) {
				int x = newtop[j - 1] - newtop[j];
				int y = newtop[j + 1] - newtop[j];
				if (x > 0 && y > 0) {
					wells += Math.min(x, y);
				}
			}
			if (newtop[0] < newtop[1]) {
				wells += newtop[1] - newtop[0];
			}
			if (newtop[TetrisProject.MainProject.State.COLS - 1] < newtop[TetrisProject.MainProject.State.COLS - 2]) {
				wells += newtop[TetrisProject.MainProject.State.COLS - 2] - newtop[TetrisProject.MainProject.State.COLS - 1];
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
	}*/
	
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
	/*
	public int fillboard(int[] top, int piece, int move) {
		int[] moves = TetrisProject.MainProject.State.legalMoves[piece][move];
		int slot = moves[TetrisProject.MainProject.State.SLOT];
		int orient = moves[TetrisProject.MainProject.State.ORIENT];
		int[] pbottom = TetrisProject.MainProject.State.getpBottom()[piece][orient];
		int pwidth = TetrisProject.MainProject.State.getpWidth()[piece][orient];
		int[] ptop = TetrisProject.MainProject.State.getpTop()[piece][orient];
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
	
	public void setboard(TetrisProject.MainProject.State s) {
		for (int j = TetrisProject.MainProject.State.ROWS - 5; j >= 0; j--) {
			for (int k = 1; k < TetrisProject.MainProject.State.COLS - 1; k++) {
				copy[j + (int) (Math.random() * 4)][k - 1] += copy[j][k] / 4;
				copy[j + (int) (Math.random() * 4)][k] += copy[j][k] / 2;
				copy[j + (int) (Math.random() * 4)][k + 1] += copy[j][k] / 4;
				copy[j][k] = 0;
			}
		}
		int[][] destination = s.getField();
		int[] top = s.getTop();
		for (int j = 0; j < TetrisProject.MainProject.State.ROWS; j++) {
			for (int k = 0; k < TetrisProject.MainProject.State.COLS; k++) {
				destination[j][k] = copy[j][k] > 50 ? 1 : 0;
				copy[j][k] = 0;
			}
		}
		for(int r = TetrisProject.MainProject.State.ROWS - 1; r >= 0; r--) {
			//check all columns in the row
			boolean full = true;
			for(int c = 0; c < TetrisProject.MainProject.State.COLS; c++) {
				if(destination[r][c] == 0) {
					full = false;
					break;
				}
			}
			//if the row was full - remove it and slide above stuff down
			if(full) {
				//slide down all bricks
				for(int j = r; j < TetrisProject.MainProject.State.ROWS - 1; j++) {
					destination[j] = destination[j+1];
				}
				destination[TetrisProject.MainProject.State.ROWS - 1] = new int[TetrisProject.MainProject.State.COLS];
			}
		}
		for (int k = 0; k < TetrisProject.MainProject.State.COLS; k++) {
			top[k] = TetrisProject.MainProject.State.ROWS - 1;
			while (top[k] >= 1 && destination[top[k] - 1][k] <= 0) top[k]--;
		}
	}
	
	public double accel(int workers) {
		double total = 0;
		int move;
		for (int i = 0; i < 24 / workers; i++) {
			for (int j = 0; j < TetrisProject.MainProject.State.ROWS; j++) {
				for (int k = 1; k < TetrisProject.MainProject.State.COLS - 1; k++) {
					copy[j][k] = 0;
				}
			}
			TetrisProject.MainProject.State s = new TetrisProject.MainProject.State();
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
	}*/
	
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

	public int singleperformance(int seed) {
		State s = new State(seed);
		int height = 0;
		int move;
		while(!s.hasLost() && height < 11) {
			move = pickMove(s,s.legalMoves());
			height = acquireheight(s.getTop(), s.nextPiece, move);
			s.makeMove(move);
		}
		return s.getRowsCleared() ;
	}

	public int singlefull(int seed) {
		State s = new State(seed);
		while(!s.hasLost()) {
			s.makeMove(pickMove(s,s.legalMoves()));
		}
		return s.getRowsCleared() ;
	}

	public double performance() {
		double total = 0;
		for (int i = 0; i < 24; i++) {
			State s = new State();
			while(!s.hasLost()) {
				s.makeMove(pickMove(s,s.legalMoves()));
			}
			total += s.getRowsCleared();
		}
		return total / 24;
	}
	
	public double distribution(int seed) {
		int[] results = new int[State.ROWS];
		int move;
		State s = new State(seed);
		while(!s.hasLost() && s.getTurnNumber() < 100000) {
			move = pickMove(s,s.legalMoves());
			//move = expertPickMove(s,s.legalMoves());
			results[acquireheight(s.getTop(), s.nextPiece, move)] += 1;
			s.makeMove(move);
		}
		int total = 0;
		double ratio = 0;
		for (int l = 0; l < State.ROWS - 1; l++) {
			if (results[l + 1] > 5) {
				total++;
				ratio += results[l] / results[l + 1];
			}
		}
		if (total > 0) {
			return ratio / total;
		} else {
			return 0;
		}
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

	public static void main(String[] args) {
		//threaditeration();
		State s = new State();
		new TFrame(s);
		NewPlayer p = new NewPlayer();
		//System.out.println(p.performance());
		
		//System.out.println(p.distribution(1));
		while(!s.hasLost()) {
			s.makeMove(p.pickMove(s,s.legalMoves()));
//			s.makeMove(p.expertPickMove(s,s.legalMoves()));
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
	
	private static void threaditeration() {
		String line = null;

		try {
			FileReader fileReader = new FileReader("test.o");
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			while((line = bufferedReader.readLine()) != null) {
				bufferedReader.readLine();
				bufferedReader.readLine();
				bufferedReader.readLine();
				line = bufferedReader.readLine();
				String[] tokens = line.split(" ");
				double[] weights = new double[tokens.length];
				for (int i = 0; i < tokens.length; i++) {
					weights[i] = Double.parseDouble(tokens[i]);
				}
				NewPlayer p = new NewPlayer(weights);
				System.out.println(p.performance());
			}
			bufferedReader.close();
        }
		catch(FileNotFoundException ex) {
			System.out.println("Unable to open file '");
			ex.printStackTrace();
		}
		catch(IOException ex) {
			System.out.println("Error reading file '");                  
			ex.printStackTrace();
		}
	}
}
