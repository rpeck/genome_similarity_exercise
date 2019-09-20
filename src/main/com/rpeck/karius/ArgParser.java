package com.rpeck.karius;

import org.apache.commons.cli.*;

public class ArgParser {
  static void usage(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("kmer-matcher", options);
  }

  static CommandLine parseArgs(String[] args) {
    Options options = new Options();

    options.addOption(
            Option.builder("?")
                    .type(Boolean.class)
                    .longOpt("help")
                    .required(false)
                    .desc("print usage info").build());

    options.addOption(
            Option.builder("v")
                    .type(Boolean.class)
                    .longOpt("verbose")
                    .required(false)
                    .desc("print verbose output to stdout").build());

    options.addOption(
            Option.builder("p")
                    .type(String.class)
                    .longOpt("path")
                    .required()
                    .hasArg()
                    .desc("path to the fasta files").build());

    options.addOption(
            Option.builder("o")
                    .type(String.class)
                    .longOpt("output")
                    .required()
                    .hasArg()
                    .desc("output path for the similarity matrix").build());

    options.addOption(
            Option.builder("k")
                    .type(Integer.class)
                    .longOpt("kmer")
                    .required(true)
                    .hasArg()
                    .desc("length of kmers to compare").build());

    options.addOption(
            Option.builder("t")
                    .type(Float.class)
                    .longOpt("threshold")
                    .required(true)
                    .hasArg()
                    .desc("fraction of kmers that must match to include a possible genome match in the output, e.g. 0.9").build());

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(options, args);
    }
    catch (ParseException e) {
      usage(options);
      System.exit(-1);
    }
    if (null == cmd) {
      usage(options);
      System.exit(-1);
    }

    if (cmd.hasOption("?"))
      usage(options);

    if (cmd.hasOption("verbose"))
      System.out.println("Running: kmer-matcher."); // TODO: richer; list command-line option values

    // TODO: add validations, e.g. --kmer must be > 0
    return cmd;
  }
}
