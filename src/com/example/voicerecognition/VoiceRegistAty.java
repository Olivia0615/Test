package com.example.voicerecognition;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.voicerecognition.utils.AccountInfo;
import com.example.voicerecognition.utils.AnimUtils;
import com.example.voicerecognition.utils.AudioRecordUtil;
import com.example.voicerecognition.utils.BaseLoadingView;
import com.example.voicerecognition.utils.DConfig;
import com.example.voicerecognition.utils.ErrorCode;
import com.example.voicerecognition.utils.HciCloudFuncHelper;
import com.example.voicerecognition.utils.ToastUtil;
import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.api.vpr.HciCloudVpr;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.InitParam;
import com.sinovoice.hcicloudsdk.common.Session;
import com.sinovoice.hcicloudsdk.common.vpr.VprConfig;
import com.sinovoice.hcicloudsdk.common.vpr.VprInitParam;

public class VoiceRegistAty extends Activity {

	/** 加载用户信息工具类 */
	private AccountInfo mAccountInfo;
	private final static int FLAG_WAV = 0;
	private int mState = -1; // -1:没再录制，0：录制wav
	private UIHandler uiHandler;
	private UIThread uiThread;
	private boolean isRegisted = false;
	public static final String REGIST_VPR_KEY = "vpr_regist";

	private Button mAudioRegistBtn;
	private Button mAudioVerifyBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addStep1Layout();
	}

	// 第一步,初始化
	private void addStep1Layout() {
		setContentView(R.layout.voice_step_1_init);
		TextView cancelTxt = (TextView) findViewById(R.id.voice_step1_cancel_txt);
		cancelTxt.setOnClickListener(cancelClickListener);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				init();
			}
		}, 1000);
	}

	private TextView mStep2SayTxt;
	private TextView mStep2ErrorTxt;
	private BaseLoadingView mStep2Loading;
	private ImageView mStep2Tip;
	private RelativeLayout mStep2SayHalo;

	// 第二步，注册声音
	private void addStep2Layout() {
		setContentView(R.layout.voice_step_2_say);
		TextView cancelTxt = (TextView) findViewById(R.id.voice_step2_cancel_txt);
		mStep2SayTxt = (TextView) findViewById(R.id.voice_step2_say_txt);
		mStep2ErrorTxt = (TextView) findViewById(R.id.voice_step2_error_txt);
		mStep2Loading = (BaseLoadingView) findViewById(R.id.voice_step2_loading);
		mStep2Tip = (ImageView) findViewById(R.id.voice_step2_tip);
		mStep2SayHalo = (RelativeLayout) findViewById(R.id.voice_step2_say_btn_halo);
		mAudioRegistBtn = (Button) findViewById(R.id.voice_step2_say_btn);
		cancelTxt.setOnClickListener(cancelClickListener);
		mAudioRegistBtn.setOnTouchListener(audioClickListener);
	}

	// 第三步，下一步提示
	private void addStep3Layout() {
		setContentView(R.layout.voice_step_3_next);
		TextView cancelTxt = (TextView) findViewById(R.id.voice_step3_cancel_txt);
		Button nextBtn = (Button) findViewById(R.id.voice_step3_next_btn);
		cancelTxt.setOnClickListener(cancelClickListener);
		nextBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				addStep4Layout();
			}
		});
	}

	// 第四步，验证声音(和第二步页面相同，只是录音后不是注册而是验证)
	private void addStep4Layout() {
		setContentView(R.layout.voice_step_2_say);
		TextView cancelTxt = (TextView) findViewById(R.id.voice_step2_cancel_txt);
		mStep2SayTxt = (TextView) findViewById(R.id.voice_step2_say_txt);
		mStep2ErrorTxt = (TextView) findViewById(R.id.voice_step2_error_txt);
		mStep2Loading = (BaseLoadingView) findViewById(R.id.voice_step2_loading);
		mStep2Tip = (ImageView) findViewById(R.id.voice_step2_tip);
		mStep2SayHalo = (RelativeLayout) findViewById(R.id.voice_step2_say_btn_halo);
		mAudioVerifyBtn = (Button) findViewById(R.id.voice_step2_say_btn);
		cancelTxt.setOnClickListener(cancelClickListener);
		mAudioVerifyBtn.setOnTouchListener(audioClickListener);
		mStep2Tip.setVisibility(View.INVISIBLE);
	}

	// 第五步，注册完毕
	private void addStep5Layout() {
		setContentView(R.layout.voice_step_4_finsh);
	}

	private void init() {
		initAudio();
		initVpr();
	}

	private void initAudio() {
		uiHandler = new UIHandler();
	}

	/**
	 * 开始录音
	 * 
	 * @param mFlag
	 *            ，0：录制wav格式，1：录音amr格式
	 */
	private void record(int mFlag) {
		if (mState != -1) {
			Message msg = new Message();
			Bundle b = new Bundle();// 存放数据
			b.putInt("cmd", CMD_RECORDFAIL);
			b.putInt("msg", ErrorCode.E_STATE_RECODING);
			msg.setData(b);

			uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
			return;
		}
		int mResult = -1;
		switch (mFlag) {
		case FLAG_WAV:
			AudioRecordUtil mRecord_1 = AudioRecordUtil.getInstance();
			mResult = mRecord_1.startRecordAndFile();
			break;
		}
		if (mResult == ErrorCode.SUCCESS) {
			uiThread = new UIThread();
			new Thread(uiThread).start();
			mState = mFlag;
		} else {
			Message msg = new Message();
			Bundle b = new Bundle();// 存放数据
			b.putInt("cmd", CMD_RECORDFAIL);
			b.putInt("msg", mResult);
			msg.setData(b);

			uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
		}
	}

	/**
	 * 停止录音
	 */
	private void stopRecord() {
		if (mState != -1) {
			switch (mState) {
			case FLAG_WAV:
				AudioRecordUtil mRecord_1 = AudioRecordUtil.getInstance();
				mRecord_1.stopRecordAndFile();
				break;
			}
			if (uiThread != null) {
				uiThread.stopThread();
			}
			if (uiHandler != null)
				uiHandler.removeCallbacks(uiThread);

			mState = -1;
		}
	}

	private final static int CMD_RECORDING_TIME = 2000;
	private final static int CMD_RECORDFAIL = 2001;
	private final static int CMD_STOP = 2002;

	private int recordTime;

	class UIHandler extends Handler {
		public UIHandler() {
		}

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			Bundle b = msg.getData();
			int vCmd = b.getInt("cmd");
			switch (vCmd) {
			case CMD_RECORDING_TIME:
				recordTime = b.getInt("msg");
				// record_txt.setText("正在录音中，已录制：" + vTime + " s");
				break;
			case CMD_RECORDFAIL:
				int vErrorCode = b.getInt("msg");
				String vMsg = ErrorCode.getErrorInfo(VoiceRegistAty.this, vErrorCode);
				// record_txt.setText("录音失败：" + vMsg);
				setLoading(false);
				break;
			default:
				break;
			}
		}
	};

	class UIThread implements Runnable {
		int mTimeMill = 0;
		boolean vRun = true;

		public void stopThread() {
			vRun = false;
		}

		public void run() {
			while (vRun) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				mTimeMill++;
				Log.d("thread", "mThread........" + mTimeMill);
				Message msg = new Message();
				Bundle b = new Bundle();// 存放数据
				b.putInt("cmd", CMD_RECORDING_TIME);
				b.putInt("msg", mTimeMill);
				msg.setData(b);

				VoiceRegistAty.this.uiHandler.sendMessage(msg); // 向Handler发送消息,更新UI
			}

		}
	}

	private void initVpr() {

		mAccountInfo = AccountInfo.getInstance();
		boolean loadResult = mAccountInfo.loadAccountInfo(this);
		if (loadResult) {
		} else {
			ToastUtil.showToast(this, "加载灵云账号失败！请在assets/AccountInfo.txt文件中填写正确的灵云账户信息，账户需要从www.hcicloud.com开发者社区上注册申请。", Toast.LENGTH_SHORT);
			return;
		}

		// 加载信息,返回InitParam, 获得配置参数的字符串
		InitParam initParam = HciCloudFuncHelper.getInitParam(this);
		String strConfig = initParam.getStringConfig();

		// 初始化
		int errCode = HciCloudSys.hciInit(strConfig, this);
		if (errCode != HciErrorCode.HCI_ERR_NONE && errCode != HciErrorCode.HCI_ERR_SYS_ALREADY_INIT) {
			ToastUtil.showToast(this, "\nhciInit error: " + HciCloudSys.hciGetErrorInfo(errCode), Toast.LENGTH_SHORT);
			Log.i("aaaaaaaaaaaaaaaaaaaaaa", "错误码"+ errCode);
			return;
		} else {
//			ToastUtil.showToast(this, "\nhciInit error: " + HciCloudSys.hciGetErrorInfo(errCode), Toast.LENGTH_SHORT);
			Log.i("aaaaaaaaaaaaaaaaaaaaaa", "错误码"+ errCode);
		}

		// 获取授权/更新授权文件 :
		errCode = HciCloudFuncHelper.checkAuthAndUpdateAuth();
		if (errCode != HciErrorCode.HCI_ERR_NONE) {
			// 由于系统已经初始化成功,在结束前需要调用方法hciRelease()进行系统的反初始化
			 HciCloudSys.hciRelease();
			return;
		}else{
			addStep2Layout();
		}
		// HciCloudFuncHelper.Func(this, mAccountInfo.getCapKey(), mLogView);
		
		
		// 初始化VPR
		// 构造VPR初始化的帮助类的实例
		VprInitParam vprInitParam = new VprInitParam();
		// 获取App应用中的lib的路径,放置能力所需资源文件。如果使用/data/data/packagename/lib目录,需要添加android_so的标记
//		String dataPath = context.getFilesDir().getAbsolutePath().replace("files", "lib");
//		initParam.addParam(VprInitParam.PARAM_KEY_DATA_PATH, dataPath);
		initParam.addParam(VprInitParam.PARAM_KEY_FILE_FLAG, VprInitParam.VALUE_OF_PARAM_FILE_FLAG_ANDROID_SO);
		initParam.addParam(VprInitParam.PARAM_KEY_INIT_CAP_KEYS, "vpr.cloud.verify");
//		ShowMessage("HciVprInit config :" + initParam.getStringConfig());
		int vprErrCode = HciCloudVpr.hciVprInit(initParam.getStringConfig());
		if (vprErrCode != HciErrorCode.HCI_ERR_NONE) {
//			ShowMessage("HciVprInit error:"	+ HciCloudSys.hciGetErrorInfo(errCode));
			Log.i("aaaaaaaaaaaaaaaaaaaa", "错误码："+vprErrCode);
			return;
		} else {
			Log.i("aaaaaaaaaaaaaaaaaaa", "HciVprInit Success");
		}
		
		
	}

	/** 注册声音 */
	private void registVoice() {
		String capKey = mAccountInfo.getCapKey();
		final StringBuffer userId = new StringBuffer("wangyue8");

		boolean enrollResult = false;
		VprConfig enrollConfig = new VprConfig();
		enrollConfig.addParam(VprConfig.UserConfig.PARAM_KEY_USER_ID, "wangyue8");
		enrollConfig.addParam(VprConfig.AudioConfig.PARAM_KEY_AUDIO_FORMAT, VprConfig.AudioConfig.VALUE_OF_PARAM_AUDIO_FORMAT_PCM_16K16BIT);
		
//		VprConfig sessionConfig = new VprConfig();
//		sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_CAP_KEY,
//				"vpr.cloud.verify");
//		Session session = new Session();
//		int errCode = HciCloudVpr.hciVprSessionStart(sessionConfig.getStringConfig(), session);
//		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			Log.i("aaaaaaaaaaaaaaaaaaaa", "错误码："+errCode);
//			return ;
//		}
		
		enrollResult = HciCloudFuncHelper.Enroll(capKey, enrollConfig);
		
		
		if (enrollResult) {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					isRegisted = true;
					setLoading(false);
					addStep3Layout();
					// Toast.makeText(getApplicationContext(), "注册成功　userid:" +
					// userId.toString(), 1).show();
				}
			});
			AudioRecordUtil.getInstance().removePcmFile();
			DConfig.Preference.setStringPref(this, REGIST_VPR_KEY, userId.toString());
		} else {
			uiHandler.post(new Runnable() {
				@Override
				public void run() {
					isRegisted = false;
					setLoading(false);
					Toast.makeText(getApplicationContext(), "注册失败", 1).show();
				}
			});
			return;
		}
//		HciCloudVpr.hciVprSessionStop(session);
	}

	private void verifyVoice() {
		boolean verifyResult = false;
		VprConfig verifyConfig = new VprConfig();
		verifyConfig.addParam(VprConfig.UserConfig.PARAM_KEY_USER_ID, "wangyue8");
//		HciCloudFuncHelper.Verify(mAccountInfo.getAppKey(), verifyConfig);
		verifyResult = HciCloudFuncHelper.Verify("vpr.cloud.verify", verifyConfig);
		setLoading(false);
		if (verifyResult) {
			AudioRecordUtil.getInstance().removePcmFile();
			ToastUtil.showToast(this, "声音识别成功", 1);
			addStep5Layout();
		} else {
			AudioRecordUtil.getInstance().removePcmFile();
			ToastUtil.showToast(this, "声音识别失败", 1);
		}

	}

	private OnClickListener cancelClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			finish();
		}
	};

	private OnTouchListener audioClickListener = new OnTouchListener() {

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				recordStart();
				record(FLAG_WAV);
				break;
			case MotionEvent.ACTION_UP:
				recordFinish();
				stopRecord();
				if (recordTime <= 1) {
					mStep2ErrorTxt.setVisibility(View.VISIBLE);
					mStep2ErrorTxt.setText("录音时间太短");
					setLoading(false);
				} else {
					mStep2ErrorTxt.setVisibility(View.INVISIBLE);
					if (isRegisted) {
						verifyVoice();
					} else {
						registVoice();
					}
				}

				break;

			case MotionEvent.ACTION_CANCEL:
				recordFinish();
				setLoading(false);
				mStep2ErrorTxt.setVisibility(View.VISIBLE);
				mStep2ErrorTxt.setText("录音时间太短");
				stopRecord();
				break;
			default:
				break;
			}
			return false;
		}
	};

	private void recordStart() {
		mStep2ErrorTxt.setVisibility(View.INVISIBLE);
		mStep2Tip.setVisibility(View.INVISIBLE);
		mStep2SayHalo.setVisibility(View.VISIBLE);
		setLoading(false);
		AnimUtils.animVoiceBtnScale(mStep2SayHalo);
	}

	private void recordFinish() {
		setLoading(true);
		mStep2SayHalo.setVisibility(View.INVISIBLE);
	}

	private void setLoading(boolean isVisible) {
		if (isVisible) {
			mStep2Loading.setVisibility(View.VISIBLE);
		} else {
			mStep2Loading.setVisibility(View.INVISIBLE);
		}
	}

	@Override
	protected void onDestroy() {
		// 释放HciCloudSys，当其他能力全部释放完毕后，才能调用HciCloudSys的释放方法
		HciCloudVpr.hciVprRelease();
		HciCloudSys.hciRelease();
		super.onDestroy();
	}

}
