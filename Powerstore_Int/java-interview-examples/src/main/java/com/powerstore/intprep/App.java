package com.powerstore.intprep;

import com.powerstore.intprep.concurrency.ConcurrencyExamples;
import com.powerstore.intprep.jvm.JvmDeepDiveExamples;
import com.powerstore.intprep.language.ModernJavaExamples;
import com.powerstore.intprep.perf.PerformanceAndProfilingExamples;

import java.util.Locale;

/**
 * Entry point for the interview examples project.
 *
 * <p>Run with Maven:
 *
 * <pre>
 * mvn -q exec:java -Dexec.args="all"
 * mvn -q exec:java -Dexec.args="jvm"
 * mvn -q exec:java -Dexec.args="java"
 * mvn -q exec:java -Dexec.args="concurrency"
 * mvn -q exec:java -Dexec.args="perf"
 * </pre>
 *
 * <p>Notes:
 *
 * <ul>
 *   <li>Project compiles on Java 17 (see {@code pom.xml}).
 *   <li>The virtual-threads demo runs only when executing on Java 21+; on Java 17 it will skip.
 *   <li>Some demos depend on OS/JVM behavior (e.g., JIT warmup, visibility effects) and are illustrative.
 * </ul>
 */
public final class App {

  private App() {}

  /**
   * Runs a selected demo group.
   *
   * @param args first arg is one of: {@code all, jvm, java, concurrency, perf}
   */
  public static void main(String[] args) {
    String mode = (args.length == 0 ? "all" : args[0]).toLowerCase(Locale.ROOT).trim();

    switch (mode) {
      case "all" -> runAll();
      case "jvm" -> JvmDeepDiveExamples.runAll();
      case "java" -> ModernJavaExamples.runAll();
      case "concurrency" -> ConcurrencyExamples.runAll();
      case "perf" -> PerformanceAndProfilingExamples.runAll();
      default -> {
        printUsage();
        throw new IllegalArgumentException("Unknown demo group: " + mode);
      }
    }
  }

  private static void runAll() {
    JvmDeepDiveExamples.runAll();
    ModernJavaExamples.runAll();
    ConcurrencyExamples.runAll();
    PerformanceAndProfilingExamples.runAll();
  }

  private static void printUsage() {
    System.out.println("Usage: mvn -q exec:java -Dexec.args=\"<group>\"");
    System.out.println("Where <group> is one of: all | jvm | java | concurrency | perf");
  }
}
