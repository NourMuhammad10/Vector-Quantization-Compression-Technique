import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Quantizer {
    void compress(String inputFilePath, String outputFilePath) {
        String originalStream = "";
        Integer numLines = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
            while (reader.ready()) {
                originalStream += reader.readLine();
                originalStream += '\n';
                numLines++;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (originalStream.equals("")) {
            return;
        }

        Integer numVectors = (int) Math.pow((double) numLines / 2, 2);
        Integer vectorLength = 4;
        Integer codebookLength = 4;

        String[] lines = originalStream.split("\n");
        List<String[]> initialVectors = new ArrayList<>();

        for (int i = 0; i < numLines; i++) {
            initialVectors.add(lines[i].split(" "));
        }

        List<Vector<Double>> vectors = new ArrayList<>();

        for (int i = 0; i < numLines; i += 2) {
            for (int j = 0; j < numLines; j += 2) {
                Vector<Double> currentVector = new Vector<>(4);

                currentVector.add(Double.valueOf(initialVectors.get(i)[j]));
                currentVector.add(Double.valueOf(initialVectors.get(i)[j + 1]));
                currentVector.add(Double.valueOf(initialVectors.get(i + 1)[j]));
                currentVector.add(Double.valueOf(initialVectors.get(i + 1)[j + 1]));

                vectors.add(currentVector);
            }
        }

//        for (int i = 0; i < numVectors; i += 1) {
//            for (int j = 0; j < vectorLength; j += 1) {
//                System.out.print(vectors.get(i).get(j) + " ");
//            }
//            System.out.println();
//        }

        List<Vector<Double>> codebook = buildCodebook(vectors, codebookLength);

        System.out.println(codebook);

        for (int i = 0; i < numVectors; i++) {
            System.out.println(vectors.get(i) + " -> " + findClosestCodebookVector(vectors.get(i), codebook));
        }

    }

    private List<Vector<Double>> buildCodebook(List<Vector<Double>> initialVectors, Integer requiredSize) {

        Integer numVectors = initialVectors.size();
        List<Vector<Double>> codebook = new ArrayList<>();

        Vector<Double> codebookInitialVector = new Vector<>();
        for (int i = 0; i < initialVectors.get(0).size(); i += 1) {
            Double avg = 0.0;
            for (int j = 0; j < numVectors; j += 1) {
                avg += initialVectors.get(j).get(i);
            }
//            System.out.println(avg / numVectors);
            codebookInitialVector.add(avg / numVectors);
        }

        codebook.add(codebookInitialVector);

        while (codebook.size() < requiredSize) {

            List<Vector<Double>> updatedCodebook = new ArrayList<>();

            for (int i = 0; i < codebook.size(); i += 1) {
                updatedCodebook.add(splitVector(codebook.get(i), -1.0));
                updatedCodebook.add(splitVector(codebook.get(i), 1.0));
            }

//            for (int i = 0; i < numVectors; i++) {
//                System.out.println("vector " + i + " " + initialVectors.get(i));
//            }
//
//            for (int i = 0; i < updatedCodebook.size(); i++) {
//                System.out.println("codebook " + i + " " + updatedCodebook.get(i));
//            }

            List<Vector<Double>> updatedCodebook2 = new ArrayList<>();

            List<Vector<Vector<Double>>> quantized = quantize(initialVectors, updatedCodebook);

//            System.out.println(quantized);

            for (int i = 0; i < quantized.size(); i++) {

                Vector<Double> codeBookVector = new Vector<>();
                for (int j = 0; j < quantized.get(i).get(0).size(); j += 1) {
                    Double avg = 0.0;
                    for (int k = 0; k < quantized.get(i).size(); k++) {
                        avg += quantized.get(i).get(k).get(j);
                    }
                    codeBookVector.add(avg / quantized.get(i).size());
                }
                updatedCodebook2.add(codeBookVector);

            }

//            System.out.println(updatedCodebook2);

            codebook = updatedCodebook;
//            System.out.println("Size = " + codebook.size());

        }

        List<Vector<Double>> updatedCodebook2;

        while (true) {

            updatedCodebook2 = new ArrayList<>();

            List<Vector<Vector<Double>>> quantized = quantize(initialVectors, codebook);

//            System.out.println(quantized);

            for (int i = 0; i < quantized.size(); i++) {

                Vector<Double> codeBookVector = new Vector<>();
                for (int j = 0; j < quantized.get(i).get(0).size(); j += 1) {
                    Double avg = 0.0;
                    for (int k = 0; k < quantized.get(i).size(); k++) {
                        avg += quantized.get(i).get(k).get(j);
                    }
                    codeBookVector.add(avg / quantized.get(i).size());
                }
                updatedCodebook2.add(codeBookVector);

            }

//            System.out.println(updatedCodebook2);

            if (codebook.equals(updatedCodebook2)) {
                break;
            }

            codebook = updatedCodebook2;
//            System.out.println("Size = " + codebook.size());

        }

        return updatedCodebook2;
    }

    private Vector<Double> splitVector(Vector<Double> initialVector, Double scale) {
        Vector<Double> split = new Vector<>(initialVector.size());
        for (int i = 0; i < initialVector.size(); i++) {
            if (initialVector.get(i) % 1 == 0) {
                split.add(initialVector.get(i) + (scale));
            } else if (scale > 0) {
                split.add(Math.ceil(initialVector.get(i)));
            } else if (scale < 0) {
                split.add(Math.floor(initialVector.get(i)));
            }
        }
        return split;
    }

    private List<Vector<Vector<Double>>> quantize(List<Vector<Double>> inputVectors, List<Vector<Double>> codebook) {

        // group vectors in a vector with index equal to its index in codebook

        List<Vector<Vector<Double>>> quantizedIndices = new ArrayList<>(codebook.size());

        for (int i = 0; i < codebook.size(); i++) {
            quantizedIndices.add(new Vector<>());
        }

        for (Vector<Double> inputVector : inputVectors) {
            int bestIndex = findClosestCodebookVector(inputVector, codebook);
            quantizedIndices.get(bestIndex).add(inputVector);
        }
        return quantizedIndices;
    }

    private List<Integer> vectorQuantization(List<Vector<Double>> inputVectors, List<Vector<Double>> codebook) {
        List<Integer> quantizedIndices = new ArrayList<>();
        for (Vector<Double> inputVector : inputVectors) {
            int bestIndex = findClosestCodebookVector(inputVector, codebook);
            quantizedIndices.add(bestIndex);
        }
        return quantizedIndices;
    }

    private Integer findClosestCodebookVector(Vector<Double> inputVector, List<Vector<Double>> codebook) {
        Integer bestIndex = 0;
        Double bestDistance = Double.MAX_VALUE;
        for (int i = 0; i < codebook.size(); i++) {
            Double distance = calculateDistance(inputVector, codebook.get(i));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private Double calculateDistance(Vector<Double> vector1, Vector<Double> vector2) {
        Double sum = 0.0;
        for (int i = 0; i < vector1.size(); i++) {
            sum += Math.pow(vector1.get(i) - vector2.get(i), 2);
        }
        return Math.sqrt(sum);
    }

    public static void main(String[] args) {

    }

    void decompress(String inputFilePath, String outputFilePath) {
        String compressedStream = "";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath));
            writer.write(compressedStream);
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
