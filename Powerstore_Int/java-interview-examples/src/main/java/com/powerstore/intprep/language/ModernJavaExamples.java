package com.powerstore.intprep.language;

import com.powerstore.intprep.util.SectionPrinter;

import java.net.URI;
import java.net.http.HttpRequest;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

/**
 * Examples of modern Java language features that are commonly asked about.
 *
 * <p>This class intentionally demonstrates features without relying on external services.
 *
 * <p>Run:
 *
 * <pre>
 * mvn -q exec:java -Dexec.args="java"
 * </pre>
 */
public final class ModernJavaExamples {

  private ModernJavaExamples() {}

  /** Runs all modern Java examples. */
  public static void runAll() {
    SectionPrinter.section("Modern Java features");

    java11VarOptionalHttpClient();
    java17RecordsSealedPatternMatchingTextBlocks();
    java21VirtualThreadsOverview();
  }

  /**
   * Java 11 era features: {@code var} (Java 10), better {@link Optional} usage patterns, and
   * {@code java.net.http.HttpClient}.
   *
   * <p>Interview talking points:
   *
   * <ul>
   *   <li>Use {@code var} for readability when the type is obvious; avoid when it hides intent.
   *   <li>Avoid {@code Optional} as a field/parameter in most designs; prefer it as a return type.
   *   <li>{@code HttpClient} supports HTTP/2, async APIs, and timeouts.
   * </ul>
   */
  public static void java11VarOptionalHttpClient() {
    SectionPrinter.subsection("Java 11: var + Optional + HttpClient (build-only)");

    var value = "hello"; // type inference (String)
    System.out.println("var example: value length=" + value.length());

    Optional<String> maybe = Optional.of("data");
    String result = maybe.filter(s -> s.length() > 2).orElse("default");
    System.out.println("Optional example: " + result);

    // Build a request without sending network traffic.
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://example.com"))
        .timeout(Duration.ofSeconds(2))
        .GET()
        .build();
    System.out.println("HttpRequest built (not sent): method=" + request.method() + ", uri=" + request.uri());
  }

  /**
   * Java 17 features: records, sealed classes, pattern matching for {@code instanceof}, and
   * text blocks.
   *
   * <p>Interview talking points:
   *
   * <ul>
   *   <li>Records are great for immutable data carriers (DTOs, events) with value semantics.
   *   <li>Sealed classes restrict subtyping to a known set (stronger domain constraints).
   *   <li>Pattern matching reduces boilerplate casts and improves readability.
   * </ul>
   */
  public static void java17RecordsSealedPatternMatchingTextBlocks() {
    SectionPrinter.subsection("Java 17: records + sealed + pattern matching + text blocks");

    User u = new User(101, "Sam");
    System.out.println("Record example: " + u + ", name=" + u.name());

    Shape s = new Circle(2.0);
    double area;
    if (s instanceof Circle c) {
      area = Math.PI * c.radius() * c.radius();
    } else if (s instanceof Rectangle r) {
      area = r.width() * r.height();
    } else {
      // For a sealed hierarchy, this should be unreachable as long as all permitted subtypes are handled.
      throw new IllegalStateException("Unknown Shape implementation: " + s.getClass());
    }
    System.out.println("Sealed/switch example: area=" + area);

    Object any = "abc";
    if (any instanceof String str) { // pattern matching for instanceof
      System.out.println("Pattern matching instanceof: upper=" + str.toUpperCase());
    }

    String json = """
        {
          "id": 101,
          "name": "Sam"
        }
        """;
    System.out.println("Text block example: " + json.replace('\n', ' '));
  }

  /**
   * Java 21 feature highlight: virtual threads.
   *
   * <p>Interview talking points:
   *
   * <ul>
   *   <li>Virtual threads make a blocking-per-request model scalable for I/O-bound workloads.
   *   <li>They do not automatically fix CPU saturation; they help with waiting/parking.
   *   <li>You still need timeouts, concurrency limits, and proper cancellation.
   * </ul>
   */
  public static void java21VirtualThreadsOverview() {
    SectionPrinter.subsection("Java 21: virtual threads (demo)");

    int feature = Runtime.version().feature();
    if (feature < 21) {
      System.out.println("Skipping: virtual threads require Java 21+. Current runtime: Java " + feature);
      return;
    }

    try {
      // Java 21+ API: Executors.newVirtualThreadPerTaskExecutor()
      Method m = java.util.concurrent.Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
      Object executor = m.invoke(null);

      java.util.concurrent.ExecutorService es = (java.util.concurrent.ExecutorService) executor;
      try {
        var futures = java.util.stream.IntStream.range(0, 10)
            .mapToObj(i -> es.submit(() -> {
              Thread.sleep(30L);
              return Thread.currentThread().toString();
            }))
            .toList();

        for (var f : futures) {
          System.out.println("Completed on: " + f.get());
        }
      } finally {
        es.shutdown();
        try {
          if (!es.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
            es.shutdownNow();
          }
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          es.shutdownNow();
        }
      }
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Virtual threads API not available despite Java " + feature, e);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * A Java {@code record} (Java 16+) is a compact way to declare an immutable "data carrier".
   *
   * <p>What Java generates for you:
   *
   * <ul>
   *   <li>{@code private final} fields for each component ({@code id} and {@code name})
   *   <li>a canonical constructor {@code new User(int id, String name)}
   *   <li>accessor methods {@code id()} and {@code name()} (note: not {@code getId()})
   *   <li>{@code equals}, {@code hashCode}, and {@code toString} based on the components
   * </ul>
   *
   * <p>When to use:
   *
   * <ul>
   *   <li>DTOs / API responses, events/messages, read models, method return values
   *   <li>Cases where value semantics are desired (two Users with same components are "equal")
   * </ul>
   *
   * <p>Important notes:
   *
   * <ul>
   *   <li>Records are <b>shallowly immutable</b>: the references are final, but referenced objects
   *       (e.g., a {@code List}) can still be mutable.
   *   <li>You can add validation by declaring a compact constructor, e.g. {@code public User { ... }}.
   * </ul>
   */
  public record User(int id, String name) {}

  /**
   * A {@code sealed} type (Java 17+) restricts which classes can implement/extend it.
   *
   * <p>Why this exists:
   *
   * <ul>
   *   <li>It encodes domain constraints in the type system ("only these implementations exist")
   *   <li>It enables safer refactoring: adding a new permitted subtype forces you to update handling
   *   <li>It improves reasoning in reviews (you can see the closed set in the {@code permits} list)
   * </ul>
   *
   * <p>Rules in short:
   *
   * <ul>
   *   <li>All permitted subtypes must be in the same module (or package, for non-modular code) and
   *       must declare one of: {@code final}, {@code sealed}, or {@code non-sealed}.
   * </ul>
   *
   * <p>In this demo, {@link Shape} can only be implemented by {@link Circle} and {@link Rectangle}.
   */
  public sealed interface Shape permits Circle, Rectangle {}

  /**
   * A permitted subtype of {@link Shape}.
   *
   * <p>We use a record here because it’s a natural fit for a value object: radius is part of the
   * identity, and we want auto-generated {@code equals/hashCode/toString}.
   */
  public record Circle(double radius) implements Shape {}

  /**
   * Another permitted subtype of {@link Shape}.
   *
   * <p>Because the hierarchy is sealed, code handling {@link Shape} can be written assuming this
   * closed set (Circle/Rectangle). If you later add {@code Triangle}, you should update the logic
   * that computes area.
   */
  public record Rectangle(double width, double height) implements Shape {}
}
