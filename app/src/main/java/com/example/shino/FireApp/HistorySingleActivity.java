package com.example.shino.FireApp;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistorySingleActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener{


    private String rideId, currentUserId, citizenId , fighterId, userFighterOrCitizen;

    private TextView rideLocation;
    private TextView rideDistance;
    private TextView rideDate;
    private TextView userName;
    private TextView userPhone;
    private ImageView userImage;
    private DatabaseReference historyRideInfoDb;
    private LatLng destinationLatLng, pickupLatLng;
    private TextView imageUser;

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_single);

        rideId = getIntent().getExtras().getString(rideId);

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
        polylines = new ArrayList<>();
        rideLocation = (TextView) findViewById(R.id.rideLocation);
        rideDistance = (TextView) findViewById(R.id.rideDistance);
        rideDate = (TextView) findViewById(R.id.rideDate);
        userName = (TextView) findViewById(R.id.userName);
        userPhone = (TextView) findViewById(R.id.userPhone);

        userImage = (ImageView) findViewById(R.id.userImage);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyRideInfoDb = FirebaseDatabase.getInstance().getReference().child("History").child(rideId);
        getRideInformation();



    }

    private void getRideInformation() {

        historyRideInfoDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    for (DataSnapshot child : dataSnapshot.getChildren()){

                        if (child.getKey().equals("Citizen")){

                            citizenId = child.getValue().toString();

                            if (!citizenId.equals(currentUserId)){

                                userFighterOrCitizen = "Fighter";

                                getUserInformation("Citizen", citizenId);
                            }
                        }
                        if (child.getKey().equals("Fighter")) {

                            fighterId = child.getValue().toString();

                            if (!fighterId.equals(currentUserId)) {

                                userFighterOrCitizen = "Citizen";

                                getUserInformation("Fighter", fighterId);

                            }
                            if (child.getKey().equals("location")) {
                                pickupLatLng = new LatLng(Double.valueOf(child.child("from").child("lat").getValue().toString()), Double.valueOf(child.child("from").child("lng").getValue().toString()));

                                destinationLatLng = new LatLng(Double.valueOf(child.child("to").child("lat").getValue().toString()), Double.valueOf(child.child("to").child("lng").getValue().toString()));
                                if (destinationLatLng != new LatLng(0,0)){


                                }
                            }
                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getUserInformation(String otherUserFighterOrCitizen, String otherUserId) {

        DatabaseReference mOtherUserDB = FirebaseDatabase.getInstance().getReference().child("Users").child(otherUserFighterOrCitizen).child(otherUserId);

        mOtherUserDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    Map<String  , Object> map = (Map<String  , Object>) dataSnapshot.getValue();
                    if (map.get("name") !=null){

                        userName.setText(map.get("name").toString());
                    }
                    if (map.get("phone") !=null){

                        userPhone.setText(map.get("phone").toString());
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

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


    @Override
    public void onMapReady(GoogleMap googleMap) {

    }
}
