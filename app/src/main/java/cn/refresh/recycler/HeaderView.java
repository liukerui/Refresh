package cn.refresh.recycler;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cn.refresh.R;
import cn.refresh.ScreenUtils;

/**
 * -----------------------------------------------------
 * 项目： CctvVideo-PAD-Main-Project
 * 作者： 刘珂瑞
 * 日期： 2017/6/29 17:16
 * 描述：
 * ------------------------------------------------------
 */
public class HeaderView extends RelativeLayout implements RefreshRecyclerView.State {

    static final int FLIP_ANIMATION_DURATION = 150;

    private boolean up = false;
    private boolean down = false;

    private final Animation mRotateAnimation, mResetRotateAnimation;

    protected final ImageView mHeaderImage;
    protected final ProgressBar mHeaderProgress;
    private final TextView mHeaderText;
    private final TextView mSubHeaderText;

    public HeaderView(Context context) {
        super(context);
        setBackgroundColor(Color.RED);
        LayoutInflater.from(context).inflate(R.layout.view_recycler_refresh_header, this);
        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        setPadding(0, ScreenUtils.dp2px(context, -50), 0, 0);

        mHeaderText = findViewById(R.id.pull_to_refresh_text);
        mHeaderProgress = findViewById(R.id.pull_to_refresh_progress);
        mSubHeaderText = findViewById(R.id.pull_to_refresh_sub_text);
        mHeaderImage = findViewById(R.id.pull_to_refresh_image);

        mRotateAnimation = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mRotateAnimation.setDuration(FLIP_ANIMATION_DURATION);
        mRotateAnimation.setFillAfter(true);

        mResetRotateAnimation = new RotateAnimation(-180, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        mResetRotateAnimation.setDuration(FLIP_ANIMATION_DURATION);
        mResetRotateAnimation.setFillAfter(true);
    }

    @Override
    public int getViewHeight() {
        return ScreenUtils.dp2px(this.getContext(), 50);
    }

    @Override
    public void onMove(float dragPercent) {

        if (dragPercent < 1 && !up) {
            up = true;
            down = false;

            mHeaderImage.setVisibility(VISIBLE);
            mHeaderProgress.setVisibility(GONE);
            mHeaderImage.startAnimation(mResetRotateAnimation);

            mHeaderText.setVisibility(VISIBLE);
            mHeaderText.setText("下拉刷新");

            mSubHeaderText.setVisibility(GONE);

        }
        if (dragPercent >= 1 && !down) {
            up = false;
            down = true;

            mHeaderImage.setVisibility(VISIBLE);
            mHeaderProgress.setVisibility(GONE);
            mHeaderImage.startAnimation(mRotateAnimation);

            mHeaderText.setVisibility(VISIBLE);
            mHeaderText.setText("释放立即刷新");

            mSubHeaderText.setVisibility(GONE);
        }
    }

    @Override
    public void onRefreshing() {
        mHeaderImage.clearAnimation();
        mHeaderImage.setVisibility(GONE);
        mHeaderProgress.setVisibility(VISIBLE);

        mHeaderText.setVisibility(VISIBLE);
        mHeaderText.setText("正在刷新");

        mSubHeaderText.setVisibility(GONE);
    }

    @Override
    public void onReset() {
        up = false;
        down = false;
    }
}
