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

  private static Set<Byte> iupacCodeSet = new HashSet<>();
  static {
    for (char c : new char[] {'R', 'Y', 'S', 'W', 'K', 'M', 'B', 'D', 'H', 'V', 'N', '.', '-'})
      iupacCodeSet.add((byte)c);
  }

  /**
   * Create a FastaParser for the given directory.
   *
   * @param path path in the local filesystem for the FASTA files
   */
  public FastaParser(String path) {
    inputDir = new File(path);
  }

  /**
   * Parse a single FASTA file and return the raw input data.
   */
  static public FastaFile parseSingleFile(Path path) throws IOException {
    GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(path.toFile()));
    BufferedReader reader = new BufferedReader(new InputStreamReader(gzip));

    String header = reader.readLine();
    StringTokenizer st = new StringTokenizer(header);
    String version = st.nextToken().substring(1); // strip the command char
    String description = st.nextToken("").substring(1); // return the rest of the line; strip the leading space

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
  static public boolean containsIupacCodes(String fragment) {
    for (char c : fragment.toCharArray()) {
      if (iupacCodeSet.contains((byte)c))
        return true;
    }
    return false;
  }

  /**
   * Represent an organism internally as a Set of kmers for efficient comparisons.
   */
  static public Organism representOrganism(FastaFile ff, int kmerLen) {
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
   * Parse all the FASTA files in <code>inputDir</code>. Skips individual bad files.
   * @throws IOException
   */
  public List<Organism> parseAllFiles(boolean verbose, int kmerLen) throws IOException {
    List<Organism> organisms = new ArrayList<>();

    if (! inputDir.exists())
      throw new FileNotFoundException("Specified path to FASTA files does not exist: " + inputDir);
    if (! inputDir.canRead())
      throw new FileNotFoundException("Specified path to FASTA files is not readable: " + inputDir);
    if (! inputDir.isDirectory())
      throw new FileNotFoundException("Specified path to FASTA files is not a directory: " + inputDir);

    // TODO: try/catch inside the loop so that we get partial results if some files are bad
      List<Path> fastaPaths =
              Files.walk(Paths.get(inputDir.getCanonicalPath()))
                      .filter(Files::isRegularFile)
                      .filter(Files::isReadable)
                      .collect(Collectors.toList());
      for (Path p : fastaPaths) {
        if (verbose) System.out.println("Parsing FASTA file: " + p.toString());

        FastaFile ff;
        try {
          ff = parseSingleFile(p);
          Organism o = representOrganism(ff, kmerLen);
          ff = null; // try to get this to GC... weak references might be necessary instead.
          organisms.add(o);
        } catch (IOException e) {
          System.err.println("Caught IO Exception parsing FASTA file: " + p + ": " + e);
        }
      }

    return organisms;
  }

}
