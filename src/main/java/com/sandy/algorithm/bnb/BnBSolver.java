package com.sandy.algorithm.bnb;

import java.util.Arrays;

import org.apache.log4j.Logger;

/** 
 * An implementation of Branch and Bounds algorithm.
 * <p>
 * http://en.wikipedia.org/wiki/Branch_and_bound
 * <p>
 * This solver provide a convenient and reasonably performant algorithm to tackle
 * data sets in the range of 100-500 elements in a reasonable time. Please note
 * that the problem this algorithm tries to solve is NP hard and although this
 * algorithm uses a tree pruning algorithm, the upper bound of time complexity
 * is largely influenced by the arrangement of data set.
 * <p>
 * For example, this algorithm can chew through three different test data sets
 * of 100 elements in 2 sec, 30 sec and 14 milli seconds respectively. Hence,
 * please keep your expectations bounded while using this solver.
 * <p>
 * Using this solver is quite simple - during construction the following 
 * values are expected:
 * <ul>
 * 	<li>Capacity of the container/knapsack</li>
 * 	<li>Array of weights of elements</li>
 * 	<li>[Optional] Array of values of elements. If this is not provided, 
 *      the weights are considered as values of the elements.</li>
 * </ul>
 * 
 * The goal of the solver is to return an array of integers (of size equal to
 * number of items) with the value at each index either 0 or 1. 0 implies that
 * the value was not selected, 1 implies that it is a part of the optimal 
 * solution.
 * <p>
 * The <code>getObjectiveValue</code> method returns the optimal value (not weight)
 * of the items chosen by the solution.
 * <p>
 * NOTE: This is not a thread safe class - use a new instance for each thread 
 * or solution.
 */
public class BnBSolver {
	
	private static final Logger logger = Logger.getLogger( BnBSolver.class ) ;
	
	private int     numItems   = 0 ;
	private double  ksCapacity = 0 ;
	private Input[] inputs     = null ;
	
	private double  objectiveValue = 0 ;
	private int[]   bestPath       = null ;

	private double  minWt          = Double.MAX_VALUE ;
	private Input[] uvSortedInputs = null ;
	private int[]   taken          = null ;
	
	private boolean LOG = false ;
	
	public BnBSolver( double ksCapacity, double[] weights ) {
		this( ksCapacity, weights, weights ) ;
	}
	
	public BnBSolver( double ksCapacity, double[] weights, double[] values ) {
		
		this.numItems       = weights.length ;
		this.ksCapacity     = ksCapacity ;
		this.inputs         = new Input[ this.numItems ] ;
		this.uvSortedInputs = new Input[ this.numItems ] ;
		this.taken          = new int[ numItems ] ;
		this.bestPath       = new int[ numItems ] ;
		
		for( int i=0; i<numItems; i++ ) {
			
			double weight = weights[i] ;
			double value  = values[i] ;
			
			Input input = new Input( i, value, weight ) ;
			inputs[ i ] = input ;
			uvSortedInputs[ i ] = input ;
			
			minWt = ( weight < minWt ) ? weight : minWt ;
		}
		Arrays.sort( uvSortedInputs ) ;
	}

	public double getObjectiveValue() { 
		return objectiveValue ; 
	}
	
	public int[] solve() {
		
		// Create the top level situation, where no items are yet in the knapsack
		Situation rootSit = new Situation( 0, ksCapacity ) ;
		
		// Fill the most optimistic value of the knapsack for the root situation
		// Note that at this point, the depth is -1, implying that we haven't 
		// yet started traversing the solution tree.
		rootSit.optimisticVal = getMostOptimisticValue( rootSit, -1 ) ;
		
		// Now delegate the solution finding to a recursive method
		solveBnB( rootSit, 0 ) ;
		
		return this.bestPath ;
	}
	
	// ======================== PRIVATE AREA ===================================
	private class Situation {
		double curVal = 0 ;
		double remainingCapacity = 0 ;
		double optimisticVal = 0 ;
		
		Situation( double val, double remCap ) {
			this.curVal = val ;
			this.remainingCapacity = remCap ;
		}
		
		Situation( Situation original, Input input ) {
			
			if( input != null ) {
				this.curVal = original.curVal + input.value ;
				this.remainingCapacity = original.remainingCapacity - input.weight ;
			}
			else {
				this.curVal = original.curVal ;
				this.remainingCapacity = original.remainingCapacity ;
			}
		}
		
		public String toString() {
			return "SIT[" + String.format( "%3d", curVal ) + ", " + 
					        String.format( "%3d", remainingCapacity ) + ", " + 
					        String.format( "%3d", optimisticVal ) + "]" ;
		}
	}
	
	/**
	 * This class is a simple encapsulation of a single input to the knapsack
	 * problem. Each input is characterized by its index, value and weight.
	 */
	public class Input implements Comparable<Input> {

		public final int index ;
		public final double value ;
		public final double weight ;
		public final double unitVal ;
		
		public Input( int index, double value, double weight ) {
			this.index   = index ;
			this.value   = value ;
			this.weight  = weight ;
			this.unitVal = ((double)value)/weight ;
		}
		
		public int compareTo( Input o ) {
			if( unitVal == o.unitVal ) {
				if( weight < o.weight ) {
					return -1 ;
				}
				else if( weight > o.weight ) {
					return 1 ;
				}
				return 0 ;
			}
			else if( unitVal < o.unitVal ) {
				return 1 ;
			}
			return -1 ;
		}
		
		public String toString() {
			return "INPUT[" + String.format( "%2d", index ) + ", " + 
					          String.format( "%2d", value ) + ", " + 
					          String.format( "%2d", weight ) + ", " + 
					          String.format( "%6.3f", unitVal ) + "]" ;
		}
	}	
	
	private String indent( int depth ) {
		if( LOG ) {
			StringBuffer buffer = new StringBuffer() ;
			for( int i=0; i<depth; i++ ) buffer.append( "   " ) ;
			return buffer.toString() ;
		}
		return "" ;
	}
	
	private String xString( int depth ) {
		StringBuffer buffer = new StringBuffer() ;
		for( int i=0; i<numItems; i++ ) {
			if( i == depth ) {
				buffer.append( "[" + taken[i] + "] " ) ;
			}
			else {
				buffer.append( taken[i] + " " ) ;
			}
		}
		return buffer.toString() ;
	}
	
	/**
	 * This function computes the most optimistic value that is possible by
	 * moving forward from the current situation. The current situation is
	 * characterized by the current state of the knapsack along with the current
	 * depth of exploration. The depth parameter can lead us to the derived
	 * knowledge of how many items have already been evaluated and the taken
	 * status of each of the evaluated items.
	 * 
	 * The most optimistic value is calculated by adding the current value of
	 * the situation with the value additions (possibly partial) of the items 
	 * which have not been evaluated in descending order of their unit value.
	 * 
	 * @param sit The situation which needs to be forward extrapolated to 
	 *        get the most optimistic value possible
	 *        
	 * @param depth The current depth of exploration. This is also equal to the
	 *        number of items already evaluated
	 *        
	 * @return The most ceiling of optimistic value possible. 
	 */
	private double getMostOptimisticValue( Situation sit, int depth ) {
		
		boolean log = false ;
		
		double optVal =  sit.curVal ;
		double remainingCapacity = sit.remainingCapacity ;
		
		if(log) logger.debug( indent(depth) + 
				            "Computing most optimistic for situation " + sit ) ;
		
		for( Input input : uvSortedInputs ) {
			
			// Break condition - If our remaining capacity is zero, we break out
			// of this loop
			if( remainingCapacity == 0 ) {
				if(log) logger.debug( indent(depth) + 
						              "Remaining capacity is zero" ) ;
				break ;
			}
			
			// If we are dealing with items in input with index lesser than the
			// depth, we can safely assume that these items have already been
			// evaluated and contribute to the current value of the situation.
			// Hence we can ignore them for this calculation.
			if( input.index <= depth ) {
				if(log) logger.debug( indent(depth) + "Input " + input + " already in sack" ) ;
				continue ;
			}
			
			// If we are here, it implies that we have capacity remaining in 
			// the sack and more items to evaluate. 
			if( input.weight <= remainingCapacity ) {
				
				// If the remaining weight is more than the minimum weight we 
				// pick up the currently offered item and see if its weight is 
				// lesser than remaining capacity of the sack - if so, we 
				// simply add it and decrease the remaining capacity
				
				optVal += input.value ;
				remainingCapacity -= input.weight ;
				
				if(log) logger.debug( indent(depth) + "Considered " + input + " for optimal " +
						      "value calc. Full weight." ) ;
				if(log) logger.debug( indent(depth) + "\toptVal = " + optVal + ", " + 
						      "remaining cap = " + remainingCapacity ) ;
			}
			else {
				// If the weight of the input is greater than available capacity
				// we take a fraction of the input and with this we have 
				// filled the sack completely.
				if(log) logger.debug( indent(depth) + "Considered " + input + " for optimal " +
						     "value calc. Partial weight " + remainingCapacity ) ;
				
				optVal += Math.ceil( input.unitVal * remainingCapacity ) ;
				remainingCapacity = 0 ;
				
				if(log) logger.debug( indent(depth) + "\toptVal = " + optVal + ", remaining cap = " + 
				              remainingCapacity ) ;
			}
		}
		
		// If we have come here, it can be either because we have filled the
		// sack with the most optimal value or we have depleted all the inputs
		// and still the sack is not full - in either case, we have found 
		// the most optimistic value that is possible by extrapolating from 
		// the given situation.
		
		return optVal ;
	}
	
	/**
	 * This method solves the optimization by branch and bound algorithm.
	 */
	private void solveBnB( Situation sit, int depth ) {
		
		boolean log = LOG & true ;
		
		Input nextInput = null ;
		
		if( depth < numItems ) {
			nextInput = inputs[ depth ] ;
		}
		
		if(log) logger.debug( indent(depth) + "Current Xi = " + xString( depth ) ) ;
		if(log) logger.debug( indent(depth) + "Exploring situation " + sit + " at depth " + depth + " input " + nextInput ) ;
		
		// If next input is null, it implies we have finished evaluating all the 
		// inputs and hence have reached a solution 
		if( nextInput == null ) {
			if(log) logger.debug( indent(depth) + "Solution reached. No more inputs" ) ;
			evaluateEndSituation( sit, depth ) ;
		}
		// If we have no remaining capacity in the sack, we have reached a 
		// solution and need not explore further
		else if( sit.remainingCapacity == 0 ) {
			if(log) logger.debug( indent(depth) + "Solution reached. No more capacity" ) ;
			evaluateEndSituation( sit, depth ) ;
			
		}
		else if( sit.remainingCapacity < minWt ) {
			if(log) logger.debug( indent(depth) + "Invalid situation. Capacity less than min weight of input" ) ;
			evaluateEndSituation( sit, depth ) ;
		}
		// If we have a situation where we have negative remaining capacity
		// it implies we have a improper solution and hence we discard this path
		else if( sit.remainingCapacity < 0 ) {
			if(log) logger.debug( indent(depth) + "Invalid situation. Capacity negative" ) ;
			// Just return.. this will ensure that this subtree is discarded 
			// and not explored further.
		}
		// If we don't have a termination condition for this situation, we 
		// continue with next input
		else { 
			Situation noPickSit = new Situation( sit, null ) ;
			Situation pickSit   = new Situation( sit, nextInput ) ;
			noPickSit.optimisticVal = getMostOptimisticValue( noPickSit, depth ) ;
			pickSit.optimisticVal   = getMostOptimisticValue( pickSit,   depth ) ;
			
			if( noPickSit.optimisticVal > pickSit.optimisticVal ) {
				
				if(log) logger.debug( indent(depth) + "Exploring situation - NOT picking up input" ) ;
				
				if( objectiveValue < noPickSit.optimisticVal ) {
					taken[depth] = 0 ;
					solveBnB( noPickSit, depth+1 ) ;
					taken[ depth ] = 0 ;
				}
				
				if( objectiveValue < pickSit.optimisticVal ) {
					taken[depth] = 1 ;
					solveBnB( pickSit, depth+1 ) ;
					taken[ depth ] = 0 ;
				}
			}
			else {
				if(log) logger.debug( indent(depth) + "Exploring situation - picking up input" ) ;
				
				if( objectiveValue < pickSit.optimisticVal ) {
					taken[depth] = 1 ;
					solveBnB( pickSit, depth+1 ) ;
					taken[ depth ] = 0 ;
				}
				
				if( objectiveValue < noPickSit.optimisticVal ) {
					taken[depth] = 0 ;
					solveBnB( noPickSit, depth+1 ) ;
					taken[ depth ] = 0 ;
				}
			}
		}
	}
	
	/**
	 * This function evaluates the given situation against the best solution
	 * encountered till now and decides if we need to further explore this
	 * situation depending upon whether the earlier solution dominates this
	 * situation or not.
	 * 
	 * This function is called upon only if we have reached a situation which 
	 * can't be explored further. This can happen if either we have taken up
	 * all the available inputs or if we have quenched the sack capacity
	 */
	private void evaluateEndSituation( Situation sit, int depth ) {

		boolean log = LOG & true ;
		
		// If we have picked up a situation where we are going beyond sack
		// capacity, this is not a valid solution
		if( sit.remainingCapacity < 0 ) {
			// Do nothing.. 
			if(log) logger.debug( indent( depth ) + "Found an invalid solution. Capacity overflow" ) ;
		}
		// If the earlier solution is equal to or better than the current 
		// situation, this situation is not good enough
		else if( objectiveValue >= sit.curVal ) {
			// Do nothing.. 
			if(log) logger.debug( indent( depth ) + "Found a non optimal solution. curVal=" + sit.curVal + 
					              ", objectiveVal=" + objectiveValue ) ;
		}
		else {
			// WOW, if we have a better solution we capture the steps to 
			// reach this solution
			System.arraycopy( taken, 0, bestPath, 0, numItems ) ;
			objectiveValue = sit.curVal ;
			
			if(log) logger.debug( indent( depth ) + "Optimal Xi = " + xString( -1 ) ) ;
			
		}
	}
}
