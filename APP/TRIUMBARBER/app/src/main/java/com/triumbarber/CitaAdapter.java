package com.triumbarber;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


public class CitaAdapter extends RecyclerView.Adapter<CitaAdapter.CitaVH> {
    private List<Cita> citas;
    private boolean esModoAdmin = false;





    public CitaAdapter(List<Cita> citas) {
        this.citas = citas;
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
        TextView txtFecha, txtNombre, txtBarbero, txtEstado;

        public CitaVH(View v) {
            super(v);
            txtFecha = v.findViewById(R.id.txtFechaHoraItem);
            txtNombre = v.findViewById(R.id.txtNombreClienteItem);
            txtBarbero = v.findViewById(R.id.txtBarberoItem);
        }
    }
}
