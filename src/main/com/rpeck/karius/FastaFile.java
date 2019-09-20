package com.rpeck.karius;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * POJO which contains the raw text from a FASTA file in a slightly more structured way.
 * Note that these fragments may contain IUPAC codes.
 */
public class FastaFile {
  private String organism;
  private String version;
  private String description;

  private List<String> fragments;

  public FastaFile(String organism, String version, String description) {
    this.organism = organism;
    this.version = version;
    this.description = description;
    this.fragments = new ArrayList<>();
  }

  public void addFragment(String fragment) {
    fragments.add(fragment);
  }

  public ListIterator<String> fragments() {
    return fragments.listIterator();
  }

  public String getOrganism() {
    return organism;
  }

  public String getVersion() {
    return version;
  }

  public String getDescription() {
    return description;
  }
}
