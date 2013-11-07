package com.pdaxrom.cctools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pdaxrom.editor.CodeEditor;
import com.pdaxrom.utils.FileDialog;
import com.pdaxrom.utils.LogItem;
import com.pdaxrom.utils.PkgMgrActivity;
import com.pdaxrom.utils.SelectionMode;
import com.pdaxrom.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.text.Html;
import android.text.InputType;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.BufferType;
import android.widget.Toast;

public class CCToolsActivity extends Activity implements OnSharedPreferenceChangeListener {
	private Context context = this;
	public static final String SHARED_PREFS_NAME = "cctoolsSettings";
	private static final String SHARED_PREFS_FILES_EDITPOS = "FilesPosition";
	private SharedPreferences mPrefs;
	private static final String website_url = "http://cctools.info";
	private static final String PKGS_LISTS_DIR = "/installed/";
	private String TAG = "cctools";
	private String toolchainDir;
	private String sdCardDir;
	private String tmpDir;
	private String filesDir;
	private String serviceDir;
	private String toolsVersion = "4.8";
	private String downloadPath = "http://cctools.info/cctools/gcc-" + toolsVersion;
	private String fileName;
	private String buildBaseDir; // Project base directory
	private boolean buildAfterSave = false;
	private boolean buildAfterLoad = false;
	private ImageButton newButton;
	private ImageButton openButton;
	private ImageButton playButton;
	private ImageButton buildButton;
	private ImageButton logButton;
	private ImageButton saveButton;
	private ImageButton saveAsButton;
	private ImageButton undoButton;
	private ImageButton redoButton;
	private View buttBar;
	private CodeEditor codeEditor;
	private static final int REQUEST_OPEN = 1;
	private static final int REQUEST_SAVE = 2;
	
	private static final int WARN_SAVE_AND_NEW = 1;
	private static final int WARN_SAVE_AND_LOAD = 2;
	private static final int WARN_SAVE_AND_LOAD_POS = 3;
	private static final int WARN_SAVE_AND_BUILD = 4;
	private static final int WARN_SAVE_AND_BUILD_FORCE = 5;
	
	private static final int TEXT_GOTO = Menu.CATEGORY_CONTAINER + 1;
	private static final int TEXT_FIND = Menu.CATEGORY_CONTAINER + 2;
	private static final int TEXT_UNDO = Menu.CATEGORY_CONTAINER + 3;
	private static final int TEXT_REDO = Menu.CATEGORY_CONTAINER + 4;
	
	private boolean forceTmpVal;

	private String showFileName;
	private int showFileLine;
	private int showFilePos;
	
	private Thread dialogServiceThread;
	private ServerSocket dialogServerSocket;
	public static Socket dialogServiceSocket;

	private static final int SERVICE_STOP = 0;
	private static final int SERVICE_START = 1;
	
	private boolean externalCommandFlag = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        sdCardDir = Environment.getExternalStorageDirectory().getPath() + "/CCTools";
        tmpDir = sdCardDir + "/tmp";
        filesDir = sdCardDir + "/backup";

        if (!(new File(sdCardDir)).exists()) {
        	(new File(sdCardDir)).mkdir();
        }
        if (!(new File(tmpDir)).exists()) {
        	(new File(tmpDir)).mkdir();
        }
        if (!(new File(filesDir)).exists()) {
        	(new File(filesDir)).mkdir();
        }
        
        toolchainDir = getCacheDir().getParentFile().getAbsolutePath() + "/root";
        if (!(new File(toolchainDir)).exists()) {
        	(new File(toolchainDir)).mkdir();
        }
        
        String dalvikCache = toolchainDir + "/cctools/var/dalvik/dalvik-cache";
        if (!(new File(dalvikCache)).exists()) {
        	(new File(dalvikCache)).mkdirs();
        }
        
        updateClassPathEnv();
        
        if (!(new File(toolchainDir + PKGS_LISTS_DIR)).exists()) {
        	(new File(toolchainDir + PKGS_LISTS_DIR)).mkdir();
        }

        serviceDir = toolchainDir + "/cctools/services";
        if (!(new File(serviceDir)).exists()) {
        	(new File(serviceDir)).mkdir();
        }
        
        codeEditor = (CodeEditor) findViewById(R.id.codeEditor);
        registerForContextMenu(codeEditor);
        
        if (savedInstanceState != null) {
        	fileName = savedInstanceState.getString("filename");
        	
        	if (fileName.contentEquals("") || fileName == null) {
        		newTitle(getString(R.string.new_file));
        	} else {
        		newTitle(fileName);
        	}
        } else {
//FIXME New installer need        	
//        	updateBasePackages();
			newTitle(getString(R.string.new_file));
			fileName = "";
        }

        if (getIntent().getExtras() != null) {
        	String pkgName = getIntent().getExtras().getString("installPackage");
        	String pkgVers = getIntent().getExtras().getString("installPackageVersion");
        	String pkgFile = getIntent().getExtras().getString("installPackageFile");
        	String pkgUrl  = getIntent().getExtras().getString("installPackageUrl");
        	if (pkgFile != null && pkgUrl != null) {
        		Log.i(TAG, "Package for installation: " + pkgName + " vers " + pkgVers + " " + pkgUrl + "/" + pkgFile);
        		externalCommandFlag = true;
        		installPackage(pkgName, pkgVers, pkgFile, pkgUrl);
        		return;
        	} else {
        		pkgName = getIntent().getExtras().getString("uninstallPackage");
        		pkgFile = getIntent().getExtras().getString("uninstallPackageFile");
        		if (pkgName != null || pkgFile != null) {
        			Log.i(TAG, "Uninstalling package: " + pkgName);
            		externalCommandFlag = true;
        			uninstallPackage(pkgName, pkgFile);
        		}
        		return;
        	}
        }

        newButton = (ImageButton) findViewById(R.id.newButton);
        newButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		warnSaveDialog(WARN_SAVE_AND_NEW);
        	}
        });
        openButton = (ImageButton) findViewById(R.id.pathButton);
        openButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		warnSaveDialog(WARN_SAVE_AND_LOAD);
        	}
        });
        saveButton = (ImageButton) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		saveFile();
        	}
        });

        saveAsButton = (ImageButton) findViewById(R.id.saveAsButton);
        saveAsButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		saveAsFile();
        	}
        });

        playButton = (ImageButton) findViewById(R.id.playButton);
        playButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		warnSaveDialog(WARN_SAVE_AND_BUILD_FORCE);
        	}
        });
        
        buildButton = (ImageButton) findViewById(R.id.buildButton);
        buildButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		warnSaveDialog(WARN_SAVE_AND_BUILD);
        	}
        });

        logButton = (ImageButton) findViewById(R.id.logButton);
        logButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		showLog();
        	}
        });
        
        undoButton = (ImageButton) findViewById(R.id.undoButton);
        undoButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		codeEditor.undo();
        	}
        });

        redoButton = (ImageButton) findViewById(R.id.redoButton);
        redoButton.setOnClickListener(new OnClickListener() {
        	public void onClick(View v) {
        		codeEditor.redo();
        	}
        });
        
        buttBar = (View) findViewById(R.id.toolButtonsBar);

        mPrefs = getSharedPreferences(SHARED_PREFS_NAME, 0);
        mPrefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(mPrefs, null);
        
        dialogServiceThread = dialogService(13527);
        serviceStartStop(SERVICE_START);
        
        // Get intent, action and MIME type
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if ((Intent.ACTION_VIEW.equals(action) ||
        	 Intent.ACTION_EDIT.equals(action)) &&
        	 type != null) {
            if (type.startsWith("text/")) {
            	Uri uri = intent.getData();
            	fileName = uri.getPath();
            	Log.i(TAG, "Load external file " + fileName);
            	if (codeEditor.loadFile(fileName)) {
    				loadFileEditPos(fileName);
    				newTitle(fileName);
            	}
            }
        }
    }
    
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		Log.i(TAG, "onSharedPreferenceChanged()");
		codeEditor.setTextSize(Float.valueOf(prefs.getString("fontsize", "12")));
		codeEditor.showSyntax(prefs.getBoolean("syntax", true));
		codeEditor.drawLineNumbers(prefs.getBoolean("drawLineNumbers", true));
		codeEditor.drawGutterLine(prefs.getBoolean("drawLineNumbers", true));
		codeEditor.setAutoPair(prefs.getBoolean("autopair", true));
		codeEditor.setAutoIndent(prefs.getBoolean("autoindent", true));
		if (prefs.getBoolean("showToolBar", true)) {
			buttBar.setVisibility(View.VISIBLE);
		} else {
			buttBar.setVisibility(View.GONE);
		}
	}

    protected void onSaveInstanceState(Bundle saveState) {
    	super.onSaveInstanceState(saveState);
    	saveState.putString("filename", fileName);
    	saveState.putBoolean("hasChanged", codeEditor.hasChanged());
    }

	protected void onDestroy() {
		if (!externalCommandFlag) {
			if (fileName != null) {
				setLastOpenedDir((new File (fileName)).getParent());				
			}
	        serviceStartStop(SERVICE_STOP);

	        if (dialogServiceThread.isAlive()) {
				Log.i(TAG, "Stop dialog service");
				dialogServiceThread.interrupt();
			}
			if ((dialogServerSocket != null) && 
				(!dialogServerSocket.isClosed())) {
				try {
					dialogServerSocket.close();
				} catch (IOException e) {
					Log.e(TAG, "Error close dialogServerSocket " + e);
				}
			}
			
			Log.i(TAG, "Clean temp directory");
			Utils.deleteDirectory(new File(toolchainDir + "/tmp"));
		}
		super.onDestroy();
	}

	public void onBackPressed() {
		if (codeEditor.hasChanged()) {
			exitDialog();
		} else {
			super.onBackPressed();
		}
	}
	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
			menu.add(0, TEXT_GOTO, 0, getString(R.string.menu_goto));
			menu.add(0, TEXT_FIND, 0, getString(R.string.menu_search));
			menu.add(0, TEXT_UNDO, 0, getString(R.string.menu_undo));
			menu.add(0, TEXT_REDO, 0, getString(R.string.menu_redo));
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	public boolean onContextItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case TEXT_GOTO:
			gotoDialog();
			break;
		case TEXT_FIND:
			searchDialog();
			break;
		case TEXT_UNDO:
			codeEditor.undo();
			break;
		case TEXT_REDO:
			codeEditor.redo();
			break;
		default:
			return super.onContextItemSelected(item);			
		}
		return true;
	}
	
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.menu, menu);
    	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			menu.add(0, TEXT_UNDO, 0, getString(R.string.menu_undo)).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			menu.add(0, TEXT_REDO, 0, getString(R.string.menu_redo)).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			menu.add(0, TEXT_GOTO, 0, getString(R.string.menu_goto)).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			menu.add(0, TEXT_FIND, 0, getString(R.string.menu_search)).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    	}
    	return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        	case R.id.item_new:
        		warnSaveDialog(WARN_SAVE_AND_NEW);
        		break;
        	case R.id.item_open:
        		warnSaveDialog(WARN_SAVE_AND_LOAD);
        		break;
        	case R.id.item_save:
        		saveFile();
        		break;
        	case R.id.item_saveas:
        		saveAsFile();
        		break;
        	case R.id.item_run:
        		warnSaveDialog(WARN_SAVE_AND_BUILD_FORCE);
        		break;
        	case R.id.item_build:
        		warnSaveDialog(WARN_SAVE_AND_BUILD);
        		break;
        	case R.id.item_buildlog:
        		showLog();
        		break;
        	case R.id.item_pkgmgr:
        		packageManager();
        		break;
        	case R.id.prefs:
        		startActivity(new Intent(this, Preferences.class));
        		break;
        	case R.id.about:
        		aboutDialog();
        		break;
        	case TEXT_GOTO:
        		gotoDialog();
        		break;
        	case TEXT_FIND:
        		searchDialog();
        		break;
        	case TEXT_UNDO:
        		codeEditor.undo();
        		break;
        	case TEXT_REDO:
        		codeEditor.redo();
        		break;
        }
        return true;
    }

    private String getPrefString(String key) {
	    SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
		return settings.getString(key, Environment.getExternalStorageDirectory().getPath() + "/CCTools/Examples");    	
    }
    
    private void setPrefString(String key, String value) {
		SharedPreferences settings = getPreferences(Activity.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(key, value);
		editor.commit();
    }
    
    private String getLastOpenedDir() {
    	return getPrefString("lastdir");
    }
    
    private void setLastOpenedDir(String dir) {
    	setPrefString("lastdir", dir);
    }
    
    private void newTitle(String title) {
    	setTitle("CCTools - " + title);
    }
    
    private void newFile() {
		newTitle(getString(R.string.new_file));
		buildAfterSave = false;
		buildAfterLoad = false;
		fileName = "";
		codeEditor.newFile();
		Toast.makeText(getBaseContext(), getString(R.string.new_file), Toast.LENGTH_SHORT).show();    	
    }
    
    private void loadFile() {
		Intent intent = new Intent(getBaseContext(), FileDialog.class);
		String dir = Environment.getExternalStorageDirectory().getPath();
		if (fileName != null) {
			dir = (new File(fileName)).getParent();
		}
		if (dir == null) {
			dir = getLastOpenedDir();
		}
		
		intent.putExtra(FileDialog.START_PATH, dir);
		intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_OPEN);
		startActivityForResult(intent, REQUEST_OPEN);
    }
    
    private void saveFile() {
		if (fileName.contentEquals("") || fileName == null) {
			String dir = Environment.getExternalStorageDirectory().getPath();
			if (fileName != null) {
				dir = (new File(fileName)).getParent();
			}
			if (dir == null) {
				dir = getLastOpenedDir();
			}
    		Intent intent = new Intent(getBaseContext(), FileDialog.class);
    		intent.putExtra(FileDialog.START_PATH, dir);
    		intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
    		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_CREATE);
    		startActivityForResult(intent, REQUEST_SAVE);
		} else {
			if (codeEditor.saveFile(fileName)) {
				saveFileEditPos(fileName);
				Toast.makeText(getBaseContext(), getString(R.string.file_saved), Toast.LENGTH_SHORT).show();
				setLastOpenedDir((new File (fileName)).getParent());
			} else {
				Toast.makeText(getBaseContext(), getString(R.string.file_not_saved), Toast.LENGTH_SHORT).show();
			}
			if (buildAfterSave) {
				buildFile(forceTmpVal);
				buildAfterSave = false;
			}
		}    	
    }
    
    private void saveAsFile() {
		Intent intent = new Intent(getBaseContext(), FileDialog.class);
		String dir = Environment.getExternalStorageDirectory().getPath();
		if (fileName != null) {
			dir = (new File(fileName)).getParent();
		}
		if (dir == null) {
			dir = getLastOpenedDir();
		}
		intent.putExtra(FileDialog.START_PATH, dir);
		intent.putExtra(FileDialog.CAN_SELECT_DIR, false);
		intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_CREATE);
		startActivityForResult(intent, REQUEST_SAVE);    	
    }
    
    private void saveFileEditPos(String file) {
    	SharedPreferences settings = getSharedPreferences(SHARED_PREFS_FILES_EDITPOS, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putInt(file, codeEditor.getSelectionStart());
    	editor.commit();
    }

    private void loadFileEditPos(String file) {
    	SharedPreferences settings = getSharedPreferences(SHARED_PREFS_FILES_EDITPOS, 0);
    	if (codeEditor.getText().toString().length() >= settings.getInt(file, 0)) {
    		codeEditor.setSelection(settings.getInt(file, 0));
    	}
    }

    private void build(boolean force) {
//TODO: add force save options to preferences?    	
//		if (codeEditor.hasChanged() && codeEditor.getText().length() > 0) {
//			buildAfterSave = true;
//			forceTmpVal = force;
//			saveFile();
//		} else 
		if (fileName.contentEquals("") || fileName == null) {
			buildAfterLoad = true;
			forceTmpVal = force;
			loadFile();
		} else
			buildFile(force);    	
    }
    
    private void buildFile(boolean force) {
		Log.i(TAG, "build activity " + fileName);
		if ((new File(fileName)).exists()) {
			buildBaseDir = (new File(fileName)).getParentFile().getAbsolutePath();
			String infile = (new File(fileName)).getName();
			if (infile.lastIndexOf(".") != -1) {
				String ext = infile.substring(infile.lastIndexOf("."));
				if (ext.contentEquals(".sh")) {
		    		Intent intent = new Intent(CCToolsActivity.this, LauncherConsoleActivity.class);
		    		intent.putExtra("executable_file", fileName);
		    		intent.putExtra("cctoolsdir", toolchainDir + "/cctools");
		    		SharedPreferences mPrefs = getSharedPreferences(CCToolsActivity.SHARED_PREFS_NAME, 0);
		    		if (force) {
		    			intent.putExtra("force", mPrefs.getBoolean("force_run", false));
		    		} else {
		    			intent.putExtra("force", false);
		    		}
		    		startActivity(intent);
		    		return;
				}
				if (ext.contentEquals(".lua") && (new File(toolchainDir + "/cctools/bin/luajit")).exists()) {
		    		Intent intent = new Intent(CCToolsActivity.this, LauncherConsoleActivity.class);
		    		intent.putExtra("executable_file", toolchainDir + "/cctools/bin/luajit " + fileName);
		    		intent.putExtra("cctoolsdir", toolchainDir + "/cctools");
		    		intent.putExtra("workdir", (new File(fileName)).getParentFile().getAbsolutePath());
		    		SharedPreferences mPrefs = getSharedPreferences(CCToolsActivity.SHARED_PREFS_NAME, 0);
		    		if (force) {
		    			intent.putExtra("force", mPrefs.getBoolean("force_run", true));
		    		} else {
		    			intent.putExtra("force", false);
		    		}
		    		startActivity(intent);
		    		return;					
				}
			}
			Intent intent = new Intent(CCToolsActivity.this, BuildActivity.class);
			intent.putExtra("filename", fileName);
			intent.putExtra("cctoolsdir", toolchainDir + "/cctools");
			intent.putExtra("tmpdir", tmpDir);
			intent.putExtra("force", force);
			startActivity(intent);
		}
    }
    
	static private final String KEY_FILE = "file";
	static private final String KEY_LINE = "line";
	static private final String KEY_POS  = "pos";
	static private final String KEY_TYPE = "type";
	static private final String KEY_MESG = "mesg";

	public class SimpleHtmlAdapter extends SimpleAdapter {
		public SimpleHtmlAdapter(Context context, List<HashMap<String, String>> items, int resource, String[] from, int[] to) {
			super(context, items, resource, from, to);
		}

	    public void setViewText (TextView view, String text) {
	        view.setText(Html.fromHtml(text),BufferType.SPANNABLE);
	    }
	}
	
    private void showLog() {
    	if (BuildActivity.errorsList.isEmpty()) {
    		Toast.makeText(getBaseContext(), getString(R.string.log_empty), Toast.LENGTH_SHORT).show();
    		return;
    	}
    	ArrayList<HashMap<String, String>> menuItems = new ArrayList<HashMap<String, String>>();
    	for (LogItem item: BuildActivity.errorsList) {
    		HashMap<String, String> map = new HashMap<String, String>();
    		map.put(KEY_FILE, item.getFile());
    		map.put(KEY_LINE, Integer.toString(item.getLine()));
    		map.put(KEY_POS, Integer.toString(item.getPos()));
    		map.put(KEY_TYPE, item.getType());
    		String color = "<font color=\"";
    		if (item.getType().contains("error")) {
    			color += "red\">ERROR: ";
    		} else {
    			color += "yellow\">WARNING: ";
    		}
    		map.put(KEY_MESG, color + item.getMessage() + "</font>");
    		menuItems.add(map);
    	}
    	final ListView listView = new ListView(this);
    	listView.setAdapter(new SimpleHtmlAdapter(
        		this,
        		menuItems,
        		R.layout.buildlog_item,
        		new String [] { KEY_FILE, KEY_LINE, KEY_POS, KEY_TYPE, KEY_MESG },
        		new int[] {R.id.buildlog_file, R.id.buildlog_line, R.id.buildlog_pos, R.id.buildlog_type, R.id.buildlog_mesg}
        	));
    	AlertDialog.Builder dialog = new AlertDialog.Builder(this);
    	dialog.setView(listView);
    	final AlertDialog alertDialog = dialog.create();
    	
    	listView.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,
					long id) {
				int line = Integer.parseInt(((TextView) view.findViewById(R.id.buildlog_line)).getText().toString());
				int pos = Integer.parseInt(((TextView) view.findViewById(R.id.buildlog_pos)).getText().toString());
				String name = ((TextView) view.findViewById(R.id.buildlog_file)).getText().toString();
				
				if (!name.startsWith("/")) {
					name = buildBaseDir + "/" + name;
				}
				
				if (!(new File(fileName)).getAbsolutePath().contentEquals((new File(name)).getAbsolutePath())) {
					alertDialog.cancel();
					showFileName = name;
					showFileLine = line;
					showFilePos = pos;
	            	Log.i(TAG, "Jump to file " + showFileName);
					warnSaveDialog(WARN_SAVE_AND_LOAD_POS);
				} else {
					if (pos > 0) {
						codeEditor.goToLinePos(line, pos);
					} else {
						codeEditor.goToLine(line);
					}
					alertDialog.cancel();
				}
			}    		
    	});
    	alertDialog.show();
    }
    
    private void packageManager() {
    	Intent intent = new Intent(CCToolsActivity.this, PkgMgrActivity.class);
    	startActivity(intent);
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_SAVE) {
    			fileName = data.getStringExtra(FileDialog.RESULT_PATH);
    			if (codeEditor.saveFile(fileName)) {
    				saveFileEditPos(fileName);
    				Toast.makeText(getBaseContext(), getString(R.string.file_saved), Toast.LENGTH_SHORT).show();
    				if (buildAfterSave) {
    					buildAfterSave = false;
    					buildFile(forceTmpVal);
    				}
    			} else {
    				Toast.makeText(getBaseContext(), getString(R.string.file_not_saved), Toast.LENGTH_SHORT).show();
    				buildAfterSave = false;
    			}
				newTitle(fileName);
			} else if (requestCode == REQUEST_OPEN) {
    			fileName = data.getStringExtra(FileDialog.RESULT_PATH);
    			if (codeEditor.loadFile(fileName)) {
    				loadFileEditPos(fileName);
    				Toast.makeText(getBaseContext(), getString(R.string.file_loaded), Toast.LENGTH_SHORT).show();
    				if (buildAfterLoad) {
    					buildAfterLoad = false;
    					buildFile(forceTmpVal);
    				}
    			} else {
    				Toast.makeText(getBaseContext(), getString(R.string.file_not_loaded), Toast.LENGTH_SHORT).show();
    				buildAfterLoad = false;
    			}
    			newTitle(fileName);
    		}
    	}
    }

    private void exitDialog() {
    	new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(getString(R.string.exit_dialog))
        .setMessage(getString(R.string.exit_dialog_text))
        .setPositiveButton(getString(R.string.exit_dialog_yes), new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		finish();
        	}
        })
        .setNegativeButton(getString(R.string.exit_dialog_no), null)
        .show();
    }

    private void loadAndShowLinePos() {
		if (codeEditor.loadFile(showFileName)) {
			Toast.makeText(getBaseContext(), getString(R.string.file_loaded), Toast.LENGTH_SHORT).show();
			if (showFilePos > 0) {
				codeEditor.goToLinePos(showFileLine, showFilePos);
			} else {
				codeEditor.goToLine(showFileLine);
			}
			fileName = showFileName;
			newTitle(fileName);
		} else {
			Toast.makeText(getBaseContext(), getString(R.string.file_not_loaded), Toast.LENGTH_SHORT).show();
		}
		
    }
    
	private void warnSaveRequest(int req) {
		switch (req) {
		case WARN_SAVE_AND_NEW:
    		newFile();
    		break;
		case WARN_SAVE_AND_LOAD:
			loadFile();
			break;
		case WARN_SAVE_AND_LOAD_POS:
			loadAndShowLinePos();
			break;
		case WARN_SAVE_AND_BUILD:
			build(false);
			break;
		case WARN_SAVE_AND_BUILD_FORCE:
			build(true);
			break;
		}
	}
	
    private void warnSaveDialog(final int req) {
    	if (!codeEditor.hasChanged()) {
    		warnSaveRequest(req);
    		return;
    	}
    	
    	new AlertDialog.Builder(this)
        .setIcon(android.R.drawable.ic_dialog_alert)
        .setTitle(getString(R.string.save_warn_dialog))
        .setMessage(getString(R.string.save_warn_text))
        .setPositiveButton(getString(R.string.exit_dialog_yes), new DialogInterface.OnClickListener() {
        	public void onClick(DialogInterface dialog, int which) {
        		warnSaveRequest(req);
        	}
        })
        .setNegativeButton(getString(R.string.exit_dialog_no), null)
        .show();
    }
    
    private void aboutDialog() {
    	String versionName;
		try {
			versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			versionName = "1.0";
		}
		final TextView textView = new TextView(this);
		textView.setAutoLinkMask(Linkify.WEB_URLS);
		textView.setLinksClickable(true);
		textView.setTextSize(16f);
		textView.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
		textView.setText(getString(R.string.about_dialog_text) +
									" " + 
									versionName + 
									"\n" + website_url + "\n" +
									getString(R.string.about_dialog_text2));
		textView.setMovementMethod(LinkMovementMethod.getInstance());
		new AlertDialog.Builder(this)
	    .setTitle(getString(R.string.about_dialog))
	    .setView(textView)
	    .show();
    }
    
    private void gotoDialog() {
    	final EditText input = new EditText(context);
    	input.setInputType(InputType.TYPE_CLASS_NUMBER);
    	input.setSingleLine(true);
    	new AlertDialog.Builder(context)
    	.setMessage(getString(R.string.goto_line))
    	.setView(input)
    	.setPositiveButton(getString(R.string.button_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					codeEditor.goToLine(Integer.valueOf(input.getText().toString()));
				} catch (Exception e) {
					Log.e(TAG, "gotoDialog() " + e);
				}
			}
		})
		.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				
			}
		})
		.show();
    }
    
    private void searchDialog() {
    	final EditText input = new EditText(context);
    	input.setInputType(InputType.TYPE_CLASS_TEXT);
    	input.setSingleLine(true);
    	input.setText(codeEditor.getLastSearchText());
    	input.setSelection(0, codeEditor.getLastSearchText().length());
    	new AlertDialog.Builder(context)
    	.setMessage(getString(R.string.search_string))
    	.setView(input)
    	.setPositiveButton(getString(R.string.button_continue), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				codeEditor.searchText(input.getText().toString());
			}
		})
		.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				
			}
		})
		.show();    	
    }
    
	private ProgressDialog pd;

	//
	// Download and install package
	//
	private void updateBasePackages() {
		Thread t = new Thread() {
			public void run() {
				downloadAndUnpackBase();
			}
		};
		t.start();
	}

	private void installPackage(final String pkgName, final String vers, final String file, final String url) {
		Thread t = new Thread() {
			public void run() {
				String name = pkgName;
				if (name == null) {
					name = file;
				}
				if (file != null) {
					if (!downloadAndUnpack(file, url, toolchainDir, null, toolchainDir + PKGS_LISTS_DIR + name + ".list")) {
						return;
					}
				} else {
					return;
				}
				show_progress();
				output(getString(R.string.wait_message));
				if ((new File(toolchainDir + "/cctools/Examples")).exists()) {
					try {
						Utils.copyDirectory(new File(toolchainDir + "/cctools/Examples"), new File(sdCardDir + "/Examples"));
						Utils.deleteDirectory(new File(toolchainDir + "/cctools/Examples"));
					} catch (IOException e) {
						Log.e(TAG, "Can't copy examples directory " + e);
					}
				}
				if ((new File(toolchainDir + "/pkgdesc")).exists()) {
					try {
						Utils.copyDirectory(new File(toolchainDir + "/pkgdesc"), new File(toolchainDir + PKGS_LISTS_DIR + name + ".pkgdesc"));
						Utils.deleteDirectory(new File(toolchainDir + "/pkgdesc"));
					} catch (IOException e) {
						Log.e(TAG, "Copy pkgdesc file failed " + e);
					}
				}
				if ((new File(toolchainDir + "/postinst")).exists()) {
					Log.i(TAG, "Execute postinst script");
					Utils.chmod(toolchainDir + "/postinst", 0x1ed);
					system(toolchainDir + "/postinst");
					(new File(toolchainDir + "/postinst")).delete();
				}
				if ((new File(toolchainDir + "/prerm")).exists()) {
					String prermFile = toolchainDir + PKGS_LISTS_DIR + name + ".prerm";
					Log.i(TAG, "Copy prerm file to " + prermFile);
					try {
						Utils.copyDirectory(new File(toolchainDir + "/prerm"), new File(prermFile));
						Utils.chmod(prermFile, 0x1ed);
					} catch (IOException e) {
						Log.e(TAG, "Copy prerm file failed " + e);
					}
					(new File(toolchainDir + "/prerm")).delete();
				}
				hide_progress();
				finishExternalCommand();
			}
		};
		t.start();
	}
	
	private void removeFilesByList(String name) {
		if (name != null) {
			show_progress();
			outputTitle(getString(R.string.removing_caption));
			output(getString(R.string.wait_message));
			String prermFile = toolchainDir + PKGS_LISTS_DIR + name + ".prerm";
			if ((new File(prermFile)).exists()) {
				Log.i(TAG, "Execute prerm script " + prermFile);
				system(prermFile);
				(new File(prermFile)).delete();
			}
			String descFile = toolchainDir + PKGS_LISTS_DIR + name + ".pkgdesc";
			if ((new File(descFile)).exists()) {
				(new File(descFile)).delete();
			}
			String logFile = toolchainDir + PKGS_LISTS_DIR + name + ".list";
			if (!(new File(logFile)).exists()) {
				hide_progress();
				return;
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
		hide_progress();		
	}
	
	private void uninstallPackage(final String pkgName, final String file) {
		Thread t = new Thread() {
			public void run() {
				String name = pkgName;
				if (name == null) {
					name = file;
				}
				removeFilesByList(name);
				finishExternalCommand();
			}
		};
		t.start();
	}
	
	final int sdk2ndk_arm[] = {
			/*   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20  21  22  23  */
			-1, -1, -1,  3,  4,  5,  5,  5,  8,  9,  9,  9,  9,  9, 14, 14, 14, 14, 18, 18, 18, 18, 18, -1
	};
	final int sdk2ndk_mips[] = {
			/*   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20 */
			-1, -1, -1, -1, -1, -1, -1, -1, -1,  9,  9, -1, -1, -1, 14, 14, 14, 14, 18, 18, 18, 18, 18, -1
	};
	final int sdk2ndk_x86[] = {
			/*   1   2   3   4   5   6   7   8   9  10  11  12  13  14  15  16  17  18  19  20 */
			-1, -1, -1, -1, -1, -1, -1, -1, -1,  9,  9, -1, -1, -1, 14, 14, 14, 14, 18, 18, 18, 18, 18, -1
	};
	final Handler handler = new Handler();

	private void downloadAndUnpackBase() {
        String cctools_base = "toolchain-";
        String cctools_sdk  = "platform-";
        int sdk_version = Build.VERSION.SDK_INT;
        int ndk_version = -1;
        if (Build.CPU_ABI.startsWith("arm")) {
			cctools_base += "arm";
        	cctools_sdk  += "arm";
        	if (sdk2ndk_arm.length > sdk_version) {
        		ndk_version = sdk2ndk_arm[sdk_version];
        	}
        } else if (Build.CPU_ABI.startsWith("mips")) {
			cctools_base += "mips";
        	cctools_sdk  += "mips";
        	if (sdk2ndk_mips.length > sdk_version) {
        		ndk_version = sdk2ndk_mips[sdk_version];
        	}
        } else {
			cctools_base += "x86";
        	cctools_sdk  += "x86";
        	if (sdk2ndk_x86.length > sdk_version) {
        		ndk_version = sdk2ndk_x86[sdk_version];
        	}
        }
        
        cctools_sdk  += "-" + ndk_version;

        if (ndk_version == -1) {
        	show_error(getString(R.string.unsupported_device) + " (" + Build.CPU_ABI + "/" + Build.VERSION.SDK_INT + ")");
        	return;
        }

		cctools_base += ".zip";
		cctools_sdk  += ".zip";

		Log.i(TAG, "TOOLCHAIN VERSION " + toolsVersion + " (" + getPrefString("base_version") + ")");
		
		if (!getPrefString("base_version").contentEquals(toolsVersion)) {
			if ((new File(toolchainDir + "/installed/" + cctools_base + ".list")).exists()) {
				removeFilesByList(cctools_base);
				new File(filesDir + "/" + cctools_base).delete();
			}
			if (!downloadAndUnpack(cctools_base, downloadPath, toolchainDir, null, null)) {
				return;
			}
			setPrefString("base_version", toolsVersion);
		}
		
		if (!getPrefString("sdk_version").contentEquals(toolsVersion)) {
			if ((new File(toolchainDir + "/installed/" + cctools_sdk + ".list")).exists()) {
				removeFilesByList(cctools_sdk);
				new File(filesDir + "/" + cctools_sdk).delete();
			}
			if (!downloadAndUnpack(cctools_sdk, downloadPath, toolchainDir, null, null)) {
				return;
			}
			setPrefString("sdk_version", toolsVersion);
		}
		
		// Common support files
		String cctools_common = "platform-common.zip";
		if (!getPrefString("common_version").contentEquals(toolsVersion)) {
			if ((new File(toolchainDir + "/installed/" + cctools_common + ".list")).exists()) {
				removeFilesByList(cctools_common);
				new File(filesDir + "/" + cctools_common).delete();
			}
			if (!downloadAndUnpack(cctools_common, downloadPath, toolchainDir, null, null)) {
				return;
			}
			setPrefString("common_version", toolsVersion);
		}

		// Examples
		String cctools_examples = "cctools-examples-1.03.zip";
		if (!(new File(toolchainDir + "/installed/" + cctools_examples + ".list")).exists()) {
			if (!downloadAndUnpack(cctools_examples, downloadPath, sdCardDir, "Examples", null)) {
				return;
			}
		}
	}

	private boolean downloadAndUnpack(String file, String from, String to, String delDir, String log) {
		show_progress();
		
		output(getString(R.string.download_file) + " " + file + "...");
		
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
					hide_progress();
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
					output(getString(R.string.received) + " " + totalread + " " + getString(R.string.from) + " " + file_size + " " + getString(R.string.bytes));
				} while (true);
				stream.close();
				out.close();
				if (totalread != file_size) {
					throw new RuntimeException("Partially downloaded file!");
				}
			} catch (Exception e) {
				temp.delete();
				Log.i(TAG, "Error downloading file " + file);
				hide_progress();
				show_error(getString(R.string.error_downloading) + " (" + file + ")");
				return false;
			}
		} else
			Log.i(TAG, "Use file " + temp.getAbsolutePath());

		String tempPath = temp.getAbsolutePath();
		output(getString(R.string.unpacking_file) + " " + file + "...");
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
				hide_progress();
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
			if (delDir != null) {
				Utils.deleteDirectory(new File(to + "/" + delDir));
			}
			temp.delete();
			Log.i(TAG, "Corrupted archive, restart application and try install again");
			hide_progress();
			show_error(getString(R.string.bad_archive) + " (" + file +")");
			return false;
		}
		hide_progress();
		return true;
	}

	private void show_progress() {
		handler.post(new Runnable() {
			public void run() {
				pd = ProgressDialog.show(context, getString(R.string.updating_caption) + "...",
						getString(R.string.establishing_handshake_message) + "...",
						true);

			}
		});
	}
	
	private void output(final String out) {
		handler.post(new Runnable() {
			public void run() {
				pd.setMessage(out);
			}
		});
	}

	private void outputTitle(final String out) {
		handler.post(new Runnable() {
			public void run() {
				pd.setTitle(out);
			}
		});
	}
	
	private void hide_progress() {
		handler.post(new Runnable() {
			public void run() {
				pd.dismiss();
			}
		});
	}
	
    private void show_error(final String message) {
    	Runnable proc = new Runnable() {
    		public void run() {
    	    	AlertDialog alertDialog = new AlertDialog.Builder(context).create();
    	    	alertDialog.setTitle(R.string.app_name);
    	    	alertDialog.setMessage(message);
    	    	alertDialog.setButton(getString(R.string.exit_button), new DialogInterface.OnClickListener() {
    	    		   public void onClick(DialogInterface dialog, int which) {
    	    			   finish();
    	    			   System.exit(0);
    	    		   }
    	    		});
    	    	alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
    	    	alertDialog.show();    			
    		}
    	};
    	handler.post(proc);
    }

	private void finishExternalCommand() {
		handler.post(new Runnable() {
			public void run() {
				Intent intent = new Intent();
				setResult(RESULT_OK, intent);
				finish();
				//TODO: probably we don't need it
				//System.exit(0);
			}
		});
	}

	private Thread dialogService(final int port) {
		Log.i(TAG, "Launch dialog service (port " + port + ")");
		Thread t = new Thread() {
			public void run() {
				try {
					//ServerSocket ss = new ServerSocket(port, 0, null /*InetAddress.getByName(null)*/);
					dialogServerSocket = new ServerSocket();
					dialogServerSocket.setReuseAddress(true);
					dialogServerSocket.bind(new InetSocketAddress(port));
					Log.i(TAG, "Waiting for incoming requests");
					while (true) {
						dialogServiceSocket = dialogServerSocket.accept();
						Intent intent = new Intent(CCToolsActivity.this, DialogWindow.class);
						startActivity(intent);
						Log.i(TAG, "Waiting for finish dialog activity");
						while(!dialogServiceSocket.isClosed()) {
							Thread.sleep(300);
						}
						Log.i(TAG, "Dialog activity finished");
					}
				} catch (BindException e) {
					Log.e(TAG, "bind failed, try again");
				} catch (IOException e) {
					Log.e(TAG, "ServerSocket " + e);
				} catch (InterruptedException e) {
					Log.e(TAG, "Interrupted " + e);
				}
			}
		};
		t.start();
		return t;
	}
	
	private void serviceStartStop(final int cmd) {
		String serviceCmd;
		if (cmd == SERVICE_START) {
			serviceCmd = "start";
		} else {
			serviceCmd = "stop";
		}
		Log.i(TAG, "Console services " + serviceCmd);
		File dir = new File(serviceDir);
		if (dir.exists()) {
			String services[] = dir.list();
			for (final String service: services) {
				Log.i(TAG, "Service " + service + " " + serviceCmd);
				new Thread() {
					public void run() {
						String serviceCmd;
						if (cmd == SERVICE_START) {
							serviceCmd = "start";
						} else {
							serviceCmd = "stop";
						}
						String app = serviceDir + "/" + service + " " + serviceCmd;
						system(app);
					}
				}.start();
			}			
		}
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
	
	private void updateClassPathEnv() {
		String cpEnvDir = toolchainDir + "/cctools/etc";
		if (! (new File(cpEnvDir)).exists()) {
			(new File(cpEnvDir)).mkdirs();
		}
		try {
			String env = "CCTOOLSCP=" + getPackageResourcePath() + "\n";
			FileOutputStream outf = new FileOutputStream(cpEnvDir + "/cp.env");
			outf.write(env.getBytes());
			outf.close();
		} catch (IOException e) {
			Log.e(TAG, "create cp.env " + e);
		}	
	}
}
