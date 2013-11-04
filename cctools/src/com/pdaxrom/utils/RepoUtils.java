package com.pdaxrom.utils;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.util.Log;

public class RepoUtils {
	private static final String TAG = "RepoUtils";
	
    // XML node keys
    static public final String KEY_PACKAGE	= "package"; // parent node
    static public final String KEY_NAME		= "name";
    static public final String KEY_FILE		= "file";
    static public final String KEY_SIZE		= "size";
    static public final String KEY_FILESIZE	= "filesize";
    static public final String KEY_VERSION	= "version";
    static public final String KEY_DESC		= "description";
    static public final String KEY_DEPENDS	= "depends";
    static public final String KEY_ARCH		= "arch";
    static public final String KEY_STATUS	= "status";

	private static String	_ndkArch;
	private static int		_ndkVersion;

	public static void setVersion(String ndkArch, int ndkVersion) {
		_ndkArch = ndkArch;
		_ndkVersion = ndkVersion;
	}
	
	public static List<PackageInfo> getRepoFromUrl(String url) {
		List<PackageInfo> list = new ArrayList<PackageInfo>();
		
		XMLParser parser = new XMLParser();
        Document doc = parser.getDomElement(replaceMacro(getRepoXmlFromUrl(url))); // getting DOM element
        NodeList nl = doc.getElementsByTagName(KEY_PACKAGE);

    	for (int i = 0; i < nl.getLength(); i++) {
    		Element e = (Element) nl.item(i);
    		PackageInfo packageInfo = new PackageInfo(
    				parser.getValue(e, KEY_NAME),
    				parser.getValue(e, KEY_FILE),
    				Integer.valueOf(parser.getValue(e, KEY_SIZE)),
    				Integer.valueOf(parser.getValue(e, KEY_FILESIZE)),
    				parser.getValue(e, KEY_VERSION),
    				parser.getValue(e, KEY_DESC),
    				parser.getValue(e, KEY_DEPENDS),
    				parser.getValue(e, KEY_ARCH));
			list.add(packageInfo);
			Log.i(TAG, "added pkg = " + packageInfo.getName());
    	}
        
		return list;
	}
	
	public static String getRepoXmlFromUrl(String url) {
		XMLParser parser = new XMLParser();
		String xml = parser.getXmlFromUrl(url + "/Packages");
		return replaceMacro(xml);
	}
	
    public static boolean isContainsPackage(List<PackageInfo> repo, String pkg) {
    	for (PackageInfo packageInfo: repo) {
    		if (packageInfo.getName().contains(pkg)) {
    			return true;
    		}
    	}
    	return false;
    }

    private static String replaceMacro(String str) {
    	if (str != null) {
    		str = str.replaceAll("\\$\\{HOSTARCH\\}", _ndkArch);
    		str = str.replaceAll("\\$\\{HOSTNDKARCH\\}", _ndkArch);
    		str = str.replaceAll("\\$\\{HOSTNDKVERSION\\}", String.valueOf(_ndkVersion));
    	}
    	return str;
    }

}