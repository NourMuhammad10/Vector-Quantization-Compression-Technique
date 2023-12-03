import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Quantizer {
    public void compress(String inputFilePath, String outputFilePath) throws FileNotFoundException {
        StringBuilder originalStream = new StringBuilder();
        Integer numLines = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFilePath));
            // read file to string and count lines
            while (reader.ready()) {
                originalStream.append(reader.readLine());
                originalStream.append('\n');
                numLines++;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (originalStream.toString().isEmpty()) {
            return;
        }

        Integer codebookLength = 4;
        Integer vectorHeight = 2;
        Integer vectorWidth = 2;
        Integer vectorLength = vectorWidth * vectorHeight;
        Integer numVectors = (int) Math.pow((double) numLines, 2) / vectorLength;

        // extract vectors from strings
        String[] lines = originalStream.toString().split("\n");
        List<String[]> initialVectors = new ArrayList<>();

        for (int i = 0; i < numLines; i++) {
            initialVectors.add(lines[i].split(" "));
        }

        // build custom-sized vectors
        List<Vector<Double>> vectors = new ArrayList<>();

        for (int i = 0; i < numLines; i += vectorHeight) {
            for (int j = 0; j < numLines; j += vectorWidth) {

                Vector<Double> currentVector = new Vector<>(vectorLength);
                for (int k = 0; k < vectorHeight; k++) {
                    for (int l = 0; l < vectorWidth; l++) {
                        currentVector.add(Double.valueOf(initialVectors.get(i + k)[j + l]));

                    }
                }

                vectors.add(currentVector);
            }
        }

        List<Vector<Double>> codebook = buildCodebook(vectors, codebookLength);

//        System.out.println(codebook);
//
//        for (int i = 0; i < numVectors; i++) {
//            System.out.println(vectors.get(i) + " -> " + findClosestCodebookVector(vectors.get(i), codebook));
//        }

        // match labels to suitable vectors
        Vector<Integer> labels = new Vector<>();
        for (int i = 0; i < numVectors; i++) {
            labels.add(findClosestCodebookVector(vectors.get(i), codebook));
        }
        // save the output
        BinaryFilesHandler.writeCompressedOutput(codebook, labels, outputFilePath);

    }

    private List<Vector<Double>> buildCodebook(List<Vector<Double>> initialVectors, Integer requiredSize) {

        Integer numVectors = initialVectors.size();
        List<Vector<Double>> codebook = new ArrayList<>();

        // calculate first average vector
        Vector<Double> codebookInitialVector = new Vector<>();
        for (int i = 0; i < initialVectors.get(0).size(); i += 1) {
            Double avg = 0.0;
            for (int j = 0; j < numVectors; j += 1) {
                avg += initialVectors.get(j).get(i);
            }
            codebookInitialVector.add(avg / numVectors);
        }
        codebook.add(codebookInitialVector);

        // loop till filling codebook
        while (codebook.size() < requiredSize) {

            List<Vector<Double>> updatedCodebook = new ArrayList<>();

            // do the splitting for every codebook vector
            for (int i = 0; i < codebook.size(); i += 1) {
                updatedCodebook.add(splitVector(codebook.get(i), -1.0));
                updatedCodebook.add(splitVector(codebook.get(i), 1.0));
            }

            List<Vector<Double>> updatedCodebook2 = new ArrayList<>();

            List<Vector<Vector<Double>>> quantized = quantize(initialVectors, updatedCodebook);

            // calculate averages for every matched group separately
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

            codebook = updatedCodebook;
        }

        List<Vector<Double>> updatedCodebook2;

        // loop till no changed groups
        while (true) {

            updatedCodebook2 = new ArrayList<>();

            List<Vector<Vector<Double>>> quantized = quantize(initialVectors, codebook);

            // calculate averages for every matched group separately
            // same as previous loop
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

            // stop when no change
            if (codebook.equals(updatedCodebook2)) {
                break;
            }

            codebook = updatedCodebook2;
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

        // do the vector-codebook matching
        for (Vector<Double> inputVector : inputVectors) {
            int bestIndex = findClosestCodebookVector(inputVector, codebook);
            quantizedIndices.get(bestIndex).add(inputVector);
        }
        return quantizedIndices;
    }

    private Integer findClosestCodebookVector(Vector<Double> inputVector, List<Vector<Double>> codebook) {
        // find index of best matching codebook vector to the input vector
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

    public static void main(String[] args) throws IOException {
//        String path = System.getProperty("user.dir") + "/compression" + ".bin";
//        Quantizer quantizer = new Quantizer();
//        quantizer.compress("input.txt", path);
//        quantizer.decompress(path);

        Quantizer quantizer = new Quantizer();
        quantizer.compress("input.txt", "compression.bin");
        quantizer.decompress("compression.bin", "decompression.txt");
    }

    public void decompress(String compressedFilePath, String decompressedFilePath) throws IOException {
        Vector<Integer> compressionValues = BinaryFilesHandler.readCompressedFile(compressedFilePath);
        int codebookSize = compressionValues.get(0);
        int singleVectorLength = compressionValues.get(1);
        int numberOfVectors = compressionValues.get(2);
        Vector<Vector<Integer>> codebook = new Vector<>();
        int endOverHead = 3 + (codebookSize * singleVectorLength);
        for (int i = 3; i < endOverHead; i++) {
            Vector<Integer> rowInCodebook = new Vector<>(singleVectorLength);
            for (int j = 0; j < singleVectorLength; j++) {
                rowInCodebook.add(compressionValues.get(i++));
            }
            codebook.add(rowInCodebook);
            i--;
        }
        int originalSize = numberOfVectors * singleVectorLength;
        int originalDimension = (int) Math.sqrt(originalSize);
        int blockDimension = (int) Math.sqrt(singleVectorLength);
        Vector<String> original = new Vector<>();
        int numBlockInRow = originalDimension / blockDimension;
        for (int i = endOverHead; i < compressionValues.size(); i++) {
            Vector<Vector<Integer>> tempCodebook = new Vector<>();
            int temp = numBlockInRow;
            while (temp != 0) {
                tempCodebook.add(codebook.get(compressionValues.get(i)));
                i++;
                temp--;
            }
            int times = blockDimension * blockDimension;
            int index = 0;
            while (index < times) {
                StringBuilder row = new StringBuilder();
                for (int j = 0; j < tempCodebook.size(); j++) {
                    int temp2 = index;
                    while (temp2 != (index + blockDimension)) {
                        row.append(Integer.toString(tempCodebook.get(j).get(temp2)));
                        row.append(" ");
                        temp2++;
                    }
                }
                index += blockDimension;
                original.add(row.toString());
            }
            i--;
        }
//        File file = new File(System.getProperty("user.dir") + "/decompressedOutput" + ".txt");
        File file = new File(decompressedFilePath);
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        try {
            for (String row : original) {
                StringBuilder sb = new StringBuilder(row);
                sb.deleteCharAt(row.length() - 1);
                writer.write(sb.toString());
                writer.write("\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
