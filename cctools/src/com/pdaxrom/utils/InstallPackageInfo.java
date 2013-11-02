package com.pdaxrom.utils;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Log;

public class InstallPackageInfo {
	private static final String TAG = "InstallPackageInfo";

    // XML node keys
    static public final String KEY_PACKAGE	= "package"; // parent node
    static public final String KEY_NAME		= "name";
    static public final String KEY_VERSION	= "version";
    static public final String KEY_DESC		= "description";
    static public final String KEY_DEPENDS	= "depends";
    static public final String KEY_SIZE		= "size";
    static public final String KEY_FILE		= "file";
    static public final String KEY_FILESIZE	= "filesize";
    static public final String KEY_STATUS	= "status";

	private String _pkg;
	private String _xmlRepo;
	private List<PackageInfo> _list;
	private int installSize = 0;
	private int downloadSize = 0;
	
	InstallPackageInfo(String xmlRepo, String pkg) {
		_pkg = pkg;
		_xmlRepo = xmlRepo;
		_list = new ArrayList<PackageInfo>();
		getDepends(_pkg, _list);
		calculateSizes();
	}
	
	public String getName() {
		return _pkg;
	}
	
	public void calculateSizes() {
		installSize = 0;
		downloadSize = 0;
		for (PackageInfo pkg: _list) {
			installSize += pkg.getSize();
			downloadSize += pkg.getFileSize();
		}
	}
	
	public int getDownloadSize() {
		return downloadSize;
	}
	
	public int getInstallSize() {
		return installSize;
	}
	
	public List<PackageInfo> getPackagesList() {
		return _list;
	}
	
	public String getPackagesStrings() {
		String packages = "";
		boolean isFirstAdded = false;
		for (PackageInfo pkg: _list) {
			if (isFirstAdded) {
				packages += " " + pkg.getName();
			} else {
				packages += pkg.getName();
			}
			isFirstAdded = true;
		}
		return packages;
	}
	
    private boolean isContainsPackage(List<PackageInfo> list, String name) {
    	for (PackageInfo packageInfo: list) {
    		if (packageInfo.getName().contains(name)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    private void getDepends(String pkg ,List<PackageInfo> list) {
    	if (isContainsPackage(list, pkg)) {
    		return;
    	}

		XMLParser parser = new XMLParser();
        Document doc = parser.getDomElement(_xmlRepo); // getting DOM element
        NodeList nl = doc.getElementsByTagName(KEY_PACKAGE);

    	for (int i = 0; i < nl.getLength(); i++) {
    		Element e = (Element) nl.item(i);    		
    		String name = parser.getValue(e, KEY_NAME);
    		if (pkg.contentEquals(name)) {
    			String deps = parser.getValue(e, KEY_DEPENDS);
    			Log.i(TAG, "pkg deps = " + deps);
    			if (deps != null && !deps.contentEquals("")) {
    				deps = deps.replaceAll("\\s+", " ");
    				for (String dep: deps.split("\\s+")) {
    					Log.i(TAG, "check package = " + dep);
    					getDepends(dep, list);
    				}
    			}
        		PackageInfo packageInfo = new PackageInfo(
        				parser.getValue(e, KEY_NAME),
        				parser.getValue(e, KEY_FILE),
        				Integer.valueOf(parser.getValue(e, KEY_SIZE)),
        				Integer.valueOf(parser.getValue(e, KEY_FILESIZE)),
        				parser.getValue(e, KEY_VERSION));
    			list.add(packageInfo);
    			Log.i(TAG, "added pkg = " + pkg);
    			break;
    		}
    	}
    	return;
    }
}
