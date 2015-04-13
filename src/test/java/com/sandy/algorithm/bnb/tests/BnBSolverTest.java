package com.sandy.algorithm.bnb.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.sandy.algorithm.bnb.BnBSolver;

public class BnBSolverTest {

	@Test
	public void verySmallInputSet() {
		
		double weights[] = { 4, 5, 8, 3 } ;
		double values[]  = { 8, 10, 15, 4 } ;
		
		BnBSolver solver = new BnBSolver( 11, weights, values ) ;
		
		int[] solution = solver.solve() ;
		assertArrayEquals( new int[]{0, 0, 1, 1}, solution ) ;
	}
	
	@Test
	public void verySmallInputSetWithOnlyWeights() {
		
		double weights[] = { 28, 25, 35, 45, 20, 45 } ;
		BnBSolver solver = new BnBSolver( 120, weights ) ;
		
		int[] solution = solver.solve() ;
		
		assertEquals( 118, solver.getObjectiveValue(), 0.001 ) ;
		assertArrayEquals( new int[]{ 1, 1, 0, 1, 1, 0 }, solution ) ;
	}

}
