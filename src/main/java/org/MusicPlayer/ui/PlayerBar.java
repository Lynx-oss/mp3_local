package org.MusicPlayer.ui;

import org.MusicPlayer.player.PlaybackController;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.*;

public class PlayerBar extends JPanel{
    private final PlaybackController controller;
    private final Runnable onTrackChanged;

    private final JLabel songLabel = new JLabel();
    private final JButton playButton = new JButton("play");
    private final JSlider progress = new JSlider(0, 1, 0);
    private final JSlider volume = new JSlider(0, 100, 100);

    public PlayerBar(PlaybackController controller, Runnable onTrackChanged) {
        super(new BorderLayout(12, 0));
        this.controller = controller;
        this.onTrackChanged = onTrackChanged;

        JButton prev = new JButton("Anterior");
        JButton next = new JButton("Siguiente");
        prev.addActionListener(e -> { controller.previous(); changed(); });
        next.addActionListener(e -> { controller.next(); changed(); });
        playButton.addActionListener(e -> { controller.togglePlayPause(); refresh(); });

        // Seek solo al soltar el mouse, para no mandar un comando por cada pixel arrastrado.
        progress.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                controller.seek(progress.getValue());
            }
        });
        volume.addChangeListener(e -> controller.setVolume(volume.getValue()));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        buttons.add(prev);
        buttons.add(playButton);
        buttons.add(next);

        JPanel center = new JPanel(new BorderLayout());
        center.add(buttons, BorderLayout.NORTH);
        center.add(progress, BorderLayout.SOUTH);

        songLabel.setPreferredSize(new Dimension(260, 40));
        volume.setPreferredSize(new Dimension(120, 40));
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(songLabel, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(volume, BorderLayout.EAST);

        refresh();
        new Timer(1000, e -> refresh()).start(); 
    }

    public void refresh() {
        controller.current().ifPresentOrElse(
                t -> songLabel.setText(t.title() + " - " + t.artist()),
                () -> songLabel.setText("Nada reproduciéndose"));
        playButton.setText(controller.isPlaying() ? "Pausa" : "Play");

        int duration = (int) controller.duration();
        if (duration > 0 && progress.getMaximum() != duration) {
            progress.setMaximum(duration);
        }
        if (!progress.getValueIsAdjusting()) {
            progress.setValue((int) controller.position());
        }
    }

    private void changed() {
        refresh();
        onTrackChanged.run();
    }
}
