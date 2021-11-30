package com.example.localim.views;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.localim.R;
import com.example.localim.models.ListServices;
import com.example.localim.models.Service;
import com.example.localim.views.helpers.Constants;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * To retrieve services data, we use the Adapter Pattern
 * It is used to bind data between the RecyclerView and a list of data.
 * When the service Adapter is notified after the result of a database request, it updates the view with the data
 */
public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ServiceViewHolder> {

    ListServices listeDeServices;
    StorageReference storageRef = FirebaseStorage.getInstance(Constants.FIREBASE_STORAGE).getReference().child("photos");

    public ServiceAdapter(ListServices listeDeServices) {
        this.listeDeServices = listeDeServices;
    }

    @NonNull
    @Override
    public ServiceViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(listeDeServices.getContext()).inflate(R.layout.fragment_service,parent,false);
        return new ServiceViewHolder(v);
    }

    @Override
    public int getItemCount() {
        return listeDeServices.getServices().size();
    }

    /**
     * Updates all the fields of the view with the datas present in the View Model
     */
    @Override
    public void onBindViewHolder(@NonNull @NotNull ServiceViewHolder holder, int position) {
        Service service = listeDeServices.getServices().get(position);
        holder.id.setText(String.valueOf(service.getId()));
        holder.titre.setText(service.getTitle());
        holder.description.setText(service.getDescription());

        if(service.getImage().equals("")){
            holder.image.setImageResource(R.drawable.service_placeholder);
        } else {
            Uri file = Uri.fromFile(new File(String.valueOf(service.getImage())));
            StorageReference imageRef = storageRef.child(file.getLastPathSegment());

            imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Glide.with(listeDeServices.getContext()).load(uri.toString()).thumbnail(0.1f).into(holder.image);
                }
            });
        }

        holder.parent_layout.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent details = new Intent(view.getContext(), ServiceDetails.class);
                details.putExtra("id", String.valueOf(service.getId()));
                details.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    view.getContext().startActivity(details);
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(view.getContext(), "Erreur lors du lancement des détails", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    /**
     * View Model used to link view and datas
     */
    public static class ServiceViewHolder extends RecyclerView.ViewHolder{

        LinearLayout parent_layout;
        TextView titre, description, id;
        ImageView image;

        public ServiceViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            id = itemView.findViewById(R.id.id);
            titre = itemView.findViewById(R.id.serviceTitle);
            description = itemView.findViewById(R.id.serviceDescription);
            image = itemView.findViewById(R.id.serviceImage);
            parent_layout = itemView.findViewById(R.id.blockService);
        }
    }
}
