package objects;

import models.Shape;

import com.jogamp.opengl.GL2;
 
public class ComplexShape extends Shape {
	private Shape[] contents;	// can contain more of the same type
	private float[][] positions;

	public ComplexShape(Shape[] contents, float[][] positions) {
		super();
		init(contents, positions);
	}
	public ComplexShape(String filename, Shape[] contents, float[][] positions) {
		super(filename);
		init(contents, positions);
	}
	private void init(Shape[] contents, float[][] positions) {
		this.contents = new Shape[contents.length];
		this.positions = new float[positions.length][3];
		System.arraycopy(contents, 0, this.contents, 0, contents.length);
		for (int i = 0; i < positions.length; i++) {
			System.arraycopy(positions[i], 0, this.positions[i], 0, 3);
		}
	}
	public void draw(GL2 gl) {
		super.draw(gl);
		for (int i = 0; i < contents.length; i++) {
			gl.glPushMatrix();
			gl.glTranslatef(positions[i][0], positions[i][1], positions[i][2]);
			contents[i].draw(gl);
			gl.glPopMatrix();
		}
	}
}
