package com.mycompany.app;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.category.DefaultCategoryDataset;

import java.util.List;

public class ChartPlotter extends ApplicationFrame {

    public ChartPlotter(String title, String metric, List<BatchSimulationRunner.Result> results) {
        super(title);
        DefaultCategoryDataset dataset = createDataset(metric, results);

        JFreeChart chart = ChartFactory.createLineChart(
                title,
                "Task Count",
                metric,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));
        setContentPane(chartPanel);
    }

    private DefaultCategoryDataset createDataset(String metric, List<BatchSimulationRunner.Result> results) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (BatchSimulationRunner.Result res : results) {
            String series = "Edge=" + res.edge;
            String category = String.valueOf(res.tasks);

            switch (metric.toLowerCase()) {
                case "makespan" -> dataset.addValue(res.makespan, series, category);
                case "energy" -> dataset.addValue(res.energy, series, category);
                case "missratio" -> dataset.addValue(res.deadlineMissRatio * 100, series, category);
                case "qos" -> dataset.addValue(res.qosScore * 100, series, category);
                default -> throw new IllegalArgumentException("Unknown metric: " + metric);
            }
        }

        return dataset;
    }

    public static void showChart(String metric, List<BatchSimulationRunner.Result> results) {
        ChartPlotter plotter = new ChartPlotter(metric.toUpperCase() + " vs Task Count", metric, results);
        plotter.pack();
        plotter.setVisible(true);
    }

    public static void showComparisonChart(String metric, List<FinalComparisonRunner.ComparisonResult> results) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (FinalComparisonRunner.ComparisonResult r : results) {
            String series = r.algorithm + " - Edge=" + r.edge;
            String category = String.valueOf(r.tasks);

            switch (metric.toLowerCase()) {
                case "makespan" -> dataset.addValue(r.makespan, series, category);
                case "energy" -> dataset.addValue(r.energy, series, category);
                case "missratio" -> dataset.addValue(r.missRatio * 100, series, category);
                case "qos" -> dataset.addValue(r.qos * 100, series, category);
                default -> throw new IllegalArgumentException("Unknown metric: " + metric);
            }
        }

        JFreeChart chart = ChartFactory.createLineChart(
                metric.toUpperCase() + " Comparison",
                "Task Count",
                metric,
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        ApplicationFrame frame = new ApplicationFrame(metric.toUpperCase() + " Comparison Chart");
        frame.setContentPane(chartPanel);
        frame.pack();
        frame.setVisible(true);
    }
}