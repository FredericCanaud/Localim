package com.example.localim.views;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.localim.views.helpers.Constants;
import com.example.localim.views.helpers.Helper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.*;
import org.jetbrains.annotations.NotNull;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.localim.R;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class NewServiceActivity extends AppCompatActivity {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private static final int RC_UPLOAD_FILE = 102;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private MapView map;
    private UploadTask mUploadTask;
    private GeoPoint loc;
    private String id = "";
    private final DatabaseReference db = FirebaseDatabase.getInstance("https://localimoges-17c14-default-rtdb.europe-west1.firebasedatabase.app").getReference().child("services");

    /**
     * Initialize the new service form,
     * then all the necessaries to get the map location picker
     * then retrieve the last id of the services in database, to increment it in submit
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_service);

        // Initialize header Toolbar
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setLogo(R.drawable.logo_min2);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        myToolbar.setTitleTextColor(Color.WHITE);

        // Block the screen orientation to PORTRAIT mode
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Initialize the map
        loc = null;
        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);

        // Place the center point to Limoges's Town Hall
        IMapController mapController = map.getController();
        mapController.setZoom(Constants.DEFAULT_ZOOM_FACTOR);
        GeoPoint point = new GeoPoint(Constants.LIMOGES_TOWNHALL_LONGITUDE,Constants.LIMOGES_TOWNHALL_LATITUDE);
        mapController.setCenter(point);

        // Add listeners to get position by touching screen, and add marker to the map corresponding location
        Overlay overlay = new Overlay(getApplicationContext()) {
            ItemizedIconOverlay<OverlayItem> items = null;

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e, MapView mapView) {
                Projection proj = mapView.getProjection();
                loc = (GeoPoint) proj.fromPixels((int)e.getX(), (int)e.getY());

                ArrayList<OverlayItem> markers = new ArrayList<>();
                OverlayItem item = new OverlayItem("", "", new GeoPoint(loc.getLatitude(), loc.getLongitude()));
                item.setMarker(ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_maps_marker));
                markers.add(item);

                // Add marker or replace current with another
                if (items == null) {
                    items = new ItemizedIconOverlay<>(getApplicationContext(), markers, null);
                    map.getOverlays().add(items);
                    map.invalidate();
                } else {
                    map.getOverlays().remove(items);
                    map.invalidate();
                    items = new ItemizedIconOverlay<>(getApplicationContext(), markers, null);
                    map.getOverlays().add(items);
                }
                return true;
            }

        };
        map.getOverlays().add(overlay);

        // WRITE_EXTERNAL_STORAGE is required in order to show the map
        requestPermissionsIfNecessary(new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        // Retrieve the last id of the services in database, to increment it in submit
        Query ref = db.orderByKey().limitToLast(1);
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    id = childSnapshot.getKey();
                }
            }
            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) { }
        });
    }

    /**
     * Submit data
     * 1) Retrieve all the data of the new service form and check if not empty
     * 2) Check if an image has been chosen. If yes :
     * 2.1) Convert it into bitmap
     * 2.2) Uploading it into Firebase Storage, using a progress bar to alert user
     * 3) Check if a geo point has been chosen
     * 4) Add the new service into the Firebase Database
     * 5) Redirect user into MainActivity
     */
    public void submit(View view) {
        EditText title = findViewById(R.id.et_title);
        EditText description = findViewById(R.id.et_description);
        EditText place = findViewById(R.id.et_place);
        EditText hours = findViewById(R.id.et_hours);
        ImageView iv = findViewById(R.id.img_to_upload);
        RadioButton rb = findViewById(((RadioGroup) findViewById(R.id.rb_tag)).getCheckedRadioButtonId());

        // Check if input data is not empty
        if(!validate(title,description,place,hours)){
            Toast.makeText(getApplicationContext(), "Veuillez remplir tous les champs nécessaires.", Toast.LENGTH_LONG).show();
            return;
        }

        // Check if an image has been chosen
        if (iv.getTag().equals("placeholder")){
            db.child(String.valueOf(Integer.parseInt(id) + 1)).child("image_url").setValue("");
        } else {
            StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("photos").child(iv.getTag().toString());

            // Convert the image into Bitmap
            BitmapDrawable drawable = (BitmapDrawable) iv.getDrawable();
            Bitmap bitmap = drawable.getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            // Uploading the into Firebase Storage, using a Progress Bar to alert user
            mUploadTask = imageRef.putBytes(data);
            Helper.initProgressDialog(this);
            Helper.mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mUploadTask.cancel();
                }
            });
            Helper.mProgressDialog.setButton(DialogInterface.BUTTON_NEUTRAL,  getResources().getString(R.string.pause), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mUploadTask.pause();
                }
            });
            Helper.mProgressDialog.show();

            mUploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Helper.dismissProgressDialog();
                }
            }).addOnSuccessListener(taskSnapshot -> {
                Helper.dismissProgressDialog();
                findViewById(R.id.button_upload_resume).setVisibility(View.GONE);
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                    }
                });
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(@NotNull UploadTask.TaskSnapshot taskSnapshot) {
                    int progress = (int) ((100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                    Helper.setProgress(progress);
                }
            }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onPaused(@NotNull UploadTask.TaskSnapshot taskSnapshot) {
                    findViewById(R.id.button_upload_resume).setVisibility(View.VISIBLE);
                }
            });
        }

        // Check if a geo point has been chosen
        String newId = String.valueOf(Integer.parseInt(id) + 1);
        if (loc == null){
            db.child(newId).child("point").child("latitude").setValue("");
            db.child(newId).child("point").child("longitude").setValue("");
        } else {
            db.child(newId).child("point").child("latitude").setValue(loc.getLatitude());
            db.child(newId).child("point").child("longitude").setValue(loc.getLongitude());
        }

        //Add the new service into the Firebase Database
        db.child(newId).child("title").setValue(title.getText().toString());
        db.child(newId).child("description").setValue(description.getText().toString());
        db.child(newId).child("image_url").setValue(iv.getTag());
        db.child(newId).child("hours").setValue(hours.getText().toString());
        db.child(newId).child("place").setValue(place.getText().toString());
        db.child(newId).child("tag").setValue(rb.getText().toString());
        db.child(newId).child("user_id").setValue(Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid());
        db.push();

        // Uploading the into Firebase Storage, using a Progress Bar to alert user
        Toast.makeText(getApplicationContext(), getResources().getString(R.string.successful_new_service), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    /**
     * Check if the new services fields are not empty.
     * If yes, return true
     * If not, return false
     */
    private boolean validate(EditText title, EditText description, EditText place, EditText hours){
        boolean valid = true;
        String required = getResources().getString(R.string.required);
        if(title.getText().toString().matches("")){
            title.setError(required);
            valid = false;
        }
        if(description.getText().toString().matches("")){
            description.setError(required);
            valid = false;
        }
        if(place.getText().toString().matches("")){
            place.setError(required);
            valid = false;
        }
        if(hours.getText().toString().matches("")){
            hours.setError(required);
            valid = false;
        }
        return valid;
    }

    /**
     * Retrieve the result of choosing permissions to read storage and take photos
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NotNull String[] permissions, @NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        ArrayList<String> permissionsToRequest = new ArrayList<>(Arrays.asList(permissions).subList(0, grantResults.length));
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                permissionsToRequest.add(permission);
            }
        }
        if (permissionsToRequest.size() > 0) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    /**
     * Launch activity to take photos
     */
    public void takePhoto(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }

    /**
     * Launch activity to choose a file into internal storage
     */
    public void chooseFile(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, RC_UPLOAD_FILE);
    }

    /**
     * Post operations after retrieving an image
     * If this is a photo, we changed the name to a random string and crop it square-shaped
     * If this is a file, we only crop it square-shaped
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ImageView iv = findViewById(R.id.img_to_upload);
        Bitmap imageBitmap;
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            imageBitmap = (Bitmap) extras.get("data");
            imageBitmap = Helper.cropBitmap(imageBitmap);

            iv.setTag(Helper.randomString());

        } else {
            String path = Helper.getPath(this, Uri.parse(data.getData().toString()));
            imageBitmap = BitmapFactory.decodeFile(path);
            imageBitmap = Helper.cropBitmap(imageBitmap);
            path = data.getData().getLastPathSegment();
            iv.setTag(path.substring(path.lastIndexOf("/")));
        }
        iv.setImageBitmap(imageBitmap);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}

