/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.util.*;

public class ScoreList {

  //  A little utility class to create a <docid, score> object.

  protected class ScoreListEntry implements Comparable<ScoreListEntry>{
    private int docid;
    private double score;
    private String externDocid;

    private ScoreListEntry(int docid, double score) {
      this.docid = docid;
      this.score = score;
      try {
    	  this.externDocid = QryEval.getExternalDocid(docid);
      }
      catch(Exception e) {
    	  e.printStackTrace();
      }
    }
    
    // A compare function to sort the ScoreListEntry by score first, then by docid
    public int compareTo(ScoreListEntry compareEntry) {
    	double compareScore = compareEntry.score;
    	if (this.score != compareScore)	return (int)(compareScore - this.score);
    	return this.externDocid.compareTo(compareEntry.externDocid);
    }
  }

  List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *  Append a document score to a score list.
   *  @param docid An internal document id.
   *  @param score The document's score.
   *  @return void
   */
  public void add(int docid, double score) {
    scores.add(new ScoreListEntry(docid, score));
  }

  /**
   *  Get the n'th document id.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  /**
   *  Get the score of the n'th document.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }

}
