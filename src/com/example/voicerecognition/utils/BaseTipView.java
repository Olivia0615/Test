package com.example.voicerecognition.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.voicerecognition.R;

public class BaseTipView extends LinearLayout {
	Context mContext;
	TextView txtContent;
	ImageView imgTip;

	public BaseTipView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		createView(context);

	}

	public BaseTipView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		createView(context);
	}

	private void createView(Context context) {
		mContext = context;
		initLayoutParams();
		addView(getTipImg());
		addView(getTipContent());
	}

	protected void initLayoutParams() {
		AbsListView.LayoutParams lp = new AbsListView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		setLayoutParams(lp);
		setOrientation(LinearLayout.VERTICAL);
		setGravity(Gravity.CENTER_HORIZONTAL);
	}

	protected View getTipImg() {
		ImageView imgTip = new ImageView(mContext);
		LayoutParams lp = new LayoutParams(DensityUtil.dip2px(mContext, 100), DensityUtil.dip2px(mContext, 100));
		lp.setMargins(0, DensityUtil.dip2px(mContext, 80), 0, 0);
		imgTip.setLayoutParams(lp);
		imgTip.setAlpha(0.1f);
		imgTip.setImageResource(R.drawable.mitu_empty);
		this.imgTip = imgTip;
		return imgTip;
	}

	protected View getTipContent() {
		TextView txtContent = new TextView(mContext);
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, DensityUtil.dip2px(mContext, 24));
		lp.setMargins(0, DensityUtil.dip2px(mContext, 20), 0, 0);
		txtContent.setLayoutParams(lp);
		txtContent.setGravity(Gravity.CENTER);
		txtContent.setTextColor(getResources().getColor(R.color.txt_listitem_content));
		txtContent.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
		this.txtContent = txtContent;
		return txtContent;
	}

	public void setTip(String tip) {
		if (txtContent != null) {
			txtContent.setText(tip);
		}
	}

	public void setTipImg(int imgRes) {
		imgTip.setImageResource(imgRes);
	}

	public void setTipImag(Bitmap bitmap) {
		imgTip.setImageBitmap(bitmap);
	}

	public TextView getTipTxt() {
		return txtContent;
	}

	public ImageView getTipImage() {
		return imgTip;
	}
}
