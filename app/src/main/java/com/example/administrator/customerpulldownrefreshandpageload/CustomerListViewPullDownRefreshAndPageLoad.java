package com.example.administrator.customerpulldownrefreshandpageload;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Created by Administrator on 2016-09-27.
 */
public class CustomerListViewPullDownRefreshAndPageLoad extends RelativeLayout {
    private final String TAG = CustomerListViewPullDownRefreshAndPageLoad.class.getSimpleName();
    private final int REFRESHMSG_CODE = 0x994;
    private final int LOADMORE_CODE = 0x887;
    private ListView listView;
    private LinearLayout footer;
    private ArrayAdapter<String> adapter;
    private ViewGroup.MarginLayoutParams marginLayoutParams;
    private ViewGroup.MarginLayoutParams marginLayoutParamspro;

    //上方的存放图标和文字的LinearLayout
    private LinearLayout linearLayout;
    //在上方存档的内容包括以下的imageView progressBar textView
    private ImageView imageArrow;
    private ProgressBar progressBar;
    private TextView textViewTip;

    private final int MARGIN_TOP = -200;
    private final int IMAGE_SIZE = 50;


    private boolean pullFlag;
    private boolean returnFlag;
    //刷新完成标志
    private boolean refreshFinishFlag;

    private int oldY;
    private int newY;
    private int distance;

    //线程
    public Thread tempThreadRefresh;

    //接口
    public interface refreshEvent {
        //下拉刷新
        void refresh();

        //分页加载
        void loadMore(int startIndex, int endIndex);

        //item点击
        void onItemClick(AdapterView<?> parent, View view, int position, long id);
    }

    private refreshEvent refreshEvent;

    //分页加载
    private boolean loadFinishFlag;
    private ProgressBar progressBarFooter;
    private TextView textViewTipFooter;
    private int startIndex;
    private int endIndex;
    private final int pageSize = 20;

    //线程
    public Thread tempThreadLoadMore;

    public CustomerListViewPullDownRefreshAndPageLoad(Context context, AttributeSet attrs) {
        super(context, attrs);
        listView = new ListView(getContext());
        RelativeLayout.LayoutParams reLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        reLayoutParams.setMargins(0, 0, 0, 0);
        listView.setLayoutParams(reLayoutParams);
        listView.setBackgroundColor(getResources().getColor(android.R.color.white));
        addView(listView);

        linearLayout = new LinearLayout(getContext());
        RelativeLayout.LayoutParams liLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        liLayoutParams.setMargins(0, MARGIN_TOP, 0, 0);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setLayoutParams(liLayoutParams);
        addView(linearLayout);

        imageArrow = new ImageView(getContext());
        imageArrow.setImageResource(R.drawable.arrowdown);
        LinearLayout.LayoutParams imgLayoutParams = new LinearLayout.LayoutParams(IMAGE_SIZE, IMAGE_SIZE);
        imageArrow.setLayoutParams(imgLayoutParams);
        linearLayout.addView(imageArrow);

        progressBar = new ProgressBar(getContext());
        LinearLayout.LayoutParams proLayoutParams = new LinearLayout.LayoutParams(IMAGE_SIZE, IMAGE_SIZE);
        progressBar.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progressbar));
        progressBar.setLayoutParams(proLayoutParams);
        linearLayout.addView(progressBar);

        textViewTip = new TextView(getContext());
        LinearLayout.LayoutParams texLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        texLayoutParams.setMargins(20, 0, 0, 0);
        textViewTip.setText("正在刷新...");
        textViewTip.setLayoutParams(texLayoutParams);
        linearLayout.addView(textViewTip);


        pullFlag = false;
        returnFlag = false;
        refreshFinishFlag = true;
        loadFinishFlag = true;

        oldY = 0;
        newY = 0;
        distance = 0;
        startIndex = 0;
        endIndex = pageSize;
        marginLayoutParams = (ViewGroup.MarginLayoutParams) listView.getLayoutParams();
        marginLayoutParamspro = (ViewGroup.MarginLayoutParams) linearLayout.getLayoutParams();

        //设置滑动事件
        listView.setOnTouchListener(new MyOnTouch());

        footer = new LinearLayout(getContext());
        footer.setGravity(Gravity.CENTER);

        progressBarFooter = new ProgressBar(getContext());
        LinearLayout.LayoutParams footerProLayoutParams = new LinearLayout.LayoutParams(IMAGE_SIZE, IMAGE_SIZE);
        progressBarFooter.setIndeterminateDrawable(getResources().getDrawable(R.drawable.progressbar));
        progressBarFooter.setLayoutParams(footerProLayoutParams);
        footer.addView(progressBarFooter);

        textViewTipFooter = new TextView(getContext());
        LinearLayout.LayoutParams footerTexLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerTexLayoutParams.setMargins(20, 0, 0, 0);
        textViewTipFooter.setText("正在加载...");
        textViewTipFooter.setLayoutParams(footerTexLayoutParams);
        footer.addView(textViewTipFooter);

        //设置分页加载
        listView.setOnScrollListener(new ScrollListener());

        //这是点击事件
        listView.setOnItemClickListener(new MyItemClick());

    }

    //滑动事件
    class MyOnTouch implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    View firstChild = listView.getChildAt(0);
                    if (firstChild != null) {
                        //当前第一个是第几项
                        int firstChildPosition = listView.getFirstVisiblePosition();
                        //第一项的坐标为0
                        int firstChildTop = firstChild.getTop();
                        if (firstChildPosition == 0 && firstChildTop == 0 && refreshFinishFlag && loadFinishFlag) {
                            pullFlag = true;
                            Log.i(TAG, "---->ACTION_DOWN" + String.valueOf(pullFlag));
                        }
                        Log.i(TAG, "---->ACTION_DOWN" + String.valueOf(firstChildPosition));
                        Log.i(TAG, "---->ACTION_DOWN" + String.valueOf(firstChildTop));
                    }
                    returnFlag = false;
                    Log.i(TAG, "---->ACTION_DOWN");
                    oldY = (int) event.getRawY();
                    distance = 0;
                    break;
                case MotionEvent.ACTION_MOVE:
                    newY = (int) event.getRawY();
                    distance = newY - oldY;
                    if (pullFlag && distance > 0) {
                        returnFlag = true;
                        marginLayoutParams.topMargin = distance / 2;
                        listView.setLayoutParams(marginLayoutParams);
                        if (distance < 300) {
                            //正在下拉
                            downStatus();
                            marginLayoutParamspro.topMargin = (distance / 2) - 100;
                            linearLayout.setLayoutParams(marginLayoutParamspro);
                            //防止强迫症，拉完有放回来
                            refreshFinishFlag = true;
                        } else {
                            //下拉大于150，能够执行刷新动作
                            //更新标志位，为完成下拉
                            refreshFinishFlag = false;
                            //显示释放
                            upStatus();
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.i(TAG, "---->ACTION_UP");
                    if (pullFlag) {
                        if (distance < 300) {
                            //下拉没有到位释放
                            marginLayoutParams.topMargin = 0;
                            //隐藏上边的部分
                            marginLayoutParamspro.topMargin = MARGIN_TOP;
                            linearLayout.setLayoutParams(marginLayoutParamspro);
                        } else {
                            marginLayoutParams.topMargin = 150;
                            //防止快速点拉
                            marginLayoutParamspro.topMargin = 50;
                            linearLayout.setLayoutParams(marginLayoutParamspro);
                            //显示正在刷新
                            refreshingStatus();
                            //下拉到位释放，启动刷新线程
                            tempThreadRefresh = new Thread() {
                                @Override
                                public void run() {
                                    super.run();
                                    //插入刷新事件
                                    refreshEvent.refresh();
                                    handler.obtainMessage(REFRESHMSG_CODE).sendToTarget();
                                }
                            };
                            tempThreadRefresh.start();
                        }
                        listView.setLayoutParams(marginLayoutParams);
                    }
                    pullFlag = false;
                    returnFlag = false;
                    break;
            }
            //true时listview不能进行滑动,false时能够进行滑动
            return returnFlag;
        }
    }

    public void downStatus() {
        imageArrow.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        imageArrow.setImageResource(R.drawable.arrowdown);
        textViewTip.setText("下拉刷新...");
    }

    public void upStatus() {
        imageArrow.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        imageArrow.setImageResource(R.drawable.arrowup);
        textViewTip.setText("释放刷新...");
    }

    public void refreshingStatus() {
        imageArrow.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        textViewTip.setText("正在刷新...");
    }

    public void setAdapter(ArrayAdapter<String> adapter) {
        this.adapter = adapter;
        listView.setAdapter(adapter);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESHMSG_CODE:
                    //通知下拉刷新
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), "刷新完成", Toast.LENGTH_SHORT).show();
                    refreshFinishFlag = true;
                    //隐藏上边的部分
                    marginLayoutParamspro.topMargin = MARGIN_TOP;
                    linearLayout.setLayoutParams(marginLayoutParamspro);
                    //ListView回到原位
                    marginLayoutParams.topMargin = 0;
                    listView.setLayoutParams(marginLayoutParams);
                    //刷新后，下拉加载重新开始
                    startIndex = 0;
                    endIndex = pageSize;
                    break;
                case LOADMORE_CODE:
                    //通知加载更多
                    adapter.notifyDataSetChanged();
                    listView.removeFooterView(footer);
                    loadFinishFlag = true;
                    break;
            }
        }
    };

    public void setRefreshEvent(CustomerListViewPullDownRefreshAndPageLoad.refreshEvent refreshEvent) {
        this.refreshEvent = refreshEvent;
    }

    public final class ScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            Log.i(TAG, "---->" + scrollState);
            switch (scrollState) {
                case SCROLL_STATE_IDLE:
                    break;
                case SCROLL_STATE_TOUCH_SCROLL:
                    break;
                case SCROLL_STATE_FLING:
                    break;
            }
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            //获取屏幕最后Item的ID
            int lastVisibleItem = listView.getLastVisiblePosition();
            if (lastVisibleItem + 1 == totalItemCount) {
                if (loadFinishFlag && refreshFinishFlag) {
                    //标志位，防止多次加载
                    loadFinishFlag = false;
                    listView.addFooterView(footer);
                    //开线程加载数据
                    tempThreadLoadMore = new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            startIndex += pageSize;
                            endIndex += pageSize;
                            //分页加载
                            if (refreshEvent != null) {
                                refreshEvent.loadMore(startIndex, endIndex);
                            }
                            Message message = handler.obtainMessage(LOADMORE_CODE);
                            message.sendToTarget();
                        }
                    };
                    tempThreadLoadMore.start();
                }
            }
        }
    }

    class MyItemClick implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(refreshFinishFlag&& loadFinishFlag){
                refreshEvent.onItemClick(parent, view, position, id);
            }
        }
    }

}
