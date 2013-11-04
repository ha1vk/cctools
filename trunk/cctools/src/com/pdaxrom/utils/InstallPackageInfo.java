package com.pdaxrom.utils;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class InstallPackageInfo {
	private static final String TAG = "InstallPackageInfo";

	private String _pkg;
	private List<PackageInfo> _list;
	private int installSize = 0;
	private int downloadSize = 0;
	
	InstallPackageInfo(List<PackageInfo> repo, String pkg) {
		_pkg = pkg;
		_list = new ArrayList<PackageInfo>();
		getDepends(repo, _pkg, _list);
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
	
    private void getDepends(List<PackageInfo> repo, String pkg ,List<PackageInfo> list) {
    	if (RepoUtils.isContainsPackage(list, pkg)) {
    		return;
    	}

    	for (PackageInfo info: repo) {
    		if (pkg.contentEquals(info.getName())) {
    			String deps = info.getDepends();
    			Log.i(TAG, "pkg deps = " + deps);
    			if (deps != null && !deps.contentEquals("")) {
    				deps = deps.replaceAll("\\s+", " ");
    				for (String dep: deps.split("\\s+")) {
    					Log.i(TAG, "check package = " + dep);
    					getDepends(repo, dep, list);
    				}
    			}
    			list.add(info);
    			Log.i(TAG, "added pkg = " + pkg);
    			break;
    		}
    	}
    	return;
    }
}
