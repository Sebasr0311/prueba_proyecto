package com.edificio.admin;

import com.edificio.admin.dao.ConexionBD;
import java.sql.*;

public class CheckAtp {
    public static void main(String[] args) throws Exception {
        try (Connection c = ConexionBD.getInstancia().getConexion();
             Statement st = c.createStatement()) {

            // Buggy active QRs (exp-gen > 2 hours = SYSTIMESTAMP bug)
            System.out.println("=== QRs activos bugueados (usado=0, exp-gen > 2h) ===");
            try (ResultSet rs = st.executeQuery(
                    "SELECT q.id_qr, q.fecha_generacion, q.fecha_expiracion, q.usado, " +
                    "       ROUND((CAST(q.fecha_expiracion AS DATE) - CAST(q.fecha_generacion AS DATE)) * 1440, 0) AS diff_min, " +
                "       v.id_visita, vt.nombres||' '||vt.apellidos AS visitante " +
                "FROM   QR_ACCESOS q " +
                "JOIN   VISITAS v ON q.id_visita = v.id_visita " +
                "JOIN   REGISTRO_VISITA rv ON v.id_visita = rv.id_visita AND rv.es_titular = 1 " +
                "JOIN   VISITANTES vt ON rv.id_visitante = vt.id_visitante " +
                "WHERE  q.usado = 0 " +
                    "  AND (CAST(q.fecha_expiracion AS DATE) - CAST(q.fecha_generacion AS DATE)) * 1440 > 120 " +
                "ORDER BY q.id_qr")) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.println("QR id=" + rs.getInt("id_qr")
                        + " gen=" + rs.getTimestamp("fecha_generacion")
                        + " exp=" + rs.getTimestamp("fecha_expiracion")
                        + " diff_min=" + rs.getBigDecimal("diff_min")
                        + " visita=" + rs.getInt("id_visita")
                        + " visitante=" + rs.getString("visitante"));
                }
                if (!found) System.out.println("(ninguno)");
            }

            // Roberto Castillo Medina
            System.out.println("\n=== Buscar Roberto Castillo Medina ===");
            try (ResultSet rs = st.executeQuery(
                "SELECT v.id_visita, v.estado, v.fecha_registro, " +
                "       r.id_residente, r.nombres||' '||r.apellidos AS residente, " +
                "       a.numero AS apto, " +
                "       vt.id_visitante, vt.numero_documento, vt.nombres||' '||vt.apellidos AS visitante " +
                "FROM   VISITAS v " +
                "JOIN   RESIDENTES r ON v.id_residente = r.id_residente " +
                "JOIN   CONTRATO_RESIDENTE cr ON v.id_contrato_res = cr.id_contrato_res " +
                "JOIN   CONTRATOS c ON cr.id_contrato = c.id_contrato " +
                "JOIN   APARTAMENTOS a ON c.id_apartamento = a.id_apartamento " +
                "JOIN   REGISTRO_VISITA rv ON v.id_visita = rv.id_visita AND rv.es_titular = 1 " +
                "JOIN   VISITANTES vt ON rv.id_visitante = vt.id_visitante " +
                "WHERE  vt.numero_documento = '20000002' " +
                "ORDER BY v.id_visita DESC")) {
                boolean found = false;
                while (rs.next()) {
                    found = true;
                    System.out.println("Visita id=" + rs.getInt("id_visita")
                        + " estado=" + rs.getString("estado")
                        + " fecha=" + rs.getTimestamp("fecha_registro")
                        + " residente=" + rs.getString("residente")
                        + " apto=" + rs.getString("apto")
                        + " visitante=" + rs.getString("visitante")
                        + " doc=" + rs.getString("numero_documento"));
                    
                    // Check for registro de acceso (entry)
                    int idVisita = rs.getInt("id_visita");
                    try (Statement st2 = c.createStatement();
                         ResultSet rs2 = st2.executeQuery(
                            "SELECT id_acceso, hora_entrada, hora_salida FROM REGISTROS_ACCESO WHERE id_visita = " + idVisita)) {
                        while (rs2.next()) {
                            System.out.println("  Acceso: id=" + rs2.getInt("id_acceso")
                                + " entrada=" + rs2.getTimestamp("hora_entrada")
                                + " salida=" + rs2.getTimestamp("hora_salida"));
                        }
                    }
                    // Check QR
                    try (Statement st3 = c.createStatement();
                         ResultSet rs3 = st3.executeQuery(
                            "SELECT id_qr, codigo_qr, fecha_generacion, fecha_expiracion, usado FROM QR_ACCESOS WHERE id_visita = " + idVisita)) {
                        while (rs3.next()) {
                            System.out.println("  QR: id=" + rs3.getInt("id_qr")
                                + " gen=" + rs3.getTimestamp("fecha_generacion")
                                + " exp=" + rs3.getTimestamp("fecha_expiracion")
                                + " usado=" + rs3.getInt("usado"));
                        }
                    }
                }
                if (!found) System.out.println("(no encontrado)");
            }
        }
    }
}
