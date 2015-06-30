package com.tchip.carlauncher.ui.activity;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.navi.BaiduMapAppNotSupportNaviException;
import com.baidu.mapapi.navi.BaiduMapNavigation;
import com.baidu.mapapi.navi.NaviPara;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUnderstander;
import com.iflytek.cloud.SpeechUnderstanderListener;
import com.iflytek.cloud.TextUnderstander;
import com.iflytek.cloud.TextUnderstanderListener;
import com.iflytek.cloud.UnderstanderResult;
import com.iflytek.sunflower.FlowerCollector;
import com.tchip.carlauncher.Constant;
import com.tchip.carlauncher.R;
import com.tchip.carlauncher.service.SpeakService;
import com.tchip.carlauncher.util.NetworkUtil;
import com.tchip.carlauncher.util.PinYinUtil;
import com.tchip.carlauncher.util.ProgressAnimationUtil;
import com.tchip.carlauncher.util.XunFeiErrorCodeUtil;
import com.tchip.carlauncher.view.ButtonFloat;
import com.tchip.carlauncher.view.CircularProgressDrawable;
import com.tchip.carlauncher.view.ResideMenu;
import com.tchip.carlauncher.view.ResideMenuInfo;
import com.tchip.carlauncher.view.ResideMenuItem;

public class ChatActivity extends FragmentActivity implements OnClickListener {
	private static String TAG = ChatActivity.class.getSimpleName();
	// 语义理解对象（语音到语义）。
	private SpeechUnderstander mSpeechUnderstander;
	// 语义理解对象（文本到语义）。
	private TextUnderstander mTextUnderstander;
	private Toast mToast;
	private TextView tvHint;
	private TextView tvQuestion, tvAnswer;
	private String strService;

	private SharedPreferences mSharedPreferences;

	private ImageView imageAnim; // 动画按钮
	private ImageView imageVoice; // 语音按钮
	private Animator currentAnimation;
	private CircularProgressDrawable drawable;

	private ScrollView scrollArea;

	private PackageManager packageManager;

	// 百度地图地址转经纬度
	private GeoCoder mEndSearch = null;
	private LatLng mEndLatLng;
	private double startLat = 0.0;
	private double startLng = 0.0;
	private double endLat = 0.0;
	private double endLng = 0.0;

	private RelativeLayout layoutBack; // 返回
	private LinearLayout layoutHelp; // 帮助

	// 左侧帮助侧边栏
	private ResideMenu resideMenu;

	private boolean isResideMenuClose = false;

	@SuppressLint("ShowToast")
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_chat);

		initLayout();
		// 初始化对象
		mSpeechUnderstander = SpeechUnderstander.createUnderstander(
				ChatActivity.this, speechUnderstanderListener);
		mTextUnderstander = TextUnderstander.createTextUnderstander(
				ChatActivity.this, textUnderstanderListener);

		mToast = Toast.makeText(ChatActivity.this, "", Toast.LENGTH_SHORT);

		// 监听屏幕熄灭与点亮
		// final IntentFilter screenFilter = new IntentFilter();
		// screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
		// screenFilter.addAction(Intent.ACTION_SCREEN_ON);
		// registerReceiver(ScreenOnOffReceiver, screenFilter);

		startSpeak("你好，有什么可以帮您？");

		Button btnToMultimedia = (Button) findViewById(R.id.btnToMultimedia);
		btnToMultimedia.setOnClickListener(this);
	}

	/**
	 * 侧边栏打开关闭监听
	 */
	private ResideMenu.OnMenuListener menuListener = new ResideMenu.OnMenuListener() {
		@Override
		public void openMenu() {
			isResideMenuClose = false;
			layoutHelp.setVisibility(View.GONE);
		}

		@Override
		public void closeMenu() {
			isResideMenuClose = true;
			layoutHelp.setVisibility(View.VISIBLE);
		}
	};

	public ResideMenu getResideMenu() {
		return resideMenu;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			backToMain();
			return true;
		} else
			return super.onKeyDown(keyCode, event);
	}

	private void backToMain() {
		finish();
		overridePendingTransition(R.anim.zms_translate_down_out,
				R.anim.zms_translate_down_in);
	}

	private final BroadcastReceiver ScreenOnOffReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String action = intent.getAction();
			if (Intent.ACTION_SCREEN_ON.equals(action)) {
				Log.e("zms", "-----------------screen is on...");
			} else if (Intent.ACTION_SCREEN_OFF.equals(action)) {
				Log.e("zms", "----------------- screen is off...");

			}
		}
	};

	/**
	 * 初始化Layout。
	 */
	private void initLayout() {
		imageAnim = (ImageView) findViewById(R.id.imageAnim);
		imageAnim.setOnClickListener(ChatActivity.this);

		tvHint = (TextView) findViewById(R.id.tvHint);

		mSharedPreferences = getSharedPreferences(
				Constant.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

		drawable = new CircularProgressDrawable.Builder()
				.setRingWidth(
						getResources().getDimensionPixelSize(
								R.dimen.drawable_ring_size))
				.setOutlineColor(getResources().getColor(R.color.white))
				.setRingColor(
						getResources().getColor(R.color.ui_chat_voice_orange))
				.setCenterColor(
						getResources().getColor(R.color.ui_chat_voice_orange))
				.create();
		imageAnim.setImageDrawable(drawable);
		imageAnim.setVisibility(View.INVISIBLE); // 隐藏动画

		imageVoice = (ImageView) findViewById(R.id.imageVoice);
		imageVoice.setOnClickListener(this);

		scrollArea = (ScrollView) findViewById(R.id.scrollArea);
		tvQuestion = (TextView) findViewById(R.id.tvQuestion);
		tvQuestion.setVisibility(View.GONE);
		tvAnswer = (TextView) findViewById(R.id.tvAnswer);

		// 返回
		layoutBack = (RelativeLayout) findViewById(R.id.layoutBack);
		layoutBack.setOnClickListener(this);

		// 帮助侧边栏
		layoutHelp = (LinearLayout) findViewById(R.id.layoutHelp);
		layoutHelp.setOnClickListener(this);

		Button btnHelp = (Button) findViewById(R.id.btnHelp);
		btnHelp.setOnClickListener(this);
		// attach to current activity;
		resideMenu = new ResideMenu(this);
		resideMenu.setBackground(R.color.grey_dark_light);
		resideMenu.attachToActivity(this);
		resideMenu.setMenuListener(menuListener);
		// valid scale factor is between 0.0f and 1.0f. leftmenu'width is
		// 150dip.
		resideMenu.setScaleValue(0.6f);
		// 禁止使用右侧菜单
		resideMenu.setDirectionDisable(ResideMenu.DIRECTION_RIGHT);

		// 创建侧边栏内容条目
		// itemHuiyuan = new ResideMenuItem(this,
		// R.drawable.ui_chat_hint__navi,"导航");
		// resideMenu.addMenuItem(itemHuiyuan, ResideMenu.DIRECTION_LEFT);
	}

	/**
	 * 初始化监听器（语音到语义）。
	 */
	private InitListener speechUnderstanderListener = new InitListener() {
		@Override
		public void onInit(int code) {
			Log.d(TAG, "speechUnderstanderListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				// 初始化失败,错误码：code
				String errorContent = XunFeiErrorCodeUtil
						.getErrorDescription(code);
				Toast.makeText(getApplicationContext(), errorContent,
						Toast.LENGTH_SHORT).show();

			}
		}
	};

	/**
	 * 初始化监听器（文本到语义）。
	 */
	private InitListener textUnderstanderListener = new InitListener() {

		@Override
		public void onInit(int code) {
			Log.d(TAG, "textUnderstanderListener init() code = " + code);
			if (code != ErrorCode.SUCCESS) {
				// 初始化失败,错误码： code
				String errorContent = XunFeiErrorCodeUtil
						.getErrorDescription(code);
				Toast.makeText(getApplicationContext(), errorContent,
						Toast.LENGTH_SHORT).show();
			}
		}
	};

	int ret = 0;// 函数调用返回值

	@Override
	public void onClick(View view) {

		switch (view.getId()) {
		case R.id.btnToMultimedia:
		case R.id.layoutBack:
			backToMain();
			break;

		// 开始语音理解
		case R.id.imageVoice:
		case R.id.imageAnim:
			if (-1 == NetworkUtil.getNetworkType(getApplicationContext())) {
				String strNoNetwork = "无网络链接";
				tvAnswer.setText(strNoNetwork);
				startSpeak(strNoNetwork);
				Toast.makeText(getApplicationContext(), strNoNetwork,
						Toast.LENGTH_SHORT).show();
			} else {
				tvHint.setText("");
				// 设置参数
				setParam();

				if (mSpeechUnderstander.isUnderstanding()) { // 开始前检查状态
					mSpeechUnderstander.stopUnderstanding(); // 停止录音
				} else {
					ret = mSpeechUnderstander
							.startUnderstanding(mRecognizerListener);
					if (ret != 0) {
						// 语义理解失败,错误码:ret
					} else {
						// showTip(getString(R.string.text_begin));
					}
				}
			}
			break;
		// 停止语音理解
		// mSpeechUnderstander.stopUnderstanding();

		// 取消语音理解
		// mSpeechUnderstander.cancel();
		case R.id.layoutHelp:
		case R.id.btnHelp:
			resideMenu.openMenu(ResideMenu.DIRECTION_LEFT);
			break;
		default:
			break;
		}
	}

	private TextUnderstanderListener textListener = new TextUnderstanderListener() {

		@Override
		public void onResult(final UnderstanderResult result) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (null != result) {
						// 显示
						// String text = result.getResultString();
						// if (!TextUtils.isEmpty(text)) {
						// tvHint.setText(text);
						// }
					} else {
						// 识别结果不正确
					}
				}
			});
		}

		@Override
		public void onError(SpeechError error) {
			// showTip("onError Code：" + error.getErrorCode());
			// 初始化失败,错误码： code
			Toast.makeText(getApplicationContext(),
					error.getErrorDescription(), Toast.LENGTH_SHORT).show();

		}
	};

	/**
	 * 识别回调。
	 */
	private SpeechUnderstanderListener mRecognizerListener = new SpeechUnderstanderListener() {

		@Override
		public void onResult(final UnderstanderResult result) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (null != result) {
						tvAnswer.setText(""); // 清空回答
						// 显示
						String text = result.getResultString();
						if (!TextUtils.isEmpty(text)) {
							tvHint.setText(text);

							try {
								JSONObject jsonObject;
								jsonObject = new JSONObject(text);
								String strQuestion = jsonObject
										.getString("text");
								tvQuestion.setVisibility(View.VISIBLE);
								tvQuestion.setText(strQuestion);

								strService = jsonObject.getString("service");
								if ("openQA".equals(strService)
										|| "datetime".equals(strService)
										|| "chat".equals(strService)) {
									String strAnswer = jsonObject
											.getJSONObject("answer").getString(
													"text");
									tvAnswer.setText(strAnswer);
									startSpeak(strAnswer);
								} else if ("baike".equals(strService)) {
									String strAnswer = jsonObject
											.getJSONObject("answer").getString(
													"text");
									tvAnswer.setText(strAnswer);
								} else if ("weather".equals(strService)) {

									JSONArray mJSONArray = jsonObject
											.getJSONObject("data")
											.getJSONArray("result");
									JSONObject todayJSON = mJSONArray
											.getJSONObject(0);
									String tempRange = todayJSON
											.getString("tempRange");
									String weather = todayJSON
											.getString("weather");
									String city = todayJSON.getString("city");
									String strAnswer = city + "天气：" + weather
											+ ",温度" + tempRange;
									tvAnswer.setText(strAnswer);
									startSpeak(strAnswer);
								} else if ("music".equals(strService)) {
									// 下载邓紫棋的喜欢你
								} else if ("map".equals(strService)) {
									// 导航到中山市图书馆 operation": "ROUTE"
									String endPoiStr = jsonObject
											.getJSONObject("semantic")
											.getJSONObject("slots")
											.getJSONObject("endLoc")
											.getString("poi");
									String endCityStr = jsonObject
											.getJSONObject("semantic")
											.getJSONObject("slots")
											.getJSONObject("endLoc")
											.getString("city");
									if ("CURRENT_CITY".equals(endCityStr))
										endCityStr = mSharedPreferences
												.getString("cityName", "未知");
									mEndSearch = GeoCoder.newInstance();
									mEndSearch
											.setOnGetGeoCodeResultListener(new MyOnGetGeoCoderResultListener());
									mEndSearch.geocode(new GeoCodeOption()
											.city(endCityStr)
											.address(endPoiStr));

								} else if ("app".equals(strService)) {
									// 打开百度地图 "operation": "LAUNCH",
									String appName = jsonObject
											.getJSONObject("semantic")
											.getJSONObject("slots")
											.getString("name");
									String operationStr = jsonObject
											.getString("operation");
									if ("LAUNCH".equals(operationStr)) {
										String packageName = getAppPackageByName(appName);
										if (!"com.tchip.carlauncher"
												.equals(packageName)) {
											String strAnswer = "正在启动："
													+ appName;
											tvAnswer.setText(strAnswer);
											startSpeak(strAnswer);
											startAppbyPackage(packageName);
										} else {
											String strAnswer = "未找到应用："
													+ appName;
											tvAnswer.setText(strAnswer);
											startSpeak(strAnswer);
										}
									}
								} else if ("telephone".equals(strService)) {
									// 打电话给张三 "operation": "CALL"
									String peopleName = jsonObject
											.getJSONObject("semantic")
											.getJSONObject("slots")
											.getString("name");
									String operationStr = jsonObject
											.getString("operation");
									if ("CALL".equals(operationStr)) {
										String phoneNum = getContactNumberByName(peopleName);
										String phoneCode = "";
										try {
											phoneCode = jsonObject
													.getJSONObject("semantic")
													.getJSONObject("slots")
													.getString("code");
										} catch (Exception e) {

										}
										if (phoneNum != null
												& phoneNum.trim().length() > 0) {
											String strAnswer = "正在打电话给："
													+ peopleName;
											tvAnswer.setText(strAnswer);
											startSpeak(strAnswer);
											phoneCall(phoneNum);
										} else if (phoneCode != null
												& phoneCode.trim().length() > 0) {
											String strAnswer = "正在打电话给："
													+ peopleName;
											tvAnswer.setText(strAnswer);
											startSpeak(strAnswer);
											phoneCall(phoneCode);
										} else {
											String phoneNumFromPinYin = getContactNumberByPinYin(PinYinUtil
													.convertAll(peopleName));

											if (phoneNumFromPinYin != null
													& phoneNumFromPinYin.trim()
															.length() > 0) {
												String strAnswer = "正在打电话给："
														+ peopleName;
												tvAnswer.setText(strAnswer);
												startSpeak(strAnswer);
												phoneCall(phoneNumFromPinYin);

											} else {
												String strAnswer = "通讯录中未找到："
														+ peopleName;
												tvAnswer.setText(strAnswer);
												startSpeak(strAnswer);
											}
										}
									}
								} else if ("message".equals(strService)) {
									// 发短信给小张晚上一起吃饭。operation:SEND
									String peopleName = jsonObject
											.getJSONObject("semantic")
											.getJSONObject("slots")
											.getString("name");

									String messageContent = "";
									try {
										messageContent = jsonObject
												.getJSONObject("semantic")
												.getJSONObject("slots")
												.getString("content");
									} catch (Exception e) {
									}
									String operationStr = jsonObject
											.getString("operation");
									if ("SEND".equals(operationStr)) {
										if (messageContent != null
												&& messageContent.trim()
														.length() > 0) {
											String phoneNum = getContactNumberByName(peopleName);
											if (phoneNum != null
													& phoneNum.trim().length() > 0) {
												String strAnswer = "正在发短信给："
														+ peopleName + "："
														+ messageContent;
												tvAnswer.setText(strAnswer);
												startSpeak(strAnswer);
												sendMessage(phoneNum,
														messageContent);
											} else {
												String phoneNumFromPinYin = getContactNumberByPinYin(PinYinUtil
														.convertAll(peopleName));

												if (phoneNumFromPinYin != null
														& phoneNumFromPinYin
																.trim()
																.length() > 0) {
													String strAnswer = "正在发短信给"
															+ peopleName + "："
															+ messageContent;
													tvAnswer.setText(strAnswer);
													startSpeak(strAnswer);
													sendMessage(
															phoneNumFromPinYin,
															messageContent);
												} else {
													String strAnswer = "通讯录中未找到："
															+ peopleName;
													tvAnswer.setText(strAnswer);
													startSpeak(strAnswer);
												}
											}
										} else {
											String strAnswer = "短信内容为空。";
											tvAnswer.setText(strAnswer);
											startSpeak(strAnswer);
										}
									}
								}

							} catch (JSONException e) {
								e.printStackTrace();
								String strNoAnswer = "小天不知道怎么回答了";
								tvAnswer.setText(strNoAnswer);
								startSpeak(strNoAnswer);
							} finally {
								// if ("map".equals(strService)) {
								// }
							}
							makeScrollViewDown(scrollArea);
						}
					} else {
						// 识别结果不正确
					}
				}
			});
		}

		/**
		 * 拨打电话
		 * 
		 * @param phoneNumer
		 */
		public void phoneCall(String phoneNumer) {
			Uri uri = Uri.parse("tel:" + phoneNumer);
			Intent intent = new Intent(Intent.ACTION_CALL, uri);
			startActivity(intent);
		}

		/**
		 * 直接发送短信，不跳转到系统界面
		 * 
		 * @param phoneNum
		 *            号码
		 * @param content
		 *            短信内容
		 */
		public void sendMessage(String phoneNum, String content) {

			String SENT_SMS_ACTION = "SENT_SMS_ACTION";
			Intent sentIntent = new Intent(SENT_SMS_ACTION);
			PendingIntent sentPI = PendingIntent.getBroadcast(
					getApplicationContext(), 0, sentIntent, 0);
			// register the Broadcast Receivers
			getApplicationContext().registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context _context, Intent _intent) {
					switch (getResultCode()) {
					case Activity.RESULT_OK:
						Toast.makeText(getApplicationContext(), "短信已发送",
								Toast.LENGTH_SHORT).show();
						break;
					case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
						break;
					case SmsManager.RESULT_ERROR_RADIO_OFF:
						break;
					case SmsManager.RESULT_ERROR_NULL_PDU:
						break;
					}
				}
			}, new IntentFilter(SENT_SMS_ACTION));

			// 处理返回的接收状态
			String DELIVERED_SMS_ACTION = "DELIVERED_SMS_ACTION";
			// create the deilverIntent parameter
			Intent deliverIntent = new Intent(DELIVERED_SMS_ACTION);
			PendingIntent deliverPI = PendingIntent.getBroadcast(
					getApplicationContext(), 0, deliverIntent, 0);
			getApplicationContext().registerReceiver(new BroadcastReceiver() {
				@Override
				public void onReceive(Context _context, Intent _intent) {
					// 收信人已经成功接收
				}
			}, new IntentFilter(DELIVERED_SMS_ACTION));

			SmsManager smsManager = SmsManager.getDefault();
			List<String> divideContents = smsManager.divideMessage(content);
			for (String messageText : divideContents) {
				smsManager.sendTextMessage(phoneNum, null, messageText, sentPI,
						deliverPI);
			}
		}

		public String getContactNumberByName(String name) {
			Cursor c = getApplicationContext().getContentResolver().query(
					Phone.CONTENT_URI, null, null, null, null);

			// 循环输出联系人号码
			while (c.moveToNext()) {
				if (name.equals(c.getString(c
						.getColumnIndex(Phone.DISPLAY_NAME)))) {
					// 可以获取到电话号码
					return c.getString(c.getColumnIndex(Phone.NUMBER));
				}
			}
			return "";
		}

		public String getContactNumberByPinYin(String pinyin) {
			Cursor c = getApplicationContext().getContentResolver().query(
					Phone.CONTENT_URI, null, null, null, null);

			// 循环输出联系人号码
			while (c.moveToNext()) {
				if (pinyin.equals(PinYinUtil.convertAll(c.getString(c
						.getColumnIndex(Phone.DISPLAY_NAME))))) {
					// 可以获取到电话号码
					return c.getString(c.getColumnIndex(Phone.NUMBER));
				}

			}
			return "";

		}

		private void startAppbyPackage(String packageName) {

			Intent intent = packageManager
					.getLaunchIntentForPackage(packageName);
			startActivity(intent);
		}

		private String getAppPackageByName(String appName) {
			packageManager = getApplicationContext().getPackageManager();
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
			List<ResolveInfo> resovleInfos = packageManager
					.queryIntentActivities(mainIntent, 0);
			for (ResolveInfo resolve : resovleInfos) {
				// 应用图标:resolve.loadIcon(packageManager)
				// 应用名称:resolve.loadLabel(packageManager)
				// 应用包名：resolve.activityInfo.packageName
				// 应用启动的第一个Activity：resolve.activityInfo.name
				if (UpperCaseLetter(appName).equals(
						resolve.loadLabel(packageManager).toString())) {
					return resolve.activityInfo.packageName.toString();
				}
			}
			return "com.tchip.carlauncher";
		}

		public String UpperCaseLetter(String b) {
			char letters[] = new char[b.length()];
			for (int i = 0; i < b.length(); i++) {

				char letter = b.charAt(i);
				if (letter >= 'a' && letter <= 'z') {
					letter = (char) (letter - 32);
				}
				letters[i] = letter;
			}
			return new String(letters);
		}

		@Override
		public void onVolumeChanged(int v) {
			// showTip("onVolumeChanged：" + v);
		}

		@Override
		public void onEndOfSpeech() {
			// showTip("onEndOfSpeech");
			if (currentAnimation != null) {
				currentAnimation.cancel();
			}
			currentAnimation = ProgressAnimationUtil
					.preparePulseAnimation(drawable);
			currentAnimation.start();

			// 显示语音按钮，隐藏动画按钮
			imageVoice.setVisibility(View.VISIBLE);
			imageAnim.setVisibility(View.INVISIBLE);
		}

		@Override
		public void onBeginOfSpeech() {
			// 隐藏语音按钮，显示动画按钮
			imageVoice.setVisibility(View.INVISIBLE);
			imageAnim.setVisibility(View.VISIBLE);

			if (currentAnimation != null) {
				currentAnimation.cancel();
			}
			currentAnimation = ProgressAnimationUtil
					.prepareStyle1Animation(drawable);
			currentAnimation.start();
		}

		@Override
		public void onError(SpeechError error) {
			// showTip("onError Code：" + error.getErrorCode());
			Toast.makeText(getApplicationContext(),
					error.getErrorDescription(), Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {

		}
	};

	/**
	 * 跳转ScrollView到底部
	 */
	private void makeScrollViewDown(ScrollView scrollView) {
		scrollView.fullScroll(ScrollView.FOCUS_DOWN);
	}

	private void startSpeak(String content) {
		Intent intent = new Intent(ChatActivity.this, SpeakService.class);
		intent.putExtra("content", content);
		startService(intent);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// 退出时释放连接
		mSpeechUnderstander.cancel();
		mSpeechUnderstander.destroy();
		if (mTextUnderstander.isUnderstanding())
			mTextUnderstander.cancel();
		mTextUnderstander.destroy();
	}

	private void showTip(final String str) {
		mToast.setText(str);
		mToast.show();
	}

	/**
	 * 参数设置
	 * 
	 * @param param
	 * @return
	 */
	public void setParam() {
		String lag = mSharedPreferences.getString("voiceAccent", "mandarin");
		if (lag.equals("en_us")) {
			// 设置语言
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "en_us");
		} else {
			// 设置语言
			mSpeechUnderstander.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
			// 设置语言区域
			mSpeechUnderstander.setParameter(SpeechConstant.ACCENT, lag);
		}
		// 设置语音前端点
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_BOS,
				mSharedPreferences.getString("voiceBos", "4000"));
		// 设置语音后端点
		mSpeechUnderstander.setParameter(SpeechConstant.VAD_EOS,
				mSharedPreferences.getString("voiceEos", "1000"));
		// 设置标点符号
		mSpeechUnderstander.setParameter(SpeechConstant.ASR_PTT,
				mSharedPreferences.getString("understander_punc_preference",
						"1"));
		// 设置音频保存路径
		mSpeechUnderstander.setParameter(
				SpeechConstant.ASR_AUDIO_PATH,
				mSharedPreferences.getString("voicePath",
						Environment.getExternalStorageDirectory()
								+ "/iflytek/wavaudio.pcm")

		);
	}

	@Override
	protected void onResume() {
		// 移动数据统计分析
		FlowerCollector.onResume(ChatActivity.this);
		FlowerCollector.onPageStart(TAG);
		super.onResume();

		// 隐藏状态栏
		View decorView = getWindow().getDecorView();
		decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
	}

	@Override
	protected void onPause() {
		// 移动数据统计分析
		FlowerCollector.onPageEnd(TAG);
		FlowerCollector.onPause(ChatActivity.this);
		super.onPause();
	}

	class MyOnGetGeoCoderResultListener implements OnGetGeoCoderResultListener {

		@Override
		public void onGetGeoCodeResult(GeoCodeResult result) {
			mEndLatLng = result.getLocation();
			if (mEndLatLng != null) {
				// 起始点：当前位置
				startLat = Double.parseDouble(mSharedPreferences.getString(
						"latitude", "0.0"));
				startLng = Double.parseDouble(mSharedPreferences.getString(
						"longitude", "0.0"));
				// 目的地
				endLat = mEndLatLng.latitude;
				endLng = mEndLatLng.longitude;
				LatLng startLatLng = new LatLng(startLat, startLng);
				LatLng endLatLng = new LatLng(endLat, endLng);
				// 构建 导航参数
				NaviPara para = new NaviPara();
				para.startPoint = startLatLng;
				para.startName = "从这里开始";
				para.endPoint = endLatLng;
				para.endName = "到这里结束";

				try {
					BaiduMapNavigation.openBaiduMapNavi(para,
							getApplicationContext());
				} catch (BaiduMapAppNotSupportNaviException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onGetReverseGeoCodeResult(ReverseGeoCodeResult result) {
		}
	}

}
