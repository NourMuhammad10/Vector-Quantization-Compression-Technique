import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class Quantizer {
    public int getImageHeight() {
        return imageHeight;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    int imageHeight;

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    int imageWidth;
    public void compress(String inputFilePath, String outputFilePath) throws FileNotFoundException {

        Vector<Vector<Integer>> imageVector = null;
        int width = 0;
        int height = 0;

        try {
            BufferedImage image = ImageIO.read(new File(inputFilePath));
            width = image.getWidth();
            height = image.getHeight();
            setImageHeight(height);
            setImageWidth(width);

//            System.out.println(width);
//            System.out.println(height);
//            System.out.println();

            imageVector = new Vector<>(height);

            for (int i = 0; i < width; i++) {
                imageVector.add(i, new Vector<>(height));
                for (int j = 0; j < height; j++) {
                    int rgb = image.getRGB(i, j);
                    int red = (rgb >> 16) & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = rgb & 0xFF;
                    int gray = (red + green + blue) / 3;

                    imageVector.get(i).add(j, (Integer) gray);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        Integer codebookLength = 8;
        Integer vectorHeight = 2;
        Integer vectorWidth = 2;
        int temp = width;
        width = height;
        height = temp;

        // build custom-sized vectors
        Vector<Vector<Vector<Integer>>> vectors = new Vector<>();

        for (int i = 0; i < height; i += vectorHeight) {
            for (int j = 0; j < width; j += vectorWidth) {
                Vector<Vector<Integer>> row = new Vector<>();
                for (int k = i; k < i + vectorHeight; k++) {
                    Vector<Integer> currentVector = new Vector<>();
                    for (int l = j; l < j + vectorWidth; l++) {
                        currentVector.add(imageVector.get(k).get(l));
                    }
                    row.add(currentVector);
                }

                vectors.add(row);
            }
        }

        Vector<Vector<Vector<Integer>>> codebook = buildCodebook(vectors, codebookLength);
//        System.out.println(codebook.size());
//        System.out.println(codebook);

//        System.out.println(imageVector);
//        System.out.println(codebook);
//
//        for (int i = 0; i < numVectors; i++) {
//            System.out.println(vectors.get(i) + " -> " + findClosestCodebookVector(vectors.get(i), codebook));
//        }

        // match labels to suitable vectors
        Vector<Integer> labels = new Vector<>();
        for (Vector<Vector<Integer>> v : vectors) {
            labels.add(findClosestCodebookVector(v, codebook));
        }

//        System.out.println(labels.size());
        BinaryFilesHandler.writeCompressedOutput(codebook, labels, outputFilePath, height, width);

//        BufferedImage image = new BufferedImage(height, width, BufferedImage.TYPE_INT_RGB);
//        Graphics2D g2d = image.createGraphics();
//        g2d.setColor(Color.WHITE);
//        g2d.fillRect(0, 0, height, width);
//
//        int position = 0;
//
//        for (int i = 0; i < height; i += vectorHeight) {
//            for (int j = 0; j < width; j += vectorWidth) {
//
//                Vector<Vector<Integer>> currentVector = getVector(labels.get(position), codebook);
//                for (int k = 0; k < currentVector.size(); k++) {
//                    for (int l = 0; l < currentVector.get(k).size(); l++) {
//
//                        int label = currentVector.get(k).get(l);
//
//                        Color color = new Color(label, label, label);
//                        int pixelX = i + k;
//                        int pixelY = j + l;
//                        g2d.setColor(color);
//                        g2d.fillRect(pixelX, pixelY, 1, 1);
//
//                    }
//                }
//
//                position++;
//            }
//        }
//
//        g2d.dispose();
//
//        try {
//            ImageIO.write(image, "png", new File("result.png"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

    }

    private Vector<Vector<Integer>> getVector(int index, Vector<Vector<Vector<Integer>>> codebook) {
        return codebook.get(index);
    }

    private Vector<Vector<Vector<Integer>>> buildCodebook(Vector<Vector<Vector<Integer>>> initialVectors, Integer requiredSize) {

        Integer numVectors = initialVectors.size();
        Vector<Vector<Vector<Integer>>> codebook = new Vector<>();

        // calculate first average vector
        Vector<Vector<Integer>> codebookInitialVector = new Vector<>();
        for (int i = 0; i < 2; i++) {
            Vector<Integer> currentAverage = new Vector<>();
            for (int j = 0; j < 2; j++) {
                int avg = 0;
                for (int k = 0; k < numVectors; k++) {
                    avg += initialVectors.get(k).get(i).get(j);
                }
                currentAverage.add(avg / numVectors);
            }
            codebookInitialVector.add(currentAverage);
        }
        codebook.add(codebookInitialVector);

        // loop till filling codebook
        while (codebook.size() < requiredSize) {

            Vector<Vector<Vector<Integer>>> updatedCodebook = new Vector<>();

            // do the splitting for every codebook vector
            for (int i = 0; i < codebook.size(); i += 1) {
                Vector<Vector<Integer>> code1 = new Vector<>(), code2 = new Vector<>();
                for (int j = 0; j < codebook.get(i).size(); j++) {
                    Vector<Integer> temp1 = new Vector<>();
                    Vector<Integer> temp2 = new Vector<>();
                    for (int k = 0; k < codebook.get(i).get(0).size(); k++) {
                        temp1.add((int) Math.floor(codebook.get(i).get(j).get(k)));
                        temp2.add((int) (Math.floor(codebook.get(i).get(j).get(k)) + 1));

                    }
                    code1.add(temp1);
                    code2.add(temp2);
                }
                updatedCodebook.add(code1);
                updatedCodebook.add(code2);
            }

            Vector<Vector<Vector<Integer>>> updatedCodebook2 = new Vector<>();

            Vector<Vector<Vector<Vector<Integer>>>> quantized = quantize(initialVectors, updatedCodebook);

            // calculate averages for every matched group separately
            for (int i = 0; i < quantized.size(); i++) {

                Vector<Vector<Integer>> codeBookVector = new Vector<>();
                for (int j = 0; j < 2; j++) {
                    Vector<Integer> currentAverage = new Vector<>();
                    for (int k = 0; k < 2; k++) {
                        int avg = 0;
                        for (int l = 0; l < quantized.get(i).size(); l++) {
                            avg += quantized.get(i).get(l).get(j).get(k);
                        }
                        currentAverage.add(avg / quantized.get(i).size());
                    }
                    codeBookVector.add(currentAverage);
                }
                updatedCodebook2.add(codeBookVector);

            }

            codebook = updatedCodebook2;
        }

        Vector<Vector<Vector<Integer>>> updatedCodebook2;

        // loop till no changed groups
//        while (true) {
//            updatedCodebook2 = new Vector<>();
//
//            Vector<Vector<Vector<Vector<Integer>>>> quantized = quantize(initialVectors, codebook);
//
//            // calculate averages for every matched group separately
//            // same as previous loop
//            for (int i = 0; i < quantized.size(); i++) {
//
//                Vector<Vector<Integer>> codeBookVector = new Vector<>();
//                for (int j = 0; j < 2; j++) {
//                    Vector<Integer> currentAverage = new Vector<>();
//                    for (int k = 0; k < 2; k++) {
//                        int avg = 0;
//                        for (int l = 0; l < quantized.get(i).size(); l++) {
//                            avg += quantized.get(i).get(l).get(j).get(k);
//                        }
//                        currentAverage.add(avg / quantized.get(i).size());
//                    }
//                    codeBookVector.add(currentAverage);
//                }
//                updatedCodebook2.add(codeBookVector);
//
//            }
//
//            // stop when no change
//            if (codebook.equals(updatedCodebook2)) {
//                break;
//            }
//
//            codebook = updatedCodebook2;
//        }

        return codebook;
    }

    private Vector<Vector<Vector<Vector<Integer>>>> quantize(Vector<Vector<Vector<Integer>>> inputVectors, Vector<Vector<Vector<Integer>>> codebook) {

        // group vectors in a vector with index equal to its index in codebook

        Vector<Vector<Vector<Vector<Integer>>>> quantizedIndices = new Vector<>(codebook.size());

        for (int i = 0; i < codebook.size(); i++) {
            quantizedIndices.add(new Vector<>());
        }

        // do the vector-codebook matching
        for (Vector<Vector<Integer>> inputVector : inputVectors) {

            int bestIndex = findClosestCodebookVector(inputVector, codebook);
            quantizedIndices.get(bestIndex).add(inputVector);

        }
        return quantizedIndices;
    }

    private Integer findClosestCodebookVector(Vector<Vector<Integer>> inputVector, Vector<Vector<Vector<Integer>>> codebook) {
        // find index of best matching codebook vector to the input vector
        Integer bestIndex = 0;
        Integer bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < codebook.size(); i++) {
            Integer distance = calculateDistance(inputVector, codebook.get(i));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private Integer calculateDistance(Vector<Vector<Integer>> vector1, Vector<Vector<Integer>> vector2) {
        int sum = 0;
        for (int i = 0; i < vector1.size(); i++) {
            for (int j = 0; j < vector1.size(); j++) {
                sum += (int) Math.pow(vector1.get(i).get(j) - vector2.get(i).get(j), 2);
            }
        }
        return (int) Math.sqrt(sum);
    }

    public void decompress(String compressedFilePath, String decompressedFilePath){
        int imageHeight = getImageHeight();
        int imageWidth = getImageWidth();
        Vector<Integer> compressionValues = BinaryFilesHandler.readCompressedFile(compressedFilePath);
        int overHeadSize = compressionValues.get(0) & 0xff;
        int codebookLength = compressionValues.get(1) & 0xff;
        int vectorHeight = compressionValues.get(2) & 0xff;
        int vectorWidth = compressionValues.get(3) & 0xff;
        Vector<Vector<Vector<Integer>>> codebook = new Vector<>();
        int index = 4;
        while (index < overHeadSize) {
            Vector<Vector<Integer>> rowInCodebook = new Vector<>();
            for (int i = 0; i < vectorHeight; i++) {
                Vector<Integer> rowInVector = new Vector<>();
                for (int j = 0; j < vectorWidth; j++) {
                    int pixelValue = ((int) compressionValues.get(index++)) & 0xff;
                    rowInVector.add(j, pixelValue);
                }
                rowInCodebook.add(i, rowInVector);
            }
            codebook.add(rowInCodebook);
        }
        Vector<Integer> labels = new Vector<>();
        for(int i = overHeadSize; i < compressionValues.size(); i++){
            int labelValue = (compressionValues.get(i)) & 0xff;
            labels.add(labelValue);
        }
        BufferedImage image = new BufferedImage(imageHeight, imageWidth, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, imageHeight, imageWidth);
        int position = 0;
        for(int i = 0; i < imageHeight; i+= vectorHeight){
            for(int j = 0; j < imageWidth; j += vectorWidth){
                Vector<Vector<Integer>> currentVector = getVector(labels.get(position), codebook);
                for(int k = 0; k < currentVector.size(); k++){
                    for(int l = 0; l < currentVector.get(k).size(); l++){
                        int label = currentVector.get(k).get(l);
                        Color color = new Color(label, label, label);
                        int pixelX = i + k;
                        int pixelY = j + l;
                        g2d.setColor(color);
                        g2d.fillRect(pixelX, pixelY, 1, 1);
                    }
                }
                position++;
            }
        }
        g2d.dispose();
        try {
            ImageIO.write(image, "png", new File(decompressedFilePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        Quantizer quantizer = new Quantizer();
        quantizer.compress("img1.png", "compression.bin");
        quantizer.decompress("compression.bin", "result2.png");
    }
}

