package com.pdaxrom.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.pdaxrom.cctools.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

/**
 * Activity para escolha de arquivos/diretorios.
 * 
 * @author android
 * 
 */
public class FileDialog extends SherlockListActivity {
	/**
	 * Chave de um item da lista de paths.
	 */
	private static final String ITEM_KEY = "key";

	/**
	 * Imagem de um item da lista de paths (diretorio ou arquivo).
	 */
	private static final String ITEM_IMAGE = "image";

	/**
	 * Diretorio raiz.
	 */
	private static final String ROOT = "/";

	/**
	 * Parametro de entrada da Activity: path inicial. Padrao: ROOT.
	 */
	public static final String START_PATH = "START_PATH";

	/**
	 * Parametro de entrada da Activity: filtro de formatos de arquivos. Padrao:
	 * null.
	 */
	public static final String FORMAT_FILTER = "FORMAT_FILTER";

	/**
	 * Parametro de saida da Activity: path escolhido. Padrao: null.
	 */
	public static final String RESULT_PATH = "RESULT_PATH";

	/**
	 * Parametro de entrada da Activity: tipo de selecao: pode criar novos paths
	 * ou nao. Padrao: nao permite.
	 * 
	 * @see {@link SelectionMode}
	 */
	public static final String SELECTION_MODE = "SELECTION_MODE";

	/**
	 * Parametro de entrada da Activity: se e permitido escolher diretorios.
	 * Padrao: falso.
	 */
	public static final String CAN_SELECT_DIR = "CAN_SELECT_DIR";

	private List<String> path = null;
	private TextView myPath;
	private EditText mFileName;
	private ArrayList<HashMap<String, Object>> mList;

	private Button selectButton;

	private LinearLayout layoutSelect;
	private LinearLayout layoutCreate;
	private InputMethodManager inputManager;
	private String parentPath;
	private String currentPath = ROOT;

	private int selectionMode = SelectionMode.MODE_CREATE;

	private String[] formatFilter = null;

	private boolean canSelectDir = false;

	private File selectedFile;
	private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

	private Context context = this;
	
	private String homeDirectory = "";
	private String sdDirectory = "";
	
	/**
	 * Called when the activity is first created. Configura todos os parametros
	 * de entrada e das VIEWS..
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED, getIntent());

		setContentView(R.layout.file_dialog_main);

		myPath = (TextView) findViewById(R.id.path);
		myPath.setSelected(true);
		//myPath.setEllipsize(TruncateAt.MARQUEE);
		//myPath.setSingleLine(true);
		
		mFileName = (EditText) findViewById(R.id.fdEditTextFile);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		}
		
		inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		selectButton = (Button) findViewById(R.id.fdButtonSelect);
		selectButton.setEnabled(false);
		selectButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (selectedFile != null) {
					getIntent().putExtra(RESULT_PATH, selectedFile.getPath());
					setResult(RESULT_OK, getIntent());
					finish();
				}
			}
		});

		selectionMode = getIntent().getIntExtra(SELECTION_MODE, SelectionMode.MODE_CREATE);

		formatFilter = getIntent().getStringArrayExtra(FORMAT_FILTER);

		canSelectDir = getIntent().getBooleanExtra(CAN_SELECT_DIR, false);

		layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);
		layoutCreate = (LinearLayout) findViewById(R.id.fdLinearLayoutCreate);

		if (selectionMode == SelectionMode.MODE_OPEN) {
			setTitle(getString(R.string.open_file));
			layoutCreate.setVisibility(View.GONE);
		} else {
			setTitle(getString(R.string.save_file));
			layoutSelect.setVisibility(View.GONE);
		}

		final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}

		});

		final Button cancelButton1 = (Button) findViewById(R.id.fdButtonCancel1);
		cancelButton1.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				finish();
			}

		});

		
		final Button createButton = (Button) findViewById(R.id.fdButtonCreate);
		createButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (mFileName.getText().length() > 0) {
					getIntent().putExtra(RESULT_PATH, currentPath + "/" + mFileName.getText());
					setResult(RESULT_OK, getIntent());
					finish();
				}
			}
		});

		String startPath = getIntent().getStringExtra(START_PATH);
		startPath = startPath != null ? startPath : ROOT;
		if (canSelectDir) {
			File file = new File(startPath);
			selectedFile = file;
			selectButton.setEnabled(true);
		}
		
		homeDirectory = getCacheDir().getParentFile().getAbsolutePath() + "/root/cctools/home";
		sdDirectory = Environment.getExternalStorageDirectory().getPath();
		
		getDir(startPath);
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, R.id.home_folder, 0, getString(R.string.homeDirectory))
    		.setIcon(R.drawable.folder_home)
    		.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

    	menu.add(0, R.id.sd_folder, 0, getString(R.string.sdDirectory))
			.setIcon(R.drawable.media_memory_sd)
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    			
    	menu.add(0, R.id.new_folder, 0, getString(R.string.newDirectory))
    		.setIcon(R.drawable.folder_new)
    		.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.home_folder:
        	if (currentPath.startsWith(Environment.getExternalStorageDirectory().getPath())) {
        		sdDirectory = currentPath;
        	}
        	if (!currentPath.startsWith(getCacheDir().getParentFile().getAbsolutePath())) {
        		getDir(homeDirectory);
        	}
        	break;
        case R.id.sd_folder:
        	if (currentPath.startsWith(getCacheDir().getParentFile().getAbsolutePath())) {
        		homeDirectory = currentPath;
        	}
        	if (!currentPath.startsWith(Environment.getExternalStorageDirectory().getPath())) {
            	getDir(sdDirectory);
        	}
        	break;
        case R.id.new_folder:
        	newDir();
        	break;
        }
        return true;
    }

    private void newDir() {
		final EditText input = new EditText(context);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		input.setSingleLine(true);
		new AlertDialog.Builder(context)
			.setMessage(getString(R.string.create_new))
			.setView(input)
		    .setPositiveButton(getString(R.string.button_continue), new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		        	if ((new File(currentPath + "/" + input.getText().toString())).mkdirs() == false) {
		    	    	new AlertDialog.Builder(context)
		    	    	.setIcon(android.R.drawable.ic_dialog_alert)
		    	    	.setTitle(R.string.newDirectory)
		    	    	.setMessage(getString(R.string.cannot_create))
		    	    	.show();
		        	} else
		        		getDir(currentPath + "/" + input.getText().toString());
		        }
		    })
		    .setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					
				}
			}).show();
    }
    
	private void getDir(String dirPath) {

		boolean useAutoSelection = dirPath.length() < currentPath.length();

		Integer position = lastPositions.get(parentPath);

		getDirImpl(dirPath);

		if (position != null && useAutoSelection) {
			getListView().setSelection(position);
		}

	}

	/**
	 * Monta a estrutura de arquivos e diretorios filhos do diretorio fornecido.
	 * 
	 * @param dirPath
	 *            Diretorio pai.
	 */
	private void getDirImpl(final String dirPath) {

		currentPath = dirPath;

		final List<String> item = new ArrayList<String>();
		path = new ArrayList<String>();
		mList = new ArrayList<HashMap<String, Object>>();

		File f = new File(currentPath);
		File[] files = f.listFiles();
		if (files == null) {
			currentPath = ROOT;
			f = new File(currentPath);
			files = f.listFiles();
		}
		myPath.setText(/*getText(R.string.location) + ": " + */currentPath);

		if (!currentPath.equals(ROOT)) {

			item.add(ROOT);
			addItem(ROOT, R.drawable.folder);
			path.add(ROOT);

			item.add("../");
			addItem("..", R.drawable.folder);
			path.add(f.getParent());
			parentPath = f.getParent();

		}

		TreeMap<String, String> dirsMap = new TreeMap<String, String>();
		TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
		TreeMap<String, String> filesMap = new TreeMap<String, String>();
		TreeMap<String, String> filesPathMap = new TreeMap<String, String>();
		for (File file : files) {
			if (file.isDirectory()) {
				String dirName = file.getName();
				dirsMap.put(dirName, dirName);
				dirsPathMap.put(dirName, file.getPath());
			} else {
				final String fileName = file.getName();
				final String fileNameLwr = fileName.toLowerCase();
				// se ha um filtro de formatos, utiliza-o
				if (formatFilter != null) {
					boolean contains = false;
					for (int i = 0; i < formatFilter.length; i++) {
						final String formatLwr = formatFilter[i].toLowerCase();
						if (fileNameLwr.endsWith(formatLwr)) {
							contains = true;
							break;
						}
					}
					if (contains) {
						filesMap.put(fileName, fileName);
						filesPathMap.put(fileName, file.getPath());
					}
					// senao, adiciona todos os arquivos
				} else {
					filesMap.put(fileName, fileName);
					filesPathMap.put(fileName, file.getPath());
				}
			}
		}
		item.addAll(dirsMap.tailMap("").values());
		item.addAll(filesMap.tailMap("").values());
		path.addAll(dirsPathMap.tailMap("").values());
		path.addAll(filesPathMap.tailMap("").values());

		SimpleAdapter fileList = new SimpleAdapter(this, mList, R.layout.file_dialog_row, new String[] {
				ITEM_KEY, ITEM_IMAGE }, new int[] { R.id.fdrowtext, R.id.fdrowimage });

		for (String dir : dirsMap.tailMap("").values()) {
			addItem(dir, R.drawable.folder);
		}

		for (String file : filesMap.tailMap("").values()) {
			addItem(file, getFileIcon(file));
		}

		fileList.notifyDataSetChanged();

		setListAdapter(fileList);

	}

	private void addItem(String fileName, int imageId) {
		HashMap<String, Object> item = new HashMap<String, Object>();
		item.put(ITEM_KEY, fileName);
		item.put(ITEM_IMAGE, imageId);
		mList.add(item);
	}

	/**
	 * Quando clica no item da lista, deve-se: 1) Se for diretorio, abre seus
	 * arquivos filhos; 2) Se puder escolher diretorio, define-o como sendo o
	 * path escolhido. 3) Se for arquivo, define-o como path escolhido. 4) Ativa
	 * botao de selecao.
	 */
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		File file = new File(path.get(position));

		hideKeyboard(v);

		if (file.isDirectory()) {
			selectButton.setEnabled(false);
			if (file.canRead()) {
				lastPositions.put(currentPath, position);
				getDir(path.get(position));
				if (canSelectDir) {
					selectedFile = file;
					v.setSelected(true);
					selectButton.setEnabled(true);
				}
			} else {
				new AlertDialog.Builder(this).setIcon(R.drawable.ic_launcher)
						.setTitle("[" + file.getName() + "] " + getText(R.string.cant_read_folder))
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {

							public void onClick(DialogInterface dialog, int which) {

							}
						}).show();
			}
		} else {
			selectedFile = file;
			mFileName.setText(file.getName());
			v.setSelected(true);
			selectButton.setEnabled(true);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			selectButton.setEnabled(false);

			if (!currentPath.equals(ROOT)) {
				getDir(parentPath);
			} else {
				return super.onKeyDown(keyCode, event);
			}

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	/**
	 * Define se o botao de SELECT e visivel.
	 * 
	 * @param v
	 */
	private void hideKeyboard(View v) {
		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
	}
	
	private int getFileIcon(String file) {
		String ext = "";
		int dot = file.lastIndexOf(".");
		if (dot != -1)
			ext = file.substring(dot);
		if (ext.contentEquals(".c"))
			return R.drawable.text_x_c;
		if (ext.contentEquals(".cpp") || ext.contentEquals(".c++"))
			return R.drawable.text_x_cpp;
		if (ext.contentEquals(".h") || ext.contentEquals(".hpp"))
			return R.drawable.text_x_h;
		if (ext.contentEquals(".sh"))
			return R.drawable.text_x_script;
		if (ext.contentEquals(".mk") || ext.contentEquals(".mak") || file.contentEquals("Makefile") || file.contentEquals("makefile"))
			return R.drawable.text_x_make;
		return R.drawable.application_octet_stream;
	}
}
