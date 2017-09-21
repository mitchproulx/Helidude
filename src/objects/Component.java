package objects;

import models.Shape;

public class Component {
	public Shape shape;
	public float[] position;

	public Component(Shape s, float[] p){
		this.shape = s;
		this.position = p;
	}
}