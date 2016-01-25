/**
 *  This class implements the OR operator for all retrieval models.
 *  
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 *
 *  @author Xinkai Wang
 */

import java.io.*;
import java.util.*;

public class QryopSlOr extends QryopSl {

  /**
   *  It is convenient for the constructor to accept a variable number
   *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
   *  @param q A query argument (a query operator).
   */
  public QryopSlOr(Qryop... q) {
    for (int i = 0; i < q.length; i++)
      this.args.add(q[i]);
  }

  /**
   *  Appends an argument to the list of query operator arguments.  This
   *  simplifies the design of some query parsing architectures.
   *  @param {q} q The query argument (query operator) to append.
   *  @return void
   *  @throws IOException
   */
  public void add (Qryop a) {
    this.args.add(a);
  }

  /**
   *  Evaluates the query operator, including any child operators and
   *  returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluate(RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean ||
    		r instanceof RetrievalModelRankedBoolean)
      return (evaluateBoolean (r));

    return null;
  }

  /**
   *  Evaluates the query operator for boolean retrieval models,
   *  including any child operators and returns the result.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @return The result of evaluating the query.
   *  @throws IOException
   */
  public QryResult evaluateBoolean (RetrievalModel r) throws IOException {

    //  Initialization

    allocDaaTPtrs (r);
    QryResult result = new QryResult ();

    //  Exact-match OR requires that ALL scoreLists contain a
    //  document id.  Use the first (shortest) list to control the
    //  search for matches.

    //  Named loops are a little ugly.  However, they make it easy
    //  to terminate an outer loop from within an inner loop.
    //  Otherwise it is necessary to use flags, which is also ugly.

    int daatSize = this.daatPtrs.size();
    int daatAvailable = daatSize;
    
    while (daatAvailable > 0) {
    	double docScore = 0.0;
    	
    	// Get the minimum docid in the daat
    	int minDocid = Integer.MAX_VALUE;
    	for (int i = 0; i < daatSize; i++) {
        	if (this.daatPtrs.get(i).nextDoc >= this.daatPtrs.get(i).scoreList.scores.size()) {
        		daatAvailable--;
        		continue;
        	}
        	DaaTPtr ptri = daatPtrs.get(i);
        	minDocid = Math.min(minDocid, ptri.scoreList.getDocid(ptri.nextDoc));
        }
    	
	    for (int i = 0; i < daatSize; i++) {
	    	if (this.daatPtrs.get(i).nextDoc >= this.daatPtrs.get(i).scoreList.scores.size())
	    		continue;
	    	DaaTPtr ptri = daatPtrs.get(i);
	    	if (ptri.scoreList.getDocid(ptri.nextDoc) == minDocid) {
	    		docScore = Math.max(docScore, ptri.scoreList.getDocidScore(ptri.nextDoc));
	    		ptri.nextDoc++;
	    	}
	    }
	    
	    if (docScore > 0.0) {
	    	if (r instanceof RetrievalModelRankedBoolean) {
	    		result.docScores.add (minDocid, docScore);
	    	}
	    	else if (r instanceof RetrievalModelUnrankedBoolean){
	    		result.docScores.add(minDocid, 1.0);
	    	}
	    }
    }

    freeDaaTPtrs ();

    return result;
  }

  /*
   *  Calculate the default score for the specified document if it
   *  does not match the query operator.  This score is 0 for many
   *  retrieval models, but not all retrieval models.
   *  @param r A retrieval model that controls how the operator behaves.
   *  @param docid The internal id of the document that needs a default score.
   *  @return The default score.
   */
  public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean)
      return (0.0);

    return 0.0;
  }

  /*
   *  Return a string version of this query operator.  
   *  @return The string version of this query operator.
   */
  public String toString(){
    
    String result = new String ();

    for (int i=0; i<this.args.size(); i++)
      result += this.args.get(i).toString() + " ";

    return ("#OR( " + result + ")");
  }

}
