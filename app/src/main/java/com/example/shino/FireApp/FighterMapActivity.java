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
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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

public class FighterMapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener, RoutingListener {

    private GoogleMap mMap;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;

    private Button mLogout, mSettings, mfireStatus;
    private Switch mWorkingSwitch;
    private int status = 0;
    private String citizenId = "" , destination;
    private LatLng  pickupLatLng;
    private Marker pickupMarker;
    private LatLng destinationLatLng;
    private Boolean isLoggingOut = false;
    private SupportMapFragment mapFragment;
    private LinearLayout mCitizenInfo;
    private ImageView mCitizenProfileImage;
    private TextView mCitizenName, mCitizenPhone, mCitizenDestination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fighter_map);

        polylines = new ArrayList<>();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FighterMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            mapFragment.getMapAsync(this);
        }
            mCitizenInfo = (LinearLayout) findViewById(R.id.citizenInfo);
        mCitizenProfileImage= (ImageView) findViewById(R.id.citizenProfileImage);

        mCitizenName= (TextView) findViewById(R.id.citizenName);

        mCitizenPhone= (TextView) findViewById(R.id.citizenPhone);

        mWorkingSwitch = (Switch) findViewById(R.id.workingSwitch);
        mWorkingSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b){

                    connectFighter();
                }else{
                    disconnectFighter();
                }
            }
        });

        mSettings = (Button)findViewById(R.id.settings);
        mLogout= (Button)findViewById(R.id.logout);
        mfireStatus =(Button)findViewById(R.id.fireStatus);
        mfireStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (status){

                    case 1:
                        status =2;
                        erasePolylines();
                        mfireStatus.setText("Fire extinguished");

                        break;

                    case 2:
                        recordArlert();
                        endRide();
                        break;
                }
            }
        });

                recordArlert();
        //remove from fighterAvailabe and takes the user to the main paper
        mLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isLoggingOut = true;
                disconnectFighter();
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(FighterMapActivity.this,MainActivity.class);
                startActivity(intent);
                finish();//so that this activity closes
                return;

            }
        });
        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(FighterMapActivity.this,FighterSettingsActivity.class);
                startActivity(intent);
                finish();//so that this activity closes
                return;
            }
        });
        getAssignedCitizen();
    }
    private void getAssignedCitizen(){

        String fighterId =FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCitizenRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Fighter").child(fighterId).child("citizenRequest").child(citizenId).child("citizenNumId");
        assignedCitizenRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    status = 1;
                        citizenId = dataSnapshot.getValue().toString();
                        getAssignedCitizenLocation();

                    getAssignedCitizenDestination();

                    getAssignedCitizenInfo();

                    }
                    else {
                    endRide();
                }


            }

                @Override
                     public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getAssignedCitizenLocation(){
        DatabaseReference assignedCitizenLocationRef = FirebaseDatabase.getInstance().getReference().child("fireAlert").child(citizenId).child("l");
        assignedCitizenLocationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    List<Object> map =(List<Object>) dataSnapshot.getValue();
                    double locationLat = 0;
                    double locationLng = 0;
                    if (map.get(0) != null) {
                        locationLat = Double.parseDouble(map.get(0).toString());

                    }
                    if (map.get(1) != null) {
                        locationLng = Double.parseDouble(map.get(1).toString());
                    }
                    LatLng locationLatLong = new LatLng(locationLat, locationLng);
                     mMap.addMarker(new MarkerOptions().position(locationLatLong).title("Fire Location").icon(BitmapDescriptorFactory.fromResource(R.mipmap.fire)));
                    getRouteToMarker(locationLatLong);

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }


    private void getAssignedCitizenDestination(){

        String fighterId =FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference assignedCitizenRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Fighter").child(fighterId).child("citizenRequest").child(citizenId);

        assignedCitizenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                    if (map.get("destination") != null) {
                        destination = map.get("destination").toString();
                        mCitizenDestination.setText("Destination:  " + destination);
                    } else {
                        mCitizenDestination.setText("Destination: --");
                    }

                    Double destinationLat = 0.0;
                    Double destinationLng = 0.0;
                    if (map.get("destinationLat")!= null){

                        destinationLat = Double.valueOf( map.get("destinationLat").toString());
                    }
                    if (map.get("destinationLng")!= null){

                        destinationLng = Double.valueOf( map.get("destinationLng").toString());

                        destinationLatLng = new LatLng(destinationLat, destinationLng);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }



    private void getAssignedCitizenInfo(){

       mCitizenInfo.setVisibility(View.VISIBLE);
        DatabaseReference mCitizenDatabase =  FirebaseDatabase.getInstance().getReference().child("Users").child("Citizen").child(citizenId);
        mCitizenDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()&& dataSnapshot.getChildrenCount()> 0){
                     Map<String, Object> map = (Map<String ,Object>) dataSnapshot.getValue();
                    if (map.get("name")!= null){
                        mCitizenName.setText(map.get("name").toString());
                    }
                    if (map.get("phone")!= null){
                        mCitizenPhone.setText(map.get("phone").toString());
                    }
                    if (map.get("profileImageUrl")!= null){
                        Glide.with(getApplication()).load(map.get("profileImageUrl").toString()).into(mCitizenProfileImage);
                    }


                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }





    private void getRouteToMarker(LatLng locationLatLong) {
        Routing routing = new Routing.Builder()
                .key("AIzaSyC2JYVYTktfmQV705zvlrUwnu4bmNLkCag")
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(false)
               .waypoints(new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude()), locationLatLong)
                .build();
        routing.execute();
    }

    private void travelMode(AbstractRouting.TravelMode driving) {
    }

    //Loads the google map





    private void endRide(){
     mfireStatus.setText("Attended Fire");
        erasePolylines();



        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Fighter").child(userId).child("fireAlert");
        driverRef.removeValue();

        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("fireAlert");
        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(citizenId);
        citizenId = "";

        if(pickupMarker != null){
            pickupMarker.remove();



            mCitizenInfo.setVisibility(View.VISIBLE);
            mCitizenName.setText("");
            mCitizenPhone.setText("");
            mCitizenDestination.setText( "Destination: -- ");
            mCitizenProfileImage.setImageResource(R.mipmap.account);

        }

    }

    private void recordArlert(){
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference driverRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Fighter").child(userId).child("History");

        DatabaseReference citizenRef = FirebaseDatabase.getInstance().getReference().child("Users").child("Citizen").child("History").child(citizenId);

        DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference().child("History");

        String requestId = historyRef.push().getKey();

        driverRef.child(requestId).setValue(true);

        citizenRef.child(requestId).setValue(true);

        HashMap map = new HashMap();
        map.put("Fighter",userId);
        map.put("Citizen",citizenId);
        map.put("RATING",0);
        map.put("Timestamp",getCurrentTimesStamp());
        map.put("destination",destination);

//

        historyRef.child(requestId).updateChildren(map);


    }

    private Long getCurrentTimesStamp() {
        Long timestamp = System.currentTimeMillis()/1000;
        return timestamp;
    }


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
       if (getApplicationContext()!=null) {

           mLastLocation = location;

           LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
           //moves camare at the same pace as the user, keep user in the middle of the map
           mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
           mMap.animateCamera(CameraUpdateFactory.zoomTo(14));

           String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
           DatabaseReference refAvailable = FirebaseDatabase.getInstance().getReference("fighterAvailable");
           DatabaseReference refWorking = FirebaseDatabase.getInstance().getReference("fightersWorking");
           GeoFire geoFireAvailable = new GeoFire(refAvailable);
           GeoFire geoFireWorking = new GeoFire(refWorking);

           switch (citizenId){

               case "":
                   geoFireWorking.removeLocation(userId);
                   geoFireAvailable.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                   break;
               default:
                   geoFireAvailable.removeLocation(userId);
                   geoFireWorking.setLocation(userId, new GeoLocation(location.getLatitude(), location.getLongitude()));
                   break;
           }

           // get id for the user that is currently logged in

            // points to child with all the availabe fire fighters
           // update the database

           //child of where the current user is stored
            // location variable contains the last updated location , store the latitude and longitude to the userID
       }
    }

    // when the map is connected and ready to work
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);// 1000 milli sec , java your set the time by 1000
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY); // best for getting the accurate location but drains battery


    }

    @Override
    public void onConnectionSuspended(int i) {


    }
    private void connectFighter(){

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(FighterMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this); //Trigger the refreshment of the location with an interval of 1000secs

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
    private void disconnectFighter(){

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
        String userId= FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("fighterAvailable");

        GeoFire geoFire = new GeoFire(ref);
        geoFire.removeLocation(userId);
    }
    // the driver only availabe when the use is using the application

    final int LOCATION_REQUEST_CODE =1;
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){

            case LOCATION_REQUEST_CODE:{
            if (grantResults.length> 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                mapFragment.getMapAsync(this);
            }
            else {

                Toast.makeText(getApplicationContext(), "Please provide the permission",Toast.LENGTH_LONG).show();
            }
                break;
            }
        }
    }


    private List<Polyline> polylines;
    private static final   int [] COLORS = new  int[]{R.color.primary_dark_material_light};

    @Override
    public void onRoutingFailure(RouteException e) {
        if (e != null){

            Toast.makeText(this,"Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Something went wrong,Try again", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingStart() {

    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        if (polylines.size()>0){
            for (Polyline poly : polylines){
                poly.remove();
            }
        }
        polylines = new ArrayList<>();
        for (int i = 0; i <route.size(); i++) {

            int colorIndex = i % COLORS.length;

            PolylineOptions polyOptions = new PolylineOptions();
            polyOptions.color(getResources().getColor(COLORS[colorIndex]));
            polyOptions.width(10 + i * 3);
            polyOptions.addAll(route.get(i).getPoints());
            Polyline polyline = mMap.addPolyline(polyOptions);
            polylines.add(polyline);
            Toast.makeText(getApplicationContext(), "Route  " + (i + 1) + "distance - " + route.get(i).getDistanceValue() + ": duration - " + route.get(i).getDurationValue(), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRoutingCancelled() {


    }
    private void erasePolylines(){

        for (Polyline line : polylines){
            line.remove();
        }
        polylines.clear();
    }
}
