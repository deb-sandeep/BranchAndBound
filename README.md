# BranchAndBound
###### A Branch and Bound solver

An implementation of Branch and Bounds algorithm.

http://en.wikipedia.org/wiki/Branch_and_bound

This solver provide a convenient and reasonably performant algorithm to tackle data sets in the range of 100-500 elements in a reasonable time. Please note that the problem this algorithm tries to solve is NP hard and although this algorithm uses a tree pruning algorithm, the upper bound of time complexity is largely influenced by the arrangement of data set.

For example, this algorithm can chew through three different test data sets of 100 elements in 2 sec, 30 sec and 14 milli seconds respectively. Hence, please keep your expectations bounded while using this solver.

Using this solver is quite simple - during construction the following values are expected:

* Capacity of the container/knapsack
* Array of weights of elements
* [Optional] Array of values of elements. If this is not provided, the weights are considered as values of the elements.

The goal of the solver is to return an array of integers (of size equal to number of items) with the value at each index either 0 or 1. 0 implies that the value was not selected, 1 implies that it is a part of the optimal solution. The getObjectiveValue method returns the optimal value (not weight) of the items chosen by the solution.

```java
	@Test
	public void verySmallInputSet() {
		
		double weights[] = { 4, 5, 8, 3 } ;
		double values[]  = { 8, 10, 15, 4 } ;
		
		BnBSolver solver = new BnBSolver( 11, weights, values ) ;
		
		int[] solution = solver.solve() ;
		assertArrayEquals( new int[]{0, 0, 1, 1}, solution ) ;
	}
```

NOTE: This is not a thread safe class - use a new instance for each thread or solution.
