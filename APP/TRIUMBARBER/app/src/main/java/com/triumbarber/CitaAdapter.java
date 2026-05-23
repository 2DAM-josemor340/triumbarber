package com.triumbarber;

import android.graphics.Color;
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
    private boolean esModoAdmin = false;


    public interface OnCitaListener {
        void onAnularClick(int p);

        void onModificarClick(int p);

        default void onHechoClick(int p) {
        }
    }


    public CitaAdapter(List<Cita> citas, OnCitaListener listener) {
        this.citas = citas;
        this.listener = listener;
    }

    public void setModoAdmin(boolean esModoAdmin) {
        this.esModoAdmin = esModoAdmin;
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

        String fechaVisible = formatearFecha(c.getFecha());
        h.txtFecha.setText(fechaVisible + " - " + c.getHora());

        String nombre = (c.getClienteNombre() != null) ? c.getClienteNombre() : "";
        String apellidos = (c.getClienteApellidos() != null) ? c.getClienteApellidos() : "";
        String nombreFull = (nombre + " " + apellidos).trim();
        h.txtNombre.setText(nombreFull.isEmpty() ? "Cliente Desconocido" : nombreFull);

        h.txtBarbero.setText("Barbero: " + (c.getBarbero() != null ? c.getBarbero() : "N/A"));
        h.txtServicio.setText("Servicio: " + (c.getServicio() != null ? c.getServicio() : "No especificado"));
        h.txtEmail.setText(c.getClienteEmail());
        h.txtTelefono.setText("Tel: " + (c.getClienteTelefono() != null ? c.getClienteTelefono() : "N/A"));

        if (c.getObservaciones() != null && !c.getObservaciones().isEmpty()) {
            h.txtNotas.setVisibility(View.VISIBLE);
            h.txtNotas.setText("Nota: " + c.getObservaciones());
        } else {
            h.txtNotas.setVisibility(View.GONE);
        }

        String estado = c.getEstado() != null ? c.getEstado().toUpperCase() : "PENDIENTE";
        if (estado.equals("HECHO")) {
            h.txtEstado.setText("HECHO");
            h.txtEstado.setBackgroundColor(Color.parseColor("#4CAF50"));
            h.btnHecho.setVisibility(View.GONE);
            h.btnAnular.setVisibility(View.GONE);
            h.btnModificar.setVisibility(View.GONE);
        } else {
            h.txtEstado.setText("PENDIENTE");
            h.txtEstado.setBackgroundColor(Color.parseColor("#444444"));
            h.btnHecho.setVisibility(esModoAdmin ? View.VISIBLE : View.GONE);
            h.btnAnular.setVisibility(View.VISIBLE);
            h.btnModificar.setVisibility(esModoAdmin ? View.GONE : View.VISIBLE);
        }

        if (listener != null) {
            h.btnAnular.setOnClickListener(v -> listener.onAnularClick(h.getAdapterPosition()));
            h.btnModificar.setOnClickListener(v -> listener.onModificarClick(h.getAdapterPosition()));
            h.btnHecho.setOnClickListener(v -> listener.onHechoClick(h.getAdapterPosition()));
        } else {
            h.btnAnular.setVisibility(View.GONE);
            h.btnModificar.setVisibility(View.GONE);
            h.btnHecho.setVisibility(View.GONE);
        }
    }

    public static String formatearFecha(String fecha) {
        if (fecha != null && fecha.contains("/")) {
            String[] partes = fecha.split("/");
            if (partes.length == 3 && partes[0].length() == 4) {
                return partes[2] + "/" + partes[1] + "/" + partes[0];
            }
        }
        return fecha;
    }

    @Override
    public int getItemCount() {
        return citas.size();
    }

    public static class CitaVH extends RecyclerView.ViewHolder {
        TextView txtFecha, txtNombre, txtEmail, txtTelefono, txtBarbero, txtServicio, txtEstado, txtNotas;
        Button btnAnular, btnModificar, btnHecho;

        public CitaVH(View v) {
            super(v);
            txtFecha = v.findViewById(R.id.txtFechaHoraItem);
            txtNombre = v.findViewById(R.id.txtNombreClienteItem);
            txtBarbero = v.findViewById(R.id.txtBarberoItem);
            txtServicio = v.findViewById(R.id.txtServicioItem);
            txtEmail = v.findViewById(R.id.txtEmailClienteItem);
            txtTelefono = v.findViewById(R.id.txtTelefonoClienteItem);
            txtNotas = v.findViewById(R.id.txtNotasItem);
            txtEstado = v.findViewById(R.id.txtEstadoItem);
            btnAnular = v.findViewById(R.id.btnAnularCita);
            btnModificar = v.findViewById(R.id.btnModificarCita);
            btnHecho = v.findViewById(R.id.btnHechoCita);
        }
    }
}
