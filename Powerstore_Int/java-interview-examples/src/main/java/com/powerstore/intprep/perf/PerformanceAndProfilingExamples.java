package com.powerstore.intprep.perf;

import com.powerstore.intprep.util.SectionPrinter;

import jdk.jfr.Recording;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Performance and profiling examples.
 *
 * <p>Focuses on interview-relevant “how would you debug this in prod?” discussions.
 *
 * <p>Run:
 *
 * <pre>
 * mvn -q exec:java -Dexec.args="perf"
 * </pre>
 */
public final class PerformanceAndProfilingExamples {

  private PerformanceAndProfilingExamples() {}

  /** Runs all performance/profiling demonstrations. */
  public static void runAll() {
    SectionPrinter.section("Performance + profiling");

    latencyVsThroughputNotes();
    jfrRecordingDemo();
    threadDumpDemo();
    allocationPressureDemo();
  }

  /**
   * Prints concise talking points for latency vs throughput.
   *
   * <p>Interview angle: show you can reason about p95/p99, load-shedding, and backpressure.
   */
  public static void latencyVsThroughputNotes() {
    SectionPrinter.subsection("Latency vs throughput (talking points)");

    System.out.println("Throughput: requests/sec, batch efficiency, CPU utilization.");
    System.out.println("Latency: response time (p50/p95/p99). Tail latency often driven by contention + queues.");
    System.out.println("Backpressure: avoid unbounded queues; apply admission control and timeouts.");
  }

  /**
   * Creates a short Java Flight Recorder (JFR) recording.
   *
   * <p>This is a safe way to show familiarity with production-grade profiling.
   *
   * <p>The recording is written to the system temp directory.
   */
  public static void jfrRecordingDemo() {
    SectionPrinter.subsection("JFR: start/stop a recording");

    Path out;
    try {
      out = Files.createTempFile("intprep-", ".jfr");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try (Recording recording = new Recording()) {
      recording.setName("intprep-recording");
      recording.setDuration(Duration.ofSeconds(2));
      recording.start();

      // Do some work while JFR runs.
      burnCpu(200);
      allocationPressure(50, 64 * 1024);

      recording.stop();
      recording.dump(out);
      System.out.println("Wrote JFR recording to: " + out);
      System.out.println("Open it with: JDK Mission Control (JMC)");
    } catch (UnsupportedOperationException e) {
      System.out.println("JFR not available on this JVM: " + e.getMessage());
    } catch (SecurityException e) {
      System.out.println("JFR blocked by security manager/policy: " + e.getMessage());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Prints a basic thread dump using {@link ThreadMXBean}.
   *
   * <p>Interview angle: when you suspect deadlocks, thread pool starvation, or blocked event loops.
   */
  public static void threadDumpDemo() {
    SectionPrinter.subsection("Thread dump (ThreadMXBean)");

    ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
    ThreadInfo[] infos = mxBean.dumpAllThreads(true, true);

    int shown = 0;
    for (ThreadInfo info : infos) {
      if (shown++ >= 6) {
        break;
      }
      System.out.println(info.getThreadName() + " state=" + info.getThreadState());
      if (info.getStackTrace().length > 0) {
        System.out.println("  at " + info.getStackTrace()[0]);
      }
    }

    System.out.println("(Tip) For a full dump, use `jcmd <pid> Thread.print` in production.");
  }

  /**
   * Demonstrates allocation pressure and the kind of signals you might observe.
   *
   * <p>Interview angle: why reducing allocations reduces GC frequency and tail latency.
   */
  public static void allocationPressureDemo() {
    SectionPrinter.subsection("Allocation pressure (illustration)");

    long before = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
    allocationPressure(200, 32 * 1024);
    long after = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();

    System.out.println("Heap used before (bytes): " + before);
    System.out.println("Heap used after  (bytes): " + after);
    System.out.println("(Tip) Use async-profiler allocation profiling or JFR 'Object Allocation in New TLAB'.");
  }

  private static void burnCpu(int millis) {
    long end = System.nanoTime() + Duration.ofMillis(millis).toNanos();
    long x = 0;
    while (System.nanoTime() < end) {
      x ^= (x << 1) + 0x9e3779b97f4a7c15L;
    }
    if (x == 42) {
      System.out.println("unreachable");
    }
  }

  private static void allocationPressure(int iterations, int bytes) {
    List<byte[]> junk = new ArrayList<>();
    for (int i = 0; i < iterations; i++) {
      junk.add(new byte[bytes]);
    }
    if (junk.size() == -1) {
      System.out.println("unreachable");
    }
  }
}
