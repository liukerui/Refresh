package cn.refresh.recycler;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import cn.refresh.R;

public class FooterView extends LinearLayout {

    public TextView text;
    public ProgressBar progress;

    public FooterView(Context context) {
        this(context, null);
    }

    public FooterView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FooterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setGravity(Gravity.CENTER);
        this.setOrientation(HORIZONTAL);
        LayoutInflater.from(context).inflate(R.layout.view_recycler_loading_more, this);
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 72));

        text = findViewById(R.id.text);
        progress = findViewById(R.id.progress);

        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, 30);

        ViewGroup.LayoutParams progressParams = progress.getLayoutParams();
        int wh = 40;
        progressParams.width = wh;
        progressParams.height = wh;
    }

    public void update(boolean mHasMore, boolean mLoadMoreFail) {
        if (!mHasMore) {
            progress.setVisibility(GONE);
            text.setText("没有更多了");
        } else if (mLoadMoreFail) {
            progress.setVisibility(GONE);
            text.setText("点击加载更多");
        } else {
            progress.setVisibility(VISIBLE);
            text.setText("正在加载更多的数据...");
        }
    }
}
