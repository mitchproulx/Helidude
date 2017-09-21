package animation;

import models.Face;
import models.Shape;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ShapeParser {

	BufferedReader input;
	String line;
	String[] tokens;
	String specifyingMaterial = null;
	String selectedMaterial;
	int[] face;
	float[] vertex;
	float[] colour;
	Shape shape;
	HashMap<String, float[]> materials;

	public ShapeParser(String filename, Shape s) {
		this.shape = s;
		materials = new HashMap<String, float[]>();
		materials.put("default", new float[] {1f,0.25f,0.25f});
		selectedMaterial = "default";
		shape.vertices.add(new float[] {0,0,0});
		int currentColourIndex = 0;
		int lineCount = 0, faceCount = 0;

		try {
			input = new BufferedReader(new FileReader(filename));
			line = input.readLine();
			while (line != null) {
				lineCount++;
				tokens = line.split("\\s+");
				if (tokens[0].equals("v")) {
					assert tokens.length == 4 : "Invalid vertex specification (line " + lineCount + "): " + line;
					vertex = new float[3];
					try {
						vertex[0] = Float.parseFloat(tokens[1]);
						vertex[1] = Float.parseFloat(tokens[2]);
						vertex[2] = Float.parseFloat(tokens[3]);
					} catch (NumberFormatException nfe) {
						assert false : "Invalid vertex coordinate (line " + lineCount + "): " + line;
					}
					shape.vertices.add(vertex);
				} else if (tokens[0].equals("newmtl")) {
					assert tokens.length == 2 : "Invalid material name (line " + lineCount + "): " + line;
					specifyingMaterial = tokens[1];
				} else if (tokens[0].equals("Kd")) {
					assert tokens.length == 4 : "Invalid colour specification (line " + lineCount + "): " + line;
					assert faceCount == 0 && currentColourIndex == 0 : "Unexpected (late) colour (line " + lineCount + "): " + line;
					colour = new float[3];
					try {
						colour[0] = Float.parseFloat(tokens[1]);
						colour[1] = Float.parseFloat(tokens[2]);
						colour[2] = Float.parseFloat(tokens[3]);
					} catch (NumberFormatException nfe) {
						assert false : "Invalid colour value (line " + lineCount + "): " + line;
					}
					for (float colourValue: colour) {
						assert colourValue >= 0.0f && colourValue <= 1.0f : "Colour value out of range (line " + lineCount + "): " + line;
					}
					if (specifyingMaterial != null) {
						materials.put(specifyingMaterial, colour);
					}
				} else if (tokens[0].equals("usemtl")) {
					assert tokens.length == 2 : "Invalid material selection (line " + lineCount + "): " + line;
					selectedMaterial = tokens[1];
				} else if (tokens[0].equals("f")) {
					assert tokens.length > 1 : "Invalid face specification (line " + lineCount + "): " + line;
					face = new int[tokens.length - 1];
					try {
						for (int i = 1; i < tokens.length; i++) {
							face[i - 1] = Integer.parseInt(tokens[i].split("/")[0]);
						}
					} catch (NumberFormatException nfe) {
						assert false : "Invalid vertex index (line " + lineCount + "): " + line;
					}
					colour = materials.get(selectedMaterial);
					if (colour == null) {
						colour = materials.get("default");
					}
					shape.faces.add(new Face(face, colour));
					faceCount++;
				}
				line = input.readLine();
			}
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
			assert false : "Error reading input file " + filename;
		}
	}
}