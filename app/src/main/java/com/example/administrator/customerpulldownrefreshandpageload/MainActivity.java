package com.example.administrator.customerpulldownrefreshandpageload;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements CustomerListViewPullDownRefreshAndPageLoad.refreshEvent {
    private final String TAG = MainActivity.class.getSimpleName();
    private CustomerListViewPullDownRefreshAndPageLoad customerListViewPullDownRefreshAndPageLoad;
    private List<String> data;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        customerListViewPullDownRefreshAndPageLoad = (CustomerListViewPullDownRefreshAndPageLoad) this.findViewById(R.id.customListView);
        data = new ArrayList<String>();
        for (int i = 0; i < 30; i++) {
            data.add("测试数据" + i);
        }
        adapter = new ArrayAdapter<String>(this, R.layout.simple_list, R.id.simple_list_textview, data);
        customerListViewPullDownRefreshAndPageLoad.setAdapter(adapter);
        customerListViewPullDownRefreshAndPageLoad.setRefreshEvent(this);
    }

    @Override
    public void refresh() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        data.clear();
        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            data.add("测试数据" + String.valueOf(random.nextInt(100) + 1));
        }
    }


    @Override
    public void loadMore(int startIndex, int endIndex) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        data.addAll(getDataService(startIndex,endIndex));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Toast.makeText(MainActivity.this, "position=" + position + " id=" + id, Toast.LENGTH_SHORT).show();
    }

    /**
     * 模拟加载数据
     *
     * @param from
     * @param to
     * @return
     */
    public List<String> getDataService(int from, int to) {
        Log.i(TAG,"----start " + from + " end" + to);
        List<String> resList = new ArrayList<>();
        for (int i = from; i < to; i++) {
            resList.add("测试数据" + i);
        }
        return resList;
    }

    @Override
    protected void onDestroy() {
        customerListViewPullDownRefreshAndPageLoad.tempThreadRefresh = null;
        customerListViewPullDownRefreshAndPageLoad.tempThreadLoadMore = null;
        super.onDestroy();
    }
}
