package com.pdaxrom.pkgmanager;

public class PackageInfo {
	private String	_name;
	private String	_file;
	private int		_size;
	private int		_filesize;
	private String	_version;
	private String	_description;
	private String	_depends;
	private String	_arch;
	
	PackageInfo(String name, String file, int size, int filesize,
				String version, String description, String depends, String arch) {
		_name = name;
		_file = file;
		_size = size;
		_filesize = filesize;
		_version = version;
		_description = description;
		_depends = depends;
		_arch = arch;
	}
	
	String getName() {
		return _name;
	}
	
	String getFile() {
		return _file;
	}
	
	int getSize() {
		return _size;
	}
	
	int getFileSize() {
		return _filesize;
	}
	
	String getVersion() {
		return _version;
	}
	
	String getDescription() {
		return _description;
	}
	
	String getDepends() {
		return _depends;
	}
	
	String getArch() {
		return _arch;
	}
}
