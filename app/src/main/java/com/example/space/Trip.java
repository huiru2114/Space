package com.example.space;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Trip {
    private String tripId;  // New field for trip_id primary key
    private String userId;
    private String tripName;
    private String country;
    private String journal;
    private Date startDate;
    private Date endDate;
    private List<String> imageUrls;

    // Raw date strings from API for parsing(Explore page)
    private String rawStartDate;
    private String rawEndDate;

    // Default constructor
    public Trip() {
        this.imageUrls = new ArrayList<>();
    }

    // Constructor with fields
    public Trip(String userId, String tripName, String country, String journal, Date startDate, Date endDate) {
        this.userId = userId;
        this.tripName = tripName;
        this.country = country;
        this.journal = journal;
        this.startDate = startDate;
        this.endDate = endDate;
        this.imageUrls = new ArrayList<>();
    }

    // Getters and Setters
    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTripName() {
        return tripName;
    }

    public void setTripName(String tripName) {
        this.tripName = tripName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getJournal() {
        return journal;
    }

    public void setJournal(String journal) {
        this.journal = journal;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    //for Explore page
    public String getRawStartDate() {
        return rawStartDate;
    }

    public void setRawStartDate(String rawStartDate) {
        this.rawStartDate = rawStartDate;
    }

    public String getRawEndDate() {
        return rawEndDate;
    }

    public void setRawEndDate(String rawEndDate) {
        this.rawEndDate = rawEndDate;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public void addImageUrl(String imageUrl) {
        if (this.imageUrls == null) {
            this.imageUrls = new ArrayList<>();
        }
        this.imageUrls.add(imageUrl);
    }

    public JSONArray getImageUrlsAsJsonArray() throws JSONException {
        JSONArray jsonArray = new JSONArray();
        if (imageUrls != null) {
            for (String url : imageUrls) {
                jsonArray.put(url);
            }
        }
        return jsonArray;
    }
}