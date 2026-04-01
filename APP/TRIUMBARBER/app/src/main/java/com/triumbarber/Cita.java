package com.triumbarber;

import java.io.Serializable;

public class Cita implements Serializable {
    private String id;
    private String barbero;
    private String fecha;
    private String hora;
    private String clienteId;
    private String clienteEmail;
    private String clienteTelefono;
    private String clienteNombre;
    private String clienteApellidos;
    private String estado;

    public Cita() {}

    public String getId() { return id; }

    public void setId(String id) { this.id = id; }

    public String getBarbero() { return barbero; }

    public void setBarbero(String barbero) { this.barbero = barbero; }

    public String getFecha() { return fecha; }

    public void setFecha(String fecha) { this.fecha = fecha; }

    public String getHora() { return hora; }

    public void setHora(String hora) { this.hora = hora; }

    public String getClienteId() { return clienteId; }

    public void setClienteId(String clienteId) { this.clienteId = clienteId; }

    public String getClienteEmail() { return clienteEmail; }

    public void setClienteEmail(String clienteEmail) { this.clienteEmail = clienteEmail; }

    public String getClienteTelefono() { return clienteTelefono; }

    public void setClienteTelefono(String clienteTelefono) { this.clienteTelefono = clienteTelefono; }

    public String getClienteNombre() { return clienteNombre; }

    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public String getClienteApellidos() { return clienteApellidos; }

    public void setClienteApellidos(String clienteApellidos) { this.clienteApellidos = clienteApellidos; }

    public String getEstado() { return estado; }

    public void setEstado(String estado) { this.estado = estado; }
}