package com.pdaxrom.term;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.pdaxrom.utils.Utils;

import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.emulatorview.UpdateCallback;

public class ShellTermSession extends TermSession {
	private final static String TAG = "ShellTermSession";
	private int mProcId;
	private FileDescriptor mFd;
    private Thread mWatcherThread;

	private static final int PROCESS_EXITED = 1;

/*
	private Handler mMsgHandler = new Handler() {
	    @Override
	    public void handleMessage(Message msg) {
	    	if (!isRunning()) {
	    		return;
	    	}
	    	if (msg.what == PROCESS_EXITED) {
	    		onProcessExit((Integer) msg.obj);
	    	}
	    }
	};
 */
	private Handler mMsgHandler;
	
	private UpdateCallback mUTF8ModeNotify = new UpdateCallback() {
		public void onUpdate() {
			Utils.setPtyUTF8Mode(mFd, getUTF8Mode());
		}
	};
	
	public ShellTermSession(String[] argv, String envp[], String cwd, Handler handler) {
		super();
		
		mMsgHandler = handler;
		
		setDefaultUTF8Mode(true);
		
		createSubProcess(argv, envp, cwd);
		
        mWatcherThread = new Thread() {
            @Override
            public void run() {
               Log.i(TAG, "waiting for: " + mProcId);
               int result = Utils.waitFor(mProcId);
               Log.i(TAG, "Subprocess exited: " + result);
               //mMsgHandler.sendMessage(mMsgHandler.obtainMessage(PROCESS_EXITED, result));
               mMsgHandler.sendEmptyMessage(123);
            }
       };
       mWatcherThread.setName("Process watcher");
	}
	
	private void onProcessExit(int result) {
//		finish();
	}

	@Override
	public void finish() {
		Log.i(TAG, "finish()");
	    Utils.hangupProcessGroup(mProcId);
	    Utils.close(mFd);
	    super.finish();
	}

	public void hangup() {
	    Utils.hangupProcessGroup(mProcId);
	}
	
	public int getPid()
	{
		return mProcId;
	}
	
	private void createSubProcess(String[] argv, String[] envp, String cwd) {
		int[] pId = new int[1];
		mFd = Utils.createSubProcess(cwd, argv[0], argv, envp, pId);
		mProcId = pId[0];
		if (mProcId > 0) {
			setTermIn(new FileInputStream(mFd));
			setTermOut(new FileOutputStream(mFd));
		}
	}
	
	@Override
	public void initializeEmulator(int columns, int rows) {
		super.initializeEmulator(columns, rows);
		
		Utils.setPtyUTF8Mode(mFd, getUTF8Mode());
		setUTF8ModeUpdateCallback(mUTF8ModeNotify);
		
        mWatcherThread.start();
	}
	
	@Override
	public void updateSize(int columns, int rows) {
		Utils.setPtyWindowSize(mFd, rows, columns, 0, 0);
		super.updateSize(columns, rows);
	}
}
