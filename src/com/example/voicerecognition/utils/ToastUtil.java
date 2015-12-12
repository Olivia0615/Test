package com.example.voicerecognition.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * @author wangyue
 * */
public class ToastUtil {

	private static Toast mToast = null;

	/**
	 * ·ÀÖ¹Æµ·±µ¯³ötoast
	 * */
	public static void showToast(Context context, String text, int duration) {

		if (context == null || text == null) {
			return;
		}

		if (mToast == null) {
			mToast = Toast.makeText(context, text, duration);
		} else {
			mToast.setText(text);
			mToast.setDuration(duration);
		}

		mToast.show();
	}

}
