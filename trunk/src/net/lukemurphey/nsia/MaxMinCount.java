package net.lukemurphey.nsia;

public class MaxMinCount {

	private int max;
	private int min;
	private int count;
	
	public MaxMinCount( int max, int min, int count ){
		this.max = max;
		this.min = min;
		this.count = count;
	}
	
	public int getMin(){
		return min;
	}
	
	public int getMax(){
		return max;
	}
	
	public int getCount(){
		return count;
	}
}
