package uniroma1.sbn.finalproject.gunturi.italianreferendum2016.AnalyticalTools;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JFrame;
import org.math.plot.Plot2DPanel;
import org.math.plot.plotObjects.BaseLabel;

/**
 * Class that manage the creation and visualization of plots.
 * @author Vamsi Gunturi
 */
public class PlotTool {

    private Plot2DPanel plot;

    /**
     * Constructor that initialize a new plot.
     */
    public PlotTool() {
        // Create the plot
        plot = new Plot2DPanel();
        plot.getAxis(0).setLabelPosition(0.5, -0.15);
        plot.getAxis(1).setLabelPosition(-0.15, 0.5);
        plot.addLegend("NORTH");
    }
    
    /**
     * Method for the creation of plots for two curves.
     * @param l1 label of curve 1
     * @param x1 x values of curve 1
     * @param y1 y values of curve 1
     * @param l2 label of curve 2
     * @param x2 x values of curve 2
     * @param y2 y values of curve 2
     * @param title title of the plot
     * @param xlabel label of x axis
     * @param ylabel label of y axis
     */
    public void createPlot(String l1, double[] x1, double[] y1,
            String l2, double[] x2, double[] y2,
            String title, String xlabel, String ylabel){
        
        addCurve(l1, x1, y1);
        addCurve(l2, x2, y2);
        setTitle(title);
        setAxisLabel(0, xlabel);
        setAxisLabel(1, ylabel);
    }

    /**
     *  Add a curve to the plot.
     * @param label curve label
     * @param x x values of the curve
     * @param y t values of the curve
     */
    public void addCurve(String label, double[] x, double[] y) {
        plot.addLinePlot(label, x, y);
    }

    /**
     * Set the title of the curve.
     * @param title corve title
     */
    public void setTitle(String title) {
        BaseLabel titleLabel = new BaseLabel(title, Color.BLACK, 0.5, 1.1);
        titleLabel.setFont(new Font("Courier", Font.BOLD, 20));
        plot.addPlotable(titleLabel);
    }

    /**
     * Set Axis label.
     * @param axis Axis of interest
     * @param label label to set
     */
    public void setAxisLabel(int axis, String label) {
        plot.setAxisLabel(axis, label);
    }

    /**
     * Set axis bounds.
     * @param axis the axis of interest
     * @param lower lower bound
     * @param upper upper bound
     */
    public void setBounds(int axis, double lower, double upper) {
        plot.setFixedBounds(axis, lower, upper);
    }

    /**
     * Plot the chart in a window of custom dimentions.
     * @param width window width
     * @param height window height
     */
    public void getPlot(int width, int height) {
        JFrame frame = new JFrame("");
        frame.setSize(width, height);
        frame.setContentPane(plot);
        frame.setVisible(true);
    }
}
