package TetrisProject.MainProject;

import java.util.concurrent.Semaphore;

/**
 * Provided by Mingrui.
 */

public class HarmonySearch {
    private static final int numFeatures = 7;
    private static final int START_T = 20000;
    private static final boolean IS_SEARCHING = true;

    public static final int workers = 24;

    private void search(double[][] weights, Semaphore[][] s, HarmonySearchThread[] threads) {

        double[] bestWeight = new double[numFeatures];
        double bestReward = 0;

        double[] rewards = new double[weights.length];
        int t = START_T;
        try {
            while (t > 0) {
                for (int i = 0; i < weights.length; i++) {
                    for (int j = 0; j < numFeatures; j++) {
                        weights[i][j] = Math.random() * 10;
                    }
                }
                for (int i = 0; i < workers; i++) {
                    s[i][0].release();
                }

                for (int i = 0; i < weights.length; i++) {
                    rewards[i] = 0;
                }

                for (int i = 0; i < workers; i++) {
                    s[i][1].acquire();
                    for (int j = 0; j < weights.length; j++) {
                        rewards[j] += threads[i].rewards[j] / workers;
                    }
                }

                int maxi = 0;
                for (int i = 0; i < weights.length; i++) {
                    if (rewards[i] > rewards[maxi]) {
                        maxi = i;
                    }
                }

                System.out.println();
                System.out.println(START_T - t + ": " + bestReward);
                System.out.println();
                for (int i = 0; i < numFeatures; i++) {
                    System.out.print(bestWeight[i] + " ");
                }

                if (rewards[maxi] > bestReward) {
                    for (int i = 0; i < numFeatures; i++) {
                        bestWeight[i] = weights[maxi][i];
                    }
                    bestReward = rewards[maxi];
                }

                t--;
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void verify(double[][] weights, Semaphore[][] s, HarmonySearchThread[] threads) {
        double[] rewards = new double[weights.length];
        try {
            for (int i = 0; i < workers; i++) {
                s[i][0].release();
            }

            for (int i = 0; i < weights.length; i++) {
                rewards[i] = 0;
            }

            for (int i = 0; i < workers; i++) {
                s[i][1].acquire();
                for (int j = 0; j < weights.length; j++) {
                    rewards[j] += threads[i].rewards[j] / workers;
                }
            }

            for (int i = 0; i < weights.length; i++) {
                System.out.println(rewards[i]);
            }
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void main(String[] Args) {
//		double[][] weights = new double[1][numFeatures];
        double[][] weights = {{0.494303423166275, 0.5696415066719055, 0.4051464900374412, 1.0873367071151736, 0.5945799827575684, 0.3837301284074783, 0.7526340544223787},
                {0.500155022740364, 0.6696415066719056, 0.4551464900374412, 1.1373367071151737, 0.9855850100517274, 0.5337301284074782, 0.8026340544223788},
                {0.544303423166275, 0.7196415066719056, 0.4551464900374412, 1.2373367071151737, 1.0355850100517274, 0.6337301284074783, 0.8526340544223788},
                {0.5501550227403641, 0.7196415066719056, 0.4551464900374412, 1.1873367071151737, 1.0355850100517274, 0.6837301284074784, 0.9026340544223789},
                {0.594303423166275, 0.7696415066719057, 0.5051464900374412, 1.1373367071151737, 1.0855850100517275, 0.5337301284074782, 0.8026340544223788},
                {0.6001550227403641, 0.7696415066719057, 0.5051464900374412, 1.1873367071151737, 1.1855850100517276, 0.6837301284074784, 0.8526340544223788},
                {0.6501550227403642, 0.8196415066719057, 0.5314195156097412, 1.3373367071151738, 1.2355850100517276, 0.5837301284074783, 1.0026340544223789},
                {0.6501550227403642, 0.8696415066719058, 0.6051464900374413, 1.3373367071151738, 1.3855850100517277, 0.6837301284074784, 1.0026340544223789},
                {0.6501550227403642, 0.8696415066719058, 0.5551464900374412, 1.3873367071151739, 1.2855850100517277, 0.6837301284074784, 1.102634054422379},
                {0.7001550227403642, 0.9196415066719058, 0.6051464900374413, 1.487336707115174, 1.4355850100517278, 0.7337301284074784, 1.152634054422379}};
        HarmonySearch harmonySearch = new HarmonySearch();

        Semaphore[][] s = new Semaphore[workers][];
        HarmonySearchThread[] threads = new HarmonySearchThread[workers];
        for (int i = 0; i < workers; i++) {
            s[i] = new Semaphore[2];
            s[i][0] = new Semaphore(0);
            s[i][1] = new Semaphore(0);
            threads[i] = new HarmonySearchThread(i, weights, s[i]);
            threads[i].start();
        }
        harmonySearch.verify(weights, s, threads);

//		harmonySearch.search(WEIGHTS, s, threads);
    }
}
