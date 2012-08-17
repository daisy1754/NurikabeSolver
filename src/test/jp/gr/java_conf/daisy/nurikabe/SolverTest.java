package test.jp.gr.java_conf.daisy.nurikabe;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.gr.java_conf.daisy.nurikabe.Solver;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SolverTest {
	@Test
	public void test() throws IOException {
		testUsingInputOutputFiles("To be specified", "To be specified");
	}

	private void testUsingInputOutputFiles(
			String inputFilePath, String outputFilePath) throws IOException {
		File inputFile = new File(inputFilePath);
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
		assertTrue(solver.checkWithAnswerFile(outputFilePath));
	}
}
