package com.hypertrack.ridesharing.adapters;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.hypertrack.ridesharing.R;
import com.hypertrack.ridesharing.models.Order;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.MyViewHolder> {
    private List<Order> orders = new ArrayList<>();
    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView address;
        public TextView date;
        public View close;

        public MyViewHolder(View v) {
            super(v);
            address = v.findViewById(R.id.address);
            date = v.findViewById(R.id.date);
            close = v.findViewById(R.id.close);
        }
    }

    @Override
    public OrdersAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                         int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_item, parent, false);
        MyViewHolder vh = new MyViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final Order order = orders.get(position);

        holder.address.setText(order.pickup.address);
        String date = DateUtils.getRelativeDateTimeString(holder.itemView.getContext(), order.createdAt.getTime(),
                TimeUnit.MINUTES.toMillis(1), TimeUnit.DAYS.toMillis(7),
                DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE |
                        DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_WEEKDAY |
                        DateUtils.FORMAT_ABBREV_ALL).toString();
        holder.date.setText(date);
        holder.close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                orders.remove(position);
                notifyDataSetChanged();
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(OrdersAdapter.this, view, position);
                }
                orders.remove(position);
                notifyDataSetChanged();
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return orders.size();
    }

    public Order getOrder(int position) {
        return orders.isEmpty() ? null : orders.get(position);
    }

    public void addAll(Collection<Order> items) {
        List<Order> newList = new ArrayList<>(orders);
        newList.addAll(items);
        Collections.sort(newList, new Comparator<Order>() {
            @Override
            public int compare(Order order1, Order order2) {
                return order2.createdAt.compareTo(order1.createdAt);
            }
        });
        orders.clear();
        if (newList.size() > 5) {
            orders.addAll(newList.subList(0, 5));
        } else {
            orders.addAll(newList);
        }
    }

    public void clear() {
        orders.clear();
    }

    public interface OnItemClickListener {
        void onItemClick(RecyclerView.Adapter<?> adapter, View view, int position);
    }
}