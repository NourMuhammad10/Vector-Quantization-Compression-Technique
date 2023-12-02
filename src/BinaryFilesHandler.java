import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class BinaryFilesHandler {
    public static void writeCompressedOutput(List<Vector<Double>> codebook, Vector<Integer> labels, String outputFilePath) throws FileNotFoundException {
        int codebookLength = codebook.size();
        int singleVectorSize = codebook.get(0).size();
        int numberOfVectors = labels.size();
        int overheadSize = 2 + (codebookLength * singleVectorSize);
       // System.out.println(overheadSize);
        byte[] overhead  = new byte[overheadSize];
        overhead[0] = toBinary(codebookLength);
        overhead[1] = toBinary(singleVectorSize);
        int st = 2;
        for(int i = 0; i < codebookLength; i++){
            for(int j = 0; j < codebook.get(i).size(); j++){
                overhead[st++] = toBinary((int) Math.round(codebook.get(i).get(j)));
            }
        }
        byte[] mappedIndices = new byte[numberOfVectors];
        for(int i = 0; i < numberOfVectors; i++){
            mappedIndices[i] = toBinary(labels.get(i));
        }
        try {
            FileOutputStream fileWriter = new FileOutputStream(outputFilePath);
            for(int i = 0; i < overheadSize; i++){
                fileWriter.write(overhead[i]);
            }
            for(int i = 0; i < numberOfVectors; i++){
                fileWriter.write(mappedIndices[i]);
            }
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Vector<Integer> readCompressedFile(String filePath) throws FileNotFoundException {
        File file = new File(filePath);
        Vector<Integer> compressedValues = new Vector<>();

        try {
            FileInputStream fileIn = new FileInputStream(file);
            DataInputStream dataIn = new DataInputStream(fileIn);
            byte temp;
            int value;
            while((temp = (byte) dataIn.read()) != -1){
                value = temp;
                compressedValues.add(value);
            }
            fileIn.close();
            dataIn.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return compressedValues;
    }

    private static byte toBinary(int num){
        StringBuilder binaryString = new StringBuilder();
        for(int i = 7; i >= 0; i--){
            int bit = (num >> i) & 1;
            binaryString.append(bit);
        }
        return binaryStringToByte(binaryString.toString());
    }

    private static byte binaryStringToByte(String binaryString){
        int decimal = Integer.parseInt(binaryString, 2);
        return (byte) decimal;
    }
}
