package com.hypertrack.ridesharing.parsers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.hypertrack.ridesharing.models.Order;

import java.util.ArrayList;
import java.util.List;

public class OrderParser extends Parser<Order> {

    @NonNull
    @Override
    public List<Order> parse(QuerySnapshot querySnapshot) {
        List<Order> orders = new ArrayList<>();
        if (querySnapshot != null) {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                orders.add(parse(doc));
            }
        }
        return orders;
    }

    @Nullable
    @Override
    public Order parse(DocumentSnapshot doc) {
        if (doc.getData() != null) {
            Order order = mapper.convertValue(doc.getData(), Order.class);
            order.id = doc.getId();
            return order;
        }
        return null;
    }
}
