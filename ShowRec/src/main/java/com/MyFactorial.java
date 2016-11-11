package com;

public class MyFactorial {
	
	
	
	
	private int factorial(int acc, int n) {
		if (n <= 1) {
			return acc;
		}
		return factorial(acc*n, n-1);
	} 
	
	public static void main(String[] args) {
		MyFactorial m = new MyFactorial();
		System.out.println(m.factorial(1, 54000));
		
		Object o = null;
		if (o != null) {
			System.out.println("xxx");
		} 
		System.out.println(o.toString());
		
		
	}
}
