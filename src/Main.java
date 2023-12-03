import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {

        Quantizer quantizer = new Quantizer();

        JFrame frame = new JFrame("Vector Quantization");
        frame.setBounds(400, 100, 520, 400);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(null);

        JButton compress = new JButton("Compress");
        compress.setBounds(200, 100, 120, 50);
        compress.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Path currentFilePath;
                JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

                int response = fileChooser.showOpenDialog(null);

                if (response == JFileChooser.APPROVE_OPTION) {
                    currentFilePath = Path.of(fileChooser.getSelectedFile().getAbsolutePath());
                } else {
                    System.out.println("Operation cancelled");
                    return;
                }

                String compressedFilePath = currentFilePath.getParent().toString() + "/" +
                        currentFilePath.getFileName().toString().split("\\.")[0] + "-compressed.bin";

                try {
                    quantizer.compress(currentFilePath.toString(), compressedFilePath);

                    JLabel label = new JLabel("File successfully compressed.");
                    label.setBounds(180, 300, 200, 20);
                    frame.add(label);
                    frame.repaint();

                    new Timer(2000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            frame.remove(label);
                            frame.repaint();
                        }
                    }).start();

                } catch (FileNotFoundException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        frame.add(compress);

        JButton decompress = new JButton("Decompress");
        decompress.setBounds(200, 200, 120, 50);
        decompress.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                Path currentFilePath;
                JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());

                int response = fileChooser.showOpenDialog(null);

                if (response == JFileChooser.APPROVE_OPTION) {
                    currentFilePath = Path.of(fileChooser.getSelectedFile().getAbsolutePath());
                } else {
                    System.out.println("Operation cancelled");
                    return;
                }

                String decompressedFilePath = currentFilePath.getParent().toString() + "/" +
                        currentFilePath.getFileName().toString().split("\\.")[0] + "-decompressed.txt";

                try {
                    quantizer.decompress(currentFilePath.toString(), decompressedFilePath);

                    JLabel label = new JLabel("File successfully decompressed.");
                    label.setBounds(180, 300, 200, 20);
                    frame.add(label);
                    frame.repaint();

                    new Timer(2000, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            frame.remove(label);
                            frame.repaint();
                        }
                    }).start();

                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        frame.add(decompress);

        frame.setVisible(true);
    }
}
