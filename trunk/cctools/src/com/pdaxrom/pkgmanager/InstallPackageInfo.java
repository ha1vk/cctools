package com.pdaxrom.pkgmanager;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;

public class InstallPackageInfo {
	private static final String TAG = "InstallPackageInfo";

	private String pkg = null;
	private List<PackageInfo> list = null;
	private int installSize = 0;
	private int downloadSize = 0;
	
	InstallPackageInfo() {
		list = new ArrayList<PackageInfo>();
	}
	
	InstallPackageInfo(PackagesLists packagesLists, String pkg) {
		installPackageInfo(packagesLists, pkg, null);
	}
	
	InstallPackageInfo(PackagesLists packagesLists, String pkg, List<PackageInfo> list) {
		installPackageInfo(packagesLists, pkg, list);
	}

	public void addPackage(PackagesLists packagesLists, String pkg) {
		if (RepoUtils.isContainsPackage(packagesLists.getAvailablePackages(), pkg)) {
			if (this.pkg == null) {
				this.pkg = pkg;
			} else {
				this.pkg += " " + pkg;
			}
			getDepends(packagesLists, pkg, list);
			calculateSizes();
		}
	}
	
	private void installPackageInfo(PackagesLists packagesLists, String pkg, List<PackageInfo> list) {		
		this.pkg = pkg;
		if (list == null) {
			this.list = new ArrayList<PackageInfo>();
		} else {
			this.list = list;
		}
		getDepends(packagesLists, this.pkg, this.list);
		calculateSizes();
	}
	
	public String getName() {
		return pkg;
	}
	
	public void calculateSizes() {
		installSize = 0;
		downloadSize = 0;
		for (PackageInfo pkg: list) {
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
		return list;
	}
	
	public String getPackagesStrings() {
		String packages = "";
		boolean isFirstAdded = false;
		for (PackageInfo pkg: list) {
			if (isFirstAdded) {
				packages += " " + pkg.getName();
			} else {
				packages += pkg.getName();
			}
			isFirstAdded = true;
		}
		return packages;
	}
	
    private void getDepends(PackagesLists packagesLists, String packageWithVariants ,List<PackageInfo> list) {
    	String[] packageVariants = packageWithVariants.replace('|', ' ').split("\\s+");
    	Log.i(TAG, "packageVariants = " + packageWithVariants.replace('|', ' '));
    	String firstPackage = packageVariants[0];
    	for (String packageVariant: packageVariants) {
    		Log.i(TAG, "packageVariant = " + packageVariant);
    		if (RepoUtils.isContainsPackage(list, packageVariant)) {
    			return;
    		}
    		if (RepoUtils.isContainsPackage(packagesLists.getInstalledPackages(), packageVariant)) {
    			firstPackage = packageVariant;
    			break;
    		}
    	}
    	
    	for (PackageInfo info: packagesLists.getAvailablePackages()) {
    		if (firstPackage.equals(info.getName())) {
    			String deps = info.getDepends();
    			Log.i(TAG, "package deps = " + deps);
    			if (deps != null && !deps.equals("")) {
    				deps = deps.replaceAll("\\s+", " ");
    				for (String dep: deps.split("\\s+")) {
    					Log.i(TAG, "check package = " + dep);
    					getDepends(packagesLists, dep, list);
    				}
    			}
    			PackageInfo installedPackage = RepoUtils.getPackageByName(
    					packagesLists.getInstalledPackages(), firstPackage);
    			if (installedPackage != null) {
    				if (installedPackage.getVersion().equals(info.getVersion())) {
    					Log.i(TAG, "the same version, skip package = " + firstPackage);
    					break;
    				}
    			}
    			list.add(info);
    			Log.i(TAG, "add package = " + firstPackage);
    			break;
    		}
    	}
    	return;
    }
}
