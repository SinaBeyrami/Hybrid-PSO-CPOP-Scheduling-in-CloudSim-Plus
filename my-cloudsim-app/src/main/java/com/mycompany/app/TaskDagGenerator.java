package com.mycompany.app;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.cloudlets.CloudletSimple;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.vms.Vm;

import java.util.*;

public class TaskDagGenerator {

    public static List<App.TaskNode> generateRandomDAG(int numTasks) {
        List<App.TaskNode> dag = new ArrayList<>();
        Random rand = new Random();

        for (int i = 0; i < numTasks; i++) {
            long length = 8000 + (i * 10);
            Cloudlet cl = new CloudletSimple(length, 2);
            cl.setSizes(1024);
            App.TaskNode task = new App.TaskNode(i, cl);
            dag.add(task);
        }

        int minEdges = numTasks / 2;
        int maxEdges = numTasks * (numTasks - 1) / 2;
        int edgeCount = rand.nextInt(maxEdges - minEdges + 1) + minEdges;

        Set<String> addedEdges = new HashSet<>();
        int added = 0;

        while (added < edgeCount) {
            int parent = rand.nextInt(numTasks - 1);
            int child = rand.nextInt(numTasks - parent - 1) + parent + 1;

            String edgeKey = parent + "," + child;
            if (addedEdges.contains(edgeKey)) continue;

            App.TaskNode parentNode = dag.get(parent);
            App.TaskNode childNode = dag.get(child);

            childNode.addDependency(parentNode);
            addedEdges.add(edgeKey);
            added++;
        }

        assignRealisticDeadlines(dag);

        return dag;
    }

    public static void assignRealisticDeadlines(List<App.TaskNode> dag) {
        CloudSim sim = new CloudSim();
        var datacenter = SimulationUtils.createSimpleDatacenter(sim);
        var broker = new DatacenterBrokerSimple(sim);
        List<Vm> vmList = SimulationUtils.createVmList(20);
        broker.submitVmList(vmList);

        Random rand = new Random();
        for (App.TaskNode node : dag) {
            Vm selectedVm = vmList.get(rand.nextInt(vmList.size()));
            node.cloudlet.setVm(selectedVm);
            broker.submitCloudlet(node.cloudlet);
        }

        sim.start();

        for (App.TaskNode node : dag) {
            double finish = node.cloudlet.getFinishTime();
            node.deadline = finish - 10 + rand.nextDouble() * 20; // range [f-10, f+10]
        }
    }

    public static List<App.TaskNode> deepCopyDag(List<App.TaskNode> original) {
        Map<Integer, App.TaskNode> copyMap = new HashMap<>();
        for (App.TaskNode node : original) {
            Cloudlet cl = new CloudletSimple(node.cloudlet.getLength(), node.cloudlet.getNumberOfPes());
            cl.setSizes(1024);
            App.TaskNode copy = new App.TaskNode(node.id, cl);
            copy.deadline = node.deadline;
            copyMap.put(node.id, copy);
        }

        for (App.TaskNode node : original) {
            for (App.TaskNode parent : node.parents) {
                copyMap.get(node.id).addDependency(copyMap.get(parent.id));
            }
        }

        return new ArrayList<>(copyMap.values());
    }
}
