package com.mycompany.app;

import org.cloudbus.cloudsim.brokers.DatacenterBrokerSimple;
import org.cloudbus.cloudsim.cloudlets.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.hosts.Host;

import java.util.*;

public class SingleSimulationRunner {

    public record SimulationResult(
            double makespan,
            double totalExecutionTime,
            double deadlineMissRatio,
            double qosScore,
            double energy
    ) {}

    public static SimulationResult runSimulation(List<App.TaskNode> dag, int vmCount) {
        CloudSim simulation = new CloudSim();
        Datacenter datacenter = SimulationUtils.createSimpleDatacenter(simulation);
        DatacenterBrokerSimple broker = new DatacenterBrokerSimple(simulation);

        List<Vm> vmList = SimulationUtils.createVmList(vmCount);
        broker.submitVmList(vmList);

        Random rand = new Random();

        dag.forEach(SimulationUtils::computeRankU);
        dag.forEach(SimulationUtils::computeRankD);

        Set<App.TaskNode> scheduled = new HashSet<>();

        boolean isPSO = dag.stream().anyMatch(t -> t.getExecutionOrder() >= 0);

        if (isPSO) {
            dag.sort(Comparator.comparingInt(App.TaskNode::getExecutionOrder));
        }

        while (scheduled.size() < dag.size()) {
            for (App.TaskNode node : dag.stream()
                    .filter(n -> !scheduled.contains(n))
                    .filter(n -> n.isReady(scheduled))
                    .sorted(isPSO ?
                            Comparator.comparingInt(App.TaskNode::getExecutionOrder)
                            : Comparator.comparingDouble(App.TaskNode::getCPOPScore).reversed()
                    )
                    .toList()) {

                Vm selectedVm;

                if (node.getPreferredVm() >= 0 && node.getPreferredVm() < vmList.size()) {
                    selectedVm = vmList.get(node.getPreferredVm());
                } else {
                    selectedVm = vmList.get(rand.nextInt(vmList.size()));
                }

                node.cloudlet.setVm(selectedVm);
                broker.submitCloudlet(node.cloudlet);
                scheduled.add(node);
            }
        }

        simulation.start();

        double totalEnergy = 0.0;
        double time = simulation.clock();
        for (Host h : datacenter.getHostList()) {
            double util = 0;
            for (Vm vm : h.getVmList()) {
                util += vm.getCpuPercentUtilization(time);
            }
            double avgUtil = h.getVmList().isEmpty() ? 0 : util / h.getVmList().size();
            double power = h.getPowerModel().getPower(avgUtil);
            totalEnergy += power * time;
        }

        List<Cloudlet> finished = broker.getCloudletFinishedList();

        double makespan = finished.stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0);

        double totalExec = finished.stream()
                .mapToDouble(Cloudlet::getActualCpuTime)
                .sum();

        long missed = dag.stream().filter(App.TaskNode::isDeadlineViolated).count();
        double missRatio = (double) missed / dag.size();

        double qos = 0;
        for (App.TaskNode task : dag) {
            double finish = task.cloudlet.getFinishTime();
            double deadline = task.deadline;
            double execTime = task.cloudlet.getActualCpuTime();

            qos += 1 - Math.max(0, (finish - deadline) / execTime);
        }

        qos = Math.max(0, qos / dag.size());

        return new SimulationResult(makespan, totalExec, missRatio, qos, totalEnergy);
    }
}