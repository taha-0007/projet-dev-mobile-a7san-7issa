package com.example.a7san7issa.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.a7san7issa.R;
import com.example.a7san7issa.models.User;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private List<User> users;
    private OnUserActionListener listener;

    public interface OnUserActionListener {
        void onToggleAdmin(int userId, boolean currentStatus);
        void onDelete(int userId);
    }

    public AdminUserAdapter(List<User> users, OnUserActionListener listener) {
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        Context context = holder.itemView.getContext();

        holder.tvUsername.setText(user.getUsername());
        holder.tvEmail.setText(user.getEmail());
        holder.tvFiliere.setText("Filière : " + (user.getFiliere() != null ? user.getFiliere() : "Non définie"));
        holder.tvLangue.setText("Langue : " + (user.getLangue() != null ? user.getLangue() : "Non définie"));

        boolean isStaff = user.isStaff();
        holder.tvStatus.setText(isStaff ? "Admin" : "Étudiant");
        holder.tvStatus.setBackgroundColor(ContextCompat.getColor(context,
                isStaff ? R.color.primary : R.color.primary_light));

        holder.btnToggleAdmin.setText(isStaff ? "Rétrograder" : "Promouvoir admin");
        holder.btnToggleAdmin.setOnClickListener(v -> listener.onToggleAdmin(user.getId(), isStaff));
        holder.btnDeleteUser.setOnClickListener(v -> listener.onDelete(user.getId()));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUsername, tvEmail, tvFiliere, tvLangue, tvStatus;
        MaterialButton btnToggleAdmin, btnDeleteUser;

        ViewHolder(View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvAdminUsername);
            tvEmail = itemView.findViewById(R.id.tvAdminEmail);
            tvFiliere = itemView.findViewById(R.id.tvAdminFiliere);
            tvLangue = itemView.findViewById(R.id.tvAdminLangue);
            tvStatus = itemView.findViewById(R.id.tvAdminStatus);
            btnToggleAdmin = itemView.findViewById(R.id.btnToggleAdmin);
            btnDeleteUser = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}