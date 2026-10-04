package ru.student;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class TestDataGenerator {

    private static final int FILE_COUNT = 128;
    private static final int WORDS_PER_FILE = 250_000;
    private static final int VOCABULARY_SIZE = 500;

    private static final int WORDS_PER_LINE = 20;

    private static final List<String> PUNCTUATION =
            Arrays.asList("", "", "", ".", ",", ";", ":", "!", "?");

    public static void main(String[] args) throws IOException {
        Path output = args.length > 0 ? Paths.get(args[0]) : Paths.get("generated-data");

        Files.createDirectories(output);

        Random random = new Random();

        List<String> vocabulary = createVocabulary();
        double[] probabilities = createRandomProbabilities(
                vocabulary.size(), random);
        double[] cumulative = createCumulative(probabilities);

        Map<String, Long> statistics = new HashMap<>();

        writeDistribution(output.resolve("distribution.txt"),
                vocabulary, probabilities);

        for (int fileIndex = 1; fileIndex <= FILE_COUNT; fileIndex++) {
            Path file = output.resolve(
                    "text-%03d.txt".formatted(fileIndex));

            generateFile(
                    file,
                    vocabulary,
                    cumulative,
                    statistics,
                    random
            );
        }

        writeStatistics(
                output.resolve("expected-statistics.txt"),
                statistics
        );

        System.out.println("Generated data: " + output.toAbsolutePath());
        System.out.println("Files: " + FILE_COUNT);
        System.out.println("Words per file: " + WORDS_PER_FILE);
        System.out.println("Vocabulary size: " + VOCABULARY_SIZE);
    }

    private static List<String> createVocabulary() {
        List<String> vocabulary = new ArrayList<>();

        for (int i = 0; i < VOCABULARY_SIZE; i++) {
            vocabulary.add("word%04d".formatted(i));
        }

        return vocabulary;
    }

    private static double[] createRandomProbabilities(
            int size,
            Random random) {

        double[] weights = new double[size];
        double sum = 0.0;

        for (int i = 0; i < size; i++) {
            /*
             * Каждому слову назначается новое случайное положительное
             * значение. После нормализации получаются вероятности.
             */
            weights[i] = 0.05 + random.nextDouble();
            sum += weights[i];
        }

        double[] probabilities = new double[size];

        for (int i = 0; i < size; i++) {
            probabilities[i] = weights[i] / sum;
        }

        return probabilities;
    }

    private static double[] createCumulative(double[] probabilities) {
        double[] cumulative = new double[probabilities.length];

        double sum = 0.0;

        for (int i = 0; i < probabilities.length; i++) {
            sum += probabilities[i];
            cumulative[i] = sum;
        }

        cumulative[cumulative.length - 1] = 1.0;

        return cumulative;
    }

    private static void generateFile(
            Path file,
            List<String> vocabulary,
            double[] cumulative,
            Map<String, Long> statistics,
            Random random) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {

            for (int i = 0; i < WORDS_PER_FILE; i++) {
                int index = selectWord(cumulative, random);

                String word = vocabulary.get(index);

                statistics.merge(word, 1L, Long::sum);

                writer.write(formatWord(word, random));

                if ((i + 1) % WORDS_PER_LINE == 0) {
                    writer.newLine();
                } else {
                    writer.write(' ');
                }
            }
        }
    }

    private static int selectWord(
            double[] cumulative,
            Random random) {

        double value = random.nextDouble();

        int left = 0;
        int right = cumulative.length - 1;

        while (left < right) {
            int middle = (left + right) >>> 1;

            if (value < cumulative[middle]) {
                right = middle;
            } else {
                left = middle + 1;
            }
        }

        return left;
    }

    private static String formatWord(
            String word,
            Random random) {

        String result = word;

        if (random.nextInt(4) == 0) {
            result = result.toUpperCase();
        } else if (random.nextInt(4) == 0) {
            result = Character.toUpperCase(result.charAt(0))
                    + result.substring(1);
        }

        String punctuation =
                PUNCTUATION.get(random.nextInt(PUNCTUATION.size()));

        return result + punctuation;
    }

    private static void writeDistribution(
            Path file,
            List<String> vocabulary,
            double[] probabilities) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("word probability");
            writer.newLine();

            for (int i = 0; i < vocabulary.size(); i++) {
                writer.write(
                        vocabulary.get(i)
                                + " "
                                + "%.12f".formatted(probabilities[i])
                );
                writer.newLine();
            }
        }
    }

    private static void writeStatistics(
            Path file,
            Map<String, Long> statistics) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(file)) {

            writer.write("word count");
            writer.newLine();

            statistics.entrySet()
                    .stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        try {
                            writer.write(
                                    entry.getKey()
                                            + " "
                                            + entry.getValue()
                            );
                            writer.newLine();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }
}