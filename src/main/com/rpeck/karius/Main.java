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
 * kmers-matcher is a program to identify organisms that have similar genomes by
 * finding the number of common subsequences (k-mers).<p>
 *
 * Some notes on correctness and space and time efficiency:<p>
 *   1. There are easy opportunities for multithreading, which will likely be
 *   necessary for real datasets. I'll discuss those below.
 *
 *   2. Generation of the k-mers from the input files is O(n) in the input space
 *   (O(lineLength - kmerSize))
 *
 *   3. Standard Java Sets are used to hold hashes of each k-mer. Currently the
 *   hashes are 32 bits. Testing with real data is needed to determine whether there
 *   are too many collisions in the Sets. This can be done by replacing the Sets with
 *   Maps that point to the full k-mer Strings and doing a full-String compare if
 *   there's a write collision. In addition, this would be used to verify that
 *   k-mers identified by the set intersection weren't false matches.
 *
 *   If there are too many collisions the first thing to do is try a better 32-bit
 *   String hash function: String.hashCode isn't the best. If that doesn't do well
 *   enough we can either replace the Sets with the Maps and always do full equality
 *   tests, or move to a 64-bit hash. Both would be at a linear cost of both space and
 *   time.
 *
 *   4. The number of comparisons is n^2 - n, so that operation is likely to be
 *   the bottleneck. Java Set intersection might not be a fast enough operation. If not,
 *   I'd first look around for a Set implementation that uses hashes as an index into
 *   a bit array; I forget what this technique is called. If there isn't a good one,
 *   write one. Set intersection could also be parallelized easily, since intersection is
 *   a read-only operation and we really only need a count of matches.
 *
 *   5. The parsing of the input files, and generation of the Organism objects, can
 *   be trivially parallelized. The only contention would be on the list of Organisms.
 *
 *   6. The pairwise comparisons can be dispatched to multiple threads, but most likely
 *   it's good enough to have one controller thread walking the pairs while the
 *   parallel set intersection gives you all the parallelism you can handle.
 *
 *   7. If multithreaded-on-one-CPU doesn't give you enough speedup then the work
 *   should be partitioned at a much coarser granularity. E.g., one could break up
 *   the dataset into 4 pieces, and have one machine compare chunk A against chunks
 *   B-D and so on.
 *
 *   8. It's a harder problem to distribute if the amount of DRAM for the Sets exceeds
 *   the size of the biggest machines, since you still have to compare all-against-all...
 *   Hopefully it doesn't come to that.
 *
 *   9. If the amount of text is huge then take care to make sure the FastaFile objects
 *   are GC'd as the k-mers are computed. They're not needed after that. We could also
 *   represent the characters as 2-bit values in a bit string, rather than 16-bit Java
 *   chars.
 */
public class Main {

  /**
   * Compute organisms that have similar genomes by:<p>
   *   reading a directory of FASTA files for various organisms<p>
   *   computing hashes for all the subsequences of size --kmer and putting those in Sets<p>
   *   computing the Set intersection for each pair of organisms<p>
   *   comparing the size of the intersection to the average number of kmers in the pair and
   *     outputting the pair if it's a larger fraction than --threshold<p>
   *
   *   TODO: move exception handling into here if we want to generate partial results on IOException.
   *   TODO: break up into generation and output to make it easier to unit test, and
   *     make it streaming between the generation and the output.
   *
   * @param outputPath local file path to which we should write the sparse similarity matrix
   * @param organisms list of Organism objects so pairwise-compare
   * @param threshold fraction that two organisms need to be similar before we will write them to the output
   */
  protected static void computeIntersectionsAndWriteOutput(String outputPath,
                                                           List<Organism>organisms,
                                                           float threshold) throws IOException {

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
        // the threshold would not be symmetric, so I'll use the average.
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

    // TODO: move value parsing into a CommandLine wrapper class. It's an abomination
    // that that CommandLine doesn't have better error handling capabilities, including
    // default values...
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
