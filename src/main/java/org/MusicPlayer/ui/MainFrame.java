package org.MusicPlayer.ui;

import org.MusicPlayer.player.Track;
import org.MusicPlayer.player.PlaybackController;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

public class MainFrame extends JFrame {

    private final PlaybackController controller;
    private final JTable table;
    private final PlayerBar bar;

    public MainFrame(PlaybackController controller, List<Track> tracks) {
        super("Music Player");
        this.controller = controller;
        controller.setQueue(tracks);

        DefaultTableModel model = new DefaultTableModel(new String[]{"Título", "Artista", "Duración"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (Track t : tracks) {
            model.addRow(new Object[]{t.title(), t.artist(), formatDuration(t.durationSeconds())});
        }

        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (e.getClickCount() == 2 && row >= 0) {
                    controller.playAt(row);
                    bar.refresh();
                }
            }
        });

        bar = new PlayerBar(controller, this::syncSelection);

        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bar, BorderLayout.SOUTH);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controller.close();
            }
        });
        setSize(700, 450);
        setLocationRelativeTo(null);
    }

    private void syncSelection() {
        int i = controller.currentIndex();
        if (i >= 0) {
            table.setRowSelectionInterval(i, i);
        }
    }

    private static String formatDuration(int seconds) {
        return seconds <= 0 ? "-" : "%d:%02d".formatted(seconds / 60, seconds % 60);
    }
}