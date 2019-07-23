package sbn.gunturi.algorithms;

import it.stilo.g.structures.WeightedGraph;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class compute comunityLPA
 * @author Vamsi Gunturi
 */
public class ComunityLPA implements Runnable {

    private static final Logger logger = LogManager.getLogger(ComunityLPA.class);

    private static Random rnd;
    private WeightedGraph g;

    private int chunk;
    private int runner;
    private CountDownLatch barrier;

    private int[] labels;
    private int[] list = null;

    private ComunityLPA(WeightedGraph g, CountDownLatch cb, int[] labels, int chunk, int runner) {
        this.g = g;
        this.runner = runner;
        this.barrier = cb;
        this.labels = labels;
        this.chunk = chunk;
    }

    // Initialize labels list
    private boolean initList() {
        // Check that the list is not been initialized yet
        if (list == null) {
            // Partitioning over worker
            list = new int[(g.in.length / runner) + runner];
            
            // Iterator over the neighbours
            int j = 0;
            
            // For each node
            for (int i = chunk; i < g.in.length; i += runner) {
                // If the node has positive indegree and the label already assigned is 0
                // MEMO: The interest is in nodes with indegree positive that have
                //       not a yes or no label yet, that are not our authorities, 
                //       hubs or brokers. 
                if (g.in[i] != null && labels[i] == 0) {
                    // Save this node in the list of nodes that need a label
                    list[j] = i;
                    j++;
                }
            }
            // Resize the list
            list = Arrays.copyOf(list, j);

            //Shuffle
            for (int i = 0; i < list.length; i++) {
                for (int z = 0; z < 10; z++) {
                    int randomPosition = rnd.nextInt(list.length);
                    int temp = list[i];
                    list[i] = list[randomPosition];
                    list[randomPosition] = temp;
                }
            }

            return true;
        }
        return false;
    }

    public void run() {
        if (!initList()) {
            for (int i = 0; i < list.length; i++) {
                int[] near = g.in[list[i]];
                int[] nearLabs = new int[near.length];
                for (int x = 0; x < near.length; x++) {
                    nearLabs[x] = labels[near[x]];
                }

                int bl = bestLabel(nearLabs);

                if (bl != -1) {
                    labels[list[i]] = bl;
                }
            }
        }
        barrier.countDown();
    }

    // Detect the label of a node looking at its neighbours
    public static int bestLabel(int[] neighborhood) {
        // Counter for yes and no neighbours
        int yes = 0;
        int no = 0;

        // Each neighbour of the node
        for (int i = 0; i < neighborhood.length; i++) {
            // If the neighbour has label yes
            if (neighborhood[i] == 1) {
                // Add one to the yes counter
                yes++;
            // Else if the neighbour has label no
            } else if (neighborhood[i] == 2) {
                // Add one to the no counter
                no++;
            }
        }

        // If the node has not yes and no neighbours
        if (yes == 0 && no == 0) {
            // Mantain the actual label of the node
            return -1;
        // Otherwise assign the label with higher value
        } else {
            if (yes > no) {
                return 1;
            } else if (yes < no) {
                return 2;
            // In case of draw select it at random
            } else {
                return rnd.nextInt(2) + 1;
            }
        }
    }

    public static int[] compute(final WeightedGraph g, double threshold, int runner, int[] initLabels) {

        ComunityLPA.rnd = new Random(123454L);
        // Get initial label values
        int[] labels = initLabels;
        int[] newLabels = labels;
        int iter = 0;

        long time = System.nanoTime();
        CountDownLatch latch = null;

        ComunityLPA[] runners = new ComunityLPA[runner];

        for (int i = 0; i < runner; i++) {
            runners[i] = new ComunityLPA(g, latch, labels, i, runner);
        }

        ExecutorService ex = Executors.newFixedThreadPool(runner);

        do {
            iter++;
            labels = newLabels;
            newLabels = Arrays.copyOf(labels, labels.length);
            latch = new CountDownLatch(runner);

            //Label Propagation
            for (int i = 0; i < runner; i++) {
                runners[i].barrier = latch;
                runners[i].labels = newLabels;
                ex.submit(runners[i]);
            }
            try {
                latch.await();
            } catch (InterruptedException e) {
                logger.debug(e);
            }
            
        } while (smoothEnd(labels, newLabels, iter, threshold));

        ex.shutdown();

        logger.info(((System.nanoTime() - time) / 1000000000d) + "\ts");
        return labels;
    }

    private static boolean smoothEnd(int[] labels, int[] newLabels, int iter, double threshold) {
        if (iter < 2) {
            return true;
        }

        int k = 3;

        if (iter > k) {
            int equality = 0;

            for (int i = 0; i < labels.length; i++) {
                if (labels[i] == newLabels[i]) {
                    equality++;
                }
            }
            double currentT = (equality / ((double) labels.length));

            return !(currentT >= threshold);
        }
        return !Arrays.equals(labels, newLabels);
    }
}
