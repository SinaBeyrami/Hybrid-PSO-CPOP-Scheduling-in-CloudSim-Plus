# Hybrid-PSO–CPOP Scheduling in CloudSim Plus

Particle Swarm Optimization (PSO) meets a CPOP-style baseline to schedule DAG workloads on edge/cloud resources, implemented on top of **CloudSim Plus**. This project generates random DAGs, assigns realistic deadlines, runs simulations, and compares **PSO** against a **Random-CPOP** baseline across makespan, energy, deadline-miss ratio, and QoS. It also plots fitness and comparison charts via **JFreeChart**.

> A short Persian write-up summarizing the second phase (adding PSO and comparing to Random-CPOP) is included; see `Phase-2-Report.pdf`.

Highlights: PSO generally lowers **makespan** and **energy** versus Random-CPOP, improves **QoS**, but can raise **miss ratio** in tightly constrained graphs, especially with very few VMs.&#x20;

---

## TL;DR

* **Baseline**: *Random-CPOP* — compute CPOP ranks; order tasks by `rankU + rankD`; VM choice is random per task.
* **Proposed**: *PSO* — a discrete PSO learns both task→VM mapping and per-VM task execution order, then we simulate the best found schedule.
* **Metrics**: Makespan, Energy (via host power models), Deadline-Miss Ratio, QoS (deadline adherence score).
* **Runners**:

  * `BatchSimulationRunner` – sweep (task count × VM count) with baseline.
  * `PSOBatchRunner` – run PSO for several settings and plot fitness.
  * `FinalComparisonRunner` – side-by-side PSO vs Random-CPOP comparisons and charts.

---

## Project structure

```
my-cloudsim-app/
├─ pom.xml
└─ src/main/java/com/mycompany/app/
   ├─ App.java                      # Task node model (DAG vertex)
   ├─ TaskDagGenerator.java         # Random DAG + realistic deadlines
   ├─ SimulationUtils.java          # Datacenter/Vm factories, CPOP ranks
   ├─ SingleSimulationRunner.java   # One end-to-end CloudSim run
   ├─ BatchSimulationRunner.java    # Baseline sweeps + charts
   ├─ PSOScheduler.java             # Discrete PSO scheduler
   ├─ PSOBatchRunner.java           # PSO sweeps + fitness chart
   ├─ FinalComparisonRunner.java    # PSO vs Random-CPOP comparisons
   ├─ ChartPlotter.java             # Metric charts (JFreeChart)
   └─ FitnessPlotter.java           # PSO fitness curve plotter
```

---

## How it works

### DAG & deadlines

* `TaskDagGenerator.generateRandomDAG(n)` builds a DAG of `n` tasks (`CloudletSimple`), with random acyclic edges and lengths.
* `assignRealisticDeadlines(...)` runs a quick simulation on a pool of VMs, then sets each task’s deadline to `finish ± 10` to mimic realistic constraints.

### Baseline: Random-CPOP

* `SimulationUtils.computeRankU/D` compute upward/downward ranks; each task’s **CPOP score** is `rankU + rankD`.
* `SingleSimulationRunner.runSimulation(...)`:

  * If no PSO ordering is present, tasks ready to run are sorted by CPOP score (desc).
  * Each task picks a **random VM** (baseline), is submitted, and the sim runs.
  * Metrics collected: makespan, total exec time, **energy** (from host power models), **deadline-miss ratio**, and **QoS**.

### PSO scheduler

* Decision variables:

  * **Mapping**: array `vmMapping[taskId] = vmIndex`.
  * **Per-VM order**: map `vmOrderMap<vm, List<taskId>>` (execution order inside each VM).
* `PSOScheduler` maintains a swarm of particles with softmax-sampled updates over discrete VM indices. Fitness is a fast proxy combining **makespan** and **QoS** (energy term is scaffolded and can be enabled).
* Best schedule (gBest) is written back into the DAG via `preferredVm` and `executionOrder`, then evaluated by a full CloudSim run in `SingleSimulationRunner`.

---

## Installation

**Prerequisites**

* Java 17+
* Maven 3.8+
* A desktop environment (JFreeChart opens AWT windows)

**Build**

```bash
cd my-cloudsim-app
mvn -q -DskipTests package
```

> If you prefer `mvn exec:java`, add the Exec plugin to your `pom.xml`:

```xml
<plugin>
  <groupId>org.codehaus.mojo</groupId><artifactId>exec-maven-plugin</artifactId>
  <version>3.1.0</version>
</plugin>
```

---

## Running simulations

### 1) Baseline sweeps (Random-CPOP only)

Runs multiple task counts × VM counts and draws **Makespan**, **Energy**, **MissRatio**, **QoS** charts.

```bash
mvn -q exec:java -Dexec.mainClass="com.mycompany.app.BatchSimulationRunner"
```

* In code, the variable named `edge` is the **VM count** (10, 20, 30).
* Task counts: 100, 200, 300, 400, 500.

### 2) PSO batches + fitness plot

Runs PSO, shows **Best Fitness over Iterations**, then evaluates in CloudSim.

```bash
mvn -q exec:java -Dexec.mainClass="com.mycompany.app.PSOBatchRunner"
```

### 3) Final comparison: PSO vs Random-CPOP

Generates tables and charts comparing both algorithms over multiple scenarios.

```bash
mvn -q exec:java -Dexec.mainClass="com.mycompany.app.FinalComparisonRunner"
```

> Heads-up: JFreeChart opens windows; run locally (not headless CI) to see them.

---

## Key classes (at a glance)

* **`App.TaskNode`** – DAG vertex: holds `Cloudlet`, parents/children, deadline, ranks, `preferredVm`, `executionOrder`.
* **`TaskDagGenerator`** – DAG synthesis + “realistic” deadlines (mini-sim → `finish ± 10`).
* **`SimulationUtils`** – data center & VM builders; upward/downward rank calculators (CPOP).
* **`SingleSimulationRunner`** – orchestrates one sim run and computes all metrics.
* **`PSOScheduler`** – discrete PSO (softmax over VM bins, per-VM lists), fitness = mix of (makespan proxy, QoS proxy).
* **`ChartPlotter`/`FitnessPlotter`** – line charts for metric comparisons and PSO fitness.

---

## Metrics

* **Makespan** – max cloudlet finish time.
* **Energy** – integrates host power over sim time using `PowerModelHostSimple`.
* **Deadline-Miss Ratio** – fraction of tasks with `finishTime > deadline`.
* **QoS** – average `1 − max(0, (finish − deadline)/execTime)` across tasks (clipped at 0).

> In the Persian report, PSO typically reduces **makespan** and **energy**, and raises **QoS** versus Random-CPOP; miss ratio can spike on sparse VM settings with tight precedence, due to per-VM ordering constraints when high parallelism is required.&#x20;

---

## PSO knobs (defaults in `PSOScheduler`)

* Swarm size: `200`
* Iterations: `100`
* Inertia `w = 0.9`, cognitive `c1 = 1.4`, social `c2 = 1.1`

You can experiment with these to trade exploration vs convergence. The repo also contains several hyper-parameter study runs (see comments/printouts in `PSOBatchRunner` and `FinalComparisonRunner`).

---

## Notes & caveats

* **“edge” means VM count** in the runners (10/20/30); it does **not** limit DAG edge count.
* The PSO **fitness** uses an analytical proxy (fast) and then validates the gBest schedule via full CloudSim.
* Randomness (DAGs, deadlines, initial swarm) affects outcomes; seed if you need reproducibility.
* JFreeChart windows require a GUI session.

---

## Acknowledgments

* [CloudSim Plus](https://cloudsimplus.org/) for simulation primitives.
* [JFreeChart](https://www.jfree.org/jfreechart/) for plotting.

---

## License

MIT © Sina Beyrami

---

## Appendix: where things happen in code

* **Ranks / CPOP**: `SimulationUtils.computeRankU/computeRankD`, `App.TaskNode.getCPOPScore()`.
* **Baseline scheduling**: `SingleSimulationRunner` (CPOP order + random VM).
* **PSO mapping & ordering**: `PSOScheduler.schedule()` → `applyToDag()` → `SingleSimulationRunner`.
* **Energy/QoS/Miss metrics**: `SingleSimulationRunner.SimulationResult` and post-sim calculations.

> For Persian readers: the attached phase-2 PDF includes setup, runs, and metric plots; it mirrors the code paths above and discusses where PSO excels and where Random-CPOP may match/beat it on specific topologies.&#x20;
