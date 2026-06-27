package com.nhom5.ftcomic.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.nhom5.ftcomic.R;
import com.nhom5.ftcomic.models.User;

import java.util.List;

public class ManageUsersAdapter extends RecyclerView.Adapter<ManageUsersAdapter.UserViewHolder> {

    public interface OnUserActionListener {
        void onEdit(User user, int position);
        void onDelete(User user, int position);
    }

    private final List<User> userList;
    private final OnUserActionListener listener;

    public ManageUsersAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, listener, position);
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvUserName;
        private final TextView tvUserEmail;
        private final MaterialButton btnEdit;
        private final MaterialButton btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(User user, OnUserActionListener listener, int position) {
            String roleText = " (Thành viên)";
            if ("admin".equals(user.getRole())) {
                roleText = " (Quản lý)";
            } else if ("translator".equals(user.getRole())) {
                roleText = " (Dịch giả)";
            }

            tvUserName.setText(user.getUsername() + roleText);
            tvUserEmail.setText(user.getEmail());

            if (listener != null) {
                btnEdit.setOnClickListener(v -> listener.onEdit(user, position));
                btnDelete.setOnClickListener(v -> listener.onDelete(user, position));
            }
        }
    }
}