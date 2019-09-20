package com.rpeck.karius;

import org.apache.commons.cli.*;

import java.io.IOException;


public class Main {
  public int one() {
    return 1;
  }

  public static void main(String[] args) {
    CommandLine cmd = ArgParser.parseArgs(args);
    boolean verbose = cmd.hasOption("verbose");

    try {
      FastaParser parser = new FastaParser(cmd.getOptionValue("path"));
      parser.parseAllFiles(verbose, Integer.valueOf(cmd.getOptionValue("k")));
    }
    catch (IOException ioe) {
      System.err.println("Exception parsing FASTA input files: " + ioe);
      System.exit(-1);
    }
    catch (NumberFormatException e) {
      System.err.println("Exception parsing command-line arguments: " + e);
      System.exit(-1);
    }

  }


}
