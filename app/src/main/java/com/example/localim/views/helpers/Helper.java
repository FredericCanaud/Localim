package com.example.localim.views.helpers;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import com.example.localim.R;

import java.util.Random;

public class Helper {

	/////////////////////////////////    PROGRESS DIALOG     ////////////////////////////////////////////

	public static ProgressDialog mProgressDialog;

	public static String getPath(Context context, Uri uri) {
		String[] projection = {MediaStore.Images.Media.DATA};
		Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
		int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
		cursor.moveToFirst();
		String path = cursor.getString(column_index);
		cursor.close();
		return path;
	}

	public static void initProgressDialog(Context context) {
		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setMessage(context.getString(R.string.loading));
		mProgressDialog.setCancelable(false);
		mProgressDialog.setMax(100);
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	}

	public static void setProgress(int i) {
		mProgressDialog.setProgress(i);
	}

	public static void dismissProgressDialog() {
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
	}

	/////////////////////////////////    BITMAP     ////////////////////////////////////////////

	public static Bitmap cropBitmap(Bitmap bitmap){
		Bitmap dstBmp;
		if (bitmap.getWidth() >= bitmap.getHeight()){
			dstBmp = Bitmap.createBitmap(bitmap,bitmap.getWidth()/2 - bitmap.getHeight()/2,0,bitmap.getHeight(),bitmap.getHeight());
		}else{
			dstBmp = Bitmap.createBitmap(bitmap,0,bitmap.getHeight()/2 - bitmap.getWidth()/2,bitmap.getWidth(), bitmap.getWidth());
		}
		return dstBmp;
	}

	/////////////////////////////////    STRING     ////////////////////////////////////////////

	public static String randomString(){
		int leftLimit = 97;
		int rightLimit = 122;
		int targetStringLength = 13;
		return new Random().ints(leftLimit, rightLimit + 1) .limit(targetStringLength).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}
}