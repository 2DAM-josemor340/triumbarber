package com.triumbarber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.CitaVH> {
    private List<Cita> citas;
    private OnCitaListener listener;

    public interface OnCitaListener {
        void onAnularClick(int p);
        void onModificarClick(int p);
    }

    public CitaAdapter(List<Cita> citas, OnCitaListener listener) {
        this.citas = citas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CitaVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cita, parent, false);
        return new CitaVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CitaVH h, int position) {
        Cita c = citas.get(position);

        String fechaBD = c.getFecha();
        String fechaVisible = fechaBD;

        if (fechaBD != null && fechaBD.contains("/")) {
            try {
                String[] partes = fechaBD.split("/");
                if (partes.length == 3) {
                    fechaVisible = partes[2] + "/" + partes[1] + "/" + partes[0];
                }
            } catch (Exception e) {
                fechaVisible = fechaBD;
            }
        }

        h.txtFecha.setText(fechaVisible + " - " + c.getHora());

        String nombre = (c.getClienteNombre() != null) ? c.getClienteNombre() : "";
        String apellidos = (c.getClienteApellidos() != null) ? c.getClienteApellidos() : "";
        String nombreFull = (nombre + " " + apellidos).trim();
        h.txtNombre.setText(nombreFull.isEmpty() ? "Cliente Desconocido" : nombreFull);

        h.txtBarbero.setText("Barbero: " + (c.getBarbero() != null ? c.getBarbero() : "N/A"));
        h.txtEmail.setText(c.getClienteEmail());
        h.txtTelefono.setText("Tel: " + (c.getClienteTelefono() != null ? c.getClienteTelefono() : "N/A"));

        if (listener != null) {
            h.btnAnular.setVisibility(View.VISIBLE);
            h.btnModificar.setVisibility(View.VISIBLE);
            h.btnAnular.setOnClickListener(v -> listener.onAnularClick(h.getAdapterPosition()));
            h.btnModificar.setOnClickListener(v -> listener.onModificarClick(h.getAdapterPosition()));
        } else {
            h.btnAnular.setVisibility(View.GONE);
            h.btnModificar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return citas.size();
    }

    public static class CitaVH extends RecyclerView.ViewHolder {
        TextView txtFecha, txtNombre, txtEmail, txtTelefono, txtBarbero;
        Button btnAnular, btnModificar;

        public CitaVH(View v) {
            super(v);
            txtFecha = v.findViewById(R.id.txtFechaHoraItem);
            txtNombre = v.findViewById(R.id.txtNombreClienteItem);
            txtBarbero = v.findViewById(R.id.txtBarberoItem);
            txtEmail = v.findViewById(R.id.txtEmailClienteItem);
            txtTelefono = v.findViewById(R.id.txtTelefonoClienteItem);
            btnAnular = v.findViewById(R.id.btnAnularCita);
            btnModificar = v.findViewById(R.id.btnModificarCita);
        }
    }
}