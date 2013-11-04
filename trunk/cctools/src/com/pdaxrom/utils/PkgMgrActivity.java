package com.pdaxrom.utils;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pdaxrom.cctools.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class PkgMgrActivity extends ListActivity {
	private static final String TAG = "PkgMgrActivity";
	private static final String URL = "http://cctools.info/repo-4.8-1/" + Build.CPU_ABI;

	private Context context = this;
	private static final String PKGS_LISTS_DIR = "/installed/";
	
	private List<PackageInfo> remoteRepo = null;
	
	private static final int ACTIVITY_PKGCTL = 1;
	
    // Last list position
    private int lastPosition = 0;
    
    private ListView lv;
    private EditText inputSearch;
    
	private String sdCardDir;
	private String filesDir;
	private String tmpDir;
	private String toolchainDir;
    private String serviceDir;

	final Handler handler = new Handler();
    private ProgressDialog pd;    
    
	final int sdk2ndk_arm[] = {
			/*   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23  */
			-1, -1, -1,  3,  4,  5,  5,  5,  8,  9,  9,  9,  9, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, -1
	};
	final int sdk2ndk_mips[] = {
			/*   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20 */
			-1, -1, -1, -1, -1, -1, -1, -1, -1,  9,  9, -1, -1, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, -1
	};
	final int sdk2ndk_x86[] = {
			/*   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20 */
			-1, -1, -1, -1, -1, -1, -1, -1, -1,  9,  9, -1, -1, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, -1
	};

	private int ndkVersion;
	private int sdkVersion;
	private String ndkArch;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pkgmgr_main);

        inputSearch = (EditText) findViewById(R.id.inputSearch);
        
        setupDirs();
        setupVersion();
        
        (new DownloadRepoTask()).execute(URL);
         
        // selecting single ListView item
        lv = getListView();
        // listening to single listitem click
        lv.setOnItemClickListener(new OnItemClickListener() { 
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // getting values from selected ListItem
                final String name = ((TextView) view.findViewById(R.id.pkg_name)).getText().toString();

            	String toolchainDir = getCacheDir().getParentFile().getAbsolutePath() + "/root";
            	String logFile = toolchainDir + PKGS_LISTS_DIR + name + ".list";
            	
            	if ((new File(logFile)).exists()) {
                	Builder dialog = new AlertDialog.Builder(context)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(getString(R.string.pkg_selected) + name)
                    .setMessage(getString(R.string.pkg_alreadyinstalled))
                    .setNeutralButton(getString(R.string.cancel), null);
/*            		
                	dialog.setPositiveButton(getString(R.string.pkg_reinstall), new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int which) {
                    	}
                    });
 */
                	dialog.setNegativeButton(getString(R.string.pkg_uninstall), new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int which) {
                    		(new UninstallPackagesTask()).execute(name);
                    	}
                    });
                	dialog.show();    	                
            	} else {
            		(new PrepareToInstallTask()).execute(name);
            	}
            }
        });
        
        //
        // text filter
        //
        lv.setTextFilterEnabled(true);
        
        inputSearch.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable s) {				
				// TODO Auto-generated method stub
				
			}
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub
				
			}
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (start > 0) {
					lv.setFilterText(s.toString());									
				} else {
					lv.clearTextFilter();
				}
			}
        });
    }

    protected void onPause() {
    	super.onPause();
    	lastPosition = this.getListView().getFirstVisiblePosition();
    	Log.i(TAG, "Position " + lastPosition);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTIVITY_PKGCTL) {
    		Log.i(TAG, "install/uninstall finished");
    		if (remoteRepo != null) {
    			showPackages(remoteRepo);
    		}
    	}
    }
    
    private void setupDirs() {
    	sdCardDir 	= Environment.getExternalStorageDirectory().getPath() + "/CCTools";
    	filesDir 	= sdCardDir + "/backup";
    	tmpDir 		= sdCardDir + "/tmp";
    	toolchainDir= getCacheDir().getParentFile().getAbsolutePath() + "/root";
        serviceDir 	= toolchainDir + "/cctools/services";

        if (!(new File(sdCardDir)).exists()) {
        	(new File(sdCardDir)).mkdir();
        }
        if (!(new File(tmpDir)).exists()) {
        	(new File(tmpDir)).mkdir();
        }
        if (!(new File(filesDir)).exists()) {
        	(new File(filesDir)).mkdir();
        }
        if (!(new File(toolchainDir)).exists()) {
        	(new File(toolchainDir)).mkdir();
        }
        if (!(new File(serviceDir)).exists()) {
        	(new File(serviceDir)).mkdir();
        }
    }
    
    private void setupVersion() {
        sdkVersion = Build.VERSION.SDK_INT;
        ndkVersion = -1;
        ndkArch = "all";
        if (Build.CPU_ABI.startsWith("arm")) {
        	ndkArch = "armel";
        	if (sdk2ndk_arm.length > sdkVersion) {
        		ndkVersion = sdk2ndk_arm[sdkVersion];
        	} else {
        		ndkVersion = sdk2ndk_arm[sdk2ndk_arm.length -1];
        	}
        } else if (Build.CPU_ABI.startsWith("mips")) {
        	ndkArch = "mipsel";
        	if (sdk2ndk_mips.length > sdkVersion) {
        		ndkVersion = sdk2ndk_mips[sdkVersion];
        	} else {
        		ndkVersion = sdk2ndk_mips[sdk2ndk_mips.length -1];
        	}
        } else {
        	ndkArch = "i686";
        	if (sdk2ndk_x86.length > sdkVersion) {
        		ndkVersion = sdk2ndk_x86[sdkVersion];
        	} else {
        		ndkVersion = sdk2ndk_x86[sdk2ndk_x86.length -1];
        	}
        }
        
        RepoUtils.setVersion(ndkArch, ndkVersion);
    }
    
    void showPackages(List<PackageInfo> repo) {
        ArrayList<HashMap<String, String>> menuItems = new ArrayList<HashMap<String, String>>();

        for (PackageInfo info: repo) {
            // creating new HashMap
            HashMap<String, String> map = new HashMap<String, String>();

            // adding each child node to HashMap key => value
            map.put(RepoUtils.KEY_NAME,		info.getName());
            map.put(RepoUtils.KEY_VERSION,	info.getVersion());
            map.put(RepoUtils.KEY_DESC,		info.getDescription());
            map.put(RepoUtils.KEY_DEPENDS,	info.getDepends());
            map.put(RepoUtils.KEY_FILESIZE,	String.valueOf(info.getFileSize()));
            map.put(RepoUtils.KEY_SIZE,		String.valueOf(info.getSize()));
            map.put(RepoUtils.KEY_FILE,		info.getFile());

            String toolchainDir = getCacheDir().getParentFile().getAbsolutePath() + "/root";
        	String logFile = toolchainDir + "/" + PKGS_LISTS_DIR + "/"
        			+ info.getName() + ".list";
        	
        	if ((new File(logFile)).exists()) {
        		map.put(RepoUtils.KEY_STATUS, getString(R.string.pkg_installed));
        	}else {
        		map.put(RepoUtils.KEY_STATUS, getString(R.string.pkg_notinstalled));        		
        	}

            // adding HashList to ArrayList
            menuItems.add(map);
        }
 
        // Adding menuItems to ListView
        ListAdapter adapter = new SimpleAdapter(
        		this, 
        		menuItems,
        		R.layout.pkgmgr_list_package,
        		new String[] {
        				RepoUtils.KEY_NAME,
        				RepoUtils.KEY_VERSION,
        				RepoUtils.KEY_DESC,
        				RepoUtils.KEY_DEPENDS,
        				RepoUtils.KEY_FILE,
        				RepoUtils.KEY_FILESIZE,
        				RepoUtils.KEY_SIZE,
        				RepoUtils.KEY_STATUS },
        		new int[] {
        				R.id.pkg_name,
        				R.id.pkg_version,
        				R.id.pkg_desciption,
        				R.id.pkg_deps,
        				R.id.pkg_file,
        				R.id.pkg_filesize,
        				R.id.pkg_size,
        				R.id.pkg_status });
 
        setListAdapter(adapter);
        
        if (lastPosition > 0 && lastPosition < repo.size()) {
        	this.getListView().setSelection(lastPosition);
        }
        
    }
    
    private class DownloadRepoTask extends AsyncTask<String, Void, List<PackageInfo>> {
    	protected void onPreExecute() {
        	super.onPreExecute();
        	showProgress(getString(R.string.pkg_repoupdatetask), 
        			getString(R.string.pkg_repodownloading));
        }

		protected List<PackageInfo> doInBackground(String... params) {
	        Log.i(TAG, "Repo URL: " + params[0] + "/Packages");
			return RepoUtils.getRepoFromUrl(params[0] + "/Packages");
		}

		protected void onPostExecute(List<PackageInfo> result) {
			super.onPostExecute(result);
	        Log.i(TAG, "Downloaded repo with " + result.size() + " packages.");
	        if (result != null) {
	        	remoteRepo = result;
	        	showPackages(result);
	        }
	        hideProgress();
		}
    }
    
    private class PrepareToInstallTask extends AsyncTask<String, Void, InstallPackageInfo> {
    	protected void onPreExecute() {
        	super.onPreExecute();
        	showProgress(getString(R.string.pkg_prepareinstalltask),
        			getString(R.string.pkg_prepareinstall));
        }

		@Override
		protected InstallPackageInfo doInBackground(String... params) {
    		return new InstallPackageInfo(remoteRepo, params[0]);
		}
    	
		protected void onPostExecute(final InstallPackageInfo info) {
			hideProgress();
        	Builder dialog = new AlertDialog.Builder(context)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(getString(R.string.pkg_selected) + info.getName())
            .setMessage(getString(R.string.pkg_selected1) + info.getPackagesStrings()
            		+ "\n\n"
            		+ getString(R.string.pkg_selected2) 
            		+ Utils.humanReadableByteCount(info.getDownloadSize(), false)
            		+ "\u0020"
            		+ getString(R.string.pkg_selected3)
            		+ Utils.humanReadableByteCount(info.getInstallSize(), false))
            .setNeutralButton(getString(R.string.cancel), null)
        	.setPositiveButton(getString(R.string.pkg_install), new DialogInterface.OnClickListener() {
            	public void onClick(DialogInterface dialog, int which) {
            		Log.i(TAG, "Get install packages = " + info.getPackagesStrings());
            		(new InstallPackagesTask()).execute(info);
            	}
            });
        	dialog.show();
		}
    }
    
    private class InstallPackagesTask extends AsyncTask<InstallPackageInfo, Void, Boolean> {
    	protected void onPreExecute() {
    		super.onPreExecute();
    		showProgress(getString(R.string.pkg_installpackagetask),
    				getString(R.string.pkg_installpackage));
    	}
    	
		protected Boolean doInBackground(InstallPackageInfo... params) {
			return installPackage(params[0]);
		}
    	
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			lastPosition = lv.getFirstVisiblePosition();
			showPackages(remoteRepo);
			hideProgress();
		}
    }

    private class UninstallPackagesTask extends AsyncTask<String, Void, Boolean> {
    	protected void onPreExecute() {
    		super.onPreExecute();
    		showProgress(getString(R.string.pkg_uninstallpackagetask),
    				getString(R.string.pkg_uninstallpackage));
    	}
    	
		protected Boolean doInBackground(String... params) {
			return uninstallPackage(params[0]);
		}
    	
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			lastPosition = lv.getFirstVisiblePosition();
			showPackages(remoteRepo);
			hideProgress();
		}
    }

	private boolean downloadAndUnpack(String file, String from, String to, String log) {
		updateProgress(getString(R.string.download_file) + " " + file + "...");
		
		File temp = new File(filesDir + "/" + file);
		if (!temp.exists()) {
			try {
				int totalread = 0;
				Log.i(TAG, "Downloading file " + from + "/" + file);
				URL url = new URL(from + "/" + file);
				URLConnection cn = url.openConnection();
				cn.setReadTimeout(3 * 60 * 1000); // timeout 3 minutes
				cn.connect();
				int file_size = cn.getContentLength();
				StatFs stat = new StatFs(filesDir);
				int sdAvailSize = stat.getAvailableBlocks();// * stat.getBlockSize();
				Log.i(TAG, "File size " + file_size);
				Log.i(TAG, "Available on SD (in blocks " + stat.getBlockSize() + ") " + sdAvailSize);
				int need_mem = (file_size + 1024 * 1024 * 4) / stat.getBlockSize();
				if (sdAvailSize < need_mem) {
					temp.delete();
					need_mem /= 1024 * 1024;
					need_mem += 1;
					show_error(getString(R.string.sd_no_memory) + " " + need_mem + " " + getString(R.string.sd_no_memory2));
					return false;
				}
				InputStream stream = cn.getInputStream();
				if (stream == null) {
					throw new RuntimeException("stream is null");
				}
				Log.i(TAG, "File is " + temp.getAbsolutePath());
				FileOutputStream out = new FileOutputStream(temp);
				byte buf[] = new byte[128 * 1024];
				do {
					int numread = stream.read(buf);
					if (numread <= 0) {
						break;
					}
					out.write(buf, 0, numread);
					totalread += numread;
					updateProgress(getString(R.string.received) + " " + totalread + " " + getString(R.string.from) + " " + file_size + " " + getString(R.string.bytes));
				} while (true);
				stream.close();
				out.close();
				if (totalread != file_size) {
					throw new RuntimeException("Partially downloaded file!");
				}
			} catch (Exception e) {
				temp.delete();
				Log.i(TAG, "Error downloading file " + file);
				show_error(getString(R.string.error_downloading) + " (" + file + ")");
				return false;
			}
		} else
			Log.i(TAG, "Use file " + temp.getAbsolutePath());

		String tempPath = temp.getAbsolutePath();
		updateProgress(getString(R.string.unpacking_file) + " " + file + "...");
		Log.i(TAG, "Unpack file " + tempPath + " to " + to);
		String logFile = log;
		try {
			int need_mem = Utils.unzippedSize(tempPath);
			if (need_mem < 0) {
				throw new RuntimeException("bad archive");
			}
			StatFs stat = new StatFs(to);
			double cacheAvailSize = stat.getAvailableBlocks();
			Log.i(TAG, "Unzipped size " + need_mem);
			Log.i(TAG, "Available (blocks) " + cacheAvailSize + "(" + stat.getBlockSize() + ")");
			cacheAvailSize *= stat.getBlockSize();
			need_mem += 1024 * 1024 * 2;
			if (cacheAvailSize < need_mem) {
				need_mem /= 1024 * 1024;
				need_mem += 1;
				cacheAvailSize /= 1024 * 1024;
				show_error(getString(R.string.cache_no_memory) +
						need_mem + 
						getString(R.string.cache_no_memory1) + 
						(int)cacheAvailSize + 
						getString(R.string.cache_no_memory2));
				return false;
			}
			if (logFile == null) {
				logFile = toolchainDir + PKGS_LISTS_DIR + file + ".list";
			}
			if (Utils.unzip(tempPath, to, logFile) != 0) {
				if ((new File(logFile)).exists()) {
					(new File(logFile)).delete();
				}
				throw new RuntimeException("bad archive");
			}
		} catch (Exception e) {
			temp.delete();
			Log.i(TAG, "Corrupted archive, restart application and try install again");
			show_error(getString(R.string.bad_archive) + " (" + file +")");
			return false;
		}
		return true;
	}

	private void showProgress(String title, String message) {
		pd = ProgressDialog.show(context, title, message, true);
	}
	
	private void updateProgress(final String out) {
		handler.post(new Runnable() {
			public void run() {
				pd.setMessage(out);
			}
		});
	}

	private void updateProgressTitle(final String out) {
		handler.post(new Runnable() {
			public void run() {
				pd.setTitle(out);
			}
		});
	}

	private void hideProgress() {
		pd.dismiss();
	}
	
    private void show_error(final String message) {
    	Runnable proc = new Runnable() {
    		public void run() {
    	    	AlertDialog alertDialog = new AlertDialog.Builder(context).create();
    	    	alertDialog.setTitle(R.string.app_name);
    	    	alertDialog.setMessage(message);
//    	    	alertDialog.setButton(getString(R.string.exit_button), new DialogInterface.OnClickListener() {
//    	    		   public void onClick(DialogInterface dialog, int which) {
//    	    			   finish();
//    	    			   System.exit(0);
//    	    		   }
//    	    		});
    	    	alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
    	    	alertDialog.show();    			
    		}
    	};
    	handler.post(proc);
    }

    private boolean installPackage(InstallPackageInfo info) {
    	List<String> postinstList = new ArrayList<String>();
    	for (PackageInfo packageInfo: info.getPackagesList()) {
    		if ((new File(toolchainDir + "/" + PKGS_LISTS_DIR + "/" 
    				+ packageInfo.getName() + ".pkgdesc")).exists()) {
        		//TODO: check package version for update
    			Log.i(TAG, "Package " + packageInfo.getName() + " already installed.");
    			continue;
    		}
    		updateProgressTitle(getString(R.string.pkg_installpackagetask) + " " + packageInfo.getName());
    		String file = packageInfo.getFile();
    		Log.i(TAG, "Install " + packageInfo.getName() + " -> " + packageInfo.getFile());
			if (!downloadAndUnpack(file, URL, toolchainDir, 
					toolchainDir + "/" + PKGS_LISTS_DIR + "/" + packageInfo.getName() + ".list")) {
				return false;
			}
			updateProgress(getString(R.string.wait_message));
			// Move package info files from root directory
			String[] infoFiles = {"pkgdesc", "postinst", "prerm"};
			for (String infoFile: infoFiles) {
				if ((new File(toolchainDir + "/" + infoFile)).exists()) {
					String infoFilePath = toolchainDir + "/" + PKGS_LISTS_DIR  + "/" 
							+ packageInfo.getName() + "." + infoFile;
					Log.i(TAG, "Copy file to " + infoFilePath);
					try {
						Utils.copyDirectory(new File(toolchainDir + "/" + infoFile),
											new File(infoFilePath));
						if (infoFile.contentEquals("postinst")) {
							postinstList.add(packageInfo.getName());
						}
					} catch (IOException e) {
						Log.e(TAG, "Copy " + infoFile + " file failed " + e);
					}
					(new File(toolchainDir + "/" + infoFile)).delete();
				}				
			}
    	}
    
    	// Move Examples to sd card
		if ((new File(toolchainDir + "/cctools/Examples")).exists()) {
			try {
				Log.i(TAG, "Move Examples to SD card");
				Utils.copyDirectory(new File(toolchainDir + "/cctools/Examples"),
									new File(sdCardDir + "/Examples"));
				Utils.deleteDirectory(new File(toolchainDir + "/cctools/Examples"));
			} catch (IOException e) {
				Log.e(TAG, "Can't copy examples directory " + e);
			}
		}

		//Execute postinst scripts
		for (String name: postinstList) {
			String postinstFile = toolchainDir + "/" + PKGS_LISTS_DIR + "/" + name + ".postinst";
			Log.i(TAG, "Execute postinst file " + postinstFile);
			Utils.chmod(postinstFile, 0x1ed);
			system(postinstFile);
			(new File(postinstFile)).delete();
		}
		
    	return true;
    }
    
	private boolean uninstallPackage(String name) {
		if (name != null) {
			updateProgressTitle(getString(R.string.removing_caption));
			updateProgress(getString(R.string.wait_message));
			String prermFile = toolchainDir + "/" + PKGS_LISTS_DIR + "/" + name + ".prerm";
			if ((new File(prermFile)).exists()) {
				Log.i(TAG, "Execute prerm script " + prermFile);
				Utils.chmod(prermFile, 0x1ed);
				system(prermFile);
				(new File(prermFile)).delete();
			}
			String descFile = toolchainDir + "/" + PKGS_LISTS_DIR + "/" + name + ".pkgdesc";
			if ((new File(descFile)).exists()) {
				(new File(descFile)).delete();
			}
			String logFile = toolchainDir + "/" + PKGS_LISTS_DIR + "/" + name + ".list";
			if (!(new File(logFile)).exists()) {
				return false;
			}
			try {
				FileInputStream fin = new FileInputStream(logFile);
				DataInputStream in = new DataInputStream(fin);
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line = "";
				while((line = reader.readLine()) != null) {
					Log.i(TAG, "Delete file: " + line);
					(new File(toolchainDir + "/" + line)).delete();
				}
				in.close();
				(new File(logFile)).delete();
			} catch (Exception e) {
				Log.e(TAG, "Error during remove files " + e);
			}
			
		}
		return true;
	}

	private void system(String cmd) {
		String cctoolsDir = toolchainDir + "/cctools";
		String[] envp = {
				"TMPDIR=" + Environment.getExternalStorageDirectory().getPath(),
				"PATH=" + cctoolsDir + "/bin:" + cctoolsDir + "/sbin:/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin",
				"ANDROID_ASSETS=/system/app",
				"ANDROID_BOOTLOGO=1",				
				"ANDROID_DATA=" + cctoolsDir + "/var/dalvik",
				"ANDROID_PROPERTY_WORKSPACE=10,32768",
				"ANDROID_ROOT=/system",
				"BOOTCLASSPATH=/system/framework/core.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar",
				"TERM=xterm",
				"LD_LIBRARY_PATH=" + cctoolsDir + "/lib",
				"SDDIR=" + sdCardDir,
				"EXTERNAL_STORAGE=" + Environment.getExternalStorageDirectory().getPath(),
				"CCTOOLSDIR=" + cctoolsDir,
				"CCTOOLSRES=" + getPackageResourcePath()
				};
		try {
			Log.i(TAG, "exec cmd " + cmd + ", cctoolsdir " + cctoolsDir);
			Process p = Runtime.getRuntime().exec(cmd, envp);
			p.waitFor();
		} catch (Exception e) {
			Log.i(TAG, "Exec exception " + e);
		}		
	}

}
