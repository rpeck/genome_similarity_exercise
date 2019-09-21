package com.rpeck.karius;

import java.io.File;
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

  /**
   * Create a FastaFile object which represents the raw data in a FASTA file.
   * @param filename name of the input file, to extract the name of the organism
   * @param version version of the genome, taken from the first token in the file
   * @param description taken from the rest of the tokens on the first line of the file
   */
  public FastaFile(String filename, String version, String description) {
    this.organism = (new File(filename)).getName().replaceFirst("_genomic.fna.gz", "");
    this.version = version;
    this.description = description;
    this.fragments = new ArrayList<>();
  }

  /**
   * Add a fragment of a genome to this organism's file representation
   * @param fragment
   */
  public void addFragment(String fragment) {
    fragments.add(fragment);
  }

  /**
   * Return the fragments in a read-only way.
   * @return an Iterator over the fragments
   */
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
