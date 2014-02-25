package edu.berkeley.cspace.test;

public enum Page {
	LOGIN 		("index.html"),
	FIND_EDIT	("findedit.html"),
	CATALOGING  ("cataloging.html");
	
	private final String path;
	
	private Page(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}
}
