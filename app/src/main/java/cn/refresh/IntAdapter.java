package cn.refresh;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class IntAdapter extends RecyclerView.Adapter {
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = new TextView(viewGroup.getContext());
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120);
        view.setLayoutParams(layoutParams);
        return new IntViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
        ((TextView) viewHolder.itemView).setText(i + "@@@" + i + "@@@" + i);
    }

    @Override
    public int getItemCount() {
        return 120;
    }

    private static class IntViewHolder extends RecyclerView.ViewHolder {

        public IntViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
