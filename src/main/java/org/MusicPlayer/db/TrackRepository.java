package org.MusicPlayer.db;

import org.MusicPlayer.player.Track;

import javax.xml.transform.Result;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TrackRepository {
    private final String url;

    private Connection connect() throws SQLException{
        return DriverManager.getConnection(url);
    }

    private void initTable(){
        String sql = """
                CREATE TABLE IF NOT EXISTS tracks (
                id TEXT PRIMARY KEY,
                title  TEXT NOT NULL,
                artist TEXT NOT NULL,
                duration_seconds  INTEGER NOT NULL,
                source TEXT NOT NULL,
                position INTEGER NOT NULL
                );
                """;

        try (Connection conn = connect();
        Statement stmt = conn.createStatement()){
            stmt.execute(sql);
        } catch (SQLException e){
            throw new RuntimeException("Error iniciando la tabla de Tracks", e);

        }


    }

    public TrackRepository(Path dbPath){
        try{
            if(dbPath.getParent() != null){
                Files.createDirectories(dbPath.getParent());
            }
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio", e);
        }
        this.url = "jdbc:sqlite:" + dbPath.toAbsolutePath();
        initTable();
    }

    public List<Track> findAll(){
        String sql = "SELECT id, title, artist, duration_seconds, source FROM tracks ORDER BY position ASC";
        List<Track> list = new ArrayList<>();

        try(Connection conn = connect();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){
            while(rs.next()){
                list.add(new Track(
                        rs.getString("id"),
                        rs.getString("Title"),
                        rs.getString("artist"),
                        rs.getInt("duration_seconds"),
                        rs.getString("source")
                ));
            }
        } catch (SQLException e){
            throw new RuntimeException("Error leyendo tracks de la bd", e);
        }
        return list;
    }

    public void saveAll(List<Track> tracks){
        String deleteSql = "DELETE FROM tracks";
        String insertSql = """
                INSERT INTO tracks (id, title, artist, duration_seconds, source, position)
                VALUES(?,?,?,?,?,?);
                """;
        try(Connection conn = connect()){
            conn.setAutoCommit(false);
            try(Statement delStmt = conn.createStatement();
                PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                delStmt.execute(deleteSql);
                for (int i = 0; i < tracks.size(); i++) {
                    Track t = tracks.get(i);
                    insertStmt.setString(1, t.id());
                    insertStmt.setString(2, t.title());
                    insertStmt.setString(3, t.artist());
                    insertStmt.setInt(4, t.durationSeconds());
                    insertStmt.setString(5, t.source());
                    insertStmt.setInt(6, i);
                    insertStmt.addBatch();

                }
                insertStmt.executeBatch();
                conn.commit();
            } catch (SQLException  e) {
                conn.rollback();
                throw e;
        }
    } catch (SQLException e){
            throw new RuntimeException("Error guardando tracks en la db", e);
        }

        }

}
