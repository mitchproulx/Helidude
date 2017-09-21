package objects;

import models.Shape;

import java.util.ArrayList;

import com.jogamp.opengl.GL2;

public class Structure {
	public ArrayList<Shape> shapes;
	public ArrayList<float[]> positions;
	public ArrayList<Component> contents;

	public Structure() {
		shapes = new ArrayList<Shape>();
		positions = new ArrayList<float[]>();
		contents = new ArrayList<Component>();
	}
	public void addComponent(Shape s, float[] p) {
		this.shapes.add(new Shape(s));
		this.positions.add(new float[] {p[0], p[1], p[2]});
		contents.add(new Component(new Shape(s), new float[] {p[0], p[1], p[2]}));
	}
	public void draw(GL2 gl) {
		for (int i = 0; i < contents.size(); i++) {
			Component c = contents.get(i);
			gl.glPushMatrix();
			gl.glTranslatef(c.position[0], c.position[1], c.position[2]);
			c.shape.draw(gl);
			gl.glPopMatrix();
		}
	}
}