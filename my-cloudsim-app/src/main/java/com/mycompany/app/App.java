package com.mycompany.app;

import org.cloudbus.cloudsim.cloudlets.Cloudlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class App {

    public static class TaskNode {
        public int id;
        public Cloudlet cloudlet;
        public double deadline;
        public List<TaskNode> parents = new ArrayList<>();
        public List<TaskNode> children = new ArrayList<>();

        public double rankU = -1, rankD = -1;

        public int preferredVm = -1;
        public int executionOrder = -1;

        public TaskNode(int id, Cloudlet cloudlet) {
            this.id = id;
            this.cloudlet = cloudlet;
        }

        public void addDependency(TaskNode parent) {
            this.parents.add(parent);
            parent.children.add(this);
        }

        public boolean isReady(Set<TaskNode> done) {
            return done.containsAll(parents);
        }

        public double getCPOPScore() {
            return rankU + rankD;
        }

        public boolean isDeadlineViolated() {
            return cloudlet.getFinishTime() > deadline;
        }

        public void setPreferredVm(int vmIndex) {
            this.preferredVm = vmIndex;
        }

        public int getPreferredVm() {
            return this.preferredVm;
        }

        public void setExecutionOrder(int ord) {
            this.executionOrder = ord;
        }

        public int getExecutionOrder() {
            return this.executionOrder;
        }

        @Override
        public String toString() {
            return "Task " + id;
        }
    }
}