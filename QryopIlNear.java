/**
 *  This class implements the NEAR/n operator for all retrieval models.
 *  The near operator creates a new inverted list that is the intersection
 *  of its constituents.  Typically it is used for find documents where
 *  distance of arguments in near operand are all between n
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 *  
 *  @author Xinkai Wang
 */

import java.io.*;
import java.util.*;

public class QryopIlNear extends QryopIl {

	  /**
	   * nearN represents the max distance between each arguments in
	   * NEAR/n operand
	   */
	  private int nearN;
	  private QryResult qResult = null;
	  
	  /**
	   *  It is convenient for the constructor to accept a variable number
	   *  of arguments, with max distance between each of them is no more than n. 
	   *  Thus new QryopIlSyn (arg1, arg2, arg3, ...).
	   */
	  public QryopIlNear(int n, Qryop... q) {
		nearN = n;
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

	    //  Initialization

	    allocDaaTPtrs (r);
	    syntaxCheckArgResults (this.daatPtrs);

	    QryResult result = new QryResult ();
	    
	    result.invertedList.field = new String (this.daatPtrs.get(0).invList.field);
	    
	    int daatSize = this.daatPtrs.size();
	    int daatAvailable = daatSize;
	    // if a daat has come to an end, then all relevant results have been found
	    while (daatAvailable == daatSize) {
	    	int nextDocid = getSmallestCurrentDocid ();
	    	// if all of the daat contains the nextDocid, then findNextDocid is true
	    	boolean findNextDocid = true; 
	    	for (int i = 0; i < daatSize; i++) {
	    		DaaTPtr ptri = this.daatPtrs.get(i);
	    		// If docid in current daat is smaller than nextDocid, then increment the nextDoc ptr
	    		while (ptri.nextDoc < ptri.invList.postings.size() && 
	    				ptri.invList.getDocid(ptri.nextDoc) < nextDocid) {
	    			ptri.nextDoc++;
	    		}
	    		// If nextDocid is not found in ptri, then the docid is not valid for later process
	    		if (ptri.nextDoc < ptri.invList.postings.size() && 
	    				ptri.invList.getDocid(ptri.nextDoc) != nextDocid) {
	    			findNextDocid = false;
	    		}
	    		// If ptri has reached the end of its size, then it's also unavailable to later process
	    		if (ptri.nextDoc >= ptri.invList.postings.size()) {
	    			daatAvailable--;
	    			findNextDocid = false;
	    			break;
	    		}
	    		if (findNextDocid == false)	break;
	    	}
	    	
	    	// positionAvailable is to record whether all the positions in a daat has been traversed
	    	// If so, decrement the var
	    	int positionAvailable = daatSize;
	    	
	    	// if the document id is found which contains all terms
	    	if (findNextDocid == true) {
	    		// Record all valid positions in daat0 of the nextDocid
	    		List<Integer> validPositions = new ArrayList<Integer>();
	    		// record index of each daat current posting position
	    		int[] positionIdx = new int[daatSize];
	    		
	    		for (int i = 0; i < daatSize; i++) {
	    			positionIdx[i] = 0;
	    		}
	    		while (positionAvailable == daatSize) {
	    			DaaTPtr daat0 = this.daatPtrs.get(0);
	    			if (daat0.invList.postings.get(daat0.nextDoc).positions.size() <= positionIdx[0]) {
	    				break;
	    			}
	    			int curPos = daat0.invList.postings.get(daat0.nextDoc).positions.get(positionIdx[0]);
	    			
	    			List<Integer> positions = new ArrayList<Integer>();
	    			positions.add(curPos);
		    		for (int i = 1; i < daatSize; i++) {
		    			DaaTPtr ptri = this.daatPtrs.get(i);
		    			// if position of next word in the doc is small than the current word position,
	    				// then continue search for the next available position
	    				while (ptri.invList.postings.get(ptri.nextDoc).positions.size() > positionIdx[i]
	    						&& ptri.invList.postings.get(ptri.nextDoc).positions.get(positionIdx[i]) < curPos) {
	    					positionIdx[i]++;
	    				}
	    				if (ptri.invList.postings.get(ptri.nextDoc).positions.size() <= positionIdx[i]) {
	    					positionAvailable--;
	    					break;
	    				}
	    				
	    				// Compare position index between two near words
		    			if (ptri.invList.postings.get(ptri.nextDoc).positions.get(positionIdx[i]) 
		    					- curPos <= nearN) {
		    				curPos = ptri.invList.postings.get(ptri.nextDoc).positions.get(positionIdx[i]);
		    				positions.add(curPos);
		    				
		    				// If i comes to the last ptri, then store the position of ptr0
		    				// as well as increment the each daat position index
		    				if (i == daatSize - 1) {
		    					
	    						validPositions.add(positions.get(0));
		    					
		    					for (int j = 0; j <= i; j++) {
		    						positionIdx[j]++;
		    						DaaTPtr ptrj = this.daatPtrs.get(j);
		    						if (ptrj.invList.postings.get(ptrj.nextDoc).positions.size() <= positionIdx[j]) {
		    	    					positionAvailable--;
		    	    					break;
		    	    				}
			    					
		    					}
		    				}
		    				continue;
		    			}
		    			else {
		    				// If valid position in a daat is not found, then increment former daat position index
		    				// and then restart the position finding process
		    				positionIdx[i-1]++;
		    				break;
		    			}
		    		}
	    		}
	    		
	    		// Append all valid positions in ptr0 to nextDocid's postings for further tf calculation
	    		if (validPositions.size() > 0)
	    			result.invertedList.appendPosting(nextDocid, validPositions);
	    	}
	    	// Increment the nextDocid var after finding all the position infomation
	    	for (int i = 0; i < daatSize; i++) {
	    		DaaTPtr ptri = this.daatPtrs.get(i);
	    		while (ptri.nextDoc < ptri.invList.postings.size() &&
	    				ptri.invList.getDocid(ptri.nextDoc) <= nextDocid) {
	    			ptri.nextDoc++;
	    		}
	    		if (ptri.nextDoc >= ptri.invList.postings.size()) {
	    			daatAvailable--;		    			
	    			break;
	    		}
    		}
	    }

	    freeDaaTPtrs();
	    
	    qResult = result;

	    return qResult;
	  }
	  
	  /**
	   *  Convert from inverted list to score list for score calculation convenience
	   *  @return the result with only contains score list, and empty inverted list
	   *  (Currently not used in HW1, reserved for later assignments use)
	   */
	  public QryResult ConvertToScoreList() {
		  
		  if (qResult == null) {
			  return null;
		  }
		  
		  if (qResult.docScores.scores.size() != 0)
			  return qResult;
		  
		  if (qResult.invertedList.postings.size() == 0)	return null;
		  
		  InvList newInvList = new InvList();
		  
		  for (int i = 0; i < qResult.invertedList.postings.size(); i++) {
			  int cnt = 1;
			  int docid = qResult.invertedList.postings.get(i).docid;
			  List<Integer> pos = new ArrayList<Integer>();
			  for (int j = 0; j < qResult.invertedList.postings.get(i).positions.size(); j++)
				  pos.add(qResult.invertedList.postings.get(i).positions.get(j));
			  while (i + 1 < qResult.invertedList.postings.size() && 
					  qResult.invertedList.postings.get(i+1).docid == docid) {
				  cnt++;
				  i++;
				  for (int j = 0; j < qResult.invertedList.postings.get(i+1).positions.size(); j++)
					  pos.add(qResult.invertedList.postings.get(i+1).positions.get(j));
			  }
			  newInvList.appendPosting(docid, pos);
		  }
		  
		  qResult.invertedList = newInvList;
		  return qResult;
	  }

	  /**
	   *  Return the smallest unexamined docid from the DaaTPtrs.
	   *  @return The smallest internal document id.
	   */
	  public int getSmallestCurrentDocid () {

	    int nextDocid = Integer.MAX_VALUE;

	    for (int i=0; i<this.daatPtrs.size(); i++) {
	      DaaTPtr ptri = this.daatPtrs.get(i);
	      if (nextDocid > ptri.invList.getDocid (ptri.nextDoc))
		nextDocid = ptri.invList.getDocid (ptri.nextDoc);
	      }

	    return (nextDocid);
	  }

	  /**
	   *  syntaxCheckArgResults does syntax checking that can only be done
	   *  after query arguments are evaluated.
	   *  @param ptrs A list of DaaTPtrs for this query operator.
	   *  @return True if the syntax is valid, false otherwise.
	   */
	  public Boolean syntaxCheckArgResults (List<DaaTPtr> ptrs) {

	    for (int i=0; i<this.args.size(); i++) {

	      if (! (this.args.get(i) instanceof QryopIl)) 
		QryEval.fatalError ("Error:  Invalid argument in " +
				    this.toString());
	      else
		if ((i>0) &&
		    (! ptrs.get(i).invList.field.equals (ptrs.get(0).invList.field)))
		  QryEval.fatalError ("Error:  Arguments must be in the same field:  " +
				      this.toString());
	    }

	    return true;
	  }

	  /*
	   *  Return a string version of this query operator.  
	   *  @return The string version of this query operator.
	   */
	  public String toString(){
	    
	    String result = new String ();

	    for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
	      result += (i.next().toString() + " ");

	    return ("#NEAR/n( " + result + ")");
	  }
	}
