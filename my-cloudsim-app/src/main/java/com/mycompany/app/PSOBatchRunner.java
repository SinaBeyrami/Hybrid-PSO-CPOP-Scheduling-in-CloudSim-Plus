package com.mycompany.app;

import java.util.List;

public class PSOBatchRunner {
    public static void main(String[] args) {
        List<Integer> edgeOptions = List.of(10, 20, 30);
        List<Integer> taskCounts = List.of(100, 300, 500);

        for (int taskCount : taskCounts) {
            List<App.TaskNode> dag = TaskDagGenerator.generateRandomDAG(taskCount);

            for (int edge : edgeOptions) {
                PSOScheduler pso = new PSOScheduler();
                List<App.TaskNode> scheduledDag = pso.schedule(dag, edge);
                FitnessPlotter.show("Fitness over Iterations (tasks: " + taskCount + " edge: " + edge + ")", pso.getFitnessHistory());
                var result = SingleSimulationRunner.runSimulation(scheduledDag, edge);

                System.out.printf("Edge: %d, Tasks: %d → Makespan: %.2f, QoS: %.2f%%, Miss: %.2f%%, Energy: %.2f\n",
                        edge, taskCount,
                        result.makespan(),
                        result.qosScore() * 100,
                        result.deadlineMissRatio() * 100,
                        result.totalExecutionTime()
                );
            }
        }
    }
}
