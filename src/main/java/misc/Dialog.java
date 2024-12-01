package misc;

import javax.swing.*;
import java.awt.*;

import static java.awt.FlowLayout.CENTER;

public class Dialog extends JWindow {
    public JLabel timer;

    public Dialog() {
        Container cp = new MotionPanel(this);
        setContentPane(cp);
        cp.setLayout(new FlowLayout(CENTER, 0, 30));
        timer = new JLabel("00:00:00");
        timer.setFont(timer.getFont().deriveFont(24f));
//        timer.setBorder(BorderFactory.createLineBorder(Color.WHITE, 10));
        cp.add(timer);
//        setUndecorated(true);
//        setResizable(false);
//        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
//        pack();
        setSize(160, 100);
//        setAlwaysOnTop(true);
//        setVisible(true);

//        addWindowListener(new WindowAdapter(){
//            @Override
//            public void windowDeactivated(WindowEvent e) {
//                toFront();
//                //requestFocus();
//                //requestFocusInWindow();
//            }
//        });
    }

    public void update(long time) {
        timer.setText(String.format("%02d:%02d:%02d", time / 3600, time % 3600 / 60, time % 60));
        timer.repaint();
    }
}
