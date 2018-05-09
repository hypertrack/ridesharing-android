package com.hypertrack.uber_consumer.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.TrafficModel;
import com.google.maps.model.TravelMode;
import com.hypertrack.lib.MapFragmentCallback;
import com.hypertrack.lib.internal.consumer.utils.HTMapUtils;
import com.hypertrack.lib.models.Place;
import com.hypertrack.lib.tracking.MapProvider.HyperTrackMapFragment;
import com.hypertrack.lib.tracking.basemvp.BaseView;
import com.hypertrack.lib.tracking.model.MarkerModel;
import com.hypertrack.uber_consumer.R;
import com.hypertrack.uber_consumer.base.MyHyperTrackMapAdapter;
import com.hypertrack.uber_consumer.models.Address;
import com.hypertrack.uber_consumer.models.Coordinate;
import com.hypertrack.uber_consumer.models.StatusEnum;
import com.hypertrack.uber_consumer.models.Trip;
import com.hypertrack.uber_consumer.models.User;
import com.hypertrack.uber_consumer.utils.Constants;
import com.hypertrack.uber_consumer.utils.SharedValues;
import com.hypertrack.uber_consumer.utils.Utils;
import com.hypertrack.uber_consumer.widgets.DetailsPanelView;

import org.joda.time.DateTime;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by pkharche on 19/04/18.
 */
public class BookRideActivity extends BaseActivity {

    private HyperTrackMapFragment mHyperTrackMapFragment;
    private MyHyperTrackMapAdapter mHyperTrackMapAdapter;
    private BaseView mBaseView;
    private DirectionsResult mDirectionsResult = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_ride);
        mCoordinatorLayout = findViewById(R.id.coordinator_layout);

        getDirectionsApiResult();
        setUpHyperTrackBottomView();
    }

    //HyperTrack
    private void setUpHyperTrackBottomView() {
        mHyperTrackMapFragment = (HyperTrackMapFragment) getSupportFragmentManager().findFragmentById(R.id.htMapfragment);
        mHyperTrackMapFragment.setMapStyle(R.raw.mapstyle_uberx);

        mHyperTrackMapAdapter = new MyHyperTrackMapAdapter(mContext);
        mHyperTrackMapFragment.setMapAdapter(mHyperTrackMapAdapter);
        mBaseView = DetailsPanelView.getInstance(); //This is the view which will be shown as bottom view
        mHyperTrackMapFragment.setUseCase(mBaseView);

        mHyperTrackMapFragment.setMapCallback(new MapFragmentCallback() {
            @Override
            public void onBottomBaseViewCreated(int useCaseType) {
                super.onBottomBaseViewCreated(useCaseType);
                setAndFillDataInBottomView();
                if(mBaseView != null) {
                    mBaseView.hideCTAButton();
                }
            }

            @Override
            public void onMapLoadedCallback(Context context, GoogleMap map) {
                super.onMapLoadedCallback(context, map);

                addMarkers();
                addPolyline();
            }
        });
    }

    private void addMarkers() {
        //DROP OFF
        String dropOffStr = SharedValues.getValue(mContext, Constants.DROPOFF_PLACE);
        Place dropOffPlace = new Gson().fromJson(dropOffStr, Place.class);
        com.google.android.gms.maps.model.LatLng dropOffLatLng = new com.google.android.gms.maps.model.LatLng(dropOffPlace.getLocation().getLatitude(), dropOffPlace.getLocation().getLongitude());

        MarkerModel dropOffMarker = new MarkerModel(dropOffLatLng, R.drawable.icondestination_marker, MarkerModel.Type.CUSTOM);
        mHyperTrackMapFragment.addOrUpdateMarker(dropOffMarker);

        String place = dropOffPlace.getAddress();

        Date now = new Date();
        now.setTime(now.getTime() + (getTimeInSecs()*1000));
        String time = getFormattedTime(now.getTime());

        View view = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_info_box, null);
        ((TextView) view.findViewById(R.id.tv_info_title)).setText(place);
        //((TextView) view.findViewById(R.id.tv_info_details)).setText(time);
        ((TextView) view.findViewById(R.id.tv_info_details)).setVisibility(View.GONE);

        MarkerModel markerModel = new MarkerModel(dropOffLatLng, -1, MarkerModel.Type.CUSTOM_INFO);
        markerModel.setView(view);
        mHyperTrackMapFragment.addCustomMarker(markerModel);


        //PICK UP
        String pickUpStr = SharedValues.getValue(mContext, Constants.PICKUP_PLACE);
        Place pickUpPlace = new Gson().fromJson(pickUpStr, Place.class);
        com.google.android.gms.maps.model.LatLng pickUpLatLng = new com.google.android.gms.maps.model.LatLng(pickUpPlace.getLocation().getLatitude(), pickUpPlace.getLocation().getLongitude());

        MarkerModel pickUpMarker = new MarkerModel(pickUpLatLng, R.drawable.iconsource_marker, MarkerModel.Type.CUSTOM);
        mHyperTrackMapFragment.addOrUpdateMarker(pickUpMarker);

        place = pickUpPlace.getAddress();
        if(TextUtils.isEmpty(place)) {
            place = getAddressFromLocation(pickUpPlace.getLocation().getLatitude(), pickUpPlace.getLocation().getLongitude());
        }
        time = getFormattedTime(System.currentTimeMillis());

        view = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_info_box, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        }

        ((TextView) view.findViewById(R.id.tv_info_title)).setText(place);
        ((TextView) view.findViewById(R.id.tv_info_title)).setTextColor(ContextCompat.getColor(mContext, R.color.text_color));

        ((TextView) view.findViewById(R.id.tv_info_details)).setVisibility(View.GONE);
        //((TextView) view.findViewById(R.id.tv_info_details)).setText(time);
        //((TextView) view.findViewById(R.id.tv_info_details)).setTextColor(ContextCompat.getColor(mContext, R.color.text_color));

        markerModel = new MarkerModel(pickUpLatLng, -1, MarkerModel.Type.CUSTOM_INFO);
        markerModel.setView(view);
        mHyperTrackMapFragment.addCustomMarker(markerModel);
    }

    private GeoApiContext getGeoApiContext() {
        GeoApiContext.Builder builder = new GeoApiContext.Builder();
        builder.apiKey(getString(R.string.google_api_server_key));
        builder.queryRateLimit(3);
        return builder.build();
    }

    //https://android.jlelse.eu/google-maps-directions-api-5b2e11dee9b0
    private void getDirectionsApiResult() {
        DateTime now = new DateTime();
        try {
            String dropOffStr = SharedValues.getValue(mContext, Constants.DROPOFF_PLACE);
            Place dropOffPlace = new Gson().fromJson(dropOffStr, Place.class);
            LatLng dropOffLatLng = new LatLng(dropOffPlace.getLocation().getLatitude(), dropOffPlace.getLocation().getLongitude());

            String pickUpStr = SharedValues.getValue(mContext, Constants.PICKUP_PLACE);
            Place pickUpPlace = new Gson().fromJson(pickUpStr, Place.class);
            LatLng pickUpLatLng = new LatLng(pickUpPlace.getLocation().getLatitude(), pickUpPlace.getLocation().getLongitude());

            mDirectionsResult = DirectionsApi.newRequest(getGeoApiContext())
                    .mode(TravelMode.DRIVING)
                    .origin(pickUpLatLng)
                    .destination(dropOffLatLng)
                    .departureTime(now)
                    .trafficModel(TrafficModel.BEST_GUESS)
                    .await();

            /*try {
                Log.d(Constants.TAG, "mDirectionsResult  " + (new JSONObject(new Gson().toJson(mDirectionsResult)).toString(3)));
            } catch(Exception e) {
                Log.e(Constants.TAG, "mDirectionsResult  ", e);
            }*/

        } catch (ApiException e) {
            Log.e(Constants.TAG, "ApiException" ,e);
        } catch (InterruptedException e) {
            Log.e(Constants.TAG, "InterruptedException" ,e);
        } catch (IOException e) {
            Log.e(Constants.TAG, "IOException" ,e);
        } catch (Exception e) {
        Log.e(Constants.TAG, "Exception" ,e);
    }
    }

    private float getDistanceInMiles() {
        try {
            if (mDirectionsResult != null) {
                        long meters = mDirectionsResult.routes[0].legs[0].distance.inMeters;
                        double inches = (39.370078 * meters);
                        float miles = (float) (inches / 63360);
                        return miles;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            Log.e(Constants.TAG, "getDistanceRequired : " + e.getMessage());
        }
        return -1;
    }

    private String getTimeRequired() {
        //Log.d(Constants.TAG,"getTimeRequired  " +mDirectionsResult);
        try {
            if(mDirectionsResult != null) {
                return mDirectionsResult.routes[0].legs[0].duration.humanReadable;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            Log.e(Constants.TAG, "getTimeRequired : " + e.getMessage());
        }
        return null;
    }

    private long getTimeInSecs() {
        //Log.d(Constants.TAG,"getTimeRequired  " +mDirectionsResult);
        try {
            if(mDirectionsResult != null) {
                return mDirectionsResult.routes[0].legs[0].duration.inSeconds;
            }
        } catch(ArrayIndexOutOfBoundsException e) {
            Log.e(Constants.TAG, "getTimeRequired : " + e.getMessage());
        }
        return -1;
    }

    private void addPolyline() {
        //https://developers.google.com/android/reference/com/google/android/gms/maps/model/Polyline
        try {
        if(mHyperTrackMapFragment != null && mDirectionsResult != null) {
            List<com.google.android.gms.maps.model.LatLng> decodedPath = HTMapUtils.decode(mDirectionsResult.routes[0].overviewPolyline.getEncodedPath());
            mHyperTrackMapFragment.addExpectedRoutePolyline(decodedPath);

            //Get complete polyline is visibility
            mHyperTrackMapFragment.bindMapBound();
        }
    } catch(ArrayIndexOutOfBoundsException e) {
        Log.e(Constants.TAG, "getTimeRequired : " + e.getMessage());
    }
    }

    private void setAndFillDataInBottomView() {
        //check if this is needed as old view is shown
        mBaseView.removeBottomItems();

        final View bookRidePanel = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.bottom_layout_book_your_ride, null);
        mBaseView.addBottomView(bookRidePanel);

        //Get and set data about Distance & Time required between source --> destination
        DecimalFormat df = new DecimalFormat("#.##");
        String formattedDistance = df.format(getDistanceInMiles());

        ((TextView) bookRidePanel.findViewById(R.id.tv_distance)).setText(formattedDistance + " miles");
        ((TextView) bookRidePanel.findViewById(R.id.tv_time)).setText(getTimeRequired());

        if(getDistanceInMiles() > 0) {
            float amount = getDistanceInMiles() * 1; //distance * rate/perMile
            String formattedAmount = df.format(amount);

            ((TextView) bookRidePanel.findViewById(R.id.tv_amount)).setText(String.format(getString(R.string.amount), formattedAmount));
        } else {
            ((TextView) bookRidePanel.findViewById(R.id.tv_amount)).setText(getString(R.string.no_data));
        }

        //Display Destination address
        String dropOffStr = SharedValues.getValue(mContext, Constants.DROPOFF_PLACE);
        if(!TextUtils.isEmpty(dropOffStr)) {
            Place dropOffPlace = new Gson().fromJson(dropOffStr, Place.class);
            if (dropOffPlace != null) {
                ((TextView) bookRidePanel.findViewById(R.id.tv_dropoff_location_text)).setText(dropOffPlace.getAddress());
            }
        }

        bookRidePanel.findViewById(R.id.ll_pickup_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(mContext, GenerateRideActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra(Constants.PICKUP_PLACE, true);
                i.putExtra(Constants.DROPOFF_PLACE, false);
                startActivity(i);
                finish();
            }
        });

        bookRidePanel.findViewById(R.id.ll_dropoff_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //launchActivity(GenerateRideActivity.class);
                Intent i = new Intent(mContext, GenerateRideActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                i.putExtra(Constants.PICKUP_PLACE, false);
                i.putExtra(Constants.DROPOFF_PLACE, true);
                startActivity(i);
                finish();
            }
        });

        bookRidePanel.findViewById(R.id.bt_book_ride).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchUser();

                try {
                    mHyperTrackMapFragment.bindMapBound();
                } catch(Exception e) {
                    Log.e(Constants.TAG, "bindMapBound", e);
                }
            }
        });

        //Display PickUp address
        String pickUpStr = SharedValues.getValue(mContext, Constants.PICKUP_PLACE);
        if(!TextUtils.isEmpty(pickUpStr)) {
            Place pickUpPlace = new Gson().fromJson(pickUpStr, Place.class);
            if (pickUpPlace != null && pickUpPlace.getLocation() != null) {
                ((TextView) bookRidePanel.findViewById(R.id.tv_pickup_location_text)).setText(getAddressFromLocation(pickUpPlace.getLocation().getLatitude(), pickUpPlace.getLocation().getLongitude()));
            }
        }
    }

    private void showLoaderBottomView() {
        //check if this is needed as old view is shown
        mBaseView.removeBottomView();

        final View findDriverPanel = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.bottom_layout_find_driver, null);
        mBaseView.addBottomView(findDriverPanel);

    }

    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = null;
        String address = null;
        try {
            geocoder = new Geocoder(mContext, Locale.getDefault());

            List<android.location.Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            address = addressList.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
            /*String city = addressList.get(0).getLocality();
            String state = addressList.get(0).getAdminArea();
            String country = addressList.get(0).getCountryName();
            String postalCode = addressList.get(0).getPostalCode();
            String knownName = addressList.get(0).getFeatureName(); // Only if available else return NULL*/

            //Log.d(Constants.TAG, "Pickup Address: " + address);

        } catch (Exception e) {
            Log.e(Constants.TAG, "getAddressFromLocation :: (" +latitude+" , " + longitude +")" + e.getMessage());
        }

        return address;
    }

    private void searchUser() {
        //Utils.showProgressDialog(mActivity, false);

        String userId = SharedValues.getValue(mContext, Constants.USER_ID);
        Log.d(Constants.TAG,"searchUser: BookRide:  userId " +userId);

        if(!TextUtils.isEmpty(userId)) {
            final FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference ridesRef = database.getReference(Constants.FIREBASE_USERS);
            Query query = ridesRef.child(userId);

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Utils.hideProgressDialog();
                    User user = dataSnapshot.getValue(User.class);

                    if (user != null) {
                        bookRide(user);

                    } else {
                        Utils.hideProgressDialog();
                        logOutFirebaseUser();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e(Constants.TAG, "onCancelled", databaseError.toException());
                    showSnackBar("Firebase Error finding user details: " + databaseError.getMessage());
                    Utils.hideProgressDialog();
                }
            });
        } else {
            showSnackBar("No User available");
        }
    }

    private void bookRide(User user) {
        //Utils.showProgressDialog(mActivity, false);

        Trip trip = new Trip();

        //Set User object
        trip.setUser(user);

        //Set Pick up object
        Address pickUpAddress = new Address();

        String pickUpStr = SharedValues.getValue(mContext, Constants.PICKUP_PLACE);
        if(!TextUtils.isEmpty(pickUpStr)) {
            Place pickUpPlace = new Gson().fromJson(pickUpStr, Place.class);
            if (pickUpPlace != null && pickUpPlace.getLocation() != null) {

                String address = getAddressFromLocation(pickUpPlace.getLocation().getLatitude(), pickUpPlace.getLocation().getLongitude());
                Log.d(Constants.TAG, "Book Ride: Pick up address:  " +address);
                pickUpAddress.setDisplayAddress(address);

                Coordinate pickUpCoordinate = new Coordinate();
                pickUpCoordinate.setLatitude(pickUpPlace.getLocation().getLatitude());
                pickUpCoordinate.setLongitude(pickUpPlace.getLocation().getLongitude());
                pickUpAddress.setCoordinate(pickUpCoordinate);
                trip.setPickup(pickUpAddress);
            }
        }

        //Set Drop off object
        String dropOffStr = SharedValues.getValue(mContext, Constants.DROPOFF_PLACE);
        Place dropOffPlace = new Gson().fromJson(dropOffStr, Place.class);

        Address dropOffAddress = new Address();
        dropOffAddress.setDisplayAddress(dropOffPlace.getAddress());
        Log.d(Constants.TAG, "Book Ride: Drop off address:  " + dropOffPlace.getAddress());

        Coordinate dropOffCoordinate = new Coordinate();
        dropOffCoordinate.setLatitude(dropOffPlace.getLocation().getLatitude());
        dropOffCoordinate.setLongitude(dropOffPlace.getLocation().getLongitude());
        dropOffAddress.setCoordinate(dropOffCoordinate);
        trip.setDrop(dropOffAddress);

        trip.setStatus(StatusEnum.trip_not_started);

        updateFireBase(trip);
    }

    private void updateFireBase(Trip trip) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference tripRef = database.getReference(Constants.FIREBASE_TRIPS);

        final String key = tripRef.push().getKey();
        trip.setId(key);
        Log.d(Constants.TAG, "Book Ride :: key:  " + key);

        tripRef.child(key).setValue(trip, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                Utils.hideProgressDialog();

                if (databaseError != null) {
                    Log.d(Constants.TAG,"Trip could not be saved on Firebase" + databaseError.getMessage());

                } else {
                    Log.d(Constants.TAG,"Trip saved successfully on Firebase");
                    SharedValues.saveValue(mContext, Constants.TRIP_ID, key);
                    //updateUser(); //TODO :Assigned rideId in user for uninstall re-install cases
                    waitForDriverToBeAssigned();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        String rideId = SharedValues.getValue(mContext, Constants.TRIP_ID);
        Log.d(Constants.TAG, "BookRide :: OnResume :  " +rideId);

        //Check if on-going ride is present
        if (!TextUtils.isEmpty(rideId)) {
            getRide();
        }
    }

    private void getRide() {
        Utils.showProgressDialog(mActivity, false);
        String rideId = SharedValues.getValue(mContext, Constants.TRIP_ID);
        Log.d(Constants.TAG,"BookRide:  getRide: rideId:  " +rideId);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ridesRef = database.getReference().child(Constants.FIREBASE_TRIPS).child(rideId);

        ridesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Utils.hideProgressDialog();

                Trip trip = dataSnapshot.getValue(Trip.class);
                Log.d(Constants.TAG, "BookRide:  getRide: id:  " + trip.getId() + "  status:  " + trip.getStatus());

                switch (trip.getStatus()) {
                    case trip_not_started:
                        waitForDriverToBeAssigned();
                        break;

                    case trip_assigned:
                        SharedValues.saveValue(mContext, Constants.TRIP_IS_ACCEPTED, "true");
                        break;

                    case started_to_pick_up_customer:
                        launchActivity(TrackRideActivity.class);
                        break;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(Constants.TAG, "onCancelled", databaseError.toException());
                showSnackBar("Firebase Error finding Trip to Start : " + databaseError.getMessage());
                Utils.hideProgressDialog();
            }
        });
    }

    private void waitForDriverToBeAssigned() {
        showLoaderBottomView();//show bottom connecting to driver view

        //Utils.showProgressDialog(mActivity, false);

        String rideId = SharedValues.getValue(mContext, Constants.TRIP_ID);
        Log.d(Constants.TAG,"StartRide:  getRide: rideId:  " +rideId);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ridesRef = database.getReference().child(Constants.FIREBASE_TRIPS).child(rideId);

        ridesRef.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                Log.d(Constants.TAG,"BookRide: onChildChanged:  " +dataSnapshot);

                if(dataSnapshot != null && dataSnapshot.getKey() != null && dataSnapshot.getKey().equals("status")) {
                    StatusEnum status = StatusEnum.valueOf((String) dataSnapshot.getValue());
                    switch (status) {
                        case trip_assigned:
                            SharedValues.saveValue(mContext, Constants.TRIP_IS_ACCEPTED, "true");
                            break;

                        case started_to_pick_up_customer:
                            launchActivity(TrackRideActivity.class);
                            break;
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }
}
