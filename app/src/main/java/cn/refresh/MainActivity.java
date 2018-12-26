package cn.refresh;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;

import cn.refresh.recycler.RecyclerViewEx;
import cn.refresh.recycler.RefreshRecyclerView;

public class MainActivity extends AppCompatActivity {


    RefreshRecyclerView mRefreshRecyclerView;
    RecyclerViewEx mRecyclerViewEx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRefreshRecyclerView = findViewById(R.id.refresh_recycler_view);
        mRecyclerViewEx = mRefreshRecyclerView.getRecyclerViewEx();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerViewEx.setLayoutManager(layoutManager);
        mRecyclerViewEx.setAdapter(new IntAdapter());
        mRefreshRecyclerView.setOnRefreshListener(new RefreshRecyclerView.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                         mRefreshRecyclerView.onRefreshComplete();
                    }
                }, 2000);
            }
        });
    }
}
