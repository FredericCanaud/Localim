package com.example.localim.models;

import android.content.Context;

import java.util.ArrayList;

public class ListServices {
    private ArrayList<Service> services;
    private Context context;

    public ListServices(Context context){
        this.context = context;
        this.services = new ArrayList<>();
    }

    public ArrayList<Service> getServices() {
        return services;
    }

    public void setServices(ArrayList<Service> services) {
        this.services = services;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void addService(Service service){
        this.services.add(service);
    }
}
