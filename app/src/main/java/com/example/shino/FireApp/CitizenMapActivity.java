package com.example.shino.FireApp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CitizenMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private static final int LOCATION_REQUEST_CODE = 1;
    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;


    private Button mLogout,mfireAlert,mSettings, mHistory;
    private LatLng fireAlertLocation;
    private SupportMapFragment mapFragment;
    private TextView mCitizenName, mCitizenPhone;
    private String destination;
    private LatLng destinationLatLng;

    private LinearLayout mFighterInfo;
    private ImageView mFighterProfileImage;
    private TextView mFighterName, mFighterPhone, mFighterCar;
    private RatingBar mRatingBar;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_citizen_map);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CitizenMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            mapFragment.getMapAsync(this);
        }

        destinationLatLng = new LatLng (0.0, 0.0);


        mFighterInfo = (LinearLayout) findViewById(R.id.fighterInfo);
        mFighterProfileImage= (ImageView) findViewById(R.id.fighterProfileImage);

        mFighterName= (TextView) findViewById(R.id.fighterName);

        mFighterPhone= (TextView) findViewById(R.id.fighterPhone);
        mFighterCar = (TextView) findViewById(R.id.fighterCar);
        mRatingBar = (RatingBar) findViewById(R.id.ratingBar);


        mCitizenName =(TextView) findViewById(R.id.citizenName);
        mCitizenPhone =(TextView) findViewById(R.id.citizenPhone);

        mLogout= (Button)findViewById(R.id.logout);
        mfireAlert=(Button)findViewById(R.id.mfireAlert);
        mSettings = (Button)findViewById(R.id.settings);
        mHistory= (Button)findViewById(R.id.history);


        //remove from fighterAvailabe and takes the user to the main paper
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(CitizenMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();//so that this activity closes
                return;

            }
        });
        mfireAlert.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("fireAlert");
                GeoFire geoFire = new GeoFire(ref);
                geoFire.setLocation(userId, new GeoLocation(mLastLocation.getLatitude(),mLastLocation.getLongitude()));

                fireAlertLocation = new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().position(fireAlertLocation).title("FIRE HERE").icon(BitmapDescriptorFactory.fromResource(R.mipmap.fire)));

                mfireAlert.setText("Getting your Fire Fighter");

                getCloserDriver();
            }
        });

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CitizenMapActivity.this, CitizenSettingsActivity.class);
                startActivity(intent);
                return;
            }
        });
        mHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CitizenMapActivity.this, HistoryActivity.class);
                intent.putExtra("citizenOrFighter", "Citizen");
                startActivity(intent);
                return;
            }
        });

        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {

                destination = place.getName().toString();
                destinationLatLng = place.getLatLng();

            }

            @Override
            public void onError(Status status) {

            }

        });
    }


    private int radius = 1;
    private Boolean fighterFound = false;
    private String fighterFoundID;

    private void getCloserDriver()
    {
        DatabaseReference fighterLocation = FirebaseDatabase.getInstance().getReference().child("fighterAvailable");

        GeoFire geoFire = new GeoFire(fighterLocation);

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(fireAlertLocation.latitude, fireAlertLocation.longitude), radius);
        geoQuery.removeAllListeners();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                if(!fighterFound) {
                    fighterFound = true;
                    fighterFoundID= key;

                    DatabaseReference fighterRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Fighter").child(fighterFoundID).child("citizenRequest");
                    String citizenId =FirebaseAuth.getInstance().getCurrentUser().getUid();
                    HashMap map = new HashMap();
                    map.put("citizenNumId", citizenId);
                    map.put("destination", destination);

                    map.put("destinationLat", destinationLatLng.latitude);

                    map.put("destinationLng", destinationLatLng.longitude);
                    fighterRef.updateChildren(map);


                    getFighterLocation();
                    getFighterInfo();
                    getHasArletEnded();
                    mfireAlert.setText("Looking for Fighter Location...");
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {
                if(!fighterFound)
                {
                    radius++;
                    getCloserDriver();
                }

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });

    }
    private Marker nFighterMarker;
    private void getFighterLocation(){

    DatabaseReference fighterLocationRef = FirebaseDatabase.getInstance().getReference().child("fightersWorking").child(fighterFoundID).child("l");
    fighterLocationRef.addValueEventListener(new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.exists()) {
                List<Object> map = (List<Object>) dataSnapshot.getValue();
                double locationLat = 0;
                double locationLng = 0;
                mfireAlert.setText("Fighter Found...");
                if (map.get(0) != null) {
                    locationLat = Double.parseDouble(map.get(0).toString());
                }
                if (map.get(1) != null) {
                    locationLng = Double.parseDouble(map.get(1).toString());
                }
                LatLng fighterLatLng = new LatLng(locationLat, locationLng);
                if (nFighterMarker != null) {
                    nFighterMarker.remove();
                }

                Location loc1 = new Location ("");
                loc1.setLatitude(fireAlertLocation.latitude);
                loc1.setLongitude(fireAlertLocation.longitude);

                Location loc2 = new Location ("");
                loc2.setLatitude(fighterLatLng.latitude);
                loc2.setLongitude(fighterLatLng.longitude);

                float distance = loc1.distanceTo(loc2);

                if (distance<100){
                    mfireAlert.setText("Your fire fighter has arrived");
                } else
                {
                    mfireAlert.setText("Fighter Found:"  +  String.valueOf(distance));
                }

                nFighterMarker = mMap.addMarker(new MarkerOptions().position(fighterLatLng).title("Your Fighter").icon(BitmapDescriptorFactory.fromResource(R.mipmap.firetruck)));
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    });

}

    private void getFighterInfo(){

        mFighterInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCitizenDatabase =  FirebaseDatabase.getInstance().getReference().child("Users").child("Fighter").child(fighterFoundID);
        mCitizenDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()&& dataSnapshot.getChildrenCount()> 0){
                    Map<String, Object> map = (Map<String ,Object>) dataSnapshot.getValue();
                    if (map.get("name")!= null){
                        mFighterName.setText(map.get("name").toString());
                    }
                    if (map.get("phone")!= null){
                        mFighterPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("car")!= null){
                        mFighterCar.setText(map.get("car").toString());
                    }
                    if (map.get("profileImageUrl")!= null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mFighterProfileImage);
                    }

                 }  mFighterInfo.setVisibility(View.VISIBLE);

                 int ratingSum = 0;
                float ratingsTotal = 0;
                float ratingAvg = 0;
                for (DataSnapshot child : dataSnapshot.child("RATING").getChildren()){
                    ratingSum = ratingSum + Integer.valueOf(child.getValue().toString());
                    ratingsTotal++;

                }
                if (ratingsTotal!= 0){

                    ratingAvg = ratingSum/ratingsTotal;
                    mRatingBar.setRating(ratingAvg);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private DatabaseReference alretHasEndedRef;
    private ValueEventListener alrertHasEndedRefListener;
    private void getHasArletEnded(){

        String fighterId =FirebaseAuth.getInstance().getCurrentUser().getUid();
         alretHasEndedRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Fighter").child(fighterId).child("citizenRequest").child(fighterFoundID).child("citizenNumId");

        alrertHasEndedRefListener = alretHasEndedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                } else {
                    endAlret();
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void endAlret(){


    }


    //Loads the google map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        buildGoogleApiClient();
        mMap.setMyLocationEnabled(true);// needs permission check
    }

    protected synchronized void buildGoogleApiClient() {
        //validates my variables
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onLocationChanged(Location location) {
        if(getApplicationContext()!=null) {
            mLastLocation = location;

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            //moves camare at the same pace as the user, keep user in the middle of the map
            mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(14));
            if(!getDriversAroundStarted)
                getDriversAround();

        }
    }

    // when the map is connected and ready to work
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);// 1000 milli sec , java your set the time by 1000
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // best for getting the accurate location but drains battery

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(CitizenMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this); //Trigger the refreshment of the location with an interval of 1000secs

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    boolean getDriversAroundStarted = false;
    List<Marker> markers = new ArrayList<Marker>();
    private void getDriversAround(){
        getDriversAroundStarted = true;
        DatabaseReference driverLocation = FirebaseDatabase.getInstance().getReference().child("fightersWorking");

        GeoFire geoFire = new GeoFire(driverLocation);
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLastLocation.getLongitude(), mLastLocation.getLatitude()), 999999999);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {

                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key))
                        return;
                }

                LatLng driverLocation = new LatLng(location.latitude, location.longitude);

                Marker mDriverMarker = mMap.addMarker(new MarkerOptions().position(driverLocation).title(key).icon(BitmapDescriptorFactory.fromResource(R.mipmap.firetruck)));
                mDriverMarker.setTag(key);

                markers.add(mDriverMarker);


            }

            @Override
            public void onKeyExited(String key) {
                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key)){
                        markerIt.remove();
                    }
                }
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                for(Marker markerIt : markers){
                    if(markerIt.getTag().equals(key)){
                        markerIt.setPosition(new LatLng(location.latitude, location.longitude));
                    }
                }
            }

            @Override
            public void onGeoQueryReady() {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
    }
}
