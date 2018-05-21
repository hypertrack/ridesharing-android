package com.hypertrack.uber_consumer.activities;

import android.content.Context;
import android.content.Intent;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.gson.Gson;
import com.hypertrack.lib.HyperTrack;
import com.hypertrack.lib.MapFragmentCallback;
import com.hypertrack.lib.callbacks.HyperTrackCallback;
import com.hypertrack.lib.internal.consumer.view.PlaceSelector.PlaceSelector;
import com.hypertrack.lib.models.ErrorResponse;
import com.hypertrack.lib.models.Place;
import com.hypertrack.lib.models.SuccessResponse;
import com.hypertrack.lib.tracking.MapProvider.HyperTrackMapFragment;
import com.hypertrack.lib.tracking.MapProvider.MapFragmentView;
import com.hypertrack.lib.tracking.model.MarkerModel;
import com.hypertrack.uber_consumer.R;
import com.hypertrack.uber_consumer.base.MyHyperTrackMapAdapter;
import com.hypertrack.uber_consumer.models.Coordinate;
import com.hypertrack.uber_consumer.utils.Constants;
import com.hypertrack.uber_consumer.utils.SharedValues;
import com.hypertrack.uber_consumer.utils.Utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Created by pkharche on 08/04/18.
 */
public class GenerateRideActivity extends BaseActivity {

    private Location mCurrentLocation = null;
    private HyperTrackMapFragment mHyperTrackMapFragment;
    private MyHyperTrackMapAdapter myHyperTrackMapAdapter;
    private boolean isPickUp;
    private int mNoOfCars = 0;
    private RequestQueue mRequestQueue = null;

    private View mLoadingView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_ride);

        mLoadingView = findViewById(R.id.loading_view);

        getIntentData();

        if(isInternetConnected()) {

            String pickUpPlace = SharedValues.getValue(mContext, Constants.PICKUP_PLACE);
            if(TextUtils.isEmpty(pickUpPlace)) {
                showCurrentLocation();
            } else {
                showPickUpLocation();
            }
        } else {
            showNetworkError();
        }

        /*if(isPickUp) {
            setUpHyperTrackBottomView();

        } else {
            showCurrentLocation();
        }*/
    }

    private void getIntentData() {
        Intent i = getIntent();
        if(i != null && i.hasExtra(Constants.PICKUP_PLACE)) {
            isPickUp = i.getBooleanExtra(Constants.PICKUP_PLACE, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkLocationServices();
    }

    @Override
    protected void locationServicesEnabled() {
        super.locationServicesEnabled();
        setUpHyperTrackBottomView();
    }

    private void setUpHyperTrackBottomView() {
        mHyperTrackMapFragment = (HyperTrackMapFragment) getSupportFragmentManager().findFragmentById(R.id.htMapfragment);
        mHyperTrackMapFragment.setMapStyle(R.raw.mapstyle_uberx);

        myHyperTrackMapAdapter = new MyHyperTrackMapAdapter(mContext);
        mHyperTrackMapFragment.setMapAdapter(myHyperTrackMapAdapter);

        final PlaceSelector placeSelector = (PlaceSelector)mHyperTrackMapFragment.setUseCaseType(MapFragmentView.Type.PLACE_SELECTOR);

        mHyperTrackMapFragment.setMapCallback(new MapFragmentCallback() {

            @Override
            public void onMapLoadedCallback(Context context, GoogleMap map) {
                super.onMapLoadedCallback(context, map);

                //Get markers in visibility if your pickup location is not current location
                mHyperTrackMapFragment.bindMapBound();
            }

            @Override
            public void onExpectedPlaceSelected(Place expectedPlace) {
                super.onExpectedPlaceSelected(expectedPlace);

                if(expectedPlace != null) {
                    if(isPickUp) {
                        SharedValues.saveValue(mContext, Constants.PICKUP_PLACE, new Gson().toJson(expectedPlace));
                    } else {
                        SharedValues.saveValue(mContext, Constants.DROPOFF_PLACE, new Gson().toJson(expectedPlace));
                    }
                    launchActivity(BookRideActivity.class);
                }
            }

            @Override
            public void onBottomBaseViewCreated(int useCaseType) {
                super.onBottomBaseViewCreated(useCaseType);
                placeSelector.setPlaceSelectorState(PlaceSelector.State.COLLAPSED);

                placeSelector.showBackArrow(false);
                placeSelector.showTitle();

                if(isPickUp) {
                    placeSelector.setShowCurrentLocation(true);
                    placeSelector.setTitle(getString(R.string.enter_pickup_location));

                } else {
                    placeSelector.setShowCurrentLocation(false);
                    placeSelector.setTitle(getString(R.string.select_place_title));
                }

            }
        });
    }

    private void showPickUpLocation() {
        String pickUpStr = SharedValues.getValue(mContext, Constants.PICKUP_PLACE);
        if(!TextUtils.isEmpty(pickUpStr)) {
            Place pickUpPlace = new Gson().fromJson(pickUpStr, Place.class);
            if (pickUpPlace != null && pickUpPlace.getLocation() != null) {
                mCurrentLocation = new Location("");
                mCurrentLocation.setLatitude(pickUpPlace.getLocation().getLatitude());
                mCurrentLocation.setLongitude(pickUpPlace.getLocation().getLongitude());

                displayCarsNearMyLocation();
            }
        }
    }

    private void showCurrentLocation() {
        //Utils.showProgressDialog(mActivity, false);

        HyperTrack.getCurrentLocation(new HyperTrackCallback() {
            @Override
            public void onSuccess(@NonNull SuccessResponse response) {
                Utils.hideProgressDialog();

                Location location = (Location) response.getResponseObject();
                mCurrentLocation = location;
                Place pickUpPlace = new Place();
                pickUpPlace.setLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                SharedValues.saveValue(mContext, Constants.PICKUP_PLACE, new Gson().toJson(pickUpPlace));

                displayCarsNearMyLocation();
            }

            @Override
            public void onError(@NonNull ErrorResponse errorResponse) {
                Utils.hideProgressDialog();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        //User just wanted to change pick up (while he/ she was on Bookride),
        // but then clicks BACK as they want to go via the earlier pickup location
        if(isPickUp) {
            launchActivity(BookRideActivity.class);
        } else {
            finish();
        }
    }

    @Override
    protected void onStop () {
        super.onStop();
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });
        }
    }

    private void displayCarsNearMyLocation() {
        Utils.showProgressDialog(mActivity, false);

        // Instantiate the cache
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024); // 1MB cap

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();

        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=" + mCurrentLocation.getLatitude() + "," + mCurrentLocation.getLongitude()
                    + "&radius=500&type=restaurant&key=" +getString(R.string.google_api_server_key);

        Log.d(Constants.TAG, "NearBySearch url: " + url);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Utils.hideProgressDialog();
                        try {
                            if(response != null && response.has("results")) {
                                JSONArray resultsArray = response.getJSONArray("results");

                                mNoOfCars = resultsArray.length();

                                if(mNoOfCars > 5) {
                                    mNoOfCars = 5; //Limit to display only 5 cars max
                                }

                                for(int i = 0; i < mNoOfCars; i++) {
                                    JSONObject resultObject = resultsArray.getJSONObject(i);
                                    if(resultObject != null && resultObject.has("geometry")) {
                                        JSONObject locationObject = resultObject.getJSONObject("geometry").getJSONObject("location");
                                        Location location = new Location("");
                                        location.setLatitude(locationObject.getDouble("lat"));
                                        location.setLongitude(locationObject.getDouble("lng"));
                                        showMarker(location, false);
                                    }
                                }

                                mLoadingView.setVisibility(View.GONE);
                                if(mNoOfCars == 0) {
                                    showSnackBar(getString(R.string.no_cars_available), false);
                                }
                            }

                        } catch(Exception e) {
                            Log.e(Constants.TAG, "NearBySearch Api error", e);
                        }

                        showMarker(mCurrentLocation,true);//written after displayCars for NoOfCars
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Utils.hideProgressDialog();
                        Log.e(Constants.TAG, "NearBySearch Api volley error", error);

                        showMarker(mCurrentLocation,true);//written after displayCars for NoOfCars
                    }
                });

        // Access the RequestQueue through your singleton class.
        mRequestQueue.add(jsonObjectRequest);
    }

    private void showMarker(Location location, boolean isMyLocation) {
        if(mHyperTrackMapFragment != null) {
            com.google.android.gms.maps.model.LatLng latLng = new com.google.android.gms.maps.model.LatLng(location.getLatitude(), location.getLongitude());

            int icon1 = R.drawable.fake_car;
            if(isMyLocation) {
                icon1 = R.drawable.icondrive;

                String place = getAddressFromLocation(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                String noOfCars = mNoOfCars + " cars nearby";

                View view = ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.layout_info_box, null);
                ((TextView) view.findViewById(R.id.tv_info_title)).setText(place);
                ((TextView) view.findViewById(R.id.tv_info_details)).setText(noOfCars);

                MarkerModel markerModel = new MarkerModel(latLng, -1, MarkerModel.Type.CUSTOM_INFO);
                markerModel.setView(view);
                mHyperTrackMapFragment.addCustomMarker(markerModel);
            }

            MarkerModel pickUpMarker = new MarkerModel(latLng, icon1, MarkerModel.Type.CUSTOM);
            mHyperTrackMapFragment.addOrUpdateMarker(pickUpMarker);
        }
    }

    private String getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = null;
        String address = null;
        try {
            geocoder = new Geocoder(mContext, Locale.getDefault());

            List<android.location.Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            address = addressList.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        } catch (Exception e) {
            Log.e(Constants.TAG, "getAddressFromLocation :: (" +latitude+" , " + longitude +")" + e.getMessage());
        }

        return address;
    }

}
