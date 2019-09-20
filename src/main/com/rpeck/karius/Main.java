package com.rpeck.karius;

import org.apache.commons.cli.*;


public class Main {
  public int one() {
    return 1;
  }

  public static void main(String[] args) {
    CommandLine cmd = ArgParser.parseArgs(args);
    
  }


}
