package com.hypertrack.ridesharing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.hypertrack.ridesharing.models.Order;
import com.hypertrack.ridesharing.models.User;

import java.util.Date;
import java.util.Map;

public class FirebaseFirestoreApi {

    public static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static Task<DocumentReference> createUser(User user) {
        ObjectMapper mapper = new ObjectMapper();
        Map data = mapper.convertValue(user, Map.class);
        data.remove("id");
        return db.collection("users").add(data);
    }

    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public static Task<Void> updateUser(User user) {
        ObjectMapper mapper = new ObjectMapper();
        Map data = mapper.convertValue(user, Map.class);
        data.remove("id");
        return db.collection("users").document(user.id).update(data);
    }

    public static Task<DocumentReference> createOrder(Order order) {
        order.status = "NEW";
        order.createdAt = new Date();
        order.updatedAt = new Date();
        ObjectMapper mapper = new ObjectMapper();
        Map data = mapper.convertValue(order, Map.class);
        data.remove("id");
        return db.collection("orders").add(data);
    }
}
