package edu.berkeley.cspace.test;

public enum Page {
	LOGIN 			("index.html"),
	FIND_EDIT		("findedit.html"),
	CATALOGING  	("cataloging.html"),
	PERSON			("person.html"),
	CONCEPT			("concept.html"),
	LOCATION		("location.html"),
	ORGANIZATION	("organization.html"),
	PLACE			("place.html"),
	WORK			("work.html"),
	CITATION		("citation.html"),
	GROUP			("group.html"),
	MEDIA			("media.html");
	
	private final String path;
	
	private Page(String path) {
		this.path = path;
	}
	
	public String getPath() {
		return path;
	}
}
