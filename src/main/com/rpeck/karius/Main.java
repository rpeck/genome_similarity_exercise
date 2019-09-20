package com.rpeck.karius;

import org.apache.commons.cli.CommandLine;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * kmers-matcher is a program which uses kmers to find genomes which have a large number
 * of common subsequences (kmers).
 */
public class Main {
  // TODO: break up into generation and output to make it easier to unit test, and
  // make it streaming.

  /**
   * Compute organisms that have similar genomes by:<p>
   *   reading a directory of FASTA files for various organisms<p>
   *   computing hashes for all the subsequences of size --kmer and putting those in Sets<p>
   *   computing the Set intersection for each pair of organisms<p>
   *   comparing the size of the intersection to the average number of kmers in the pair and
   *     outputting the pair if it's a larger fraction than --threshold<p>
   *
   *   TODO: move exception handling into here if we want to generate partial results on IOException.
   *
   * @param outputPath
   * @param organisms
   * @param threshold
   */
  protected static void computeIntersectionsAndWriteOutput(String outputPath,
                                                           List<Organism>organisms, float threshold) throws IOException {

    File f = new File(outputPath);
    File d = f.getParentFile();
    if (! d.exists()) {
      d.mkdirs();
    }

    BufferedWriter writer = new BufferedWriter(new FileWriter(f));

    // generate and evaluate all intersections
    for (int i = 0; i < organisms.size(); i++) {
      for (int j = i + 1; j < organisms.size(); j++) {
        Organism first = organisms.get(i);
        Organism second = organisms.get(j);
        Set<Integer> intersection = first.intersect(second);

        // TODO: what does the threshold mean if the number of fragments differ?
        // Is it a fraction of the total number of kmers? Of the first in the
        // comparison? The average? If it's a fraction of only the first then
        // the threshold would not be symmetric, so I'll take this as the average.
        float averageKmers = (first.totalKmers() + second.totalKmers()) / 2.0f;
        float scaledThreshold = averageKmers * threshold;
        if (intersection.size() > scaledThreshold) {
          writer.write(first.getOrganism() + " X " + second.getOrganism() + ": "
                  + (intersection.size() / averageKmers) + "\n");
        }
      }
    }
    writer.close();
  }

  public static void main(String[] args) {
    CommandLine cmd = ArgParser.parseArgs(args);
    boolean verbose = cmd.hasOption("verbose");
    List<Organism> organisms = new ArrayList<>(); // no nulls, please...
    float threshold = 1.0f;

    try {
      threshold = Float.valueOf(cmd.getOptionValue("threshold"));
      FastaParser parser = new FastaParser(cmd.getOptionValue("path"));
      organisms = parser.parseAllFiles(verbose, Integer.valueOf(cmd.getOptionValue("kmer")));
    }
    catch (IOException ioe) {
      System.err.println("Exception parsing FASTA input files: " + ioe);
      System.exit(-1);
    }
    catch (NumberFormatException e) {
      // TODO: move value parsing into a CommandLine wrapper class. It's an abomination
      // that that CommandLine doesn't have better error handling options with defaults...
      System.err.println("Exception parsing command-line arguments: " + e);
      System.exit(-1);
    }

    try {
      computeIntersectionsAndWriteOutput(cmd.getOptionValue("output"), organisms, threshold);
    }
    catch (IOException ioe) {
      System.err.println("Exception writing output file: " + ioe);
      System.exit(-1);
    }
  }

}
