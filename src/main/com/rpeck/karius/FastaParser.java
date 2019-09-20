package com.rpeck.karius;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * File parsing utilities for reading a directory of FASTA files.
 */
public class FastaParser {
  private File inputDir = null;

  private static final char[] iupacCodes = {'R', 'Y', 'S', 'W', 'K', 'M', 'B', 'D', 'H', 'V', 'N', '.', '-'};
  private static final Set<Byte> iupacCodeSet = new HashSet(Arrays.asList(iupacCodes));

  /**
   * Create a FastaParser for the given directory.
   *
   * TODO: throw better exception types
   * @param path path in the local filesystem for the FASTA files
   */
  public FastaParser(String path) throws FileNotFoundException {
    inputDir = new File(path);

    if (! inputDir.exists())
      throw new FileNotFoundException("Specified path to FASTA files does not exist: " + path);
    if (! inputDir.canRead())
      throw new FileNotFoundException("Specified path to FASTA files is not readable: " + path);
  }

  /**
   * Parse a single FASTA file and return the raw input data.
   */
  public FastaFile parseSingleFile(Path path) throws IOException {
    GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(path.toFile()));
    BufferedReader reader = new BufferedReader(new InputStreamReader(gzip));

    String header = reader.readLine();
    StringTokenizer st = new StringTokenizer(header);
    String version = st.nextToken();
    String description = st.nextToken(""); // return the rest of the line

    FastaFile ff = new FastaFile(
            path.getFileName().toString(),
            version,
            description
            );
    for (String fragment : reader.lines().collect(Collectors.toList())) {
      ff.addFragment(fragment);
    }
    return ff;
  }

  /**
   * Does this fragment contain IUPAC codes?
   */
  private boolean containsIupacCodes(String fragment) {
    for (char c : fragment.toCharArray()) {
      if (iupacCodeSet.contains(c))
        return true;
    }
    return false;
  }

  /**
   * Represent an organism internally as a bunch of kmers for efficient comparisons.
   */
  public Organism representOrganism(FastaFile ff, int kmerLen) {
    Organism o = new Organism(
            ff.getOrganism(),
            ff.getVersion(),
            ff.getDescription());

    ListIterator<String> i = ff.fragments();
    while (i.hasNext()) {
      String fragment = i.next();
      if (! containsIupacCodes(fragment))
        o.addFragment(fragment, kmerLen);
    }

    return o;
  }

  /**
   * Parse all the FASTA files in <code>inputDir</code>.
   * @throws IOException
   */
  public void parseAllFiles(boolean verbose, int kmerLen) throws IOException {
    try {
      List<Path> fastaPaths =
              Files.walk(Paths.get(inputDir.getCanonicalPath()))
                      .filter(Files::isRegularFile)
                      .filter(Files::isReadable)
                      .collect(Collectors.toList());
      for (Path p : fastaPaths) {
        if (verbose) System.out.println("Parsing FASTA file: " + p.toString());

        FastaFile ff = parseSingleFile(p);
        Organism o = representOrganism(ff, kmerLen);
      }
    }
    catch (IOException e) {
      System.err.println("Caught IO Exception parsing FASTA files from " + inputDir + ": " + e);
      throw e;
    }
  }

}
