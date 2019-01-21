package cn.refresh;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;

import cn.refresh.recycler2.RefreshRecyclerViewEx;

public class MainActivity2 extends AppCompatActivity {


    RefreshRecyclerViewEx mRefreshRecyclerView;
    IntAdapter mIntAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        mRefreshRecyclerView = findViewById(R.id.refresh_recycler_view);
        mRefreshRecyclerView.setEnableHeader(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRefreshRecyclerView.setLayoutManager(layoutManager);
        mIntAdapter = new IntAdapter();
        mRefreshRecyclerView.setAdapter(mIntAdapter);
        mRefreshRecyclerView.setOnRefreshListener(new RefreshRecyclerViewEx.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshRecyclerView.setRefreshing(false);
                        mIntAdapter.notifyDataSetChanged();
                    }
                }, 3500);
            }
        });
    }
}
