/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uniroma1.sbn.finalproject.gunturi.italianreferendum2016.AnalyticalTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import uniroma1.sbn.finalproject.gunturi.italianreferendum2016.Manager.TweetsIndexManager;

/**
 * @author Vamsi Gunturi
 */
public class ActionReaction {

    public static void compareTimeSeries(String[] words, String title) throws IOException {

        // Set the time interval to 3 hours
        double stepSize = 3600000d;

        // Initialize a TweetsIndexManager for the index of all yes tweets based on yes pols
        TweetsIndexManager yesTim = new TweetsIndexManager("index/AllYesTweetsIndex");
        // Initialize a TweetsIndexManager for the index of all no tweets based on no pols
        TweetsIndexManager noTim = new TweetsIndexManager("index/AllNoTweetsIndex");

        // Get the NO frequency
        // Store all the arrays for the no here
        ArrayList<double[]> wordsFreqNo = new ArrayList<>();
        // Store all the arrays for the yes here
        ArrayList<double[]> wordsFreqYes = new ArrayList<>();

        // fill the array iterating for every word
        for (String word : words) {

            // add the frequency for the word itself
            wordsFreqNo.add(noTim.getTermTimeSeries(word, "tweetText", stepSize));
            wordsFreqYes.add(yesTim.getTermTimeSeries(word, "tweetText", stepSize));

            // now add the frequency of the hashtag
            String hashWord = "#" + word;
            wordsFreqNo.add(noTim.getTermTimeSeries(hashWord, "hashtags", stepSize));
            wordsFreqYes.add(yesTim.getTermTimeSeries(hashWord, "hashtags", stepSize));

        }

        // sum the results up to make a unique list
        double[] WordsFreqNoTotal = addAllTerms(wordsFreqNo);
        double[] WordsFreqYesTotal = addAllTerms(wordsFreqYes);

        // Let's start the plot preparation
        PlotTool plot = new PlotTool();

        // Rescale tweets frequency data on a log scale
        double[] yYes = new double[WordsFreqYesTotal.length];
        double[] yNo = new double[WordsFreqNoTotal.length];
        double[] x = new double[WordsFreqNoTotal.length];

        for (int i = 0; i < WordsFreqNoTotal.length; i++) {
            x[i] = i + 1;
            yYes[i] = Math.log(1 + WordsFreqYesTotal[i]);
            yNo[i] = Math.log(1 + WordsFreqNoTotal[i]);
        }

        // Create plot
        plot.createPlot("Yes", x, yYes, "No", x, yNo, title, "Time", "Frequency");
        plot.setBounds(0, 0D, 242D);
        plot.setBounds(1, 0D, 5.5D);
        plot.getPlot(1200, 600);

    }

    private static double[] addAllTerms(ArrayList<double[]> allTerms) {

        // create the vector that will store the result of the final sum
        double[] sum = new double[allTerms.get(0).length];
        Arrays.fill(sum, 0);

        // add all of them to sum (one at a time)
        for (double[] vec : allTerms) {

            sum = addTwoVectors(sum, vec);

        }

        return sum;
    }

    private static double[] addTwoVectors(double[] vec1, double[] vec2) {

        double[] out = new double[vec1.length];

        for (int i = 0; i < vec1.length; i++) {

            out[i] = vec1[i] + vec2[i];

        }

        return out;

    }

}
