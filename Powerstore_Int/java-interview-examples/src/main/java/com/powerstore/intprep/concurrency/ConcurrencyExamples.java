package com.powerstore.intprep.concurrency;

import com.powerstore.intprep.util.SectionPrinter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Concurrency examples that map to common interview topics.
 *
 * <p>Covers:
 *
 * <ul>
 *   <li>{@code synchronized} and {@code volatile} fundamentals</li>
 *   <li>Executors, {@link CompletableFuture}, and typical composition</li>
 *   <li>Locks: {@link ReentrantLock}, {@link ReadWriteLock}</li>
 *   <li>Atomics: {@link AtomicInteger}, {@link LongAdder} and contention tradeoffs</li>
 * </ul>
 *
 * <p>Run:
 *
 * <pre>
 * mvn -q exec:java -Dexec.args="concurrency"
 * </pre>
 */
public final class ConcurrencyExamples {

  private ConcurrencyExamples() {}

  /** Runs all concurrency demonstrations. */
  public static void runAll() {
    SectionPrinter.section("Concurrency");

    synchronizedAndVisibility();
    executorsAndCompletableFuture();
    locksReentrantAndReadWrite();
    atomicsAndAdders();
  }

  /**
   * Demonstrates {@code synchronized} for mutual exclusion and {@code volatile} for visibility.
   *
   * <p>Interview talking points:
   *
   * <ul>
   *   <li>{@code synchronized} provides mutual exclusion + happens-before on monitor enter/exit.</li>
   *   <li>{@code volatile} provides visibility + ordering, not atomicity for compound updates.</li>
   * </ul>
   */
  public static void synchronizedAndVisibility() {
    SectionPrinter.subsection("Primitives: synchronized + volatile");

    Counter counter = new Counter();

    Thread t1 = new Thread(() -> {
      for (int i = 0; i < 100_000; i++) {
        counter.incSynchronized();
      }
      counter.stop();
    }, "sync-inc");

    Thread t2 = new Thread(() -> {
      while (!counter.isStopped()) {
        // busy wait; volatile ensures this thread observes the stop request
      }
      System.out.println("Observed stop signal (volatile)" );
    }, "volatile-reader");

    t1.start();
    t2.start();

    try {
      t1.join();
      t2.join();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    }

    System.out.println("Counter value (synchronized): " + counter.get());
  }

  /**
   * Demonstrates thread pools and {@link CompletableFuture} composition.
   *
   * <p>Interview talking points:
   *
   * <ul>
   *   <li>Always use timeouts when calling external dependencies.</li>
   *   <li>Be explicit about which executor runs which stage (avoid accidental common pool usage).</li>
   * </ul>
   */
  public static void executorsAndCompletableFuture() {
    SectionPrinter.subsection("JUC: ExecutorService + CompletableFuture");

    ExecutorService ioPool = Executors.newFixedThreadPool(4);
    try {
      CompletableFuture<String> f = CompletableFuture.supplyAsync(() -> slow("A"), ioPool)
          .thenCombine(CompletableFuture.supplyAsync(() -> slow("B"), ioPool), (a, b) -> a + b)
          .orTimeout(300, TimeUnit.MILLISECONDS)
          .exceptionally(ex -> "fallback");

      System.out.println("CompletableFuture result: " + f.join());
    } finally {
      ioPool.shutdown();
    }
  }

  /**
   * Demonstrates explicit locks and why you might choose them.
   *
   * <p>Interview talking points:
   *
   * <ul>
   *   <li>{@link ReentrantLock} supports try-lock, timed lock, and fairness options.</li>
   *   <li>{@link ReadWriteLock} can increase throughput for read-heavy data structures.</li>
   * </ul>
   */
  public static void locksReentrantAndReadWrite() {
    SectionPrinter.subsection("Locks: ReentrantLock + ReadWriteLock");

    ReentrantLock lock = new ReentrantLock();
    lock.lock();
    try {
      System.out.println("ReentrantLock acquired; critical section executed");
    } finally {
      lock.unlock();
    }

    ReadWriteLock rw = new ReentrantReadWriteLock();
    rw.readLock().lock();
    try {
      System.out.println("Read lock acquired");
    } finally {
      rw.readLock().unlock();
    }
  }

  /**
   * Demonstrates atomics and adders.
   *
   * <p>Interview talking points:
   *
   * <ul>
   *   <li>{@link AtomicInteger} is a single CAS location; under high contention it can retry a lot.</li>
   *   <li>{@link LongAdder} stripes updates to reduce contention; better for high-write counters.</li>
   * </ul>
   */
  public static void atomicsAndAdders() {
    SectionPrinter.subsection("Non-blocking: AtomicInteger vs LongAdder");

    AtomicInteger atomic = new AtomicInteger();
    LongAdder adder = new LongAdder();

    int threads = 8;
    int incrementsPerThread = 50_000;

    ExecutorService pool = Executors.newFixedThreadPool(threads);
    try {
      List<Callable<Void>> tasks = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        tasks.add(() -> {
          for (int j = 0; j < incrementsPerThread; j++) {
            atomic.incrementAndGet();
            adder.increment();
          }
          return null;
        });
      }

      pool.invokeAll(tasks, Duration.ofSeconds(2).toMillis(), TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } finally {
      pool.shutdown();
    }

    System.out.println("AtomicInteger value: " + atomic.get());
    System.out.println("LongAdder value:     " + adder.sum());
  }

  private static String slow(String s) {
    try {
      Thread.sleep(120);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return s;
  }

  private static final class Counter {
    private int value;
    private volatile boolean stopped;

    synchronized void incSynchronized() {
      value++;
    }

    synchronized int get() {
      return value;
    }

    void stop() {
      stopped = true;
    }

    boolean isStopped() {
      return stopped;
    }
  }
}
