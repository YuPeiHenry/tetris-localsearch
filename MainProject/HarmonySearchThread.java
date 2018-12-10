package TetrisProject.MainProject;

import java.util.concurrent.Semaphore;

public class HarmonySearchThread extends Thread {
	private double[][] weights;
	public Semaphore[] s;
	public double[] rewards;
	
	public int ID;
	private NewPlayer player;
	
	public HarmonySearchThread(int ID, double[][] weights, Semaphore[] s) {
		this.ID = ID;
		this.weights = weights;
		this.s = s;
		rewards = new double[weights.length];
		
		player = new NewPlayer();
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				s[0].acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			process();
			s[1].release();
		}
	}
	
	private void process() {
		for (int i = 0; i < weights.length; i++) {
			player.set(weights[i]);
			rewards[i] = player.singlefull(ID);
		}
	}
}