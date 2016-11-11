package com;

public class SHowNull {
public static void foo() {
	
	
	String s = Math.random() > 0.5 ? "hello" : null;
	if (s == null) {
		System.out.println("ignore");
	}
	System.out.println(s.length());
	
}
}
