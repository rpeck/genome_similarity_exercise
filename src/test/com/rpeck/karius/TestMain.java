package com.rpeck.karius;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

class TestMain {

  // TODO: use Java resource magic to find the files.
  private static final String testFile1 = "/Users/rpeck/Source/Interviews/Similarity_Exercise_Raymond/data/GCF_000018125.1_ASM1812v1_genomic.fna.gz";
  private static final String testFile2 = "/Users/rpeck/Source/Interviews/Similarity_Exercise_Raymond/data/GCF_000013505.1_ASM1350v1_genomic.fna.gz";

  @BeforeEach
  void setUp() {
  }

  @AfterEach
  void tearDown() {
  }

  @Test
  void testFileParse() throws Exception {
    FastaFile ff = FastaParser.parseSingleFile(new File(testFile1).toPath());
    assertEquals("GCF_000018125.1_ASM1812v1", ff.getOrganism());
    assertEquals("NC_011375.1", ff.getVersion());
    assertEquals("Streptococcus pyogenes NZ131, complete genome", ff.getDescription());

    Iterator i = ff.fragments();
    assertEquals("TTGTTGATATTCTGTTTTTTCTTTTTTAGTTTTCCACATAAAAAATAGTTGAAAACAATAGCGGTGTCACCTTAAAATGA",
            i.next());
    assertEquals("CTTTTCCACAGGTTGTGGAGAACCCAAATTAACAGTGTTAATTTATTTTCCACAGATTGTGGAAAAACTAACTATTATCC",
            i.next());

    int remainingFragements = 0;
    while (i.hasNext()) {
      remainingFragements++;
      i.next();
    }

    assertEquals(22696, remainingFragements);
  }

  @Test
  void testIupacChecking() {
    String iupac = "TGATGCCCGTCTATTAAAGGTCGATAAGCATATTGCAACTATTTACTTAGATCAAATGAAAGARCTCTTTTGGGAAAAAA";
    String noIupac = "TGATGCCCGTCTATTAAAGGTCGATAAGCATATTGCAACTATTTACTTAGATCAAATGAAAGACTCTTTTGGGAAAAAA";

    assertTrue(FastaParser.containsIupacCodes(iupac));
    assertFalse(FastaParser.containsIupacCodes(noIupac));
  }

  @Test
  void testOrganismCreation() throws Exception {
    FastaFile ff = FastaParser.parseSingleFile(new File(testFile1).toPath());

    Iterator<String> i = ff.fragments();
    int iupacCount = 0;
    while (i.hasNext()) {
      if (FastaParser.containsIupacCodes(i.next()))
        iupacCount++;
    }
    assertEquals(38, iupacCount);

    // one k-mer [long enough] line
    Organism o1 = FastaParser.representOrganism(ff, 80);
    assertEquals("GCF_000018125.1_ASM1812v1", o1.getOrganism());
    assertEquals("NC_011375.1", o1.getVersion());
    assertEquals("Streptococcus pyogenes NZ131, complete genome", o1.getDescription());
    assertEquals(22697 - iupacCount, o1.totalKmers()); // NOTE: last line isn't 80, so no k-mer!

    // two k-mers per [long-enough] line
    Organism o2 = FastaParser.representOrganism(ff, 79);
    assertEquals("GCF_000018125.1_ASM1812v1", o2.getOrganism());
    assertEquals("NC_011375.1", o2.getVersion());
    assertEquals("Streptococcus pyogenes NZ131, complete genome", o2.getDescription());
    // assume iupac chars aren't the first or last:
    assertEquals(2 * (22697 - iupacCount) - o2.duplicateHashes(), o2.totalKmers()); // NOTE: last line isn't 80, so no k-mer!

    // one k-mer per unique character
    Organism o3 = FastaParser.representOrganism(ff, 1);
    assertEquals("GCF_000018125.1_ASM1812v1", o3.getOrganism());
    assertEquals("NC_011375.1", o3.getVersion());
    assertEquals("Streptococcus pyogenes NZ131, complete genome", o3.getDescription());
    // should have only 4: A, C, G and T:
    assertEquals(4, o3.totalKmers()); // NOTE: de-duped char count
  }

  @Test
  void testKmerMatching() {
    Organism o1 = new Organism("org1", "v1", "desc");
    o1.addFragment("ABCD", 4);

    Organism o2 = new Organism("org2", "v1", "desc");
    o2.addFragment("QABCDZ", 4);

    assertEquals(1, o1.intersect(o1).size());
    assertEquals(3, o2.intersect(o2).size());
    assertEquals(1, o1.intersect(o2).size());
    assertEquals(1, o2.intersect(o1).size());

    Organism o3 = new Organism("org3", "v1", "desc");
    o3.addFragment("9ABCDAQBCDABCDZ", 4);
    assertEquals(1, o1.intersect(o3).size()); // ABCD is a dupe; not counted twice
    assertEquals(1, o3.intersect(o1).size()); // ABCD is a dupe; not counted twice
    assertEquals(10, o3.intersect(o3).size());

    Organism o4 = new Organism("org4", "v1", "desc");
    o4.addFragment("ABCDBCDA", 4);
    assertEquals(2, o3.intersect(o4).size()); // ABCD BCDA
    assertEquals(2, o4.intersect(o3).size()); // ABCD BCDA
    assertEquals(5, o4.intersect(o4).size());
  }
}