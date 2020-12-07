package com.example.shino.FireApp.historyRecyclerView;

import java.text.SimpleDateFormat;

/**
 * Created by ping 127.0.0.1 on 30/10/2020.
 */

public class HistoryObject {
    private String rideId;
    private SimpleDateFormat time;

    public HistoryObject(String rideId, SimpleDateFormat time){
        this.rideId = rideId;
        this.time = time;
    }

    public String getRideId(){return rideId;}
    public void setRideId(String rideId) {
        this.rideId = rideId;
    }

    public SimpleDateFormat getTime(){return time;}
    public void setTime(SimpleDateFormat time) {
        this.time = time;
    }
}
