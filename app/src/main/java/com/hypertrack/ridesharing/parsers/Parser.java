package com.hypertrack.ridesharing.parsers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public abstract class Parser<T> {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    final SimpleDateFormat dateFormat;

    ObjectMapper mapper = new ObjectMapper();

    public Parser() {
        dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
    }

    @NonNull
    abstract List<T> parse(QuerySnapshot querySnapshot);

    @Nullable
    abstract T parse(DocumentSnapshot doc);
}
