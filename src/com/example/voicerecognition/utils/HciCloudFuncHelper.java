package com.example.voicerecognition.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.api.HciCloudUser;
import com.sinovoice.hcicloudsdk.api.vpr.HciCloudVpr;
import com.sinovoice.hcicloudsdk.common.AuthExpireTime;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.InitParam;
import com.sinovoice.hcicloudsdk.common.Session;
import com.sinovoice.hcicloudsdk.common.vpr.VprConfig;
import com.sinovoice.hcicloudsdk.common.vpr.VprEnrollResult;
import com.sinovoice.hcicloudsdk.common.vpr.VprEnrollVoiceData;
import com.sinovoice.hcicloudsdk.common.vpr.VprEnrollVoiceDataItem;
import com.sinovoice.hcicloudsdk.common.vpr.VprIdentifyResult;
import com.sinovoice.hcicloudsdk.common.vpr.VprInitParam;
import com.sinovoice.hcicloudsdk.common.vpr.VprVerifyResult;

public class HciCloudFuncHelper {
	private static final String TAG = HciCloudFuncHelper.class.getSimpleName();

	/**
	 * 加载初始化信息
	 * 
	 * @param context
	 *            上下文语境
	 * @return 系统初始化参数
	 */
	public static InitParam getInitParam(Context context) {
		String authDirPath = context.getFilesDir().getAbsolutePath();

		// 前置条件：无
		InitParam initparam = new InitParam();
		// 授权文件所在路径，此项必填
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_AUTH_PATH, authDirPath);
		// 灵云云服务的接口地址，此项必填
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_CLOUD_URL, AccountInfo.getInstance().getCloudUrl());
		// 开发者Key，此项必填，由捷通华声提供
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_DEVELOPER_KEY, AccountInfo.getInstance().getDeveloperKey());
		// 应用Key，此项必填，由捷通华声提供
		initparam.addParam(InitParam.AuthParam.PARAM_KEY_APP_KEY, AccountInfo.getInstance().getAppKey());

		// 配置日志参数
		String sdcardState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(sdcardState)) {
			String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();
			String packageName = context.getPackageName();

			String logPath = sdPath + File.separator + "sinovoice" + File.separator + packageName + File.separator + "log" + File.separator;

			// 日志文件地址
			File fileDir = new File(logPath);
			if (!fileDir.exists()) {
				fileDir.mkdirs();
			}

			// 日志的路径，可选，如果不传或者为空则不生成日志
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_PATH, logPath);
			// 日志数目，默认保留多少个日志文件，超过则覆盖最旧的日志
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_COUNT, "5");
			// 日志大小，默认一个日志文件写多大，单位为K
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_SIZE, "1024");
			// 日志等级，0=无，1=错误，2=警告，3=信息，4=细节，5=调试，SDK将输出小于等于logLevel的日志信息
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_LEVEL, "5");
		}

		return initparam;
	}

	/**
	 * 获取授权
	 * 
	 * @return true 成功
	 */
	public static int checkAuthAndUpdateAuth() {

		// 获取系统授权到期时间
		int initResult;
		AuthExpireTime objExpireTime = new AuthExpireTime();
		initResult = HciCloudSys.hciGetAuthExpireTime(objExpireTime);
		if (initResult == HciErrorCode.HCI_ERR_NONE) {
			// 显示授权日期,如用户不需要关注该值,此处代码可忽略
			Date date = new Date(objExpireTime.getExpireTime() * 1000);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
			Log.i(TAG, "expire time: " + sdf.format(date));

			if (objExpireTime.getExpireTime() * 1000 > System.currentTimeMillis()) {
				// 已经成功获取了授权,并且距离授权到期有充足的时间(>7天)
				Log.i(TAG, "checkAuth success");
				return initResult;
			}

		}

		// 获取过期时间失败或者已经过期
		initResult = HciCloudSys.hciCheckAuth();
		if (initResult == HciErrorCode.HCI_ERR_NONE) {
			Log.i(TAG, "checkAuth success");
			return initResult;
		} else {
			Log.e(TAG, "checkAuth failed: " + initResult);
			return initResult;
		}
	}

	/*
	 * VPR注册或训练
	 */
	public static boolean Enroll(String capkey, VprConfig enrollConfig) {
		// 组装音频，可以一次传入多段音频
		int nEnrollDataCount = 1;
		int nIndex = 0;
		ArrayList<VprEnrollVoiceDataItem> enrollVoiceDataList = new ArrayList<VprEnrollVoiceDataItem>();

		for (; nIndex < nEnrollDataCount; nIndex++) {
			byte[] voiceData = getPcmFileData();
			if (null == voiceData) {
//				ShowMessage("Open input voice file" + voiceDataName + "error!");
				break;
			}
			VprEnrollVoiceDataItem voiceDataItem = new VprEnrollVoiceDataItem();
			voiceDataItem.setVoiceData(voiceData);
			enrollVoiceDataList.add(voiceDataItem);
		}
		if (nIndex <= 0) {
//			ShowMessage("no enroll data found in assets folder!");
			return false;
		}

		// 启动 VPR Session
		VprConfig sessionConfig = new VprConfig();
		sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);
		// 根据实际情况指定资源前缀
		if (capkey.contains("local")) {
			sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_RES_PREFIX, "16k_");
		}

		Session session = new Session();
		int errCode = HciCloudVpr.hciVprSessionStart(sessionConfig.getStringConfig(), session);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("hciVprSessionStart return " + errCode);
			return false;
		}

		// VPR 注册
		VprEnrollVoiceData enrollVoiceData = new VprEnrollVoiceData();
		enrollVoiceData.setEnrollVoiceDataCount(nEnrollDataCount);
		enrollVoiceData.setEnrollVoiceDataList(enrollVoiceDataList);
		VprEnrollResult enrollResult = new VprEnrollResult();
		errCode = HciCloudVpr.hciVprEnroll(session, enrollVoiceData, enrollConfig.getStringConfig(), enrollResult);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
			// 启动失败
			HciCloudVpr.hciVprSessionStop(session);
//			ShowMessage("hciVprEnroll return " + errCode);
			return false;
		}

		// 关闭session
		HciCloudVpr.hciVprSessionStop(session);
		return true;
	}

	/*
	 * VPR 确认（Verify）
	 */
	public static boolean Verify(String capkey, VprConfig verifyConfig) {
		byte[] voiceDataVerify = getPcmFileData();
		if (null == voiceDataVerify) {
//			ShowMessage("Open input voice file " + voiceDataName + " error!");
			return false;
		}

		// 启动 VPR Session
		VprConfig sessionConfig = new VprConfig();
		sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);
		// 根据实际情况指定资源前缀
		if (capkey.contains("local")) {
			sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_RES_PREFIX, "16k_");
		}

		Session session = new Session();
		int errCode = HciCloudVpr.hciVprSessionStart(sessionConfig.getStringConfig(), session);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("hciVprSessionStart return " + errCode);
			return false;
		}

		// 开始校验
		VprVerifyResult verifyResult = new VprVerifyResult();
		errCode = HciCloudVpr.hciVprVerify(session, voiceDataVerify, verifyConfig.getStringConfig(), verifyResult);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("Hcivpr hciVprVerify return " + errCode);
			HciCloudVpr.hciVprSessionStop(session);
			return false;
		}

		if (verifyResult.getStatus() == VprVerifyResult.VPR_VERIFY_STATUS_MATCH) {
			
			
		} else {
//			ShowMessage("voice data doesn't match with user_id !");
		}

		HciCloudVpr.hciVprSessionStop(session);
		return true;
	}

	/*
	 * VPR 辨识（Identify）
	 */
	public static boolean Identify(String capkey, VprConfig identifyConfig) {

		byte[] voiceDataVerify = getPcmFileData();
		if (null == voiceDataVerify) {
//			ShowMessage("Open input voice file " + voiceDataName + " error!");
			return false;
		}

		// 启动 VPR Session
		VprConfig sessionConfig = new VprConfig();
		sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);
		// 根据实际情况指定资源前缀
		if (capkey.contains("local")) {
			sessionConfig.addParam(VprConfig.SessionConfig.PARAM_KEY_RES_PREFIX, "16k_");
		}

		Session session = new Session();
		int errCode = HciCloudVpr.hciVprSessionStart(sessionConfig.getStringConfig(), session);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("Hcivpr hciVprSessionStart return " + errCode);
			return false;
		}

		// 辨识
		VprIdentifyResult identifyResult = new VprIdentifyResult();
		errCode = HciCloudVpr.hciVprIdentify(session, voiceDataVerify, identifyConfig.getStringConfig(), identifyResult);
		if (HciErrorCode.HCI_ERR_NONE != errCode) {
//			ShowMessage("Hcivpr hciVprIdentify return " + errCode);
			HciCloudVpr.hciVprSessionStop(session);
			return false;
		}

		HciCloudVpr.hciVprSessionStop(session);
		return true;
	}
	
	
	
	
	
	public static byte[] getPcmFileData() {
		InputStream in = null;
		int size = 0;
		try {
			in = new FileInputStream(AudioRecordUtil.getInstance().getAudioPcmFile());
			size = in.available();
			byte[] data = new byte[size];
			in.read(data, 0, size);

			return data;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	
}
