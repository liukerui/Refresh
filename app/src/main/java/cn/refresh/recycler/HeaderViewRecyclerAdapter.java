package cn.refresh.recycler;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

public class HeaderViewRecyclerAdapter extends RecyclerView.Adapter {

    public static final int SHOW_HEADER = -1;
    public static final int SHOW_FOOTER = -2;
    private View mHeaderView;
    private View mFooterView;
    private RecyclerView.Adapter mAdapter;

    public HeaderViewRecyclerAdapter(View headerView, View footerView, RecyclerView.Adapter adapter) {
        mHeaderView = headerView;
        mFooterView = footerView;
        mAdapter = adapter;
    }

    @Override
    public int getItemViewType(int position) {
        //header
        int numHeaders = getHeadersCount();
        if (position < numHeaders) {
            return SHOW_HEADER;
        }
        // Adapter
        final int adjPosition = position - numHeaders;
        int adapterCount = 0;
        if (mAdapter != null) {
            adapterCount = mAdapter.getItemCount();
            if (adjPosition < adapterCount) {
                return mAdapter.getItemViewType(adjPosition);
            }
        }
        Log.e("lkr", "1234432333");
        // Footer (off-limits positions will throw an IndexOutOfBoundsException)
        return SHOW_FOOTER;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == SHOW_HEADER) {
            return new HeaderViewHolder(mHeaderView);
        }
        if (viewType == SHOW_FOOTER) {
            return new FooterViewHolder(mFooterView);
        }
        return mAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        //也要划分三个区域
        int numHeaders = getHeadersCount();
        if (position < numHeaders) {//是头部
            return;
        }
        //adapter body
        final int adjPosition = position - numHeaders;
        int adapterCount = 0;
        if (mAdapter != null) {
            adapterCount = mAdapter.getItemCount();
            if (adjPosition < adapterCount) {
                mAdapter.onBindViewHolder(holder, adjPosition);
                return;
            }
        }
        //footer
    }

    @Override
    public int getItemCount() {
        if (mAdapter != null) {
            return getFootersCount() + getHeadersCount() + mAdapter.getItemCount();
        } else {
            return getFootersCount() + getHeadersCount();
        }
    }

    public int getHeadersCount() {
        return mHeaderView == null ? 0 : 1;
    }

    public View getHeaderView() {
        return mHeaderView;
    }

    public View getFooterView() {
        return mFooterView;
    }

    public int getFootersCount() {
        return mFooterView == null ? 0 : 1;
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    class FooterViewHolder extends RecyclerView.ViewHolder {

        public FooterViewHolder(View itemView) {
            super(itemView);
        }
    }
}