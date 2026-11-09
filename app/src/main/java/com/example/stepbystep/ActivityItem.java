package com.example.stepbystep;

import java.util.Locale;

/**
 * Data Model class representing a step and shake activity session.
 */
public class ActivityItem {
    private long id;
    private String dateTime;
    private int steps;
    private int shakes;
    private long durationSeconds;
    private double avgStepsPerMin;
    private double avgShakesPerMin;

    public ActivityItem() {
    }

    public ActivityItem(long id, String dateTime, int steps, int shakes, long durationSeconds, double avgStepsPerMin, double avgShakesPerMin) {
        this.id = id;
        this.dateTime = dateTime;
        this.steps = steps;
        this.shakes = shakes;
        this.durationSeconds = durationSeconds;
        this.avgStepsPerMin = avgStepsPerMin;
        this.avgShakesPerMin = avgShakesPerMin;
    }

    public ActivityItem(String dateTime, int steps, int shakes, long durationSeconds, double avgStepsPerMin, double avgShakesPerMin) {
        this.dateTime = dateTime;
        this.steps = steps;
        this.shakes = shakes;
        this.durationSeconds = durationSeconds;
        this.avgStepsPerMin = avgStepsPerMin;
        this.avgShakesPerMin = avgShakesPerMin;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public int getShakes() {
        return shakes;
    }

    public void setShakes(int shakes) {
        this.shakes = shakes;
    }

    public long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(long durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public double getAvgStepsPerMin() {
        return avgStepsPerMin;
    }

    public void setAvgStepsPerMin(double avgStepsPerMin) {
        this.avgStepsPerMin = avgStepsPerMin;
    }

    public double getAvgShakesPerMin() {
        return avgShakesPerMin;
    }

    public void setAvgShakesPerMin(double avgShakesPerMin) {
        this.avgShakesPerMin = avgShakesPerMin;
    }

    public String getFormattedDuration() {
        long mins = durationSeconds / 60;
        long secs = durationSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", mins, secs);
    }

    public String getFormattedDetailedDuration() {
        long mins = durationSeconds / 60;
        long secs = durationSeconds % 60;
        return String.format(Locale.getDefault(), "%02d นาที %02d วินาที", mins, secs);
    }
}
