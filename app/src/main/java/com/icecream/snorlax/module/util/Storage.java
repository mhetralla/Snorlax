package com.icecream.snorlax.module.util;

import java.io.File;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Environment;
import android.support.v4.content.ContextCompat;

import com.icecream.snorlax.R;

public class Storage {
	private static File publicDirectory;

	public static File getPublicDirectory(Resources resources) {
		if (publicDirectory == null) {
			synchronized (Storage.class) {
				if (publicDirectory == null) {
					publicDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), resources.getString(R.string.app_name));
				}
			}
		}

		return publicDirectory;
	}

	/* Checks if external storage is available for read and write */
	public static boolean isExternalStorageWritable(final Context context) {
		final String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			Log.d("External storage not writable");
			return false;
		}

		final int writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (writePermission != PackageManager.PERMISSION_GRANTED) {
			Log.d("Write external storage permission : " + writePermission);
			return false;
		}

		return true;
	}

	/* Checks if external storage is available to at least read */
	public static boolean isExternalStorageReadable(final Context context) {
		final String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state) && !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			Log.d("External storage not readable");
			return false;
		}

		final int readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
		if (readPermission != PackageManager.PERMISSION_GRANTED) {
			Log.d("Read external storage permission : " + readPermission);
			return false;
		}

		return true;
	}
}
