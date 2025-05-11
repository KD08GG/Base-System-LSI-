package com.tu.proyecto;

import org.tartarus.snowball.ext.englishStemmer; // Importar el stemmer en inglés
import java.util.*;

public class TextProcessor {

    // Lista básica de stopwords (en inglés)
    private static final Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            "the", "and", "is", "in", "at", "of", "a", "to"
    ));

    /**
     * Procesa texto crudo y devuelve un mapa: palabra -> ocurrencias.
     * Aquí se integra Snowball para lematizar palabras.
     */
    public Map<String, Integer> processText(String text) {
        Map<String, Integer> wordCounts = new HashMap<>();

        // Dividir texto usando cualquier carácter no alfanumérico
        String[] words = text.toLowerCase().split("\\W+");

        // Crear instancia del lematizador Snowball (en inglés)
        englishStemmer stemmer = new englishStemmer();

        for (String word : words) {
            if (word.isEmpty() || STOPWORDS.contains(word)) {
                continue; // Saltar vacíos y stopwords
            }

            // Aplicar lematización con Snowball
            stemmer.setCurrent(word);
            stemmer.stem();
            String stemmedWord = stemmer.getCurrent();

            // Incrementar conteo
            wordCounts.put(stemmedWord, wordCounts.getOrDefault(stemmedWord, 0) + 1);
        }

        return wordCounts;
    }
}