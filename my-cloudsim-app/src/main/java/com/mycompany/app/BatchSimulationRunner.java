package com.mycompany.app;

import com.mycompany.app.SingleSimulationRunner.SimulationResult;

import java.util.*;

public class BatchSimulationRunner {

    public static class Result {
        int edge;
        int tasks;
        double makespan;
        double energy;
        double deadlineMissRatio;
        double qosScore;

        public Result(int edge, int tasks, double makespan, double deadlineMissRatio, double qosScore, double energy) {
            this.edge = edge;
            this.tasks = tasks;
            this.makespan = makespan;
            this.energy = energy;
            this.deadlineMissRatio = deadlineMissRatio;
            this.qosScore = qosScore;
        }

        @Override
        public String toString() {
            return String.format(
                    "Edge: %d | Tasks: %d | Makespan: %.2f | Energy: %.2f | MissRatio: %.2f%% | QoS: %.2f%%",
                    edge, tasks, makespan, energy, deadlineMissRatio * 100, qosScore * 100
            );
        }
    }

    public static void main(String[] args) {
        List<Integer> edgeOptions = List.of(10, 20, 30);
        List<Integer> taskCounts = List.of(100, 200, 300, 400, 500);

        List<Result> results = new ArrayList<>();

        System.out.println("========== BATCH SIMULATION STARTED ==========");

        for (int taskCount : taskCounts) {
            List<App.TaskNode> dagBase = TaskDagGenerator.generateRandomDAG(taskCount);
            for (int edge : edgeOptions) {
                List<App.TaskNode> dagCopy = TaskDagGenerator.deepCopyDag(dagBase);
                System.out.printf("Running simulation: Tasks = %d, Edge = %d\n", taskCount, edge);
                SimulationResult simResult = SingleSimulationRunner.runSimulation(dagCopy, edge);
                Result result = new Result(
                        edge, taskCount,
                        simResult.makespan(),
                        simResult.deadlineMissRatio(),
                        simResult.qosScore(),
                        simResult.energy()
                );
                results.add(result);
                System.out.println("Result: " + result);
                System.out.println("--------------------------------------------------");
            }
        }

        System.out.println("\n================== FINAL SUMMARY ==================");
        for (Result res : results) System.out.println(res);
        System.out.println("===================================================");

        ChartPlotter.showChart("makespan", results);
        ChartPlotter.showChart("energy", results);
        ChartPlotter.showChart("missratio", results);
        ChartPlotter.showChart("qos", results);
    }
}
