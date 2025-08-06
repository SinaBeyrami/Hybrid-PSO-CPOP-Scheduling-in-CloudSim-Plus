package com.mycompany.app;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.List;

    public class FitnessPlotter extends ApplicationFrame {

    public FitnessPlotter(String title, List<Double> fitnessValues) {
        super(title);

        XYSeries series = new XYSeries("Best Fitness");
        for (int i = 0; i < fitnessValues.size(); i++) {
            series.add(i + 1, fitnessValues.get(i));
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                title,
                "Iteration",
                "Fitness",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel panel = new ChartPanel(chart);
        panel.setPreferredSize(new java.awt.Dimension(800, 600));
        setContentPane(panel);
    }

    public static void show(String title, List<Double> fitnessValues) {
        FitnessPlotter plot = new FitnessPlotter(title, fitnessValues);
        plot.pack();
        plot.setVisible(true);
    }
}
