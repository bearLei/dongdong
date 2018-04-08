package com.houwei.guaishang.activity.newui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.flyco.dialog.entity.DialogMenuItem;
import com.flyco.dialog.listener.OnOperItemClickL;
import com.flyco.dialog.widget.NormalListDialog;
import com.google.gson.reflect.TypeToken;
import com.houwei.guaishang.R;
import com.houwei.guaishang.activity.BaseActivity;
import com.houwei.guaishang.activity.BasePhotoGridActivity;
import com.houwei.guaishang.activity.SelectPictureActivity;
import com.houwei.guaishang.bean.BaseBean;
import com.houwei.guaishang.bean.BaseResponse;
import com.houwei.guaishang.bean.IndustryBean;
import com.houwei.guaishang.bean.LocationBean;
import com.houwei.guaishang.bean.UserBean;
import com.houwei.guaishang.easemob.EaseEmojicon;
import com.houwei.guaishang.easemob.EaseEmojiconGroupEntity;
import com.houwei.guaishang.easemob.EaseEmojiconMenu;
import com.houwei.guaishang.layout.DialogUtils;
import com.houwei.guaishang.manager.MyLocationManager.LocationListener;
import com.houwei.guaishang.manager.MyUserBeanManager;
import com.houwei.guaishang.tools.DealResult;
import com.houwei.guaishang.tools.HttpUtil;
import com.houwei.guaishang.tools.JsonParser;
import com.houwei.guaishang.tools.ValueUtil;
import com.houwei.guaishang.views.AnimationYoYo;
import com.houwei.guaishang.views.InputControlEditText;
import com.houwei.guaishang.views.InputControlEditText.GetInputLengthListener;
import com.houwei.guaishang.views.InputControlEditText.InputLengthHintListener;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.Callback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//发布动态
public class ReleaseActivity extends BasePhotoGridActivity implements
		InputLengthHintListener, GetInputLengthListener, LocationListener {
	public final static int RELEASE_SUCCESS = 0x23;
	private InputControlEditText content_et;

	private TextView showlength_tv;
	private final int maxlength = 300;

	private CheckBox location_checkbox;
	private FrameLayout emojiconMenuContainer;
	private ImageView  face_btn;
	private LocationBean currentLocationBean; //经纬度位置信息
	private CheckBox redpacket_checkbox;
	// 选择的原图路径
	private ArrayList<String> selectedPicture = new ArrayList<String>();

	// 压缩后的临时路径
	public ArrayList<String> thumbPictures = new ArrayList<String>();

	private static final int REQUEST_PICK = 0;

	private MyHandler handler = new MyHandler(this);
	private MyUserBeanManager myUserBeanManager;
	private Button follow_btn;
	private TextView header_location;
	private TextView header_name;
	private ImageView avator;
	private TextView tvChooseIndustry;
	private TextView tvConfirm;

	private static class MyHandler extends Handler {
		
		private WeakReference<Context> reference;
		
	    public MyHandler(Context context) {
	    	reference = new WeakReference<Context>(context);
	    }
	    
	    @Override
		public void handleMessage(Message msg) {
	    	final ReleaseActivity activity = (ReleaseActivity) reference.get();
			if(activity == null){
				return;
			}
			activity.progress.dismiss();

			switch (msg.what) {
			case NETWORK_SUCCESS_DATA_RIGHT:
				BaseResponse retMap = (BaseResponse) msg.obj;
				if (retMap.isSuccess()) {
					activity.hideKeyboard();
					UserBean ub = activity.getITopicApplication().getMyUserBeanManager()
							.getInstance();
					ub.setTopicCount(ub.getTopicCount() + 1);
					activity.getITopicApplication().getMyUserBeanManager()
							.storeUserInfo(ub);
					activity.getITopicApplication().getMyUserBeanManager()
							.notityUserInfoChanged(ub);
					activity.setResult(RELEASE_SUCCESS);
					activity.finish();
				} else {
					activity.showErrorToast(retMap.getMessage());
				}
				break;
			default:
				activity.showErrorToast("发布失败");
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_release_new);
		myUserBeanManager = getITopicApplication().getMyUserBeanManager();
		initView();
		initListener();
		int type=getIntent().getIntExtra("type",1);
		startPhoto(type);
	}

	private void startPhoto(int type) {
		switch (type){
			case 0:
				pickPhotoFromCamera();
				break;
			case 1:
				Intent i = new Intent(ReleaseActivity.this, SelectPictureActivity.class);
				i.putExtra(SelectPictureActivity.INTENT_SELECTED_PICTURE, selectedPicture);
				i.putExtra("MaxLimit", getMaxLimit());
				startActivityForResult(i,REQUEST_PICK);
				break;
		}
	}

	protected void initView() {
		// TODO Auto-generated method stub
		redpacket_checkbox = (CheckBox)findViewById(R.id.redpacket_checkbox);
		super.initView();
		avator = (ImageView) findViewById(R.id.avator);
		header_name = (TextView) findViewById(R.id.header_name);
		header_location = (TextView) findViewById(R.id.header_location);
		tvChooseIndustry = (TextView) findViewById(R.id.tv_choose_industry);
		tvConfirm = (TextView) findViewById(R.id.tv_confirm);
		follow_btn = (Button) findViewById(R.id.follow_btn);
		UserBean bean=myUserBeanManager.getInstance();
		ImageLoader.getInstance().displayImage(bean.getAvatar().findOriginalUrl(), avator);
		header_name.setText(bean.getName());
		header_location.setText(bean.getPersonalTags());
		follow_btn.setText(ValueUtil.getRelationTypeStringSimple(bean.getFriendship()));
		follow_btn.setBackgroundResource(ValueUtil.getRelationTypeDrawableSimple(bean.getFriendship()));
		follow_btn.setTextColor(ValueUtil.getRelationTextColorSimple(bean.getFriendship()));
//
		location_checkbox = (CheckBox)findViewById(R.id.location_checkbox);
		face_btn = (ImageView) findViewById(R.id.face_btn);
		content_et = (InputControlEditText) findViewById(R.id.content_et);
		showlength_tv = (TextView) findViewById(R.id.showLength);
		content_et.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				// TODO Auto-generated method stub
				if (keyCode == KeyEvent.KEYCODE_ENTER
						&& event.getAction() == KeyEvent.ACTION_DOWN) {
					// doneClick();
					return true;
				}
				return false;
			}
		});

		content_et.setOnMaxInputListener(maxlength, this);

		content_et.setOnGetInputLengthListener(this);

		setShowLength(maxlength);

		emojiconMenuContainer = (FrameLayout) findViewById(R.id.emojicon_menu_container);
		// 表情栏，只添加小表情
		EaseEmojiconMenu emojiconMenu = (EaseEmojiconMenu) LayoutInflater.from(this).inflate(R.layout.ease_layout_emojicon_menu, null);
		List<EaseEmojiconGroupEntity> emojiconGroupList = new ArrayList<EaseEmojiconGroupEntity>();

		emojiconGroupList.add(new EaseEmojiconGroupEntity(
				R.drawable.expression_1, getITopicApplication()
						.getFaceManager().getEmojiconList()));
		((EaseEmojiconMenu) emojiconMenu).init(emojiconGroupList);
		emojiconMenuContainer.addView(emojiconMenu);

		emojiconMenu.setEmojiconMenuListener(new EaseEmojiconMenu.EaseEmojiconMenuListener() {
			
			@Override
			public void onExpressionClicked(EaseEmojicon emojicon) {
				// TODO Auto-generated method stub
				content_et.insertEmotion(ReleaseActivity.this, emojicon);
			}
			
			@Override
			public void onDeleteImageClicked() {
				// TODO Auto-generated method stub
				content_et.onEmojiconDeleteEvent();
			}
		});
		tvChooseIndustry.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				OkGo.<String>post(HttpUtil.IP+"topic/brand")
						.execute(new StringCallback() {
							@Override
							public void onSuccess(Response<String> response) {
								DealResult dr=new DealResult();
								IndustryBean bean=dr.dealData(ReleaseActivity.this,response,new TypeToken<BaseBean<IndustryBean>>(){}.getType());
								if(bean==null){
									return;
								}
								final List<IndustryBean.ItemsBean> itemsBeen=bean.getItems();
								if(itemsBeen.isEmpty()){
									return;
								}
								ArrayList<DialogMenuItem> lists=new ArrayList<DialogMenuItem>();
								for(IndustryBean.ItemsBean itemsBean :itemsBeen){
									DialogMenuItem item=new DialogMenuItem(itemsBean.getBrandName(),0);
									lists.add(item);
								}
								final NormalListDialog dialog = DialogUtils.getNormalListDialog(ReleaseActivity.this, "选择行业", lists);
								dialog.setOnOperItemClickL(new OnOperItemClickL() {
											@Override
											public void onOperItemClick(AdapterView<?> parent, View view, int position, long id) {
												tvChooseIndustry.setText("选择行业："+itemsBeen.get(position).getBrandName());
												brandId=Integer.valueOf(itemsBeen.get(position).getId());
												dialog.dismiss();
											}
										});
								dialog.show();

							}
						});
			}
		});

		tvConfirm.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doneClick();
			}
		});
		

		Handler hander = new Handler();
		hander.postDelayed(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				showKeyboard(content_et);
			}
		}, 150);
	}

	private void initListener() {
		// TODO Auto-generated method stub
		getITopicApplication().getLocationManager().addLocationListener(this);
		getITopicApplication().getLocationManager().startLoction(false);
		findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				// TODO Auto-generated method stub
				hideKeyboard();
				finish();
			}
		});
		/*findViewById(R.id.title_right).setOnClickListener(
				new View.OnClickListener() {

					public void onClick(View v) {
						// TODO Auto-generated method stub
						if (checkLogined()) {
							doneClick();
						}
					}
				});*/
		content_et.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				showKeyboard(content_et);
				emojiconMenuContainer.setVisibility(View.GONE);
				face_btn.setImageResource(R.drawable.compose_emoticonbutton_background_highlighted);
				return false;
			}
		});
		face_btn.setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if (emojiconMenuContainer.getVisibility() != View.VISIBLE) {
							hideKeyboard();
							emojiconMenuContainer.setVisibility(View.VISIBLE);
							((ImageView)v).setImageResource(R.drawable.compose_keyboardbutton_background_highlighted);
						} else {
							showKeyboard(content_et);
							emojiconMenuContainer.setVisibility(View.GONE);
							((ImageView)v).setImageResource(R.drawable.compose_emoticonbutton_background_highlighted);
						}
					}
				});
		findViewById(R.id.gallery_btn).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				showBottomPopupWin();
			}
		});
		findViewById(R.id.redpacket_ll).setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				redpacket_checkbox.setChecked(!redpacket_checkbox.isChecked());
				if (redpacket_checkbox.isChecked()) {
					//选中了红包照片，只能发一张图，这里直接清空让用户重新选好了
					clearSelectPicturesWithoutFirst();
				}else{
					//取消了红包照片，这里刷新一下，只为了让 + 显示出来
					resetAdapter();
				}
			}
		});
	}

	@Override
	public int getMaxLimit(){
		return redpacket_checkbox.isChecked()?1:9;
	}
	
	private void doneClick() {
		if (content_et.getText().toString().trim().equals("")) {
			AnimationYoYo.shakeView(findViewById(R.id.content_et));
			return;
		}
//		if(location_checkbox.isChecked() && thumbPictures.size() != 2)
		progress.show();
		new Thread(new UpdateImagesRun(thumbPictures)).start();
	}
	private int brandId;
	// 一次HTTP请求上传多张图片 + 各种参数
	private class UpdateImagesRun implements Runnable {
		private ArrayList<String> thumbPictures;

		// thumbPictures 是 List<压缩图路径>
		public UpdateImagesRun(ArrayList<String> thumbPictures) {
			this.thumbPictures = new ArrayList<String>();
			for (String string : thumbPictures) {
				if (!string.equals(""+BasePhotoGridActivity.PICTURE_UPDATE_ICON)) {
					//去掉最后一个 +图片
					this.thumbPictures.add(string);
				}
			}
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			BaseResponse response = null;
			try {
				HashMap<String, String> data = new HashMap<String, String>();
				data.put("userid", ReleaseActivity.this.getUserID());
				data.put("brandid", ""+brandId);
				data.put("content", content_et.getText().toString().trim());
//				data.put("redpacket", redpacket_checkbox.isChecked()?"1":"0");
				data.put("address", location_checkbox.isChecked()?location_checkbox.getText().toString(): "来自上海市");
				if(location_checkbox.isChecked() && currentLocationBean!=null){
					//用户愿意上传经纬度，该动态会出现在 附近的动态 里
					data.put("latitude", ""+currentLocationBean.getLatitude());
					data.put("longitude", ""+currentLocationBean.getLongitude());
				}

				response = JsonParser.getBaseResponse(HttpUtil.upload(data, thumbPictures,HttpUtil.IP +"topic/release"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (response != null) {
				handler.sendMessage(handler.obtainMessage(
						BaseActivity.NETWORK_SUCCESS_DATA_RIGHT, response));
			} else {
				handler.sendEmptyMessage(BaseActivity.NETWORK_FAIL);
			}
		}
	}
	
	private void setShowLength(int requestCode) {
		int currentLength = content_et.getText().length();
		setShowTextLength(currentLength, maxlength);
	}

	@Override
	public void getInputLength(int length) {
		setShowTextLength(length, maxlength);
	}

	private void setShowTextLength(int currentLength, int maxLength) {
		showlength_tv.setText(currentLength + "/" + maxLength + " 字");
	}

	@Override
	public void onOverFlowHint() {
		// TODO Auto-generated method stub
		AnimationYoYo.shakeView(showlength_tv);
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

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		getITopicApplication().getLocationManager().removeLocationListener(this);
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		hideKeyboard();
		emojiconMenuContainer.setVisibility(View.GONE);
		super.onPause();
	}

	@Override
	public void onLocationFail() {
		// TODO Auto-generated method stub
		location_checkbox.setText("定位失败");
	}

	@Override
	public void onLocationSuccess(final LocationBean currentLocationBean) {
		// TODO Auto-generated method stub
		location_checkbox.setEnabled(true);
		location_checkbox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean checked) {
				// TODO Auto-generated method stub
				location_checkbox.setText(checked?currentLocationBean.getAddress():"不显示位置");
			}
		});
		location_checkbox.setChecked(true);
		this.currentLocationBean = currentLocationBean;
	}

}
