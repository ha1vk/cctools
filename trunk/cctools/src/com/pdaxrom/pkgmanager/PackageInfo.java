package com.pdaxrom.pkgmanager;

public class PackageInfo {
	private String	name;
	private String	file;
	private int		size;
	private int		filesize;
	private String	version;
	private String	description;
	private String	depends;
	private String	arch;
	private String replaces;
	
	PackageInfo(String name, String file, int size, int filesize,
				String version, String description, String depends,
				String arch, String replaces) {
		this.name = name;
		this.file = file;
		this.size = size;
		this.filesize = filesize;
		this.version = version;
		this.description = description;
		this.depends = depends;
		this.arch = arch;
		this.replaces = replaces;
	}
	
	String getName() {
		return name;
	}
	
	String getFile() {
		return file;
	}
	
	int getSize() {
		return size;
	}
	
	int getFileSize() {
		return filesize;
	}
	
	String getVersion() {
		return version;
	}
	
	String getDescription() {
		return description;
	}
	
	String getDepends() {
		return depends;
	}
	
	String getArch() {
		return arch;
	}
	
	String getReplaces() {
		return replaces;
	}
}
