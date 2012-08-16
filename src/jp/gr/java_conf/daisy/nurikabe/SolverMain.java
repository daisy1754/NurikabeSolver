package jp.gr.java_conf.daisy.nurikabe;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SolverMain {
	private static String INPUT_FILE_PATH = "To be defined";
	private static String OUTPUT_FILE_PATH = "To be defined";
	public static void main(String[] args) {
		try {
			File inputFile = new File(INPUT_FILE_PATH);
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			List<String> inputs = new ArrayList<String>();
			String line;
			while ((line = reader.readLine()) != null) {
				inputs.add(line);
			}
			Solver solver = new Solver(inputs);
			solver.solve();
			solver.outputTo(System.out);
			reader.close();
			solver.checkWithAnswerFile(OUTPUT_FILE_PATH);
		} catch (FileNotFoundException err) {
			System.err.println("file named " + INPUT_FILE_PATH + " is not found");
		} catch (IOException err) {
			err.printStackTrace();
		}
	}
}
