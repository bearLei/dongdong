package com.houwei.guaishang.layout;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TextView.BufferType;

import com.github.jdsjlzx.interfaces.OnItemClickListener;
import com.github.jdsjlzx.recyclerview.LRecyclerView;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.houwei.guaishang.R;
import com.houwei.guaishang.activity.BaseActivity;
import com.houwei.guaishang.activity.Constant;
import com.houwei.guaishang.activity.TopicDetailActivity;
import com.houwei.guaishang.activity.newui.TopicDetailMeActivity;
import com.houwei.guaishang.adapter.OfferAdapter;
import com.houwei.guaishang.bean.AvatarBean;
import com.houwei.guaishang.bean.CommentBean;
import com.houwei.guaishang.bean.OffersBean;
import com.houwei.guaishang.bean.PraiseBean;
import com.houwei.guaishang.bean.TopicBean;
import com.houwei.guaishang.easemob.EaseConstant;
import com.houwei.guaishang.easemob.PreferenceManager;
import com.houwei.guaishang.layout.PictureGridLayout.RedPacketClickListener;
import com.houwei.guaishang.manager.FaceManager;
import com.houwei.guaishang.manager.ITopicApplication;
import com.houwei.guaishang.manager.MyUserBeanManager;
import com.houwei.guaishang.tools.DealResult;
import com.houwei.guaishang.tools.HttpUtil;
import com.houwei.guaishang.tools.ShareUtil2;
import com.houwei.guaishang.tools.ToastUtils;
import com.houwei.guaishang.tools.ValueUtil;
import com.houwei.guaishang.view.NumberProgressBar;
import com.houwei.guaishang.view.OrderBuyDialog;
import com.houwei.guaishang.views.CircleBitmapDisplayer1;
import com.houwei.guaishang.views.SpannableTextView;
import com.houwei.guaishang.views.SpannableTextView.MemberClickListener;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.HashMap;
import java.util.List;

import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;


public class TopicAdapter extends BaseAdapter {
    private final Drawable attention;
    private final Drawable attentionUn;
    private List<TopicBean> list;
    private LayoutInflater mInflater;
    private ImageLoader imageLoader = ImageLoader.getInstance();
    private BaseActivity mContext;
    private FaceManager faceManager;
    private String userId;
    private DisplayImageOptions options;
    private TopicBeanDeleteListener onTopicBeanDeleteListener;
    private RedPacketClickListener onRedPacketClickListener;
    private TopicBeanFollowClickListener onTopicBeanFollowClickListener;
    private TopicBeanBaojiaClickListener onTopicBeanBaojiaClickListener;
    private int face_item_size;
    private MProgressDialog dialog;
    private RxPermissions rxPermissions;

    //	设置list跳转不同详情页
    private int jumpType;
    public TopicAdapter(BaseActivity mContext, List<TopicBean> list, int jumpType) {
        this.list = list;
        this.mInflater = LayoutInflater.from(mContext);
        this.mContext = mContext;
        this.userId = mContext.getITopicApplication().getMyUserBeanManager().getUserId();
        this.faceManager = mContext.getITopicApplication().getFaceManager();
        this.options = mContext.getITopicApplication().getOtherManage().getRectDisplayImageOptions();
        this.face_item_size = (int) mContext.getResources().getDimension(R.dimen.face_tiny_item_size);
        this.jumpType=jumpType;
        dialog = new MProgressDialog(mContext, true);
        rxPermissions = new RxPermissions(mContext);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            attention = mContext.getDrawable(R.mipmap.attention);
            attentionUn = mContext.getDrawable(R.mipmap.attention_un);
        } else {
            attention = mContext.getResources().getDrawable(R.mipmap.attention);
            attentionUn = mContext.getResources().getDrawable(R.mipmap.attention_un);
        }
        attention.setBounds(0, 0, attention.getIntrinsicWidth(), attention.getIntrinsicHeight());
        attentionUn.setBounds(0, 0, attentionUn.getIntrinsicWidth(), attentionUn.getIntrinsicHeight());
    }

    public int getCount() {
        return list.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_topic, null);
             View finalConvertView = convertView;

            holder.avator = (ImageView) convertView.findViewById(R.id.avator);
            holder.imageMyOrder = (ImageView) convertView.findViewById(R.id.image_myorder);
            holder.header_name = (TextView) convertView.findViewById(R.id.header_name);
            holder.recyclerView = (LRecyclerView) convertView.findViewById(R.id.recyclerView_offer);
            holder.header_location = (TextView) convertView.findViewById(R.id.header_location);
            holder.follow_btn = (Button) convertView.findViewById(R.id.follow_btn);
            holder.imgTitle = (ImageView) convertView.findViewById(R.id.img_title);
            holder.tvCount = (TextView) convertView.findViewById(R.id.tv_count);
            holder.barNum = (NumberProgressBar) convertView.findViewById(R.id.bar_num);
            holder.content = (TextView) convertView.findViewById(R.id.content);
            holder.header_time = (TextView) convertView.findViewById(R.id.header_time);
            holder.zan_count_btn = (PraiseTextView) convertView
                    .findViewById(R.id.zan_count_btn);
            holder.review_count_btn = (TextView) convertView
                    .findViewById(R.id.review_count_btn);
            holder.delete_btn = (TextView) convertView
                    .findViewById(R.id.delete_btn);
            holder.price_tv = (TextView) convertView
                    .findViewById(R.id.price_tv);
            holder.praise_ll = convertView.findViewById(R.id.praise_ll);
            holder.comment_ll = convertView.findViewById(R.id.comment_ll);
            holder.share_ll = convertView.findViewById(R.id.share_ll);
            holder.share_count_btn = (TextView) convertView.findViewById(R.id.share_count_btn);
            holder.order_btn = (Button) convertView.findViewById(R.id.order_btn);
            holder.chat_btn = (Button) convertView.findViewById(R.id.chat_btn);
            holder.linearLayoutForListView = (LinearLayoutForListView) convertView.findViewById(R.id.linearLayoutForListView);
            holder.linearLayoutForListView.setDisableDivider(true);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        final TopicBean bean = list.get(position);
        final String memberId = bean.getMemberId();
        try {
            int max = Integer.valueOf(bean.getSetRob());
            int progress = Integer.valueOf(bean.getNowRob());
            holder.barNum.setMax(max);
            holder.barNum.setProgress(progress);
            if(progress==max){
                holder.order_btn.setVisibility(View.INVISIBLE);
            }else{
                holder.order_btn.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(TextUtils.equals(mContext.getUserID(),memberId)){
            holder.order_btn.setVisibility(View.INVISIBLE);
            holder.imageMyOrder.setVisibility(View.VISIBLE);
            holder.recyclerView.setVisibility(View.VISIBLE);
            initRecyclerView(bean,holder.recyclerView,bean.getOfferPrice());
        }else{
            holder.recyclerView.setVisibility(View.GONE);
            holder.order_btn.setVisibility(View.VISIBLE);
            holder.imageMyOrder.setVisibility(View.INVISIBLE);
        }

        //imageLoader.displayImage(bean.getMemberAvatar().findSmallUrl(), holder.avator);
        imageLoader.displayImage(bean.getMemberAvatar().findSmallUrl(), holder.avator, mContext.getITopicApplication().getOtherManage().getCircleOptionsDisplayImageOptions());
        imageLoader.displayImage(bean.getCover(), holder.imgTitle, mContext.getITopicApplication().getOtherManage().getRectDisplayImageOptions());
        holder.content.setText(faceManager.
                        convertNormalStringToSpannableString(mContext, bean.getContent()),
                BufferType.SPANNABLE);
        FaceManager.extractMention2Link(holder.content);

        holder.content.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View arg0) {
                // TODO Auto-generated method stub
                MenuDialog followDialog = new MenuDialog(mContext,
                        new MenuDialog.ButtonClick() {

                            @Override
                            public void onSureButtonClick() {
                                // TODO Auto-generated method stub
                                ClipboardManager clipboard = (ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE);
                                clipboard.setText(bean.getContent());
                            }
                        });
                followDialog.show();
                return true;
            }
        });

        holder.linearLayoutForListView.setAdapter(new CommentItemAdapter(
                mContext, bean.getComments()));


        String locationTemp = bean.getDistance() != null ? bean.getDistanceString() : bean.getAddress();
        String location = "";

        if(locationTemp.contains("省")){
            location = locationTemp.substring((locationTemp.indexOf("省")+1), locationTemp.length());
        }else {
            location = locationTemp;
        }
        if(locationTemp.contains("市")){
            location = location.substring(0, (location.indexOf("市")+1));
        }

        holder.header_location.setText(location);
        holder.header_name.setText(bean.getMemberName());
        holder.header_time.setText(bean.getTimeString());
//        holder.linearLayoutForListView.setVisibility((bean.getComments() == null || bean.getComments().isEmpty()) ? View.GONE : View.VISIBLE);
        holder.linearLayoutForListView.setVisibility(View.GONE);
        holder.review_count_btn.setText("" + bean.getCommentCount());
        holder.price_tv.setText("￥" + bean.getPrice());
        holder.share_count_btn.setText(bean.getShareNum()+"");

//        holder.jcVideoPlayer.setUp(bean.getVideourl(), "");
        //ImageLoader.getInstance().displayImage(bean.getCover(), holder.jcVideoPlayer.thumbImageView, kkk(mContext));
//        ImageLoader.getInstance().displayImage(bean.getCover(),holder.jcVideoPlayer.thumbImageView,options);
//		Bitmap bitmap = ImageLoader.getInstance().loadImageSync(bean.getCover());
//		holder.jcVideoPlayer.thumbImageView,kkk()
//		float[] outerR = new float[] { 120, 120, 0, 0, 0, 0, 0, 0 };
//		RoundRectShape rectShape=new RoundRectShape(outerR, null, null);
//		ShapeDrawable mDrawables= new ShapeDrawable(rectShape);
//		mDrawables.getPaint().setColor(Color.RED);
//		mDrawables.draw(new Canvas(bitmap.copy(Bitmap.Config.ARGB_8888,true)));
//holder.jcVideoPlayer.thumbImageView.setImageBitmap(bitmap);
//        if (bean.isPraised()) {
//            holder.zan_count_btn.setCompoundDrawables(null, attention, null, null);
//        } else {
//            holder.zan_count_btn.setCompoundDrawables(null, attentionUn, null, null);
//        }
        holder.zan_count_btn.setText(bean.getSumPrice());
       /* holder.zan_count_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dealPraise(holder.zan_count_btn,bean);
            }
        });*/
//        holder.zan_count_btn.setPraiseState(mContext, bean);
//        holder.praise_ll.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                // TODO Auto-generated method stub
//                PraiseTextView tv = (PraiseTextView) v.findViewById(R.id.zan_count_btn);
//                tv.clickPraise(mContext, bean);
//            }
//        });
        holder.comment_ll.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                if(TextUtils.equals(mContext.getUserID(),memberId)){
                    Intent intent=new Intent(mContext,TopicDetailMeActivity.class);
                    intent.putExtra("TopicBean", bean);
                    intent.putExtra("position", 0);
                    mContext.startActivityForResult(intent, 0);
                    return;
                }
                Intent i = new Intent();
                if(jumpType==0){
                    i.setClass(mContext, TopicDetailActivity.class);
                }else{
                    i.setClass(mContext, TopicDetailMeActivity.class);

                }
                i.putExtra("TopicBean", bean);
                i.putExtra("position", 0);
                i.putExtra("needPay", !bean.getOffer());
                mContext.startActivityForResult(i, 0);
            }
        });

        holder.avator.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                Log.i("WXCH","jumpToHisInfoActivity");
                mContext.jumpToHisInfoActivity(bean.getMemberId(), bean.getMemberName(), bean.getMemberAvatar());
            }
        });

        holder.share_ll.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                ShareUtil2 shareUtil2 = new ShareUtil2(mContext, new PlatformActionListener() {
                    @Override
                    public void onComplete(Platform platform, int i, HashMap<String, Object> hashMap) {
                        Log.i("onComplete","platform="+platform.getName()+" 分享成功");
                        //Message message=new Message();
                        //Bundle bundle=new Bundle();
                        //bundle.putString("memberId", bean.getMemberId());
                        //message.setData(bundle);
                        //message.what=1;
                        //holder.share_count_btn.setText((bean.getShareNum()+1)+"");
                        //notifyDataSetChanged();

                        //HashMap<String, String> data = new HashMap<String, String>();
                        //data.put("id", msg.getData().getString("memberId"));
                        //String s = HttpUtil.postMsg(HttpUtil.getData(data), HttpUtil.IP + "mission/Sharing/");
                        //Log.i("WXCH","SSSSSS:" + s);
                    }

                    @Override
                    public void onError(Platform platform, int i, Throwable throwable) {

                    }

                    @Override
                    public void onCancel(Platform platform, int i) {

                    }
                });
                shareUtil2.setContent(bean.getContent());
                shareUtil2.setUrl(HttpUtil.SHARE_TOPIC_IP + bean.getTopicId());
                if (bean.getPicture() != null && !bean.getPicture().isEmpty()) {
                    shareUtil2.setImageUrl(bean.getPicture().get(0).findOriginalUrl());
                }
                shareUtil2.showBottomPopupWin();
            }
        });

       /* if (userId.equals(bean.getMemberId())) {
            holder.delete_btn.setVisibility(View.VISIBLE);
            holder.chat_btn.setVisibility(View.GONE);
            holder.order_btn.setVisibility(View.GONE);
            holder.follow_btn.setVisibility(View.GONE);
        } else {
            holder.delete_btn.setVisibility(View.GONE);
            holder.chat_btn.setVisibility(View.VISIBLE);
            holder.order_btn.setVisibility(View.VISIBLE);
            holder.follow_btn.setVisibility(View.VISIBLE);
        }*/

        holder.delete_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                SureOrCancelDialog followDialog = new SureOrCancelDialog(
                        mContext, "删除掉该条商品", "好",
                        new SureOrCancelDialog.SureButtonClick() {

                            @Override
                            public void onSureButtonClick() {
                                // TODO Auto-generated method stub
                                if (onTopicBeanDeleteListener != null) {
                                    onTopicBeanDeleteListener.onTopicBeanDeleteClick(bean);
                                }
                            }
                        });
                followDialog.show();
            }
        });

        holder.chat_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                /*mContext.jumpToChatActivity(bean.getMemberId(),
                        bean.getMemberName(), bean.getMemberAvatar(), EaseConstant.CHATTYPE_SINGLE);*/

                rxPermissions.request(Manifest.permission.CALL_PHONE)
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(@NonNull Boolean aBoolean) throws Exception {
                                if (aBoolean) {
                                    //用intent启动拨打电话
                                    String number = bean.getMobile();
                                    if(TextUtils.isEmpty(number)){
                                        ToastUtils.toastForShort(mContext,"电话号码不能为空");
                                        return;
                                    }
                                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + number));
                                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                        mContext.startActivity(intent);
                                    }
                                }
                            }
                        });

            }
        });

//		holder.order_btn.setText(ValueUtil.getTopicTypeBuyButtonString(bean.getType()));
        holder.order_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Log.i("WXCH","userId:"+userId+",getMemberId:"+bean.getMemberId());
                // TODO Auto-generated method stub
                if (mContext.checkLogined()) {
                    if(!bean.getMemberId().equals(mContext.getUserID())){
                        orderBuyOrNextPage(bean,true);
                    }
                }
            }
        });
//        if(jumpType!=0){
//            holder.order_btn.setVisibility(View.INVISIBLE);
//        }


        holder.follow_btn.setText(ValueUtil.getRelationTypeStringSimple(bean.getFriendship()));
        holder.follow_btn.setBackgroundResource(ValueUtil.getRelationTypeDrawableSimple(bean.getFriendship()));
        holder.follow_btn.setTextColor(ValueUtil.getRelationTextColorSimple(bean.getFriendship()));
        holder.follow_btn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if (mContext.checkLogined()) {
                    if(!bean.getMemberId().equals(mContext.getUserID())){
                        if (onTopicBeanFollowClickListener != null) {
                            onTopicBeanFollowClickListener.onTopicBeanFollowClick(bean);
                        }
                    }

                }
                /*if (onTopicBeanFollowClickListener != null) {
                    onTopicBeanFollowClickListener.onTopicBeanFollowClick(bean);
                }*/
            }
        });

        holder.content.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                if(TextUtils.equals(mContext.getUserID(),memberId)){
                    Intent intent=new Intent(mContext,TopicDetailMeActivity.class);
                    intent.putExtra("TopicBean", bean);
                    intent.putExtra("position", 0);
                    mContext.startActivityForResult(intent, 0);
                    return;
                }
                Intent i = new Intent();
                if(jumpType==0){
                    i.setClass(mContext, TopicDetailActivity.class);
                }else{
                    i.setClass(mContext, TopicDetailMeActivity.class);

                }
                i.putExtra("TopicBean", bean);
                i.putExtra("position", 0);
                i.putExtra("needPay", !bean.getOffer());
                mContext.startActivityForResult(i, 0);

            }
        });
        convertView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                //Log.i("WXCH","bean:"+bean);
                if(TextUtils.equals(mContext.getUserID(),memberId)){
                    Intent intent=new Intent(mContext,TopicDetailMeActivity.class);
                    intent.putExtra("TopicBean", bean);
                    intent.putExtra("position", 0);
                    mContext.startActivityForResult(intent, 0);
                    return;
                }
                Intent i = new Intent();
                if(jumpType==0){
                    i.setClass(mContext, TopicDetailActivity.class);
                }else{
                    i.setClass(mContext, TopicDetailMeActivity.class);

                }
                i.putExtra("TopicBean", bean);
                i.putExtra("position", 0);
                i.putExtra("needPay", !bean.getOffer());
                mContext.startActivityForResult(i, 0);

            }
        });
        return convertView;
    }

    private void orderBuyOrNextPage(final TopicBean bean, final boolean fromBtnClick) {
        OkGo.<String>post(HttpUtil.IP+"Topic/is_rob")
                .tag(this)
                .params("user_id", userId)
                .params("topicid", bean.getTopicId())
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        String res=response.body().toString().trim();
                        //Log.i("WXCH","SSSSSS:" + res);
                        if(res.contains("1")){
                            OrderBuyDialog.getInstance(mContext)
                                    .setData(PreferenceManager.getInstance().getUserPoint(), bean)
                                    .show();
                        }else if (fromBtnClick){
                            ToastUtils.toastForShort(mContext, "此单您已抢过");
                        } else {
                            Intent i = new Intent();
                            i.putExtra("TopicBean", bean);
                            i.putExtra("position", 0);
                            mContext.jumpToChatActivityCom(bean,0,bean.getMemberId(), bean.getMemberName(), bean.getMemberAvatar(),EaseConstant.CHATTYPE_SINGLE);
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {

                        super.onError(response);
                    }
                });
    }

    private void initRecyclerView(final TopicBean topicBean, LRecyclerView recyclerViewOffer, List<OffersBean.OfferBean> beans) {
        if(beans==null||beans.isEmpty()){
            return;
        }
        LinearLayoutManager manager=new LinearLayoutManager(mContext);
        manager.setAutoMeasureEnabled(true);
        final OfferAdapter mAdapter = new OfferAdapter(mContext);
//        TopicLinearLayoutManager manager1=new TopicLinearLayoutManager(mContext,mAdapter);
        recyclerViewOffer.setLayoutManager(manager);
        mAdapter.setDataList(beans);
        mAdapter.setTopicBean(topicBean);
        final LRecyclerViewAdapter lRecyclerViewAdapter=new LRecyclerViewAdapter(mAdapter);
        recyclerViewOffer.setAdapter(lRecyclerViewAdapter);
        recyclerViewOffer.setLoadMoreEnabled(false);
        lRecyclerViewAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                OffersBean.OfferBean bean = mAdapter.getDataList().get(position);
                if(TextUtils.equals(mContext.getUserID(),bean.getOfferId())){
                    ToastUtils.toastForShort(mContext,"不能同自己聊天");
                    return;
                }
                AvatarBean avatarBean=new AvatarBean();
                avatarBean.setOriginal(bean.getAvatar());
                avatarBean.setSmall(bean.getAvatar());
                mContext.jumpToChatActivityCom(topicBean,0,bean.getOfferId(), bean.getName(), avatarBean, EaseConstant.CHATTYPE_SINGLE);
            }
        });
        recyclerViewOffer.setPullRefreshEnabled(false);
        recyclerViewOffer.setLoadMoreEnabled(false);
        recyclerViewOffer.refresh();
    }

    private void dealPraise(final PraiseTextView zan_count_btn, final TopicBean topicBean) {
        dialog.show();
        OkGo.<String>post(HttpUtil.IP+"topic/praise")
                .tag(this)
                .params("userid", userId)
                .params("topicid", topicBean.getTopicId())
                .execute(new StringCallback() {
                    @Override
                    public void onSuccess(Response<String> response) {
                        dialog.dismiss();
                        PraiseBean bean=DealResult.getInstace().dealData(mContext,response,PraiseBean.class);
                        if(bean==null){return;}
                        if(bean.getCode()==Constant.SUCESS){
                            if(topicBean.isPraised()){
                                zan_count_btn.setText((topicBean.getPraiseCount()-1)+"");
                                topicBean.setPraiseCount(topicBean.getPraiseCount()-1);
                                zan_count_btn.setCompoundDrawables(null,attentionUn,null,null);
                                topicBean.setPraised(false);
                            }else{
                                zan_count_btn.setText((topicBean.getPraiseCount()+1)+"");
                                topicBean.setPraiseCount(topicBean.getPraiseCount()+1);
                                zan_count_btn.setCompoundDrawables(null,attention,null,null);
                                topicBean.setPraised(true);
                            }
//                            topicBean.setFriendship(bean.getData());
                        }
                    }

                    @Override
                    public void onError(Response<String> response) {
                        dialog.dismiss();
                        super.onError(response);
                    }
                });
    }

    public static class ViewHolder {
        private TextView content, header_name, header_time, header_location,tvCount;
        private PraiseTextView zan_count_btn;
        private ImageView avator,imgTitle,imgIndicate,imageMyOrder;
        private TextView review_count_btn, delete_btn,tvProgress;
        private View praise_ll, comment_ll, share_ll;
        private TextView share_count_btn;
        private LinearLayoutForListView linearLayoutForListView;
        private NumberProgressBar barNum;
        private LRecyclerView recyclerView;

        private Button chat_btn, order_btn, follow_btn;

        private TextView price_tv;


    }

    private DisplayImageOptions kkk(Context context) {

        return new DisplayImageOptions.Builder()
                .displayer(new CircleBitmapDisplayer1(context)).build();

    }

    public interface TopicBeanDeleteListener {
        public void onTopicBeanDeleteClick(TopicBean topicBean);
    }

    public void setOnTopicBeanDeleteListener(TopicBeanDeleteListener onTopicBeanDeleteListener) {
        this.onTopicBeanDeleteListener = onTopicBeanDeleteListener;
    }

    public TopicBeanDeleteListener getOnTopicBeanDeleteListener() {
        return onTopicBeanDeleteListener;
    }

    public interface TopicBeanFollowClickListener {
        public void onTopicBeanFollowClick(TopicBean topicBean);
    }
    public interface TopicBeanBaojiaClickListener {
        public void TopicBeanBaojiaClick(TopicBean topicBean);
    }


    public class CommentItemAdapter extends BaseAdapter {

        private BaseActivity mContext;
        private List<CommentBean> cellReviewList;
        private LayoutInflater mLayoutInflater;

        public CommentItemAdapter(BaseActivity context, List<CommentBean> cellReviewList) {
            this.mContext = context;
            this.cellReviewList = cellReviewList;
            this.mLayoutInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public int getCount() {
            return cellReviewList == null ? 0 : cellReviewList.size();
        }

        @Override
        public String getItem(int index) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.listview_review_textview, null);
            }
            final CommentBean cellBean = cellReviewList.get(position);
            SpannableTextView comment_tiny_tv = (SpannableTextView) convertView
                    .findViewById(R.id.comment_tiny_tv);


            comment_tiny_tv.setCommentItem(cellBean, new MemberClickListener() {

                @Override
                public void onMemberClick(CommentBean commentBean) {
                    // TODO Auto-generated method stub

                    mContext.jumpToHisInfoActivity(commentBean.getMemberId(),
                            commentBean.getMemberName(),
                            commentBean.getMemberAvatar());
                }
            }, new MemberClickListener() {

                @Override
                public void onMemberClick(CommentBean commentBean) {
                    // TODO Auto-generated method stub

                    mContext.jumpToHisInfoActivity(commentBean.getToMemberId(),
                            commentBean.getToMemberName(),
                            commentBean.getToMemberAvatar());

                }
            });

            comment_tiny_tv.append(faceManager.
                    convertNormalStringToSpannableString(mContext, cellBean.getContent(), face_item_size));

            return convertView;
        }
    }

    public RedPacketClickListener getOnRedPacketClickListener() {
        return onRedPacketClickListener;
    }

    public void setOnRedPacketClickListener(RedPacketClickListener onRedPacketClickListener) {
        this.onRedPacketClickListener = onRedPacketClickListener;
    }

    public TopicBeanFollowClickListener getOnTopicBeanFollowClickListener() {
        return onTopicBeanFollowClickListener;
    }

    public void setOnTopicBeanFollowClickListener(
            TopicBeanFollowClickListener onTopicBeanFollowClickListener) {
        this.onTopicBeanFollowClickListener = onTopicBeanFollowClickListener;
    }

    public TopicBeanBaojiaClickListener getTopicBeanBaojiaClickListener() {
        return onTopicBeanBaojiaClickListener;
    }

    public void setTopicBeanBaojiaClickListener(
            TopicBeanBaojiaClickListener onTopicBeanBaojiaClickListener) {
        this.onTopicBeanBaojiaClickListener = onTopicBeanBaojiaClickListener;
    }


}

