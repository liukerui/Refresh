package cn.refresh.recycler;

import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class HeaderSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {

    private View mHeaderView;
    private View mFooterView;
    private int mSpanCount;
    private GridLayoutManager.SpanSizeLookup mSpanSizeLookup;
    private RecyclerView.Adapter mAdapter;

    public HeaderSpanSizeLookup(View headerView, View footerView, int spanCount, GridLayoutManager.SpanSizeLookup spanSizeLookup, RecyclerView.Adapter adapter) {
        mHeaderView = headerView;
        mFooterView = footerView;
        mSpanCount = spanCount;
        mSpanSizeLookup = spanSizeLookup;
        mAdapter = adapter;
    }

    public HeaderSpanSizeLookup setHeaderView(View headerView) {
        this.mHeaderView = headerView;
        return this;
    }

    public HeaderSpanSizeLookup setFooterView(View footerView) {
        this.mFooterView = footerView;
        return this;
    }

    public HeaderSpanSizeLookup setAdapter(RecyclerView.Adapter adapter) {
        this.mAdapter = adapter;
        return this;
    }

    @Override
    public int getSpanSize(int position) {
        int numHeaders = getHeadersCount();
        if (position < numHeaders) {
            return mSpanCount;
        }
        // Adapter
        final int adjPosition = position - numHeaders;
        if (mAdapter != null) {
            int adapterCount = mAdapter.getItemCount();
            if (adjPosition < adapterCount) {
                if (mSpanSizeLookup != null) {
                    return mSpanSizeLookup.getSpanSize(adjPosition);
                }
            }
        }
        return mSpanCount;
    }

    private int getHeadersCount() {
        return mHeaderView == null ? 0 : 1;
    }

    private int getFootersCount() {
        return mFooterView == null ? 0 : 1;
    }
}
