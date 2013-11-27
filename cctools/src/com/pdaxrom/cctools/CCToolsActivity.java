package com.pdaxrom.cctools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.pdaxrom.editor.CodeEditor;
import com.pdaxrom.pkgmanager.PkgManagerActivity;
import com.pdaxrom.utils.FileDialog;
import com.pdaxrom.utils.LogItem;
import com.pdaxrom.utils.SelectionMode;
import com.pdaxrom.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
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
	private String fileName;
	private String buildBaseDir; // Project base directory
	private boolean buildAfterSave = false;
	private boolean buildAfterLoad = false;
	private ImageButton newButton;
	private ImageButton openButton;
	private ImageButton playButton;
	private ImageButton buildButton;
	private ImageButton logButton;
	private ImageButton terminalButton;
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
        
        mPrefs = getSharedPreferences(SHARED_PREFS_NAME, 0);

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
        	installOrUpgradeToolchain();
        	
			newTitle(getString(R.string.new_file));
			fileName = "";
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
        
        terminalButton = (ImageButton) findViewById(R.id.terminalButton);
        terminalButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				runTerminal();
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

        dialogServiceThread = dialogService(13527);
        serviceStartStop(SERVICE_START);

        mPrefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(mPrefs, null);

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
        	case R.id.item_terminal:
        		runTerminal();
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
    	Intent intent = new Intent(CCToolsActivity.this, PkgManagerActivity.class);
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
    
    private void runTerminal() {
		Intent myIntent = new Intent(this, TermActivity.class);
		myIntent.putExtra("filename", getShell());
		myIntent.putExtra("cctoolsdir", toolchainDir + "/cctools");
		String workDir = toolchainDir + "/cctools/home";
		if ((new File(fileName)).exists()) {
			workDir = (new File(fileName)).getParentFile().getAbsolutePath();
		}
		myIntent.putExtra("workdir", workDir);
		startActivity(myIntent);
    }
    
    private void installOrUpgradeToolchain() {
		if (!getPrefString("use_package_manager").equals("yes")) {
			(new RemoveOldToolchainTask()).execute((Void)null);
		} else if (!getPrefString("toolchain_installed").equals("yes")) {
			installToolchainPackage();
		} else {
			if (mPrefs.getBoolean("updater", true)) {
				Intent intent = new Intent(this, PkgManagerActivity.class);
				intent.putExtra(PkgManagerActivity.INTENT_CMD, PkgManagerActivity.CMD_UPDATE);
				startActivity(intent);
			}
		}
    }

    //FIXME
	int toolchainPackageToInstall = 0;
    private void installToolchainPackage() {
    	final String[] toolchainPackage = {
    			"build-essential-clang-compact",
    			"build-essential-gcc-compact",
    			"build-essential-fortran-compact",
    			"build-essential-gcc-avr",
    			"build-essential-luajit"
    	};
    	
    	setPrefString("toolchain_installed", "yes");
    	
		final Spinner spinner = new Spinner(context);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
		        R.array.toolchain_selector, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);	
		
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> arg0, View view,
					int position, long id) {
				Log.i(TAG, "selected " + toolchainPackage[position]);
				toolchainPackageToInstall = position;
			}
			public void onNothingSelected(AdapterView<?> arg0) {
				// TODO Auto-generated method stub
			}
		});
		
		new AlertDialog.Builder(this)
		.setTitle(getText(R.string.toolchain_selector))
		.setMessage(getText(R.string.toolchain_selectormsg))
		.setView(spinner)
		.setPositiveButton(getText(R.string.pkg_install), new DialogInterface.OnClickListener() {			
			public void onClick(DialogInterface dialog, int which) {
	        	Intent intent = new Intent(context, PkgManagerActivity.class);
	        	intent.putExtra(PkgManagerActivity.INTENT_CMD, PkgManagerActivity.CMD_INSTALL);
	        	intent.putExtra(PkgManagerActivity.INTENT_DATA, toolchainPackage[toolchainPackageToInstall]);
	        	startActivity(intent);
			}
		})
		.show();
    }
    
    private class RemoveOldToolchainTask extends AsyncTask<Void, Void, Void> {
    	protected void onPreExecute() {
    		super.onPreExecute();
    	}

		protected Void doInBackground(Void... params) {
			final String[] oldPackages = {
					"cctools-examples-1.00.zip",	"cctools-examples-1.01.zip","cctools-examples-1.02.zip",
					"cctools-examples-1.03.zip", 	"platform-arm-3.zip",		"platform-arm-8.zip",
					"platform-mips-14.zip", 		"platform-x86-14.zip",		"toolchain-arm.zip",
					"platform-arm-14.zip",			"platform-arm-4.zip",		"platform-arm-9.zip",
					"platform-mips-18.zip",			"platform-x86-18.zip",		"toolchain-mips.zip",
					"platform-arm-18.zip",			"platform-arm-5.zip",		"platform-common.zip",
					"platform-mips-9.zip",			"platform-x86-9.zip",		"toolchain-x86.zip" };

			if (getPrefString("use_package_manager").equals("yes")) {
				return null;
			}
			
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String lowercaseName = name.toLowerCase();
					for (String file: oldPackages) {
						if (lowercaseName.equals(file + ".list")) {
							return true;
						}
					}
					return false;
				}
			};
			File dir = new File(toolchainDir + PKGS_LISTS_DIR);
			if (dir.isDirectory()) {
				for (String fileName: dir.list(filter)) {
					try {
						Log.i(TAG, "uninstalling " + dir.getPath() + "/" + fileName);
						FileInputStream fin = new FileInputStream(dir.getPath() + "/" + fileName);
						DataInputStream in = new DataInputStream(fin);
						BufferedReader reader = new BufferedReader(new InputStreamReader(in));
						String line = "";
						while((line = reader.readLine()) != null) {
							Log.i(TAG, "Delete file: " + line);
							(new File(toolchainDir + "/" + line)).delete();
						}
						in.close();
						(new File(dir.getPath() + "/" + fileName)).delete();
					} catch (Exception e) {
						Log.e(TAG, "Error during remove files " + e);
					}
				}
			}
			setPrefString("use_package_manager", "yes");
			return null;
		}
		
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			installToolchainPackage();
		}
    }
    
	private Thread dialogService(final int port) {
		Log.i(TAG, "Launch dialog service (port " + port + ")");
		Thread t = new Thread() {
			public void run() {
				try {
					dialogServerSocket = new ServerSocket();
					dialogServerSocket.setReuseAddress(true);
					dialogServerSocket.bind(new InetSocketAddress(port));
					Log.i(TAG, "Waiting for incoming requests");
					while (true) {
						dialogServiceSocket = dialogServerSocket.accept();
						Log.i(TAG, "Dialog request from " + dialogServiceSocket.getInetAddress().toString());
						if (!dialogServiceSocket.getInetAddress().toString().equals("/127.0.0.1")) {
							continue;
						}
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
	
	private void system(String cmdline) {
		String cctoolsDir = toolchainDir + "/cctools";
		String[] envp = {
				"TMPDIR=" + Environment.getExternalStorageDirectory().getPath(),
				"PATH=" + cctoolsDir + "/bin:" + cctoolsDir + "/sbin:/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin",
				"ANDROID_ASSETS=/system/app",
				"ANDROID_BOOTLOGO=1",				
				"ANDROID_DATA=" + cctoolsDir + "/var/dalvik",
				"ANDROID_PROPERTY_WORKSPACE=" + getEnv(cctoolsDir, "ANDROID_PROPERTY_WORKSPACE"),
				"ANDROID_ROOT=/system",
				"BOOTCLASSPATH=/system/framework/core.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar",
				"CCTOOLSDIR=" + cctoolsDir,
				"CCTOOLSRES=" + getPackageResourcePath(),
				"LD_LIBRARY_PATH=" + cctoolsDir + "/lib",
				"HOME=" + cctoolsDir + "/home",
				"SHELL=" + getShell(),
				"TERM=xterm",
				"PS1=$ ",
				"SDDIR=" + sdCardDir,
				"EXTERNAL_STORAGE=" + Environment.getExternalStorageDirectory().getPath(),
				};
		try {
			Log.i(TAG, "exec cmd " + cmdline + ", cctoolsdir " + cctoolsDir);
			Process p = Runtime.getRuntime().exec(cmdline, envp);
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
	
	private String getEnv(String baseDir, String variable) {
		String ret = null;
		String[] envp = {
				"TMPDIR=" + Environment.getExternalStorageDirectory().getPath(),
				"PATH=" + baseDir + "/bin:" + baseDir + "/sbin:/sbin:/vendor/bin:/system/sbin:/system/bin:/system/xbin",
				"ANDROID_ASSETS=/system/app",
				"ANDROID_BOOTLOGO=1",				
				"ANDROID_DATA=" + baseDir + "/var/dalvik",
				"ANDROID_ROOT=/system",
				"BOOTCLASSPATH=/system/framework/core.jar:/system/framework/ext.jar:/system/framework/framework.jar:/system/framework/android.policy.jar:/system/framework/services.jar",
				"CCTOOLSDIR=" + baseDir,
				"CCTOOLSRES=" + getPackageResourcePath(),
				"LD_LIBRARY_PATH=" + baseDir + "/lib",
				"HOME=" + baseDir + "/home",
				"SHELL=" + getShell(),
				"TERM=xterm",
				"PS1=$ ",
				"SDDIR=" + sdCardDir,
				"EXTERNAL_STORAGE=" + Environment.getExternalStorageDirectory().getPath(),
				};
		String[] argv = { "/system/bin/sh", "-c", "set"};
		int[] pId = new int[1];
		FileDescriptor fd = Utils.createSubProcess(baseDir, argv[0], argv, envp, pId);
		FileInputStream fis = new FileInputStream(fd);
		DataInputStream in = new DataInputStream(fis);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = "";
		try {
			while((line = reader.readLine()) != null) {
				if (line.startsWith(variable + "=")) {
					if (line.indexOf("=") != -1) {
						ret = line.substring(line.indexOf("=") + 1);
						break;
					}
				}
			}
			in.close();
			Utils.waitFor(pId[0]);
		} catch (Exception e) {
			Log.e(TAG, "exception " + e);
		}
		return ret;
	}
	
	private String getShell() {
		String[] shellList = {
				toolchainDir + "/cctools/bin/bash",
				toolchainDir + "/cctools/bin/ash",
		};
		
		for (String shell: shellList) {
			if ((new File(shell)).exists()) {
				return shell;
			}
		}

		return "/system/bin/sh";
	}
}
