package com.example.voicerecognition.utils;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.View;

public class AnimUtils {

	public static void animVoiceBtnScale(View view) {
		if (view == null) {
			return;
		}
		final ObjectAnimator scale1X = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.4f);
		final ObjectAnimator scale1Y = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.4f);
		
		final ObjectAnimator scale2X = ObjectAnimator.ofFloat(view, "scaleX", 1.4f, 2.2f);
		final ObjectAnimator scale2Y = ObjectAnimator.ofFloat(view, "scaleY", 1.4f, 2.2f);
		
		final ObjectAnimator scale3X = ObjectAnimator.ofFloat(view, "scaleX", 2.2f, 1f);
		final ObjectAnimator scale3Y = ObjectAnimator.ofFloat(view, "scaleY", 2.2f, 1f);

		AnimatorSet set = new AnimatorSet();
		set.play(scale1X).with(scale1Y);
		set.play(scale2X).after(scale1X);
		set.play(scale2Y).after(scale1Y);
		set.play(scale3X).after(scale2X);
		set.play(scale3Y).after(scale2Y);
		set.setDuration(200);
		set.start();
	}

}
