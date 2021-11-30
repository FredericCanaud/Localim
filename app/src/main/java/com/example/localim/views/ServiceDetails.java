package com.example.localim.views;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.example.localim.R;
import com.example.localim.views.helpers.Constants;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import org.jetbrains.annotations.NotNull;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class ServiceDetails extends AppCompatActivity {

    StorageReference storageRef = FirebaseStorage.getInstance(Constants.FIREBASE_STORAGE).getReference();
    private MapView map;

    /**
     * Initialize first a RecyclerView grid with all the services of the database
     * Then, check one time asynchronously device location if localization is enabled
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_details);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setLogo(R.drawable.logo_min2);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        myToolbar.setTitleTextColor(Color.WHITE);

        // Bitmap par défaut
        ImageView placeholder = (ImageView) findViewById(R.id.image_placeholder);
        Bitmap bMap = BitmapFactory.decodeResource(getResources(), R.drawable.service_placeholder);
        Bitmap adapte = Bitmap.createScaledBitmap(bMap, 500, 500, false);

        // Récupération des blocs d'affichage
        TextView title = (TextView) findViewById(R.id.title_service);
        TextView descriptionV = (TextView) findViewById(R.id.description);
        TextView addressV = (TextView) findViewById(R.id.address);
        TextView hoursV = (TextView) findViewById(R.id.hours);
        TextView tagV = (TextView) findViewById(R.id.tag);

        Intent intent = getIntent();
        String id = intent.getStringExtra("id");
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        DatabaseReference db = FirebaseDatabase.getInstance(Constants.FIREBASE_DATABASE).getReference().child("services").child(id);

        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                String titre = (String) snapshot.child("title").getValue();
                String description = (String) snapshot.child("description").getValue();
                String url_image = (String) snapshot.child("image_url").getValue();
                String place = (String) snapshot.child("place").getValue();
                String hours = (String) snapshot.child("hours").getValue();
                String tag = (String) snapshot.child("tag").getValue();

                if (!Objects.equals(url_image, "")) {
                    assert url_image != null;
                    Uri file = Uri.fromFile(new File(url_image));
                    StorageReference imageRef = storageRef.child("photos").child(file.getLastPathSegment());
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> Glide.with(getApplicationContext()).load(uri.toString()).thumbnail(0.1f).into(placeholder));
                }

                title.setText(titre);
                descriptionV.setText(description);
                addressV.setText(place);
                hoursV.setText(hoursV.getText().toString() + hours + " heures");
                tagV.setText(tag);

                IMapController mapController = map.getController();
                mapController.setZoom(14.0);
                GeoPoint geoPoint = new GeoPoint(Double.parseDouble(Objects.requireNonNull(snapshot.child("point").child("latitude").getValue()).toString()), Double.parseDouble(Objects.requireNonNull(snapshot.child("point").child("longitude").getValue()).toString()));
                mapController.setCenter(geoPoint);

                ArrayList<OverlayItem> markers = new ArrayList<>();
                OverlayItem item = new OverlayItem("", "", geoPoint);
                item.setMarker(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_maps_marker));
                markers.add(item);
                ItemizedIconOverlay<OverlayItem> items = new ItemizedIconOverlay<>(getApplicationContext(), markers, null);
                map.getOverlays().add(items);
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });

        placeholder.setImageBitmap(adapte);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}