package com.example.localim.views;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.localim.R;
import com.example.localim.models.ListServices;
import com.example.localim.models.Service;
import com.example.localim.views.auth.EmailPasswordActivity;
import com.example.localim.views.helpers.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import org.jetbrains.annotations.NotNull;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity implements LocationListener {

    RecyclerView recyclerView;
    ServiceAdapter serviceAdapter;
    ListServices listServices;
    DatabaseReference db;
    AtomicReference<GeoPoint> currentPoint;
    LocationManager mLocationManager;

    /**
     * Initialize first a RecyclerView grid with all the services of the database
     * Then, check one time asynchronously device location if localization is enabled
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Configuration.getInstance().load(getApplicationContext(), PreferenceManager.getDefaultSharedPreferences(getApplicationContext()));

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setLogo(R.drawable.logo_min2);
        myToolbar.setTitleTextColor(Color.WHITE);

        recyclerView = findViewById(R.id.serviceRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        listServices = new ListServices(getApplicationContext());
        serviceAdapter = new ServiceAdapter(listServices);
        recyclerView.setAdapter(serviceAdapter);

        db = FirebaseDatabase.getInstance(Constants.FIREBASE_DATABASE).getReference().child("services");
        getAllServices();

        findViewById(R.id.fab_new_post).setOnClickListener(view -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                startActivity(new Intent(MainActivity.this, EmailPasswordActivity.class));
            } else {
                startActivity(new Intent(MainActivity.this, NewServiceActivity.class));
            }
        });

        currentPoint = new AtomicReference<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    /**
     * Inflate menu in the ActionToolbar
     */
    @Override
    public boolean onCreateOptionsMenu(@NotNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Choose a request to make according to selected item menu
     */
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.tousLesTags:
                getAllServices();
                return true;
            case R.id.geolocalisation:
                rechercheParLocalisation();
                return true;
            case R.id.bricolageEtJardinage:
                rechercheParTag(getString(R.string.bricolage_et_jardinage));
                return true;
            case R.id.coursParticuliers:
                rechercheParTag(getString(R.string.cours_particuliers));
                return true;
            case R.id.covoiturage:
                rechercheParTag(getString(R.string.covoiturage));
                return true;
            case R.id.cuisine:
                rechercheParTag(getString(R.string.cuisine));
                return true;
            case R.id.serviceADomicile:
                rechercheParTag(getString(R.string.service_domicile));
                return true;
            case R.id.divers:
                rechercheParTag(getString(R.string.divers));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Getting all the services from the database,
     * then notifying the adapter to fill fragments in grid-shaped
     */
    private void getAllServices() {

        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                listServices.setServices(new ArrayList<>());
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String id = dataSnapshot.getKey();
                    String titre = (String) dataSnapshot.child("title").getValue();
                    String description = (String) dataSnapshot.child("description").getValue();
                    String url_image = (String) dataSnapshot.child("image_url").getValue();
                    Service newService = new Service(id, titre, description, url_image);
                    listServices.addService(newService);
                }
                serviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    /**
     * Getting all the services from the database including the selected tag,
     * then notifying the adapter to fill fragments in grid-shaped
     */
    private void rechercheParTag(String tag) {
        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                listServices.setServices(new ArrayList<>());
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    if (Objects.equals(dataSnapshot.child("tag").getValue(), tag)) {
                        String id = dataSnapshot.getKey();
                        String titre = (String) dataSnapshot.child("title").getValue();
                        String description = (String) dataSnapshot.child("description").getValue();
                        String url_image = (String) dataSnapshot.child("image_url").getValue();
                        Service newService = new Service(id, titre, description, url_image);
                        listServices.addService(newService);
                    }
                }
                serviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    /**
     * Getting all the services from the database,
     * then sorting the services according to device location,
     * then notifying the adapter to fill fragments in grid-shaped
     */
    private void rechercheParLocalisation() {

        if (currentPoint.get() == null) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        db.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                listServices.setServices(new ArrayList<>());
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    String id = dataSnapshot.getKey();
                    String titre = (String) dataSnapshot.child("title").getValue();
                    String description = (String) dataSnapshot.child("description").getValue();
                    String url_image = (String) dataSnapshot.child("image_url").getValue();
                    GeoPoint geoPoint = new GeoPoint(Double.parseDouble(Objects.requireNonNull(dataSnapshot.child("point").child("latitude").getValue()).toString()), Double.parseDouble(Objects.requireNonNull(dataSnapshot.child("point").child("longitude").getValue()).toString()));
                    Service newService = new Service(id, titre, description, url_image, geoPoint);
                    listServices.addService(newService);
                }

                listServices.getServices().sort((service1, service2) -> {
                    double distance1 = currentPoint.get().distanceToAsDouble(service1.getPosition());
                    double distance2 = currentPoint.get().distanceToAsDouble(service2.getPosition());

                    return Double.compare(distance1, distance2);
                });

                serviceAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }

    /**
     * Detecting the first device location,
     * then stopping updating this location
     */
    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentPoint.set(new GeoPoint(location.getLatitude(), location.getLongitude()));
        mLocationManager.removeUpdates(this);
    }

    /**
     * Retrieve the user choice when he's asked the location permission
     * If he accepted, a single location request is launched
     */
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (grantResults.length > 0) {

                boolean locationPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                if (locationPermission) {
                    mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                }
            }
        }
    }

    /**
     * Redirecting to user authentification
     */
    public void authActivity(MenuItem item) {
        Intent intent = new Intent(this, EmailPasswordActivity.class);
        startActivity(intent);
    }
}