package com.hypertrack.ridesharing.parsers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.hypertrack.ridesharing.models.User;

import java.util.ArrayList;
import java.util.List;

public class UserParser extends Parser<User> {

    @NonNull
    @Override
    public List<User> parse(QuerySnapshot querySnapshot) {
        List<User> users = new ArrayList<>();
        if (querySnapshot != null) {
            for (QueryDocumentSnapshot doc : querySnapshot) {
                users.add(parse(doc));
            }
        }
        return users;
    }

    @Nullable
    @Override
    public User parse(DocumentSnapshot doc) {
        if (doc.getData() != null) {
            User user = mapper.convertValue(doc.getData(), User.class);
            user.id = doc.getId();
            return user;
        }
        return null;
    }
}
