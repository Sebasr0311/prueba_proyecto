package com.edificio.admin;

import com.edificio.admin.dao.ConexionBD;
import java.sql.*;

public class FixVisita75 {
    public static void main(String[] args) throws Exception {
        try (Connection c = ConexionBD.getInstancia().getConexion()) {
            // Get Bogota time and a valid user
            int idVigilante;
            Timestamp bogota;
            try (Statement st = c.createStatement()) {
                try (ResultSet rs = st.executeQuery("SELECT MIN(id_usuario) FROM USUARIOS")) {
                    rs.next(); idVigilante = rs.getInt(1);
                }
                try (ResultSet rs = st.executeQuery(
                    "SELECT CAST(SYSTIMESTAMP AT TIME ZONE 'America/Bogota' AS TIMESTAMP) FROM DUAL")) {
                    rs.next(); bogota = rs.getTimestamp(1);
                }
            }
            System.out.println("id_vigilante=" + idVigilante + " bogota=" + bogota);

            // Check if REGISTROS_ACCESO exists for id_visita=75
            boolean accesoExists = false;
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM REGISTROS_ACCESO WHERE id_visita = 75")) {
                rs.next(); accesoExists = rs.getInt(1) > 0;
            }
            System.out.println("REGISTROS_ACCESO exists=" + accesoExists);

            if (!accesoExists) {
                // Insert with entrada only, then update salida (check constraint: salida > entrada)
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO REGISTROS_ACCESO (id_visita, id_vigilante, hora_entrada) VALUES (?, ?, ?)")) {
                    ps.setInt(1, 75);
                    ps.setInt(2, idVigilante);
                    ps.setTimestamp(3, bogota);
                    ps.executeUpdate();
                }
                System.out.println("INSERT REGISTROS_ACCESO OK");
                Timestamp bogotaPlus1 = new Timestamp(bogota.getTime() + 1000);
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE REGISTROS_ACCESO SET hora_salida = ? WHERE id_visita = 75")) {
                    ps.setTimestamp(1, bogotaPlus1);
                    ps.executeUpdate();
                }
                System.out.println("UPDATE hora_salida OK");
            } else {
                System.out.println("REGISTROS_ACCESO ya existe, saltando INSERT");
            }

            // Force estado to FINALIZADA
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE VISITAS SET estado = 'FINALIZADA' WHERE id_visita = 75")) {
                int rows = ps.executeUpdate();
                System.out.println("UPDATE VISITAS estado=FINALIZADA: " + rows);
            }

            // Mark QR as used (triggers TRG_QR_USAR -> sets estado='ACTIVA')
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE QR_ACCESOS SET usado = 1, fecha_uso = ?, id_vigilante_uso = ? WHERE id_visita = 75")) {
                ps.setTimestamp(1, bogota);
                ps.setInt(2, idVigilante);
                int rows = ps.executeUpdate();
                System.out.println("UPDATE QR usado=1: " + rows);
            }

            // Re-set estado to FINALIZADA (TRG_QR_USAR overrode it to ACTIVA)
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE VISITAS SET estado = 'FINALIZADA' WHERE id_visita = 75")) {
                int rows = ps.executeUpdate();
                System.out.println("Re-set VISITAS estado=FINALIZADA: " + rows);
            }

            System.out.println("FIX COMPLETED");
        }
    }
}
