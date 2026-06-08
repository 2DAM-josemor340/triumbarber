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
        default void onHechoClick(int p) {}
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
        Cita cita = citas.get(position);

        String horaInicio = cita.getHora();
        String horaFin = horaInicio;

        try {
            String[] partes = horaInicio.split(":");
            int horaEntera = Integer.parseInt(partes[0]);
            int minutos = Integer.parseInt(partes[1]);

            if ("Corte+Barba".equalsIgnoreCase(cita.getServicio())) {
                minutos += 60;
            } else if ("Decoloracion".equalsIgnoreCase(cita.getServicio())) {
                minutos += 120;
            } else {
                minutos += 30;
            }

            if (minutos >= 60) {
                horaEntera += (minutos / 60);
                minutos = (minutos % 60);
            }

            String stringMinutos = minutos == 0 ? "00" : String.valueOf(minutos);
            horaFin = horaEntera + ":" + stringMinutos;
        } catch (Exception e) {}

        String textoFechaHora = formatearFecha(cita.getFecha()) + " | " + horaInicio + " a " + horaFin;
        h.txtFecha.setText(textoFechaHora);

        h.txtBarbero.setText("Barbero: " + cita.getBarbero());
        h.txtServicio.setText("Servicio: " + cita.getServicio());
        h.txtNotas.setText(cita.getObservaciones() != null && !cita.getObservaciones().isEmpty() ? "Notas: " + cita.getObservaciones() : "Sin observaciones");

        if (cita.getClienteNombre() != null) {
            h.txtNombre.setText("Cliente: " + cita.getClienteNombre() + " " + cita.getClienteApellidos());
            h.txtEmail.setText("Email: " + cita.getClienteEmail());
            h.txtTelefono.setText("Tlf: " + cita.getClienteTelefono());
            h.txtNombre.setVisibility(View.VISIBLE);
            h.txtEmail.setVisibility(View.VISIBLE);
            h.txtTelefono.setVisibility(View.VISIBLE);
        } else {
            h.txtNombre.setVisibility(View.GONE);
            h.txtEmail.setVisibility(View.GONE);
            h.txtTelefono.setVisibility(View.GONE);
        }

        if ("HECHO".equalsIgnoreCase(cita.getEstado())) {
            h.txtEstado.setText("FINALIZADA");
            h.txtEstado.setBackgroundColor(Color.parseColor("#2E7D32"));
            h.btnHecho.setVisibility(View.GONE);
            h.btnAnular.setVisibility(View.GONE);
            h.btnModificar.setVisibility(View.GONE);
        } else {
            h.txtEstado.setText("PENDIENTE");
            h.txtEstado.setBackgroundColor(Color.parseColor("#424242"));

            if (esModoAdmin) {
                h.btnHecho.setVisibility(View.VISIBLE);
                h.btnModificar.setVisibility(View.GONE);
                h.btnAnular.setVisibility(View.VISIBLE);
            } else {
                h.btnHecho.setVisibility(View.GONE);
                h.btnModificar.setVisibility(View.VISIBLE);
                h.btnAnular.setVisibility(View.VISIBLE);
            }
        }

        h.btnAnular.setOnClickListener(v -> {
            if (listener != null && h.getAdapterPosition() != RecyclerView.NO_POSITION) {
                listener.onAnularClick(h.getAdapterPosition());
            }
        });

        h.btnModificar.setOnClickListener(v -> {
            if (listener != null && h.getAdapterPosition() != RecyclerView.NO_POSITION) {
                listener.onModificarClick(h.getAdapterPosition());
            }
        });

        h.btnHecho.setOnClickListener(v -> {
            if (listener != null && h.getAdapterPosition() != RecyclerView.NO_POSITION) {
                listener.onHechoClick(h.getAdapterPosition());
            }
        });
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