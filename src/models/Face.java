package models;

import java.util.ArrayList;

import com.jogamp.opengl.GL2;

public class Face {
	private int[] indices;
	private float[] colour;

	public Face(int[] indices, float[] colour) {
		this.indices = new int[indices.length];
		this.colour = new float[colour.length];
		System.arraycopy(indices, 0, this.indices, 0, indices.length);
		System.arraycopy(colour, 0, this.colour, 0, colour.length);
	}

	public void draw(GL2 gl, ArrayList<float[]> vertices, boolean useColour) {
		if (useColour) {
			if (colour.length == 3)
				gl.glColor3f(colour[0], colour[1], colour[2]);
			else
				gl.glColor4f(colour[0], colour[1], colour[2], colour[3]);
		}

		if (indices.length == 1) {
			gl.glBegin(GL2.GL_POINTS);
		} else if (indices.length == 2) {
			gl.glBegin(GL2.GL_LINES);
		} else if (indices.length == 3) {
			gl.glBegin(GL2.GL_TRIANGLES);
		} else if (indices.length == 4) {
			gl.glBegin(GL2.GL_QUADS);
		} else {
			gl.glBegin(GL2.GL_POLYGON);
		}

		for (int i: indices) {
			gl.glVertex3f(vertices.get(i)[0], vertices.get(i)[1], vertices.get(i)[2]);
		}
		gl.glEnd();
	}
}
