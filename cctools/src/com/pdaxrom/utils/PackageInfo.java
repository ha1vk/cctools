package com.pdaxrom.utils;

public class PackageInfo {
	private String	_name;
	private String	_file;
	private int		_size;
	private int		_filesize;
	private String	_version;
	
	PackageInfo(String name, String file, int size, int filesize, String version) {
		_name = name;
		_file = file;
		_size = size;
		_filesize = filesize;
		_version = version;
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
}
