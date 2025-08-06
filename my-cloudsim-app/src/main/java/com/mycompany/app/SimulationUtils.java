package com.mycompany.app;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.datacenters.Datacenter;
import org.cloudbus.cloudsim.datacenters.DatacenterSimple;
import org.cloudbus.cloudsim.hosts.Host;
import org.cloudbus.cloudsim.hosts.HostSimple;
import org.cloudbus.cloudsim.resources.Pe;
import org.cloudbus.cloudsim.resources.PeSimple;
import org.cloudbus.cloudsim.schedulers.vm.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.vms.Vm;
import org.cloudbus.cloudsim.vms.VmSimple;
import org.cloudbus.cloudsim.power.models.PowerModelHostSimple;


import java.util.*;

public class SimulationUtils {

    public static Datacenter createSimpleDatacenter(CloudSim simulation) {
        List<Host> hostList = new ArrayList<>();
        int hostsNeeded = (int) Math.ceil(100.0 / 8);
        for (int i = 0; i < hostsNeeded; i++) {
            List<Pe> peList = new ArrayList<>();
            for (int j = 0; j < 16; j++) peList.add(new PeSimple(1000));
            Host host = new HostSimple(8192, 40000, 1000000, peList);
            host.setVmScheduler(new VmSchedulerTimeShared());
            host.enableUtilizationStats();

            var powerModel = new PowerModelHostSimple(250, 100);
            powerModel.setStartupPower(50).setShutDownPower(30);
            host.setPowerModel(powerModel);

            hostList.add(host);
        }
        return new DatacenterSimple(simulation, hostList);
    }

    public static List<Vm> createVmList(int count) {
        List<Vm> vmList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Vm vm = new VmSimple(1000, 2);
            vm.enableUtilizationStats();
            vm.setRam(512).setBw(1000).setSize(10000);
            vmList.add(vm);
        }
        return vmList;
    }

    public static double computeRankU(App.TaskNode node) {
        if (node.rankU >= 0) return node.rankU;
        if (node.children.isEmpty()) return node.rankU = node.cloudlet.getLength();
        double max = 0;
        for (App.TaskNode child : node.children) {
            max = Math.max(max, computeRankU(child));
        }
        return node.rankU = node.cloudlet.getLength() + max;
    }

    public static double computeRankD(App.TaskNode node) {
        if (node.rankD >= 0) return node.rankD;
        if (node.parents.isEmpty()) return node.rankD = node.cloudlet.getLength();
        double max = 0;
        for (App.TaskNode parent : node.parents) {
            max = Math.max(max, computeRankD(parent));
        }
        return node.rankD = node.cloudlet.getLength() + max;
    }
}
