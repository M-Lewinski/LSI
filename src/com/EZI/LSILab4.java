package com.EZI;
import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class LSILab4 {
    Matrix M;
    Matrix Q;

    public static void main(String [] args) {
    	LSILab4 lsi = new LSILab4();
        lsi.go();
    }

    private void go() {
        // init the matrix and the query
        M = readMatrix("data.txt");
        Q = readMatrix("query.txt");
        
        // print
        System.out.println("Matrix:");
        M.print(3, 2);

        // print the dimensions of the matrix
        System.out.println("M: " + dim(M));
        // print the query
        System.out.println("Query:");
        Q.print(3, 2);
        System.out.println("Q: " + dim(Q));

        // do svd
        svd();
    }

    private void svd() {

	//TODO implement your solution
        // A = U*S*V' => M = K*S*D'
        SingularValueDecomposition svd = new SingularValueDecomposition(M);
        // get K, S, and D

        Matrix K = svd.getU();

        Matrix S = svd.getS();

        Matrix D = svd.getV();
        Matrix DT = D.transpose();
        // set number of largest singular values to be considered
        int s = 4;
        IndexValue[] indexValueArray = new IndexValue[s];

        // find s largest values in matrix S
        for (int i = 0; i < S.getColumnDimension(); i++) {
            IndexValue next = new IndexValue(i,S.get(i,i));
            for (int j = 0; j < s; j++) {
                if (next == null) break;
                if (indexValueArray[j] == null || indexValueArray[j].value < next.value){
                    IndexValue temp = indexValueArray[j];
                    indexValueArray[j] = next;
                    next = temp;
                }
            }
        }
        // cut off appropriate columns and rows from K, S, and D

        Matrix Sreduced = new Matrix(s,s);

        int[] indexArray = new int[s];

        for (int i = 0; i < s; i++) {
            IndexValue max = indexValueArray[i];
            Sreduced.set(max.index,max.index,max.value);
            indexArray[i] = max.index;
        }

        Matrix Kreduced = K.getMatrix(0,K.getRowDimension()-1,indexArray);
        Matrix DTreduced = DT.getMatrix(indexArray,0,DT.getColumnDimension()-1);

        // transform the query vector

        // transpose vector q to q^T
        Matrix QT = Q.transpose();

        // calculate vector q'
        Matrix Qinverted = QT.times(Kreduced);

        // Inverse matrix S
        Matrix Sinverted = Sreduced.inverse();

        Qinverted = Qinverted.times(Sinverted);


        // Calculate magnitude of vector q'
        double QinvertedMagnitude = 0.0;
        for (int i = 0; i < Qinverted.getColumnDimension(); i++) {
            QinvertedMagnitude +=  Math.pow(Qinverted.get(0,i),2);
        }
        QinvertedMagnitude = Math.sqrt(QinvertedMagnitude);

        // compute similaraty of the query and each of the documents, using cosine measure
        IndexValue[] simArray = new IndexValue[DTreduced.getColumnDimension()];
        for (int i = 0; i < DTreduced.getColumnDimension(); i++) {
            double documentMagnitude = 0.0;
            double sumproduct = 0.0;
            for (int j = 0; j < DTreduced.getRowDimension(); j++) {
                double value = DTreduced.get(j,i);
                documentMagnitude += Math.pow(value,2);
                sumproduct += Qinverted.get(0,j)*value;
            }
            documentMagnitude = Math.sqrt(documentMagnitude);
            double sim = sumproduct/(documentMagnitude*QinvertedMagnitude);
            simArray[i] = new IndexValue(i,sim);
        }

        // Print number of document and sim value
        for (IndexValue docSim : simArray) {
            System.out.println(new StringBuilder().append("Document: ").append(docSim.index+1).append(" sim: ").append(docSim.value).toString());
        }

        // Save values to file
        String fileName = new StringBuilder().append("s_").append(s).append(".txt").toString();
        try {
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(fileName), "utf-8"))) {
                for (IndexValue docSim : simArray) {
                    writer.write(new StringBuilder().append("Document: ").append(docSim.index+1).append(" sim: ").append(docSim.value).append("\n").toString());
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }


    // returns the dimensions of a matrix
    private String dim(Matrix M) {
        return M.getRowDimension() + "x" + M.getColumnDimension();
    }

    // reads a matrix from a file
    private Matrix readMatrix(String filename) {
        Vector<Vector<Double>> m = new Vector<Vector<Double>>();
        int colums = 0;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            while (br.ready()) {
                Vector<Double> row = new Vector<Double>();
                m.add(row);
                String line = br.readLine().trim();
                StringTokenizer st = new StringTokenizer(line, ", ");
                colums = 0;
                while (st.hasMoreTokens()) {
                    row.add(Double.parseDouble(st.nextToken()));
                    colums++;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        int rows = m.size();
        Matrix M = new Matrix(rows, colums);
        int rowI = 0;
        for (Vector<Double> vector : m) {
            int colI = 0;
            for (Double d : vector) {
                M.set(rowI, colI, d);
                colI++;
            }
            rowI++;
        }
        return M;
    }


    public class IndexValue{
        int index = -1;
        Double value = null;

        public IndexValue() {
        }

        public IndexValue(int index, Double value) {
            this.index = index;
            this.value = value;
        }

    }

}
