package cn.refresh.recycler;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.AttributeSet;
import android.view.View;

/**
 * -----------------------------------------------------
 * 项目： CctvVideo-PAD-Main-Project
 * 作者： 刘珂瑞
 * 日期： 2017/2/6 10:41
 * 描述：
 * ------------------------------------------------------
 */
public class RecyclerViewEx extends RecyclerView {

    private boolean mEnableHeader = false;
    private boolean mEnableFooter = false;

    OnLoadMoreListener mListener;
    private boolean mLoadingMore = false;
    private boolean mLoadMoreFail = false;
    private boolean mHasMore = true;

    private View mFooterView;
    private View mHeaderView;
    private HeaderViewRecyclerAdapter mHeaderAdapter;
    private RecyclerView.Adapter mAdapter;
    private HeaderSpanSizeLookup mHeaderSpanSizeLookup;

    private OnScrollListener mOnScrollListener;

    private AdapterDataObserver mAdapterDataObserver = new AdapterDataObserver() {

        @Override
        public void onChanged() {
            mHeaderAdapter.notifyDataSetChanged();
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount) {
            mHeaderAdapter.notifyItemRangeChanged(positionStart + mHeaderAdapter.getHeadersCount(), itemCount);
        }

        @Override
        public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
            mHeaderAdapter.notifyItemRangeChanged(positionStart + mHeaderAdapter.getHeadersCount(), itemCount, payload);
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            mHeaderAdapter.notifyItemRangeInserted(positionStart + mHeaderAdapter.getHeadersCount(), itemCount);
        }

        @Override
        public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
            mHeaderAdapter.notifyItemMoved(fromPosition, toPosition);
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            mHeaderAdapter.notifyItemRangeRemoved(positionStart + mHeaderAdapter.getHeadersCount(), itemCount);
        }
    };

    @Override
    public ViewHolder findViewHolderForAdapterPosition(int position) {
        return super.findViewHolderForAdapterPosition(position + getHeaderCount());
    }

    @Override
    public void smoothScrollToPosition(int position) {
        super.smoothScrollToPosition(position + getHeaderCount());
    }

    public RecyclerViewEx(Context context) {
        this(context, null);
    }

    public RecyclerViewEx(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RecyclerViewEx(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setLayoutManager(LayoutManager layout) {
        super.setLayoutManager(layout);
        if (layout instanceof GridLayoutManager) {
            int spanCount = ((GridLayoutManager) layout).getSpanCount();
            GridLayoutManager.SpanSizeLookup spanSizeLookup = ((GridLayoutManager) layout).getSpanSizeLookup();
            mHeaderSpanSizeLookup = new HeaderSpanSizeLookup(mHeaderView, mFooterView, spanCount, spanSizeLookup, mAdapter);
            ((GridLayoutManager) layout).setSpanSizeLookup(mHeaderSpanSizeLookup);
        }
    }

    @Override
    public void setAdapter(Adapter adapter) {
        mAdapter = adapter;
        mHeaderAdapter = new HeaderViewRecyclerAdapter(mHeaderView, mFooterView, mAdapter);
        adapter.registerAdapterDataObserver(mAdapterDataObserver);
        if (mHeaderSpanSizeLookup != null) {
            mHeaderSpanSizeLookup.setHeaderView(mHeaderView).setFooterView(mFooterView).setAdapter(mAdapter);
        }
        super.setAdapter(mHeaderAdapter);
    }

    public void setEnableHeader(boolean enable) {
        if (enable) {
            mHeaderView = new HeaderView(getContext());
        }
    }

    public void setEnableFooter(boolean enable) {
        if (enable) {
            mFooterView = new FooterView(getContext());
            mOnScrollListener = new OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                }

                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (canLoad()) {
                        mLoadingMore = true;
                        if (mListener != null) {
                            mListener.onLoadMore();
                        }
                    }
                }
            };
            addOnScrollListener(mOnScrollListener);
        }
    }

    public View getHeaderView() {
        if (getAdapter() instanceof HeaderViewRecyclerAdapter) {
            return ((HeaderViewRecyclerAdapter) getAdapter()).getHeaderView();
        }
        return null;
    }

    public int getHeaderCount() {
        if (getAdapter() instanceof HeaderViewRecyclerAdapter) {
            return ((HeaderViewRecyclerAdapter) getAdapter()).getHeadersCount();
        }
        return 0;
    }

    public void updateFooterView(boolean hasMore, boolean loadMoreFail) {
        if (getAdapter() instanceof HeaderViewRecyclerAdapter) {
            View view = ((HeaderViewRecyclerAdapter) getAdapter()).getFooterView();
            if (view instanceof FooterView) {
                ((FooterView) view).update(hasMore, loadMoreFail);
            }
        }
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mListener = listener;
    }

    public void setLoadMoreFail(boolean fail) {
        mLoadMoreFail = fail;
        setHasMore(mHasMore, mLoadingMore);
    }

    public void setHasMore(boolean hasMore, boolean fail) {
        mHasMore = hasMore;
        mLoadMoreFail = fail;
        if (getAdapter() instanceof HeaderViewRecyclerAdapter) {
            View footerView = ((HeaderViewRecyclerAdapter) getAdapter()).getFooterView();
            if (footerView instanceof FooterView) {
                ((FooterView) footerView).update(hasMore, fail);
            }
        }

        mLoadingMore = false;
    }

    /**
     * 是否可以加载更多, 条件是到了最底部
     *
     * @return isCanLoad
     */
    private boolean canLoad() {
        return isScrollBottom() && !mLoadingMore && !mLoadMoreFail && mHasMore;
    }

    /**
     * 判断是否到了最底部
     */
    private boolean isScrollBottom() {
        return (this != null && this.getAdapter() != null)
                && getLastVisiblePosition() == (this.getAdapter().getItemCount() - 1);
    }

    /**
     * 获取RecyclerView可见的最后一项
     *
     * @return 可见的最后一项position
     */
    public int getLastVisiblePosition() {
        int position;
        if (this.getLayoutManager() instanceof LinearLayoutManager) {
            position = ((LinearLayoutManager) this.getLayoutManager()).findLastVisibleItemPosition();
        } else if (this.getLayoutManager() instanceof GridLayoutManager) {
            position = ((GridLayoutManager) this.getLayoutManager()).findLastVisibleItemPosition();
        } else if (this.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) this.getLayoutManager();
            int[] lastPositions = layoutManager.findLastVisibleItemPositions(new int[layoutManager.getSpanCount()]);
            position = getMaxPosition(lastPositions);
        } else {
            position = this.getLayoutManager().getItemCount() - 1;
        }
        return position;
    }

    /**
     * 获得最大的位置
     *
     * @param positions 获得最大的位置
     * @return 获得最大的位置
     */
    private int getMaxPosition(int[] positions) {
        int maxPosition = Integer.MIN_VALUE;
        for (int position : positions) {
            maxPosition = Math.max(maxPosition, position);
        }
        return maxPosition;
    }

    public interface OnLoadMoreListener {

        void onLoadMore();
    }

}