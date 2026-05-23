package com.triumbarber;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SeleccionServicioActivity extends AppCompatActivity {

    private String barberoSeleccionado;
    private boolean modoEdicion = false;
    private String citaIdParaEditar = null;

    private boolean modoTelefonico = false;
    private String telNombre, telApellidos, telDni, telTelefono, telEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccion_servicio);

        barberoSeleccionado = getIntent().getStringExtra("BARBERO_SELECCIONADO");
        modoEdicion = getIntent().getBooleanExtra("MODO_EDICION", false);
        citaIdParaEditar = getIntent().getStringExtra("CITA_ID");

        modoTelefonico = getIntent().getBooleanExtra("MODO_TELEFONICO", false);
        if (modoTelefonico) {
            telNombre = getIntent().getStringExtra("TEL_NOMBRE");
            telApellidos = getIntent().getStringExtra("TEL_APELLIDOS");
            telDni = getIntent().getStringExtra("TEL_DNI");
            telTelefono = getIntent().getStringExtra("TEL_TELEFONO");
            telEmail = getIntent().getStringExtra("TEL_EMAIL");
        }

        findViewById(R.id.btnServicioCorte).setOnClickListener(v -> continuarACalendario("Corte"));
        findViewById(R.id.btnServicioBarba).setOnClickListener(v -> continuarACalendario("Barba"));
        findViewById(R.id.btnServicioCorteBarba).setOnClickListener(v -> continuarACalendario("Corte+Barba"));
        findViewById(R.id.btnServicioDecoloracion).setOnClickListener(v -> continuarACalendario("Decoloracion"));
    }

    private void continuarACalendario(String servicio) {
        Intent intent = new Intent(this, CalendarioActivity.class);
        intent.putExtra("BARBERO_SELECCIONADO", barberoSeleccionado);
        intent.putExtra("SERVICIO_SELECCIONADO", servicio);
        
        if (modoEdicion) {
            intent.putExtra("CITA_ID", citaIdParaEditar);
        }
        
        if (modoTelefonico) {
            intent.putExtra("MODO_TELEFONICO", true);
            intent.putExtra("TEL_NOMBRE", telNombre);
            intent.putExtra("TEL_APELLIDOS", telApellidos);
            intent.putExtra("TEL_DNI", telDni);
            intent.putExtra("TEL_TELEFONO", telTelefono);
            intent.putExtra("TEL_EMAIL", telEmail);
        }
        
        startActivity(intent);
        finish();
    }
}