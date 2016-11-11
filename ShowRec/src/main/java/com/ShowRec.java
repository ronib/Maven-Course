package com;

public class ShowRec {
	
	// x>= 0
	private  int add(int n, int x) {
		if (x == 0)  
			return n;
		return add(n+1, x-1);
	}  
 
	public static void main(String[] args) {
		ShowRec sr = new ShowRec();
		System.out.println(sr.add(90, 100000));
	}
}
