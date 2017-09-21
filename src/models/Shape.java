package models;

import animation.ShapeParser;
import java.util.ArrayList;
import com.jogamp.opengl.GL2;

public class Shape {

	public float[] line_colour = null; // outline
	public boolean visible = true;
	public float angle = 0;
	public ArrayList<float[]> vertices;
	public ArrayList<Face> faces;

	float defaultWidth = 1, defaultHeight = 1, defaultDepth = 1;
	float r = 1, g = 1, b = 1;

	public Shape() {
		init();
	}
	
	protected void init() {
		vertices = new ArrayList<float[]>();
		faces = new ArrayList<Face>();
		line_colour = new float[] { 0.75f, 0.75f, 0.75f };
	}

	public void setColor(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;

		for (int i = 0; i < faces.size(); i ++) {
			faces.remove(i);
		}
		faces.add(new Face(new int[] { 0, 1, 2, 3 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 0, 3, 7, 4 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 7, 6, 5, 4 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 2, 1, 5, 6 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 3, 2, 6, 7 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 1, 0, 4, 5 }, new float[] { r, g, b } ));
	}

	public Shape(Shape other) {
		init();
		for (int i = 0; i < other.vertices.size(); i++) {
			this.vertices.add(other.vertices.get(i));
		}
		for (int f = 0; f < other.faces.size(); f++) {
			this.faces.add(other.faces.get(f));
		}
		this.defaultWidth = other.defaultWidth;
		this.defaultHeight = other.defaultHeight;
		this.defaultDepth = other.defaultDepth;
		this.r = other.r;
		this.g = other.g;
		this.b = other.b;
	}

	public Shape(float w, float h, float d, float r, float g, float b) {
		init();
		this.defaultWidth = w;
		this.defaultHeight = h;
		this.defaultDepth = d;
		this.r = r;
		this.g = g;
		this.b = b;

		if (visible) {
			vertices.add(new float[] { -defaultWidth, -defaultHeight, defaultDepth });
			vertices.add(new float[] { defaultWidth, -defaultHeight, defaultDepth });
			vertices.add(new float[] { defaultWidth, defaultHeight, defaultDepth });
			vertices.add(new float[] { -defaultWidth, defaultHeight, defaultDepth });
			vertices.add(new float[] { -defaultWidth, -defaultHeight, -defaultDepth });
			vertices.add(new float[] { defaultWidth, -defaultHeight, -defaultDepth });
			vertices.add(new float[] { defaultWidth, defaultHeight, -defaultDepth });
			vertices.add(new float[] { -defaultWidth, defaultHeight, -defaultDepth });

			faces.add(new Face(new int[] { 0, 1, 2, 3 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 0, 3, 7, 4 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 7, 6, 5, 4 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 2, 1, 5, 6 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 3, 2, 6, 7 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 1, 0, 4, 5 }, new float[] { r, g, b } ));
		}
	}
	public Shape(String filename) {
		init();

		new ShapeParser(filename, this);
	}

	public void draw(GL2 gl) {
		for (Face f: faces) {
			if (line_colour == null) {
				f.draw(gl, vertices, true);
			} else {
				gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
				gl.glPolygonOffset(1.0f, 1.0f);
				f.draw(gl, vertices, true);
				gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);

				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
				gl.glLineWidth(0.75f);
				gl.glColor3f(line_colour[0], line_colour[1], line_colour[2]);
				f.draw(gl, vertices, false);
				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
			}
		}
	}
}
