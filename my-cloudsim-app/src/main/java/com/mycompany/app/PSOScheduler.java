package com.mycompany.app;

import java.util.*;

public class PSOScheduler {

    private static class Particle {
        int[] vmMapping;
        Map<Integer, List<Integer>> vmOrderMap;
        double[][] velocity;
        int[] pBestMapping;
        Map<Integer, List<Integer>> pBestOrderMap;
        double pBestFit;

        Particle(int[] vmMapping, Map<Integer, List<Integer>> vmOrderMap, double[][] velocity, double fit) {
            this.vmMapping = vmMapping;
            this.vmOrderMap = deepCopyMap(vmOrderMap);
            this.velocity = velocity;
            this.pBestMapping = vmMapping.clone();
            this.pBestOrderMap = deepCopyMap(vmOrderMap);
            this.pBestFit = fit;
        }

        private Map<Integer, List<Integer>> deepCopyMap(Map<Integer, List<Integer>> original) {
            Map<Integer, List<Integer>> copy = new HashMap<>();
            for (var entry : original.entrySet()) {
                copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
            return copy;
        }
    }

    private final List<Double> fitnessHistory = new ArrayList<>();

    private final int swarmSize = 200;
    private final int maxIter = 100;
    private final double inertia = 0.9;
    private final double c1 = 1.4;
    private final double c2 = 1.1;

    private int taskCount, vmCount;
    private List<App.TaskNode> originalDag;

    public List<App.TaskNode> schedule(List<App.TaskNode> dag, int vmCount) {
        this.originalDag = dag;
        this.taskCount = dag.size();
        this.vmCount = vmCount;

        List<Particle> swarm = initSwarm();

        int[] gBestMapping = null;
        Map<Integer, List<Integer>> gBestOrderMap = null;
        double gBestFitness = Double.MAX_VALUE;

        for (int iter = 0; iter < maxIter; iter++) {
            for (Particle p : swarm) {
                double fit = fitness(p.vmMapping, p.vmOrderMap);
                if (fit < p.pBestFit) {
                    p.pBestFit = fit;
                    p.pBestMapping = p.vmMapping.clone();
                    p.pBestOrderMap = p.deepCopyMap(p.vmOrderMap);
                }
                if (fit < gBestFitness) {
                    gBestFitness = fit;
                    gBestMapping = p.vmMapping.clone();
                    gBestOrderMap = p.deepCopyMap(p.vmOrderMap);
                }
            }

            fitnessHistory.add(gBestFitness);

            for (Particle p : swarm) {
                for (int t = 0; t < taskCount; t++) {
                    for (int v = 0; v < vmCount; v++) {
                        double r1 = Math.random(), r2 = Math.random();
                        double cog = c1 * r1 * ((p.pBestMapping[t] == v) ? 1 : 0);
                        double soc = c2 * r2 * ((gBestMapping != null && gBestMapping[t] == v) ? 1 : 0);
                        p.velocity[t][v] = inertia * p.velocity[t][v] + cog + soc;
                    }

                    double[] expVals = Arrays.stream(p.velocity[t]).map(Math::exp).toArray();
                    double sum = Arrays.stream(expVals).sum();
                    double rnd = Math.random(), cum = 0;
                    for (int v = 0; v < vmCount; v++) {
                        cum += expVals[v] / sum;
                        if (rnd <= cum) {
                            p.vmMapping[t] = v;
                            break;
                        }
                    }
                }

                p.vmOrderMap = generateVmOrderMap(p.vmMapping);
            }

            if (iter % 20 == 0 && gBestMapping != null) {
                List<App.TaskNode> dagWithPos = applyToDag(gBestMapping, gBestOrderMap);
                var res = SingleSimulationRunner.runSimulation(dagWithPos, vmCount);
                System.out.printf("Iter %d → Makespan=%.2f | QoS=%.2f%%\n", iter, res.makespan(), res.qosScore() * 100);
            }
        }

        return applyToDag(gBestMapping, gBestOrderMap);
    }

    public List<Double> getFitnessHistory() {
        return fitnessHistory;
    }

    private List<Particle> initSwarm() {
        List<Particle> swarm = new ArrayList<>();
        Random rnd = new Random();

        for (int i = 0; i < swarmSize; i++) {
            int[] mapping = new int[taskCount];
            double[][] velocity = new double[taskCount][vmCount];
            for (int t = 0; t < taskCount; t++) {
                mapping[t] = rnd.nextInt(vmCount);
                for (int v = 0; v < vmCount; v++) {
                    velocity[t][v] = rnd.nextDouble(-1, 1);
                }
            }

            var orderMap = generateVmOrderMap(mapping);
            double fit = fitness(mapping, orderMap);

            swarm.add(new Particle(mapping, orderMap, velocity, fit));
        }
        return swarm;
    }

    private Map<Integer, List<Integer>> generateVmOrderMap(int[] mapping) {
        Map<Integer, List<Integer>> map = new HashMap<>();
        for (int vm = 0; vm < vmCount; vm++) map.put(vm, new ArrayList<>());
        Random rnd = new Random();
        List<Integer> taskIds = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) taskIds.add(i);
        Collections.shuffle(taskIds);
        for (int taskId : taskIds) {
            int vm = mapping[taskId];
            map.get(vm).add(taskId);
        }
        return map;
    }

    private double fitness(int[] mapping, Map<Integer, List<Integer>> orderMap) {
        double[] vmWork = new double[vmCount];

        for (int vm = 0; vm < vmCount; vm++) {
            for (int taskId : orderMap.get(vm)) {
                long len = originalDag.get(taskId).cloudlet.getLength();
                vmWork[vm] += len;
            }
        }

        double cap = 2000.0;
        double makespan = 0;
        // double energy = 0;
        for (int vm = 0; vm < vmCount; vm++) {
            double exec = vmWork[vm] / cap;
            makespan = Math.max(makespan, exec);
            // double util = exec == 0 ? 0 : vmWork[vm] / (cap * makespan);
            // double power = 100 + util * 150;
            // energy += power * exec;
        }

        double qos = 0;
        for (int i = 0; i < taskCount; i++) {
            double ft = vmWork[mapping[i]] / cap;
            double d = originalDag.get(i).deadline;
            double l = originalDag.get(i).cloudlet.getLength() / cap;
            qos += 1 - Math.max(0, (ft - d) / l);
        }

        qos = Math.max(0, qos / taskCount);
        return 0.5 * (makespan / 10.0) - 0.3 * qos;
    }

    private List<App.TaskNode> applyToDag(int[] mapping, Map<Integer, List<Integer>> orderMap) {
        List<App.TaskNode> copy = TaskDagGenerator.deepCopyDag(originalDag);
        for (int i = 0; i < taskCount; i++) {
            copy.get(i).setPreferredVm(mapping[i]);
        }

        for (Map.Entry<Integer, List<Integer>> entry : orderMap.entrySet())
            for (int order = 0; order < entry.getValue().size(); order++) {
                int taskId = entry.getValue().get(order);
                copy.get(taskId).setExecutionOrder(order);
            }

        return copy;
    }
}