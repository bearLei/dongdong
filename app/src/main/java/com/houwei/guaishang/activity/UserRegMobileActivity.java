package com.houwei.guaishang.activity;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import com.houwei.guaishang.R;
import com.houwei.guaishang.views.AnimationYoYo;
import com.mob.MobSDK;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import cn.smssdk.EventHandler;
import cn.smssdk.SMSSDK;


/**
 * 用户效验手机号界面（验证码界面）
 * 本案采用的验证码是基于mob的短信smssdk，免费，且不需要服务器端接入。但是会带来一堆libssmssdk.so
 */
public class UserRegMobileActivity extends BaseActivity {
	public final static int REG_LOGIN_SUCCESS = 0x99;
	
//	// 填写从短信SDK应用后台注册得到的APPKEY  guaishang 2.0
	public static String APPKEY = "15b5b9e067b56";
	// 填写从短信SDK应用后台注册得到的APPSECRET
	public static String APPSECRET = "7b60e80917dd1d9b1f90223b02215b9b";//
	
	
	private EditText phone_et, check_pw_et;
	private Button check_pw_get_btn;
	private Timer timer;
	private TimerTask task;
	private int CURRENTDELAYTIME;
	private final int DELAYTIME = 60;

	private MyHandler handler = new MyHandler(this);
	private static class MyHandler extends Handler {
		
		private WeakReference<Context> reference;
		
	    public MyHandler(Context context) {
	    	reference = new WeakReference<Context>(context);
	    }
	    
	    @Override
		public void handleMessage(Message msg) {
	    	final UserRegMobileActivity activity = (UserRegMobileActivity) reference.get();
			if(activity == null){
				return;
			}
			switch (msg.what) {
			case NETWORK_OTHER: // 验证码倒计时
				if (activity.CURRENTDELAYTIME <= 0) {
					activity.cancelTime();
				} else {
					activity.CURRENTDELAYTIME--;
					activity.check_pw_get_btn.setText(activity.CURRENTDELAYTIME + "秒后重获");
				}
				break;
			}
		}
	};

	private SMShandler smshandler = new SMShandler(this);
	private static class SMShandler extends Handler {

		private WeakReference<Context> reference;

	    public SMShandler(Context context) {
	    	reference = new WeakReference<Context>(context);
	    }

	    @Override
		public void handleMessage(Message msg) {
	    	final UserRegMobileActivity activity = (UserRegMobileActivity) reference.get();
			if(activity == null){
				return;
			}
			// TODO Auto-generated method stub
			activity.progress.dismiss();
			int event = msg.arg1;
			int result = msg.arg2;
			Object data = msg.obj;
			if (result == SMSSDK.RESULT_COMPLETE) {
				//短信注册成功后，返回MainActivity,然后提示新好友
				if (event == SMSSDK.EVENT_SUBMIT_VERIFICATION_CODE) {//提交验证码成功
					//验证码效验争取
					HashMap<String,String> phoneMap = (HashMap<String, String>) data;
					Intent i = new Intent(activity,UserRegInfoPersonalActivity.class);
					i.putExtra("mobile",  phoneMap.get("phone"));
					activity.startActivity(i);
				} else if (event == SMSSDK.EVENT_GET_VERIFICATION_CODE || event == SMSSDK.EVENT_GET_VOICE_VERIFICATION_CODE){
					activity.showErrorToast("验证码已经发送");
					activity.check_pw_get_btn.setClickable(false);
					activity.check_pw_get_btn.setText(activity.DELAYTIME+"秒后重获");
					activity.startTime();
				} else if (event ==SMSSDK.EVENT_GET_SUPPORTED_COUNTRIES){//返回支持发送验证码的国家列表
					activity.showErrorToast("获取国家列表成功");
				}
			} else {
				activity.showErrorToast("验证码错误");
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_reg_mobile);
		initView();
		initListener();
	}

	private void initView() {
		// TODO Auto-generated method stub
		MobSDK.init(this,"15b5b9e067b56","7b60e80917dd1d9b1f90223b02215b9b");
		BackButtonListener();
		timer = new Timer();
		phone_et = (EditText) findViewById(R.id.phone_et);
		check_pw_et = (EditText) findViewById(R.id.check_pw_et);
		initProgressDialog(false, null);
		handler.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				showKeyboard(phone_et);
			}
		}, 150);
		EventHandler eh=new EventHandler(){

			@Override
			public void afterEvent(int event, int result, Object data) {
				
				Message msg = new Message();
				msg.arg1 = event;
				msg.arg2 = result;
				msg.obj = data;
//				Log.d("CCC","e:"+event+"-re:"+result+"-data:"+data);
				smshandler.sendMessage(msg);
			}
		};
		SMSSDK.registerEventHandler(eh);
	}

	private void startTime() {
		CURRENTDELAYTIME = DELAYTIME;
		task = new TimerTask() {

			@Override
			public void run() {
				handler.sendEmptyMessage(NETWORK_OTHER);
			}
		};
		timer.schedule(task, 0, 1000);
	}

	private void cancelTime() {
		if (task!=null) {
			task.cancel();
		}
		check_pw_get_btn.setClickable(true);
		check_pw_get_btn.setText("获取验证码");
	}

	private void initListener() {
		// TODO Auto-generated method stub
		check_pw_get_btn = (Button) findViewById(R.id.check_pw_get_btn);
		check_pw_get_btn.setText("获取验证码");
		check_pw_get_btn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				if (phone_et.getText().toString().equals("")) {
					showErrorToast("请输入手机号码");
					return;
				}
				if (check_pw_get_btn.isClickable()) {
					check_pw_get_btn.setClickable(false);
					check_pw_get_btn.setText(DELAYTIME+"秒后重获");
				}
				progress.show();
				SMSSDK.getVerificationCode("86",phone_et.getText().toString().trim());
			}
		});

		findViewById(R.id.voice_tv).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				if (phone_et.getText().toString().equals("")) {
					showErrorToast("请输入手机号码");
					return;
				}
				if (check_pw_get_btn.isClickable()) {
					check_pw_get_btn.setClickable(false);
					check_pw_get_btn.setText(DELAYTIME+"秒后重获");
				}
				progress.show();
				SMSSDK.getVoiceVerifyCode("86",phone_et.getText().toString().trim());
			}
		});
		
		findViewById(R.id.next_btn).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub
						if (phone_et.getText().toString().trim().equals("")){
							AnimationYoYo.shakeView(findViewById(R.id.phone_et));
							showErrorToast("请输入手机号");
							return;
						}
						if (check_pw_et.getText().toString().trim().equals("")){
							AnimationYoYo.shakeView(findViewById(R.id.check_pw_et));
							showErrorToast("请输入验证码");
							return;
						}
		
						//debug 测试期间可以用这段代码跳过验证码
						Intent i = new Intent(UserRegMobileActivity.this,UserRegInfoPersonalActivity.class);
						i.putExtra("mobile",  phone_et.getText().toString().trim());
						startActivity(i);
						
//						progress.show();
//						SMSSDK.submitVerificationCode("86", phone_et.getText().toString().trim(), check_pw_et.getText().toString().trim());
					}
				});
		
		findViewById(R.id.gotologin).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent i = new Intent(UserRegMobileActivity.this, UserLoginActivity.class);
				startActivity(i);
			}
		});
		
		findViewById(R.id.title_right).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent i = new Intent(UserRegMobileActivity.this, UserRegInfoPersonalActivity.class);
				String mobileend =  ""+System.currentTimeMillis();
				String mobile = "1" + mobileend.substring(mobileend.length() - 10,mobileend.length());
				i.putExtra("mobile", mobile);
				i.putExtra("auto", true);
				startActivity(i);
			}
		});
		
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		if (task != null) {
			task.cancel();
			timer.cancel();
			timer = null;
			task = null;
		}
		super.onDestroy();
		SMSSDK.unregisterAllEventHandler();
	}
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (getCurrentFocus() != null) {
			((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
					.hideSoftInputFromWindow(
							getCurrentFocus().getWindowToken(),
							InputMethodManager.HIDE_NOT_ALWAYS);
		}
		return super.onTouchEvent(event);
	}

}
