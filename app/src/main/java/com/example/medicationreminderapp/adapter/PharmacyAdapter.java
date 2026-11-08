package com.example.medicationreminderapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.medicationreminderapp.R;
import com.example.medicationreminderapp.model.Pharmacy;
import java.util.List;

public class PharmacyAdapter extends RecyclerView.Adapter<PharmacyAdapter.PharmacyViewHolder> {

    private List<Pharmacy> mList;
    private Context mContext;

    public PharmacyAdapter(Context context, List<Pharmacy> list) {
        this.mContext = context;
        this.mList = list;
    }

    @NonNull
    @Override
    public PharmacyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pharmacy, parent, false);
        return new PharmacyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PharmacyViewHolder holder, int position) {
        Pharmacy pharmacy = mList.get(position);
        if (pharmacy == null) return;

        holder.tvName.setText(pharmacy.getName());
        holder.imgPhoto.setImageResource(pharmacy.getImageResId());

        holder.btnWeb.setOnClickListener(v -> {
            String url = pharmacy.getWebsiteUrl();
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            mContext.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return mList != null ? mList.size() : 0;
    }

    public class PharmacyViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPhoto;
        TextView tvName;
        ImageButton btnWeb;

        public PharmacyViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPhoto = itemView.findViewById(R.id.imgPharmacy);
            tvName = itemView.findViewById(R.id.tvPharmacyName);
            btnWeb = itemView.findViewById(R.id.btnOpenWeb);
        }
    }
}