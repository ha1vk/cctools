package com.pdaxrom.pkgmanager;

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
import com.pdaxrom.utils.Utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class PkgManagerActivity extends ListActivity {
	private static final String TAG = "PkgMgrActivity";
	//private static final String URL = "http://cctools.info/repo-4.8-1/" + Build.CPU_ABI;
	//private static final String URL = "http://192.168.111.1/cctools/packages/" + Build.CPU_ABI;
	private static final String URL = "http://sashz-laptop/cctools/packages/" + Build.CPU_ABI;

	private Context context = this;
	private static final String PKGS_LISTS_DIR	= "/installed/";

	public static final String INTENT_CMD		= "command";
	public static final String INTENT_DATA		= "data";
	public static final String INTENT_RETURN	= "return";
	public static final String CMD_INSTALL		= "install";
	public static final String CMD_UNINSTALL	= "uninstall";
	public static final String CMD_UPDATE		= "update";
	
	private PackagesLists packagesLists = new PackagesLists();
	
	private static boolean fCheckedUpdatesAtStartup = false;
	
	private String activityCmd	= null;
	private String activityData	= null;
	
    // Last list position
    private int lastPosition = 0;
    
    private ListView lv;
    private EditText inputSearch;
    
	private String sdCardDir;
	private String filesDir;
	private String tmpDir;
	private String toolchainDir;
    private String serviceDir;
    private String homeDir;

	final Handler handler = new Handler();
    private ProgressDialog pd;    
    
    String errorString = null;
    
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
        
        setupDirs();
        setupVersion();
        
        activityCmd = null;
        activityData = null;
        if (getIntent().getExtras() != null) {
        	setTheme(android.R.style.Theme_Translucent_NoTitleBar);
        	super.onCreate(savedInstanceState);
        	        	
        	String cmd = getIntent().getExtras().getString(INTENT_CMD);
        	
        	Log.i(TAG, "External command " + cmd);
        	
        	if (cmd.equals(CMD_UPDATE)) {
        		activityCmd = cmd;
        		fCheckedUpdatesAtStartup = false;
        	} else if (cmd.equals(CMD_INSTALL)) {
        		activityCmd = cmd;
        		activityData = getIntent().getExtras().getString(INTENT_DATA);
        	} else if (cmd.equals(CMD_UNINSTALL)) {
        		activityCmd = cmd;
        		activityData = getIntent().getExtras().getString(INTENT_DATA);
        	}
        	
            (new DownloadRepoTask()).execute(URL);
            
            return;
        } else {
        	super.onCreate(savedInstanceState);
            setContentView(R.layout.pkgmgr_main);

            inputSearch = (EditText) findViewById(R.id.inputSearch);
            
            (new DownloadRepoTask()).execute(URL);
        }
         
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

    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.pkgmanager_menu, menu);
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.item_update:
        		fCheckedUpdatesAtStartup = false;
                (new DownloadRepoTask()).execute(URL);
        		break;
        }
        return true;
    }
        
    private void setupDirs() {
    	sdCardDir 	= Environment.getExternalStorageDirectory().getPath() + "/CCTools";
    	filesDir 	= sdCardDir + "/backup";
    	tmpDir 		= sdCardDir + "/tmp";
    	toolchainDir= getCacheDir().getParentFile().getAbsolutePath() + "/root";
        serviceDir 	= toolchainDir + "/cctools/services";
        homeDir		= toolchainDir + "/cctools/home";

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
        if (!(new File(homeDir)).exists()) {
        	(new File(homeDir)).mkdir();
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
			packagesLists.setInstalledPackages(RepoUtils.getRepoFromDir(toolchainDir + "/" + PKGS_LISTS_DIR));
			packagesLists.setAvailablePackages(RepoUtils.getRepoFromUrl(params[0] + "/Packages"));
			if (packagesLists.getAvailablePackages() == null ||
				packagesLists.getInstalledPackages() == null) {
				return null;
			}
			return RepoUtils.checkingForUpdates(packagesLists);
		}

		protected void onPostExecute(List<PackageInfo> result) {
			super.onPostExecute(result);
	        if (packagesLists.getAvailablePackages() != null && activityCmd == null) {
		        Log.i(TAG, "Downloaded repo with " + packagesLists.getAvailablePackages().size() + " packages.");
	        	showPackages(packagesLists.getAvailablePackages());
	        }
	        hideProgress();
       		
       		if (activityCmd != null && activityCmd.equals(CMD_INSTALL)) {
       			if (packagesLists.getAvailablePackages() != null) {
       				(new PrepareToInstallTask()).execute(activityData);
       			} else {
       				setResult(RESULT_CANCELED);
       				finish();
       			}
       			return;
       		}

       		if (activityCmd != null && activityCmd.equals(CMD_UNINSTALL)) {
       			(new UninstallPackagesTask()).execute(activityData);
       			return;
       		}

       		if (!fCheckedUpdatesAtStartup) {
	        	fCheckedUpdatesAtStartup = true;
	        	if (result != null) {
		        	final InstallPackageInfo updateInfo = new InstallPackageInfo();
					for (PackageInfo pkg: result) {
						updateInfo.addPackage(packagesLists, pkg.getName());
					}
					Log.i(TAG, "update list = " + updateInfo.getPackagesStrings());
		        	Builder dialog = new AlertDialog.Builder(context)
		            .setIcon(android.R.drawable.ic_dialog_alert)
		            .setTitle(getString(R.string.pkg_selectedforupdate))
		            .setMessage(getString(R.string.pkg_selectedforupdate1) + updateInfo.getPackagesStrings()
		            		+ "\n\n"
		            		+ getString(R.string.pkg_selected2) 
		            		+ Utils.humanReadableByteCount(updateInfo.getDownloadSize(), false)
		            		+ "\u0020"
		            		+ getString(R.string.pkg_selected3)
		            		+ Utils.humanReadableByteCount(updateInfo.getInstallSize(), false))
		            .setOnCancelListener(new DialogInterface.OnCancelListener() {
						public void onCancel(DialogInterface dialog) {
							if (activityCmd != null && activityCmd.equals(CMD_UPDATE)) {
								setResult(RESULT_CANCELED);
		        				finish();
							}
						}
					})
		            .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (activityCmd != null && activityCmd.equals(CMD_UPDATE)) {
								setResult(RESULT_CANCELED);
		        				finish();
							}
						}
					})
		        	.setPositiveButton(getString(R.string.pkg_install), new DialogInterface.OnClickListener() {
		            	public void onClick(DialogInterface dialog, int which) {
		            		Log.i(TAG, "Get install packages = " + updateInfo.getPackagesStrings());
		            		(new InstallPackagesTask()).execute(updateInfo);
		            	}
		            });
		        	dialog.show();
	        	}
	        }
       		if (result == null && activityCmd != null && activityCmd.equals(CMD_UPDATE)) {
       			setResult(RESULT_OK);
       			finish();
       		}
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
			// Update installed packages list
			packagesLists.setInstalledPackages(RepoUtils.getRepoFromDir(toolchainDir + "/" + PKGS_LISTS_DIR));
    		return new InstallPackageInfo(packagesLists, params[0]);
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
            .setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					if (activityCmd != null) {
						setResult(RESULT_CANCELED);
		        		finish();
					}
				}
			})
            .setNeutralButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
		    		if (activityCmd != null) {
		    			setResult(RESULT_CANCELED);
		    			finish();
		    		}
				}
			})
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
			hideProgress();
			if (result) {
	    		if (activityCmd != null && 
	    				(activityCmd.equals(CMD_UPDATE) || activityCmd.equals(CMD_INSTALL))) {
	    			setResult(RESULT_OK);
	    			finish();
	    			return;
	    		}
			} else {
				showError(errorString);
				return;
			}
			lastPosition = lv.getFirstVisiblePosition();
			showPackages(packagesLists.getAvailablePackages());
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
			hideProgress();
			if (activityCmd != null && activityCmd.equals(CMD_UNINSTALL)) {
				setResult(RESULT_OK);
				finish();
				return;
			}
			lastPosition = lv.getFirstVisiblePosition();
			showPackages(packagesLists.getAvailablePackages());
		}
    }
    
	private boolean downloadAndUnpack(String file, String from, String to, String log) {
		updateProgress(getString(R.string.download_file) + " " + file + "...");
		
		errorString = null;
		
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
				int need_mem = file_size / stat.getBlockSize();
				if (sdAvailSize < need_mem) {
					temp.delete();
					errorString = getString(R.string.sd_no_memory) + 
									" " + Utils.humanReadableByteCount(need_mem, false) + 
									" " + getString(R.string.sd_no_memory2);
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
				errorString = getString(R.string.error_downloading) + " (" + file + ")";
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
			if (cacheAvailSize < need_mem) {
				errorString = getString(R.string.cache_no_memory) +
						Utils.humanReadableByteCount(need_mem, false) + 
						getString(R.string.cache_no_memory1) + 
						Utils.humanReadableByteCount((int)cacheAvailSize, false) + 
						getString(R.string.cache_no_memory2);
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
			errorString = getString(R.string.bad_archive) + " (" + file +")";
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
	
    private void showError(String message) {
    	new AlertDialog.Builder(context)
    	.setTitle(R.string.pkgmgr_name)
    	.setMessage(message)
    	.setNeutralButton(getText(R.string.button_continue), new DialogInterface.OnClickListener() {			
			public void onClick(DialogInterface dialog, int which) {
	    		if (activityCmd != null) {
	    			setResult(RESULT_CANCELED);
	    			finish();
	    			return;
	    		}
			}
		})
		.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
	    		if (activityCmd != null) {
	    			setResult(RESULT_CANCELED);
	    			finish();
	    			return;
	    		}
			}
		})
		.setIcon(android.R.drawable.ic_dialog_alert)
		.show();
    }

    private boolean installPackage(InstallPackageInfo info) {
    	List<String> postinstList = new ArrayList<String>();
    	for (PackageInfo packageInfo: info.getPackagesList()) {
    		if ((new File(toolchainDir + "/" + PKGS_LISTS_DIR + "/" 
    				+ packageInfo.getName() + ".pkgdesc")).exists()) {
    			PackageInfo oldPackage = RepoUtils.getPackageByName(packagesLists.getInstalledPackages(),
    																packageInfo.getName());
    			if (packageInfo.getVersion().equals(oldPackage.getVersion())) {
    				Log.i(TAG, "Package " + packageInfo.getName() + " already installed.");
    				continue;
    			} else {
    				uninstallPackage(packageInfo.getName());
    				if ((new File(filesDir + "/" + oldPackage.getFile())).exists()) {
    					(new File(filesDir + "/" + oldPackage.getFile())).delete();
    				}
    			}
    		}
    		
    		if (RepoUtils.isContainsPackage(packagesLists.getInstalledPackages(), packageInfo.getReplaces())) {
    			Log.i(TAG, "Replace package " + packageInfo.getReplaces());
    			PackageInfo oldPackage = RepoUtils.getPackageByName(packagesLists.getInstalledPackages(),
						packageInfo.getReplaces());
				uninstallPackage(oldPackage.getName());
				if ((new File(filesDir + "/" + oldPackage.getFile())).exists()) {
					(new File(filesDir + "/" + oldPackage.getFile())).delete();
				}
    		}
    		
    		updateProgressTitle(getString(R.string.pkg_installpackagetask) + " " + packageInfo.getName());
    		String file = packageInfo.getFile();
    		Log.i(TAG, "Install " + packageInfo.getName() + " -> " + packageInfo.getFile());
			if (!downloadAndUnpack(file, URL, toolchainDir, 
					toolchainDir + "/" + PKGS_LISTS_DIR + "/" + packageInfo.getName() + ".list")) {
				if (errorString != null) {
					errorString += "\u0020" + info.getName();
				}
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
						if (infoFile.equals("postinst")) {
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
			updateProgressTitle(getString(R.string.pkg_uninstallpackagetask) + " " + name);
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
				"CCTOOLSDIR=" + cctoolsDir,
				"CCTOOLSRES=" + getPackageResourcePath(),
				"LD_LIBRARY_PATH=" + cctoolsDir + "/lib",
				"HOME=" + homeDir,
				"TERM=xterm",
				"SDDIR=" + sdCardDir,
				"EXTERNAL_STORAGE=" + Environment.getExternalStorageDirectory().getPath(),
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
