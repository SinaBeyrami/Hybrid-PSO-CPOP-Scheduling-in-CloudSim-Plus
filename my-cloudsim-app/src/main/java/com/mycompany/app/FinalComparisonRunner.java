package com.mycompany.app;

import com.mycompany.app.SingleSimulationRunner.SimulationResult;

import java.util.*;

public class FinalComparisonRunner {

    public static class ComparisonResult {
        int edge;
        int tasks;
        String algorithm;
        double makespan;
        double energy;
        double missRatio;
        double qos;

        public ComparisonResult(int edge, int tasks, String algorithm,
                                double makespan, double energy, double missRatio, double qos) {
            this.edge = edge;
            this.tasks = tasks;
            this.algorithm = algorithm;
            this.makespan = makespan;
            this.energy = energy;
            this.missRatio = missRatio;
            this.qos = qos;
        }

        @Override
        public String toString() {
            return String.format("Algo: %s | Edge: %d | Tasks: %d | Makespan: %.2f | Energy: %.2f | Miss: %.2f%% | QoS: %.2f%%",
                    algorithm, edge, tasks, makespan, energy, missRatio * 100, qos * 100);
        }
    }

    public static void main(String[] args) {
        List<Integer> taskSizes = List.of(100, 200, 300, 400, 500);
        List<Integer> edgeCounts = List.of(10, 20, 30);

        List<ComparisonResult> results = new ArrayList<>();

        for (int taskCount : taskSizes) {
            List<App.TaskNode> baseDag = TaskDagGenerator.generateRandomDAG(taskCount);
            for (int edge : edgeCounts) {
                System.out.printf("\n=== Running TaskCount=%d | Edge=%d ===\n", taskCount, edge);

                // Run Random-CPOP
                List<App.TaskNode> dagCPOP = TaskDagGenerator.deepCopyDag(baseDag);
                SimulationResult cpopResult = SingleSimulationRunner.runSimulation(dagCPOP, edge);
                results.add(new ComparisonResult(edge, taskCount, "CPOP",
                        cpopResult.makespan(), cpopResult.energy(),
                        cpopResult.deadlineMissRatio(), cpopResult.qosScore()));
                System.out.println("Random-CPOP finished");

                // Run PSO
                List<App.TaskNode> dagPSO = TaskDagGenerator.deepCopyDag(baseDag);
                PSOScheduler pso = new PSOScheduler();
                List<App.TaskNode> optimizedDag = pso.schedule(dagPSO, edge);
                SimulationResult psoResult = SingleSimulationRunner.runSimulation(optimizedDag, edge);
                results.add(new ComparisonResult(edge, taskCount, "PSO",
                        psoResult.makespan(), psoResult.energy(),
                        psoResult.deadlineMissRatio(), psoResult.qosScore()));
                System.out.println("PSO finished");
            }
        }

        System.out.println("\n\n=========== Final Comparison Table ===========");
        for (ComparisonResult r : results) System.out.println(r);

        ChartPlotter.showComparisonChart("makespan", results);
        ChartPlotter.showComparisonChart("energy", results);
        ChartPlotter.showComparisonChart("missratio", results);
        ChartPlotter.showComparisonChart("qos", results);
    }
}