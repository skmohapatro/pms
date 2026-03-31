package com.powerstore.intprep.util;

/**
 * Tiny utility to print consistent section headers in console output.
 */
public final class SectionPrinter {

  private SectionPrinter() {}

  /**
   * Prints a section title with separators.
   *
   * @param title section title
   */
  public static void section(String title) {
    System.out.println();
    System.out.println("============================================================");
    System.out.println(title);
    System.out.println("============================================================");
  }

  /**
   * Prints a subsection title.
   *
   * @param title subsection title
   */
  public static void subsection(String title) {
    System.out.println();
    System.out.println("-- " + title);
  }
}
