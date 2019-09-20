package com.rpeck.karius;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Organism {
  private String organism;
  private String version;
  private String description;

  /*
   * Most likely we'll get a lot of collisions if we use a 32-bit String hash
   * like String.hashCode(). If so, try a 64-bit one.
   * <br>
   * TODO: test for collisions by using a Map<Long, String> and exact comparisons
   * for the kmers, and if we still get a lot of collisions with a 64-bit hash
   * we'll need to add exact comparisons to the matching.
   */
  private Set<Integer> kmers;

  /**
   * The internal representation of an organism's genome fragments, optimized for kmer matching.
   * These fragments have been cleaned of IUPAC codes.
   *
   * @param organism
   * @param version
   * @param description
   */
  public Organism(String organism, String version, String description) {
    this.organism = organism;
    this.version = version;
    this.description = description;
    this.kmers = new HashSet<>();
  }

  public void addFragment(String fragment, int kmerLen) {
    // Walk across the fragment generating hashes for each contained kmerLen subfragment
    // Note that we're creating and throwing away a lot of Strings here, which isn't
    // super efficient. If this turns out to perform poorly implement a version of
    // String.hashCode() which takes a range. If the sequences are long we might do better
    // by using a char[] for the fragments, or using CharSequence. PROFILE!
    //
    //
    // TODO: add test to check for off-by-one errors: for now I've only manually tested...
    for (int i = 0; i < fragment.length() - kmerLen + 1; i++) {
      kmers.add(fragment.substring(i, i + kmerLen).hashCode());
    }
  }

  public Iterator<Integer> kmers() {
    return kmers.iterator();
  }

  public Set<Integer> intersect(Organism other) {
    Set<Integer> intersection = new HashSet<Integer>(this.kmers);
    intersection.retainAll(other.kmers);
    return intersection;
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

  public int totalKmers() {
    return kmers.size();
  }

}
