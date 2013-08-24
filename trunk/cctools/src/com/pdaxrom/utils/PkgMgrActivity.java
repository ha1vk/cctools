package com.pdaxrom.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.pdaxrom.cctools.CCToolsActivity;
import com.pdaxrom.cctools.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class PkgMgrActivity extends ListActivity {
	private static final String TAG = "PkgMgrActivity";
	private static final String URL = "http://cctools.info/repo-4.8/" + Build.CPU_ABI;
	private Context context = this;
	private static final String PKGS_LISTS_DIR = "/installed/";
	
	private String xmlRepo;
	
	private static final int ACTIVITY_PKGCTL = 1;
	
    // XML node keys
    static final String KEY_PACKAGE		= "package"; // parent node
    static final String KEY_NAME		= "name";
    static final String KEY_VERSION		= "version";
    static final String KEY_DESC		= "description";
    static final String KEY_SIZE		= "size";
    static final String KEY_FILE		= "file";
    static final String KEY_STATUS		= "status";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pkgmgr_main);

        (new DownloadXmlTask()).execute(URL);
         
        // selecting single ListView item
        ListView lv = getListView();
        // listening to single listitem click
        lv.setOnItemClickListener(new OnItemClickListener() { 
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                // getting values from selected ListItem
                final String name = ((TextView) view.findViewById(R.id.pkg_name)).getText().toString();
                final String version = ((TextView) view.findViewById(R.id.pkg_version)).getText().toString();
                //String description = ((TextView) view.findViewById(R.id.pkg_desciption)).getText().toString();
                final String file = ((TextView) view.findViewById(R.id.pkg_file)).getText().toString();
                //String size = ((TextView) view.findViewById(R.id.pkg_size)).getText().toString();

            	Builder dialog = new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(getString(R.string.pkgmgr_name))
                .setMessage(getString(R.string.pkg_selected) + name)
                .setNeutralButton(getString(R.string.cancel), null);

            	String toolchainDir = getCacheDir().getParentFile().getAbsolutePath() + "/root";
            	String logFile = toolchainDir + PKGS_LISTS_DIR + name + ".list";
            	
            	if ((new File(logFile)).exists()) {
                	dialog.setPositiveButton(getString(R.string.pkg_reinstall), new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int which) {
                    		Intent intent = new Intent(getApplicationContext(), CCToolsActivity.class);
                    		intent.putExtra("installPackage", name);
                    		intent.putExtra("installPackageVersion", version);
                    		intent.putExtra("installPackageFile", file);
                    		intent.putExtra("installPackageUrl", URL);
                    		startActivityForResult(intent, ACTIVITY_PKGCTL);
                    	}
                    });
                	dialog.setNegativeButton(getString(R.string.pkg_uninstall), new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int which) {
                    		Intent intent = new Intent(getApplicationContext(), CCToolsActivity.class);
                    		intent.putExtra("uninstallPackage", name);
                    		intent.putExtra("uninstallPackageVers", version);
                    		intent.putExtra("uninstallPackageFile", file);
                    		intent.putExtra("uninstallPackageUrl", URL);
                    		startActivityForResult(intent, ACTIVITY_PKGCTL);
                    	}
                    });
            	} else {
                	dialog.setPositiveButton(getString(R.string.pkg_install), new DialogInterface.OnClickListener() {
                    	public void onClick(DialogInterface dialog, int which) {
                    		Intent intent = new Intent(getApplicationContext(), CCToolsActivity.class);
                    		intent.putExtra("installPackage", name);
                    		intent.putExtra("installPackageVersion", version);
                    		intent.putExtra("installPackageFile", file);
                    		intent.putExtra("installPackageUrl", URL);
                    		startActivityForResult(intent, ACTIVITY_PKGCTL);
                    	}
                    });
            	}

            	dialog.show();    	                
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == ACTIVITY_PKGCTL) {
    		Log.i(TAG, "install/uninstall finished");
    		if (xmlRepo != null) {
    			showPackages(xmlRepo);
    		}
    	}
    }
    
    void showPackages(String xml) {
        ArrayList<HashMap<String, String>> menuItems = new ArrayList<HashMap<String, String>>();
		XMLParser parser = new XMLParser();

        Document doc = parser.getDomElement(xml); // getting DOM element
        
        NodeList nl = doc.getElementsByTagName(KEY_PACKAGE);
        // looping through all item nodes <item>
        for (int i = 0; i < nl.getLength(); i++) {
            // creating new HashMap
            HashMap<String, String> map = new HashMap<String, String>();
            Element e = (Element) nl.item(i);
            // adding each child node to HashMap key => value
            map.put(KEY_NAME, parser.getValue(e, KEY_NAME));
            map.put(KEY_VERSION, parser.getValue(e, KEY_VERSION));
            map.put(KEY_DESC, parser.getValue(e, KEY_DESC));
            map.put(KEY_SIZE, parser.getValue(e, KEY_SIZE));
            map.put(KEY_FILE, parser.getValue(e, KEY_FILE));

            String toolchainDir = getCacheDir().getParentFile().getAbsolutePath() + "/root";
        	String logFile = toolchainDir + PKGS_LISTS_DIR + parser.getValue(e, KEY_NAME) + ".list";
        	
        	if ((new File(logFile)).exists()) {
        		map.put(KEY_STATUS, getString(R.string.pkg_installed));
        	}else {
        		map.put(KEY_STATUS, getString(R.string.pkg_notinstalled));        		
        	}

            // adding HashList to ArrayList
            menuItems.add(map);
        }
 
        // Adding menuItems to ListView
        ListAdapter adapter = new SimpleAdapter(this, menuItems,
        										R.layout.pkgmgr_list_package,
        										new String[] { KEY_NAME, KEY_VERSION, KEY_DESC, KEY_FILE, KEY_SIZE, KEY_STATUS },
        										new int[] {	R.id.pkg_name, R.id.pkg_version, R.id.pkg_desciption, R.id.pkg_file, R.id.pkg_size, R.id.pkg_status });
 
        setListAdapter(adapter);    	
    }
    
    private class DownloadXmlTask extends AsyncTask<String, Void, String> {
		protected String doInBackground(String... arg0) {
	        Log.i(TAG, "Repo URL: " + arg0[0] + "/Packages");
			XMLParser parser = new XMLParser();
			String xml = parser.getXmlFromUrl(arg0[0] + "/Packages");
			return xml;
		}
		
		protected void onPostExecute(String result) {
	        Log.i(TAG, "Downloaded: " + result);
	        if (result != null) {
	        	xmlRepo = result;
	        	showPackages(result);
	        }
		}

    }
    
}