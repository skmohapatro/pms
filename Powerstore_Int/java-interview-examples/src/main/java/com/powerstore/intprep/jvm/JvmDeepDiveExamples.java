package com.powerstore.intprep.jvm;

import com.powerstore.intprep.util.SectionPrinter;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM / core language deep-dive demos.
 *
 * <p>This class focuses on topics that are commonly discussed in senior interviews:
 *
 * <ul>
 *   <li>Memory model basics (stack/heap intuition)
 *   <li>Safe publication / visibility (happens-before)
 *   <li>GC signals and memory pools (observability)
 *   <li>JIT warmup effects (why benchmarking needs warmup)
 *   <li>Classloading signals (class count / unloaded count)
 * </ul>
 *
 * <p>Run:
 *
 * <pre>
 * mvn -q exec:java -Dexec.args="jvm"
 * </pre>
 */
public final class JvmDeepDiveExamples {

  private JvmDeepDiveExamples() {}

  /** Runs all JVM deep-dive demonstrations. */
  public static void runAll() {
    SectionPrinter.section("JVM deep dive");

    stackVsHeapIntuition();
    safePublicationAndVisibility();
    gcSignalsViaMxBeans();
    jitWarmupIllustration();
    classLoadingSignals();
  }

  /**
   * Demonstrates stack vs heap intuition.
   *
   * <p>In Java, local variables are stored in stack frames (references included), while objects are
   * allocated on the heap. You can’t directly “see the stack”, but you can observe allocation and
   * retained heap usage.
   */
  public static void stackVsHeapIntuition() {
    SectionPrinter.subsection("Memory model: stack vs heap (intuition)");

    int localPrimitive = 42;
    Object localRef = new Object();

    System.out.println("Local primitive value: " + localPrimitive);
    System.out.println("Local reference points to heap object identityHashCode: " + System.identityHashCode(localRef));

    // Allocate some objects to make heap usage visible.
    List<byte[]> allocations = new ArrayList<>();
    for (int i = 0; i < 32; i++) {
      allocations.add(new byte[256 * 1024]);
    }

    MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    System.out.println("Heap used after allocations (bytes): " + heap.getUsed());

    // Keep allocations live until after printing.
    if (allocations.size() == 0) {
      System.out.println("unreachable");
    }
  }

  /**
   * Demonstrates safe publication with {@code volatile}.
   *
   * <p>Key interview points:
   *
   * <ul>
   *   <li>{@code volatile} gives visibility + ordering (happens-before) but not atomicity for
   *       compound operations.
   *   <li>Safe publication patterns include: final fields + proper construction, volatile reference,
   *       synchronized blocks, and thread-safe containers.
   * </ul>
   *
   * <p>This demo publishes a fully constructed object by writing it to a volatile field.
   */
  public static void safePublicationAndVisibility() {
    SectionPrinter.subsection("Concurrency: safe publication + visibility");

    VolatilePublisher publisher = new VolatilePublisher();

    Thread writer = new Thread(() -> publisher.publish(new Holder(123, "ready")), "writer");
    Thread reader = new Thread(() -> {
      Holder h;
      while ((h = publisher.read()) == null) {
        // spin
      }
      System.out.println("Observed holder: a=" + h.a + ", b=" + h.b);
    }, "reader");

    reader.start();
    writer.start();

    try {
      writer.join();
      reader.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }
  }

  /**
   * Shows GC-related signals using MXBeans.
   *
   * <p>In interviews, it’s useful to explain what you would look at first:
   *
   * <ul>
   *   <li>Heap usage, allocation rate, GC pauses, and promotion to old gen
   *   <li>Whether the app is latency-sensitive (p99) or throughput-oriented
   *   <li>Whether there is memory leak/bloat (e.g., retained heap growth)
   * </ul>
   */
  public static void gcSignalsViaMxBeans() {
    SectionPrinter.subsection("GC signals (MXBeans)");

    MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    MemoryUsage before = memoryMXBean.getHeapMemoryUsage();

    // Create temporary garbage.
    for (int i = 0; i < 10_000; i++) {
      byte[] g = new byte[8 * 1024];
      if (g.length == -1) {
        System.out.println("unreachable");
      }
    }

    MemoryUsage after = memoryMXBean.getHeapMemoryUsage();

    System.out.println("Heap used before (bytes): " + before.getUsed());
    System.out.println("Heap used after  (bytes): " + after.getUsed());
    System.out.println("(Tip) To see GC pauses, use JFR or `jcmd <pid> GC.class_histogram` / `GC.run` in a sandbox.");
  }

  /**
   * Illustrates why “warmup” matters due to JIT compilation.
   *
   * <p>In real benchmarking, use JMH. This is a conceptual demo:
   *
   * <ul>
   *   <li>First iterations are slower (interpreted / less optimized)
   *   <li>Later iterations often speed up (inlining, escape analysis, optimized machine code)
   * </ul>
   */
  public static void jitWarmupIllustration() {
    SectionPrinter.subsection("JIT warmup (illustration)");

    long t1 = timeLoop(2_000_000);
    long t2 = timeLoop(2_000_000);
    long t3 = timeLoop(2_000_000);

    System.out.println("Loop timing (ns): first=" + t1 + ", second=" + t2 + ", third=" + t3);
    System.out.println("(Tip) For proper results, use JMH; for production profiling use JFR/async-profiler.");
  }

  /** Prints classloading counts (loaded/unloaded). */
  public static void classLoadingSignals() {
    SectionPrinter.subsection("Classloading signals");

    ClassLoadingMXBean cl = ManagementFactory.getClassLoadingMXBean();
    System.out.println("Currently loaded class count: " + cl.getLoadedClassCount());
    System.out.println("Total loaded class count:     " + cl.getTotalLoadedClassCount());
    System.out.println("Unloaded class count:         " + cl.getUnloadedClassCount());
    System.out.println("(Tip) Classloading issues often show up as slow startups, metaspace pressure, or reflection-heavy frameworks.");
  }

  private static long timeLoop(int n) {
    long start = System.nanoTime();
    long x = 0;
    for (int i = 0; i < n; i++) {
      x += (i * 31L) ^ (x >>> 1);
    }
    long end = System.nanoTime();

    // Prevent dead-code elimination.
    if (x == 42) {
      System.out.println("unreachable");
    }

    return end - start;
  }

  private static final class VolatilePublisher {
    private volatile Holder holder;

    void publish(Holder holder) {
      this.holder = holder;
    }

    Holder read() {
      return holder;
    }
  }

  private static final class Holder {
    final int a;
    final String b;

    Holder(int a, String b) {
      this.a = a;
      this.b = b;
    }
  }
}
