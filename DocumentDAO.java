// DocumentDAO.java
// Clase encargada de subir documentos y buscar documentos en base de datos relacional
package com.tu.proyecto;
import java.sql.*;
import java.util.*;



public class DocumentDAO {

    // 🔥 Define aquí tu URL y credenciales de conexión a base de datos
    private static final String DB_URL = "jdbc:mysql://localhost:3306/SystemDocs102";
    private static final String USER = "root";
    private static final String PASS = "Troll246!";

    /**
     * Sube documento a base de datos: almacena matriz palabra -> ocurrencias
     */
    /*public void uploadDocument(String docId, Map<String, Integer> wordCounts) {
        try {
            // 🔥 Conexión a base de datos
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);

            // Por cada palabra, insertar en tabla documents
            for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
                String word = entry.getKey();
                int count = entry.getValue();

                // Insertar fila
                PreparedStatement stmt = conn.prepareStatement(
                    "INSERT INTO documents (doc_id, word, count) VALUES (?, ?, ?)"
                );
                stmt.setString(1, docId);
                stmt.setString(2, word);
                stmt.setInt(3, count);
                stmt.executeUpdate();
            }

            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    } */

    public void uploadDocument(String docId, Map<String, Integer> wordCounts) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            // Insertar documento en la tabla TEXT
            String textInsertSql = "INSERT INTO TEXT (url, title) VALUES (?, ?)";
            try (PreparedStatement textStmt = conn.prepareStatement(textInsertSql, Statement.RETURN_GENERATED_KEYS)) {
                textStmt.setString(1, "C:\\Docs\\doc" + docId + ".txt"); // URL genérica
                textStmt.setString(2, "Documento " + docId); // Título genérico
                textStmt.executeUpdate();

                // Obtener el ID generado automáticamente para el documento
                ResultSet generatedKeys = textStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int documentId = generatedKeys.getInt(1);

                    // Insertar palabras procesadas en la tabla documents
                    String docInsertSql = "INSERT INTO documents (doc_id, word, count) VALUES (?, ?, ?)";
                    try (PreparedStatement docStmt = conn.prepareStatement(docInsertSql)) {
                        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
                            String word = entry.getKey();
                            int count = entry.getValue();

                            docStmt.setString(1, docId); // Usar docId personalizado
                            docStmt.setString(2, word);
                            docStmt.setInt(3, count);
                            docStmt.executeUpdate();
                        }
                    }

                    // Insertar datos en la tabla frequency_matrix
                    String freqInsertSql = "INSERT INTO frequency_matrix (doc_id, term, frequency) VALUES (?, ?, ?)";
                    try (PreparedStatement freqStmt = conn.prepareStatement(freqInsertSql)) {
                        for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
                            String term = entry.getKey();
                            int frequency = entry.getValue();

                            freqStmt.setString(1, docId);
                            freqStmt.setString(2, term);
                            freqStmt.setInt(3, frequency);
                            freqStmt.executeUpdate();
                        }
                    }
                } else {
                    throw new SQLException("No se generó un ID para el documento.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Busca documentos relevantes comparando distancia euclidiana
     */
    public Map<String, Double> searchDocuments(Map<String, Integer> queryCounts, String distanceType) {
        Map<String, Double> results = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT doc_id, word, count FROM documents"
            );

            // Mapa: doc_id -> (palabra -> ocurrencias)
            Map<String, Map<String, Integer>> dbDocs = new HashMap<>();

            while (rs.next()) {
                String docId = rs.getString("doc_id");
                String word = rs.getString("word");
                int count = rs.getInt("count");

                dbDocs.putIfAbsent(docId, new HashMap<>());
                dbDocs.get(docId).put(word, count);
            }

            // Calcular distancia según el tipo seleccionado
            for (Map.Entry<String, Map<String, Integer>> entry : dbDocs.entrySet()) {
                String docId = entry.getKey();
                Map<String, Integer> docCounts = entry.getValue();

                double distance = computeDistance(queryCounts, docCounts, distanceType);

                // Solo incluir si hay palabras en común (distancia > 0)
                if (distance > 0) {
                    results.put(docId, distance);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }

    private double computeDistance(Map<String, Integer> query, Map<String, Integer> doc, String distanceType) {
        switch (distanceType.toLowerCase()) {
            case "euclidean":
                return computeEuclideanDistance(query, doc);
            case "cosine":
                return computeCosineSimilarity(query, doc);
            case "dice":
                return computeDiceCoefficient(query, doc);
            default:
                throw new IllegalArgumentException("Tipo de distancia no soportado: " + distanceType);
        }
    }

    private double computeEuclideanDistance(Map<String, Integer> query, Map<String, Integer> doc) {
        double sum = 0.0;
        for (String word : query.keySet()) {
            int queryCount = query.get(word);
            int docCount = doc.getOrDefault(word, 0);
            sum += Math.pow(queryCount - docCount, 2);
        }
        return Math.sqrt(sum);
    }

    private double computeCosineSimilarity(Map<String, Integer> query, Map<String, Integer> doc) {
        double dotProduct = 0.0, queryNorm = 0.0, docNorm = 0.0;

        for (String word : query.keySet()) {
            int queryCount = query.get(word);
            int docCount = doc.getOrDefault(word, 0);
            dotProduct += queryCount * docCount;
            queryNorm += Math.pow(queryCount, 2);
            docNorm += Math.pow(docCount, 2);
        }

        return dotProduct / (Math.sqrt(queryNorm) * Math.sqrt(docNorm));
    }

    private double computeDiceCoefficient(Map<String, Integer> query, Map<String, Integer> doc) {
        double intersection = 0.0, querySum = 0.0, docSum = 0.0;

        for (String word : query.keySet()) {
            int queryCount = query.get(word);
            int docCount = doc.getOrDefault(word, 0);
            intersection += Math.min(queryCount, docCount);
            querySum += queryCount;
            docSum += docCount;
        }

        return (2 * intersection) / (querySum + docSum);
    }

    /**
     * Calcula distancia euclidiana entre query y documento
     */
    private double computeDistance(Map<String, Integer> query, Map<String, Integer> doc) {
        double sum = 0.0;

        for (String word : query.keySet()) {
            int queryCount = query.get(word);
            int docCount = doc.getOrDefault(word, 0);

            // Sumar (q - d)^2
            sum += Math.pow(queryCount - docCount, 2);
        }

        return Math.sqrt(sum);
    }
    public Map<String, Map<String, Integer>> getFrequencyMatrix() {
        Map<String, Map<String, Integer>> frequencyMatrix = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            String query = "SELECT doc_id, term, frequency FROM frequency_matrix";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    String docId = rs.getString("doc_id");
                    String term = rs.getString("term");
                    int frequency = rs.getInt("frequency");

                    frequencyMatrix.putIfAbsent(docId, new HashMap<>());
                    frequencyMatrix.get(docId).put(term, frequency);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return frequencyMatrix;
    }
}
