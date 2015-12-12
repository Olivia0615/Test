package com.example.voicerecognition.utils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.voicerecognition.R;

public class BaseLoadingView extends RelativeLayout {

	ImageView img;

	public BaseLoadingView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		createView(context);
	}

	public BaseLoadingView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		createView(context);
	}

	public BaseLoadingView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		createView(context);
	}

	private void createView(Context context) {
		img = new ImageView(context);
		int nSize = DensityUtil.dip2px(context, 28);
		LayoutParams lp = new LayoutParams(nSize, nSize);
		lp.addRule(RelativeLayout.CENTER_IN_PARENT, TRUE);
		img.setLayoutParams(lp);
		img.setImageResource(R.drawable.progressbar_indeterminate_circle_light);
		img.startAnimation(AnimationUtils.loadAnimation(context, R.anim.roate_anim));
		addView(img);
	}

}
