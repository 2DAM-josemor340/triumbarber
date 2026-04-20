package com.triumbarber;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RecuperaPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private Button btnEnviar;
    private TextView tvVolver;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperapassword);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmailRecuperar);
        btnEnviar = findViewById(R.id.btnEnviarEnlace);
        tvVolver = findViewById(R.id.tvVolverLogin);

        btnEnviar.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            if (email.isEmpty()) {
                etEmail.setError("Por favor, introduce tu email");
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Introduce un formato de email válido");
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(RecuperaPasswordActivity.this,
                                    "Enlace enviado. Por favor, revisa tu correo electrónico.",
                                    Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(RecuperaPasswordActivity.this,
                                    "Error: No se pudo enviar el correo. Verifica que el email sea correcto.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        tvVolver.setOnClickListener(v -> finish());
    }
}