package com.rpeck.karius;

import org.apache.commons.cli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class Main {
  public int one() {
    return 1;
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
          System.out.println(first.getOrganism() + " X " + second.getOrganism() + ": "
                  + (intersection.size() / averageKmers));
        }
      }
    }
  }


}
