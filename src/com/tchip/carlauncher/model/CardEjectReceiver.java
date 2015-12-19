package com.tchip.carlauncher.model;

import java.io.File;

import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.MyApplication;
import com.tchip.carlauncher.service.SpeakService;
import com.tchip.carlauncher.util.StorageUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class CardEjectReceiver extends BroadcastReceiver {

	private Context context;

	@Override
	public void onReceive(Context context, Intent intent) {
		this.context = context;
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_MEDIA_EJECT)
				|| action.equals(Intent.ACTION_MEDIA_BAD_REMOVAL)
				|| action.equals(Intent.ACTION_MEDIA_UNMOUNTED)) {
			if (!StorageUtil.isVideoCardExists()) {
				MyApplication.isVideoCardEject = true;
			}

			// 规避播放音乐时拔SD,media-server died,从而导致主界面录像预览卡死问题
			// 但会导致播放网络音乐拔SD卡,同样关掉酷我
			context.sendBroadcast(new Intent("com.tchip.KILL_APP").putExtra(
					"value", "music_kuwo"));

		} else if (action.equals(Intent.ACTION_MEDIA_MOUNTED)) {

			// 插入录像卡自动录像
			if ("/storage/sdcard2".equals(intent.getData().getPath())
					&& MyApplication.isAccOn) {
				MyApplication.shouldMountRecord = true;
			}

			if (StorageUtil.isVideoCardExists()) {
				MyApplication.isVideoCardEject = false;

				SharedPreferences sharedPreferences = context
						.getSharedPreferences(Constant.MySP.NAME,
								Context.MODE_PRIVATE);
				Editor editor = sharedPreferences.edit();

				if (sharedPreferences.getBoolean("isFirstLaunch", true)) {

					new Thread(new DeleteVideoDirThread()).start();

					Log.e(Constant.TAG, "Delete video directory:tachograph !!!");

					editor.putBoolean("isFirstLaunch", false);
					editor.commit();
				} else {
					Log.e(Constant.TAG, "App isn't first launch");
				}
			}
		}
	}

	private class DeleteVideoDirThread implements Runnable {

		@Override
		public void run() {
			// 初次启动清空录像文件夹
			String sdcardPath = Constant.Path.SDCARD_2 + File.separator; // "/storage/sdcard2/";
			File file = new File(sdcardPath + "tachograph/");
			StorageUtil.RecursionDeleteFile(file);
		}

	}

	private void startSpeak(String content) {
		Intent intent = new Intent(context, SpeakService.class);
		intent.putExtra("content", content);
		context.startService(intent);
	}

}
