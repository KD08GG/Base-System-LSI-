package com.tu.proyecto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Interfaz gráfica principal para subir documentos y realizar consultas.
 */
public class MainGUI {
    private JTextArea inputArea;
    private JButton uploadButton;
    private JButton searchButton;
    private JButton showMatrixButton; // Nuevo botón para mostrar la matriz de frecuencia
    private JTextArea resultArea;
    private JPanel mainPanel;
    private JComboBox<String> distanceComboBox;

    // Instancias de procesador de texto y DAO para DB
    private TextProcessor processor = new TextProcessor();
    private DocumentDAO dao = new DocumentDAO();

    public MainGUI() {
        // Crear componentes
        inputArea = new JTextArea(5, 30); // Área de entrada de texto
        uploadButton = new JButton("Upload document"); // Botón para subir documentos
        searchButton = new JButton("Search"); // Botón para buscar documentos
        showMatrixButton = new JButton("Show Frequency Matrix"); // Botón para mostrar la matriz de frecuencia
        resultArea = new JTextArea(10, 30);
        resultArea.setEditable(false); // Resultados no editables
        mainPanel = new JPanel();
        distanceComboBox = new JComboBox<>(new String[]{"euclidean", "cosine", "dice"}); // Selector de distancia

        // Configurar layout principal
        mainPanel.setLayout(new BorderLayout());

        // Panel superior
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        // Agregar un ejemplo predefinido en el área de entrada
        inputArea.setText("How does technology influence culture?");
        topPanel.add(new JScrollPane(inputArea), BorderLayout.CENTER);

        // Panel de botones
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(uploadButton);
        buttonPanel.add(searchButton);
        buttonPanel.add(showMatrixButton); // Agregar el nuevo botón aquí
        buttonPanel.add(new JLabel("Distance:")); // Etiqueta para el JComboBox
        buttonPanel.add(distanceComboBox);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Agregar componentes al panel principal
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Acción al presionar "Upload document"
        uploadButton.addActionListener(e -> {
            String text = inputArea.getText();

            // Procesar texto: eliminar stopwords y lematizar
            Map<String, Integer> wordCounts = processor.processText(text);

            // Generar ID único para documento
            String docId = "doc_" + System.currentTimeMillis();

            // Subir documento a base de datos
            dao.uploadDocument(docId, wordCounts);

            // Mostrar confirmación
            resultArea.setText("Documento subido con ID: " + docId);
        });

        // Acción al presionar "Search"
        searchButton.addActionListener(e -> {
            String query = inputArea.getText();

            // Procesar query con mismo pipeline que documentos
            Map<String, Integer> queryCounts = processor.processText(query);

            // Obtener el tipo de distancia seleccionada
            String selectedDistance = (String) distanceComboBox.getSelectedItem();

            // Buscar documentos relevantes en base de datos
            Map<String, Double> results = dao.searchDocuments(queryCounts, selectedDistance);

            // Mostrar resultados ordenados por relevancia (distancia)
            String display = results.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue())
                    .map(entry -> entry.getKey() + " (distancia: " + entry.getValue() + ")")
                    .collect(Collectors.joining("\n"));

            // Mejorar la visualización de los resultados
            resultArea.setText("Consulta realizada usando distancia: " + selectedDistance + "\n\n" +
                    (display.isEmpty() ? "No se encontraron documentos relevantes." : display));
        });

        // Acción al presionar "Show Frequency Matrix"
        showMatrixButton.addActionListener(e -> {
            // Obtener la matriz de frecuencia desde la base de datos
            Map<String, Map<String, Integer>> frequencyMatrix = dao.getFrequencyMatrix();

            // Construir el texto para mostrar en el área de resultados
            StringBuilder display = new StringBuilder();
            for (Map.Entry<String, Map<String, Integer>> entry : frequencyMatrix.entrySet()) {
                String docId = entry.getKey();
                Map<String, Integer> terms = entry.getValue();

                display.append("Documento: ").append(docId).append("\n");
                for (Map.Entry<String, Integer> termEntry : terms.entrySet()) {
                    String term = termEntry.getKey();
                    int frequency = termEntry.getValue();
                    display.append("  ").append(term).append(": ").append(frequency).append("\n");
                }
                display.append("\n");
            }

            // Mostrar la matriz de frecuencia en el área de resultados
            resultArea.setText(display.toString());
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Buscador de Documentos");
        frame.setContentPane(new MainGUI().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}