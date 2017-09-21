package models;

import objects.ComplexShape;
import objects.Component;
import objects.Structure;
import models.Dude;
import models.Shape;

import java.util.ArrayList;

import animation.GameRenderer;
import animation.GameRenderer.Scene;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;

public class Dude {

	public GameRenderer game = null;
	public Structure dude;
	private Shape head, arm, chest, leg, flame;
	public ComplexShape ghost = null, sandal = null, shuttle = null;
	public ArrayList<Shape> dudeShapes = null;

	// starting position of the dude --> head[0], arm[1], chest[2], leg[3]
	public float[][] dudePositions = {{0, 1.05f, 0}, {0, 0.8f, -0.15f}, {0, 0.8f, 0f}, {0, 0.5f, -0.05f}};

	// colors the dude when speed orb obtained
	public float [][] boostArray = { 
			{1.0f, 0.0f, 0.0f}, {0.75f, 0.25f, 0.0f}, {0.50f, 0.50f, 0.0f}, {0.25f, 0.75f, 0.0f},
			{0.0f, 1.0f, 0.0f}, {0.0f, 0.75f, 0.25f}, {0.0f, 0.50f, 0.50f}, {0.0f, 0.25f, 0.75f},
			{0.0f, 0.0f, 1.0f}, {0.25f, 0.0f, 0.75f}, {0.50f, 0.0f, 0.50f}, {0.75f, 0.0f, 0.25f}
	};
	public int boostColorPos = 0;

	public static int flameCount = 0;
	public float flameSizeX = 1.25f, flameSizeY = 1f;
	public static float[][] flameArray = new float[][] {{0.545f, 0, 0}, {0.698f, 0.133f, 0.133f}, 
		{1,0,0}, {0.863f, 0.078f, 0.235f}, {0.980f, 0.502f, 0.447f}, {1, 0.498f, 0.314f}, 
		{1, 0.388f, 0.278f}, {1, 0.271f, 0}, {1, 0.549f, 0},{1, 0.647f, 0}, {1, 1, 0}};

	// for animating the motion of the arms
	public static final int numArms = 2, numLegs = 2, numFeet = 2;
	private static final double PERIOD = 88, SCALE = 50;

	public static final String INPUT_PATH_NAME = "resources/";
	private static final GLU glu = new GLU();

	public String currDirection = "";
	public int index = 90, limbsRotatePos = 0;
	public int boostLife = 100, slowTime = 25;
	public float turnAngle = 90f;			// default turn position dude pointing straight
	public float rollAngle = 0f;			// default ship position no roll angle
	public float pitchAngle = 0f;			// ship default no pitch
	public float yMove = 0f, forwardMove = 0f, xMove = 0f;
	public boolean slow = false, speedBoost = false, jumpActivate = false, up = false, stop = false;
	public boolean reachedShip = false, turnLeft = false, turnRight = false, backUp = false;
	public boolean speedUp = false, jump = false, displayed = false, rollingRight = false;
	public boolean rollingLeft = false, pitchingDown = false, pitchingUp = false;
	public boolean explosion = false, normalizeRoll = false, normalizePitch = false;
	public float exp_size = 35f;

	public Dude(GameRenderer gR) {

		game = gR;

		dude = new Structure();
		dudeShapes = new ArrayList<Shape>();

		// Next 2 objects from --> [ http://people.sc.fsu.edu/~jburkardt/data/obj/obj.html ]
		ghost = new ComplexShape(new Shape[] { new Shape(INPUT_PATH_NAME + "dodecahedron.obj") }, new float[][] {{0.0f, 2.0f, 0.0f}});
		sandal = new ComplexShape(new Shape[] { new Shape(INPUT_PATH_NAME + "sandal.obj") }, new float[][] {{0.0f, 2.0f, 0.0f}});
		shuttle = new ComplexShape(new Shape[] { new Shape(INPUT_PATH_NAME + "shuttle.obj") }, new float[][] {{0.0f, 2.0f, 0.0f}});

		// Shape( width, height, depth, red, green, blue )
		head = new Shape(0.075f, 0.065f, 0.05f, 0.45f, 0.82f, 0.28f);
		leg = new Shape(0.05f, 0.15f, 0.05f, 0.7f, 0.82f, 0.5f);
		chest = new Shape(0.07f, 0.2f, 0.1f, 0.2f, 0.2f, 0.8f);
		arm = new Shape(0.04f, 0.1f, 0.05f, 0.67f, 0.55f, 0.41f);

		// head[0], arm[1], chest[2], leg[3]
		dudeShapes.add(head);
		dudeShapes.add(arm);
		dudeShapes.add(chest);
		dudeShapes.add(leg);

		for (int i = 0; i < dudeShapes.size() && dudeShapes.size() == dudePositions.length; i++) {
			dude.addComponent(dudeShapes.get(i), dudePositions[i]);
		}

		// dudes ship flames
		flame = new Shape(0.025f, 0.25f, 0.025f, 1, 0, 0);
		flame.line_colour = new float[] {0,0,0};
	}
	public void updateLimbPosition() {
		// will simulate an oscillation for the legs and arms using the sine function
		index++;
		limbsRotatePos = (int)(Math.sin(index*2*Math.PI/PERIOD)*(SCALE/2) + (SCALE/2)) - 90;
		if (index == PERIOD*2) {
			index = 0;
		}
	}
	public void drawDude(GL2 gl) {

		if (turnAngle < 90){
			turnAngle += 7.5f;
		}else if (turnAngle > 90){
			turnAngle -= 7.5f;
		}
		gl.glRotatef(turnAngle, 0, 1, 0);

		if (speedBoost){
			updateLimbPosition();
			updateLimbPosition();
		}else{
			updateLimbPosition();				// will oscillate the rotation of the arms and legs
		}
		boostColorPos = (boostColorPos + 1) % boostArray.length;
		drawHead(gl);
		drawArms(gl);
		drawLegs(gl);
		drawBody(gl);
	}
	public void drawHead(GL2 gl) {
		// head[0], arm[1], chest[2], leg[3]
		Component head = dude.contents.get(0);

		// HEAD
		gl.glPushMatrix();
		gl.glTranslatef(head.position[0], head.position[1], head.position[2]);
		gl.glRotatef((limbsRotatePos*2)-90, 0, 1, 0);
		gl.glScalef(1, 1.25f, 1);

		if (speedBoost) {
			head.shape.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
		}else{
			head.shape.setColor(0.2f, 0.84f, 0.2f);
		}
		head.shape.draw(gl);

		// GLU SPHERE to connect the helicopter blade to head
		gl.glTranslatef(0, 0.1f, 0);
		gl.glColor3f(0.3f, 0.5f, 1f);
		GLUquadric sphere = glu.gluNewQuadric();
		glu.gluQuadricDrawStyle(sphere, GLU.GLU_FILL);
		final float radius = 0.05f;
		final int slices = 16;
		final int stacks = 16;
		glu.gluSphere(sphere, radius, slices, stacks);
		glu.gluDeleteQuadric(sphere);

		// HELECOPTER HAT
		if (yMove > 0.1f) {
			gl.glRotatef(limbsRotatePos*32, 0, 1, 0);
			gl.glScalef(10f, 0.25f, 0.5f);
		}else{
			gl.glRotatef(limbsRotatePos*2, 0, 1, 0);
			gl.glScalef(3.5f, 0.25f, 0.5f);
		}
		if (speedBoost) {
			head.shape.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
		}else{
			head.shape.setColor(1.0f, 0.2f, 0.2f);
		}
		head.shape.draw(gl);
		gl.glPopMatrix();
	}
	public void drawBody(GL2 gl) {
		// head[0], arm[1], chest[2], leg[3]
		Component chest = dude.contents.get(2);

		// CHEST
		gl.glPushMatrix();
		gl.glTranslatef(chest.position[0], chest.position[1], chest.position[2]);
		gl.glScalef(1.5f, 1, 1);

		if (speedBoost) {
			chest.shape.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
		}else{
			chest.shape.setColor(0.2f, 0.2f, 0.8f);
		}
		chest.shape.draw(gl);
		gl.glPopMatrix();
	}
	public void drawArms(GL2 gl){
		// head[0], arm[1], chest[2], leg[3]
		Component arm = dude.contents.get(1);
		int changeSide = 1;

		for (int i = 0; i < numArms; i++) {
			gl.glPushMatrix();

			if (i % 1 == 0) {
				changeSide *= -1;	// each odd number change, side of the body
			}
			// UPPER ARM
			gl.glTranslatef(arm.position[0], arm.position[1], changeSide * arm.position[2]);
			if (changeSide < 1) {
				gl.glRotatef(changeSide*limbsRotatePos*2+15, 0, 0, 1);
			}else{
				gl.glRotatef(changeSide*limbsRotatePos*2-15, 0, 0, 1);
			}
			gl.glScalef(1.25f, 1, 1);

			if (speedBoost) {
				arm.shape.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			}else{
				arm.shape.setColor(0.75f, 0.25f, 0.25f);
			}
			arm.shape.draw(gl);

			// LOWER ARM
			if (changeSide < 1) {			// right arm
				gl.glTranslatef(-0.055f, 0.1125f, 0);
				gl.glRotatef(changeSide*limbsRotatePos*2+115, 0, 0, 1);
				gl.glScalef(1, 1.15f, 1);

			}else{							// left arm
				gl.glTranslatef(-0.025f, 0.145f, 0);
				gl.glRotatef(changeSide*limbsRotatePos*2-15, 0, 0, 1);
				gl.glScalef(1, 1.15f, 1);
			}

			if (speedBoost) {
				arm.shape.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			}else{
				arm.shape.setColor(0.75f, 0.25f, 0.25f);
			}
			arm.shape.draw(gl);

			drawHand(gl, arm);
			gl.glPopMatrix();
		}
	}
	public void drawHand(GL2 gl, Component arm) {
		// ROTATING HAND
		gl.glTranslatef(0, -arm.shape.defaultHeight*3, 0);	
		gl.glRotatef(limbsRotatePos*32, 0, 1, 0);
		gl.glScalef(0.05f, 0.1f, 0.05f);
		ghost.draw(gl);

		// 2 FINGERS
		arm.shape.setColor(1, 1, 0);
		gl.glTranslatef(0, arm.shape.defaultHeight*8, 0.25f);	
		gl.glScalef(5f, 5f, 5f);
		arm.shape.line_colour = new float[] {0.5f, 0.5f, 0.5f};
		arm.shape.draw(gl);
		gl.glTranslatef(0,0, -0.25f);
		arm.shape.draw(gl);
		arm.shape.line_colour = new float[] {0.75f, 0.75f, 0.75f};
	}
	public void drawLegs(GL2 gl){
		// head[0], arm[1], chest[2], leg[3]
		Component leg = dude.contents.get(3);
		int changeSide = 1;
		float shift = 0.3f;

		for (int i = 0; i < numLegs; i++) {
			gl.glPushMatrix();
			if (i % 1 == 0) {
				changeSide *= -1;	// each odd number change, side of the body
			}

			// UPPER LEG
			gl.glTranslatef(leg.position[0], leg.position[1]+shift, changeSide * leg.position[2]);
			if (changeSide < 1) {		// right leg
				gl.glTranslatef(0, 0, 0.05f);
				gl.glRotatef(limbsRotatePos*2-45, 0, 0, 1);
			}else{						// left leg
				gl.glTranslatef( 0, 0, 0);
				gl.glRotatef(-limbsRotatePos*2+45, 0, 0, 1);
			}
			gl.glTranslatef(0, shift, 0);

			if (speedBoost) {
				leg.shape.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			}else{
				leg.shape.setColor(0.2f, 0.6f, 0.2f);
			}
			leg.shape.draw(gl);

			// LOWER LEG
			if (changeSide < 1) {		// right leg
				gl.glTranslatef(-0.2f + leg.shape.defaultHeight*2, 0.175f, 0);
				gl.glRotatef(limbsRotatePos*2+75, 0, 0, 1);
			}else{						// left leg
				gl.glTranslatef(0.0675f, 0.2f, 0);
				gl.glRotatef(-limbsRotatePos*2+180, 0, 0, 1);
			}
			leg.shape.draw(gl);

			gl.glTranslatef(-0.05f, 0.2f, 0);
			gl.glRotatef(-90, 0, 1, 0);
			if (changeSide < 1) {		// right foot
				gl.glScalef(0.025f, -0.05f, 0.0325f);
			}else{						// left foot
				gl.glScalef(-1*0.025f, -0.05f, 0.0325f);
			}
			sandal.draw(gl);
			gl.glPopMatrix();
		}
	}
	public void run(GL2 gl){ 
		gl.glPushMatrix();

		getMovement();									// check which way dude is moving
		if (game.currScene != Scene.TWO) {
			jump();										// activate helicopter hat
			if (!game.TEST){
				didHitWall();							// hit a wall ( long cylinder )
				if (game.currScene == Scene.ONE) {
					didHitObstacle();					// hit a short cylinder or trap
				}
			}
			speedBoost();				// got speed orb
		}
		getDudeStatus();				// display if the dude hit anything
		didReachEnd();					// got to the end
		adjustSpeed();					// speed, normal, slow or stop

		// increase distance as you move further back
		if (game.currScene == Scene.ONE) {
			if (forwardMove % 5 == 0) {
				game.distance += 10;
			}
		}

		gl.glTranslatef(xMove, yMove, forwardMove);
		if (game.currScene == Scene.THREE) {
			drawShip(gl);
		}else{
			drawDude(gl);
		}
		gl.glPopMatrix();
	}
	public void drawShip(GL2 gl) {

		// adjust the ship if turning
		adjustRoll();

		// adjust the ship if climbing or diving
		adjustPitch();

		gl.glRotatef(rollAngle, 0, 0, 1);
		gl.glRotatef(pitchAngle, 1, 0, 0);

		// position the space ship
		gl.glPushMatrix();
		gl.glRotatef(-90, 1, 0, 0);
		gl.glRotatef(-90, 0, 0, 1);
		gl.glScalef(0.15f, 0.2f, 0.1f);
		gl.glTranslatef(0f, 0f, 10f);

		if (!explosion) {
			shuttle.draw(gl);
			drawFlames(gl);
		}else{
			drawExplosion(gl);
		}

		gl.glPopMatrix();
	}

	public void drawExplosion(GL2 gl) {
		gl.glPushMatrix();

		if (exp_size < 450f) {
			exp_size += 5f;
		}else{
			exp_size = 35f;
		}


		// GLU SPHERE to connect the helicopter blade to head
		gl.glScalef(exp_size, exp_size, exp_size);
		gl.glTranslatef(xMove, yMove, 0);
		gl.glColor3f(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
		flameCount = (flameCount+1) % flameArray.length;
		GLUquadric sphere = glu.gluNewQuadric();
		glu.gluQuadricDrawStyle(sphere, GLU.GLU_FILL);
		final float radius = 0.05f;
		final int slices = 16;
		final int stacks = 16;
		glu.gluSphere(sphere, radius, slices, stacks);
		glu.gluDeleteQuadric(sphere);

		gl.glPopMatrix();
	}

	private void drawFlames(GL2 gl) {

		// flames movement
		float r1 = (float)Math.random();
		float r2 = (float)Math.random();
		float r3 = (float)Math.random();
		float r4 = (float)Math.random();
		float r5 = (float)Math.random();

		if (speedBoost) {
			flameSizeX = 4f;
			flameSizeY = 4f;
		}else{
			flameSizeX = 1.25f;
			flameSizeY = 1f;
		}

		//initial flame attributes
		gl.glPushMatrix();
		gl.glRotatef(-90, 0, 0, 1);
		gl.glScalef(5f, 15f, 5f);

		// FLAME 1 ( left )
		gl.glPushMatrix();
		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.3f, 0.35f, -0.05f); // x, z, y
			gl.glScalef(flameSizeX, r1, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.3f, 0.35f, -0.05f); // x, z, y
			gl.glScalef(flameSizeX, r1, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		// FLAME 2 ( left-center )
		gl.glPushMatrix();
		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.35f, 0.35f, -0.05f); // x, z, y
			gl.glScalef(flameSizeX, r2, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.35f, 0.35f, -0.05f); // x, z, y
			gl.glScalef(flameSizeX, r2, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		// FLAME 3 ( right-center )
		gl.glPushMatrix();
		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.45f, 0.35f, -0.05f);
			gl.glScalef(flameSizeX, r3, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.45f, 0.35f, -0.05f);
			gl.glScalef(flameSizeX, r3, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		// FLAME 4 ( right )
		gl.glPushMatrix();

		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.5f, 0.35f, -0.05f);
			gl.glScalef(flameSizeX, r1, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.5f, 0.35f, -0.05f);
			gl.glScalef(flameSizeX, r1, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		// FLAME 5 ( center )
		gl.glPushMatrix();

		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.4f, 0.35f, 0f);
			gl.glScalef(flameSizeX, r4, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.4f, 0.35f, 0f);
			gl.glScalef(flameSizeX, r4, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		// FLAME 6 ( center bottom )
		gl.glPushMatrix();
		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.4f, 0.35f, -0.05f);
			gl.glScalef(flameSizeX, r3, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.4f, 0.35f, -0.05f);
			gl.glScalef(flameSizeX, r3, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		// FLAME 7 ( center bottom bottom )
		gl.glPushMatrix();
		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.4f, 0.35f, -0.1f);
			gl.glScalef(flameSizeX, r2, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.4f, 0.35f, -0.1f);
			gl.glScalef(flameSizeX, r2, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		// FLAME 8 ( center top )
		gl.glPushMatrix();
		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.4f, 0.35f, 0.125f);
			gl.glScalef(flameSizeX, r4, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.4f, 0.35f, 0.125f);
			gl.glScalef(flameSizeX, r4, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		// FLAME 9 ( center right )
		gl.glPushMatrix();
		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.45f, 0.35f, 0.125f);
			gl.glScalef(flameSizeX, r5, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.45f, 0.35f, 0.125f);
			gl.glScalef(flameSizeX, r5, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		// FLAME 10 ( center left )
		gl.glPushMatrix();
		if (speedBoost) {
			flame.setColor(boostArray[boostColorPos][0], boostArray[boostColorPos][1], boostArray[boostColorPos][2]);
			boostColorPos = (boostColorPos + 1) % boostArray.length;
			gl.glTranslatef(-0.35f, 0.35f, 0.125f);
			gl.glScalef(flameSizeX, r5, flameSizeY);
		}else{
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			gl.glTranslatef(-0.35f, 0.35f, 0.125f);
			gl.glScalef(flameSizeX, r5, flameSizeY);
		}
		flame.draw(gl);
		flameCount = (flameCount+1) % flameArray.length;
		gl.glPopMatrix();

		gl.glPopMatrix();
	}
	public void getMovement() {

		if (game.currScene == Scene.THREE) {	// jump for the dude turns into --> pitch upwards for the space ship
			if (speedUp) {						// in scene 3
				jump = true;
				speedUp = false;
			}
		}

		// strafe left
		if (turnLeft) {					
			if (xMove > -2.5f) {
				if (game.currScene == Scene.ONE) {
					xMove -= 0.5f;
				}else if (game.currScene == Scene.THREE) {
					xMove -= 1.25f;
				}
			}
			if (game.currScene == Scene.ONE) {					// DUDE LEFT
				turnAngle += 45;
			}else if (game.currScene == Scene.THREE) {			// SHIP ROLLING LEFT
				rollingLeft = true;
				normalizeRoll = false;
			}
			turnLeft = false;

			// strafe right	
		}else if (turnRight) {			
			if (xMove < 2.5f) {
				if (game.currScene == Scene.ONE) {
					xMove += 0.5f;
				}else if (game.currScene == Scene.THREE) {
					xMove += 1.25f;
				}
			}
			if (game.currScene == Scene.ONE) {					// DUDE RIGHT
				turnAngle -= 45;
			}else if (game.currScene == Scene.THREE) {			// SHIP ROLL RIGHT
				rollingRight = true;
				normalizeRoll = false;
			}
			turnRight = false;

			// move dude backwards
		}else if (backUp) {				
			if (game.currScene == Scene.ONE) {
				// SHOOT ?

			} else if (game.currScene == Scene.THREE) {			// SHIP PITCH DOWN
				if (yMove > 0) {
					yMove -= 0.4f;
					pitchingDown = true;
					normalizePitch = false;
				}
			}
			backUp = false;

			// move dude up and then down
		}else if (jump) {				
			if (game.currScene == Scene.ONE) {
				jumpActivate = true;

			}else if (game.currScene == Scene.THREE) {			// SHIP PITCHING UP
				if (yMove < 2.5f) {
					yMove += 0.4f;
					pitchingUp = true;
					normalizePitch = false;
				}
			}
			jump = false;

			// (TEST ONLY) move the dude forward extra fast
		}else if (speedUp){				
			if (game.TEST) {
				forwardMove -= 5f;
				speedUp = false;
			}else if (game.currScene == Scene.TWO) {
				stop = false;
				forwardMove -= 0.15f;
			}
		}
	}
	public void adjustSpeed() {
		// DEFAULT always move forward, unless backing up. SPEED affected by boosts and Traps
		if (stop) {
			// dont move
		}else{ 
			if (game.TEST) {
				forwardMove -= 0.5f;
			}else{
				if(speedBoost){							// speed increase from SPEED BOOST
					if (game.currScene == Scene.ONE) {
						forwardMove -= 0.2f;
					}else if (game.currScene == Scene.THREE) {
						forwardMove -= 0.35f;
					}
					boostLife -= 1;
					if (boostLife < 1) {				// LIFETIME of the BOOST
						boostLife = 100;
						speedBoost = false;
					}
				}else if (!slow) {						// if the dude is going NORMAL SPEED
					if (game.currScene == Scene.ONE) {
						forwardMove -= 0.15f;
					}else if (game.currScene == Scene.THREE){
						forwardMove -= 0.15f;
					}
				}else{									// hit a SLOW TRAP
					forwardMove -= 0.05f;		
					slowTime -= 1;
					if (slowTime < 1) {
						boostLife = 50;
						slow = false;
					}
				}
			}
		}
	}
	public void didReachEnd() {
		if (game.currScene == Scene.ONE) {
			if (forwardMove < -game.MAX_DIST-7f || game.hitWall){
				if (forwardMove < -5f) {

					for (int orbs = 0; orbs < game.orbHit.length; orbs++) {		// reset the orbs
						if (game.orbHit[orbs] == 1) {
							game.totalOrbs++;
						}
						game.orbHit[orbs] = 0;	
					}

					// reached the end, move to scene 2
					if (forwardMove < -game.MAX_DIST) {
						System.out.println("\n\n\n!!! YOU MADE TO THE END CONGRATS!!!\n\n\n");
						stop = true;
						game.currScene = Scene.TWO;
					}

					System.out.println("\n\n");
					System.out.println("[ TOTAL ORBS BEFORE RESET: " + game.totalOrbs + " ]");
					if (game.totalOrbs > 25) {
						System.out.println("!!! <<< UNBELIEVABLE >>> !!!");
					}else if (game.totalOrbs > 16) {
						System.out.println("!!! AMAZING !!!");
					}else if (game.totalOrbs > 16) {
						System.out.println("!! GREAT !!");
					}else if (game.totalOrbs > 12) {
						System.out.println("! GOOD !");
					}else if (game.totalOrbs > 8) {
						System.out.println("< NOT BAD >");
					}else if (game.totalOrbs > 4) {
						System.out.println("C'MON... YOU CAN DO BETTER....");
					}else{
						System.out.println("YOU SUCK!!!");
					}
					System.out.println("\n\n");
					game.hitWall = false;
					forwardMove = 0;
					game.totalOrbs = 0;
				}
			}
		}else if (game.currScene == Scene.TWO) {
			if ( forwardMove < game.shipDist+33.5f ) {
				reachedShip = true;
			}
		}else{
			if (game.hitWall) {
				xMove = 0;
				yMove = 0;
				stop = true;
				if (!explosion) {
					System.out.println("\n\n!!! YOU CRASHED :( !!!\n\n");
				}
				explosion = true;
			}
			else if (forwardMove < -(2*game.MAX_DIST)){
				if (forwardMove < -5f) {
					xMove = 0;
					yMove = 0;
					stop = true;
					if (!explosion) {
						System.out.println("\n\n!!! YOU MADE IT TO THE END !!!\n\n\tGAME OVER!!!");
					}
					explosion = true;
				}
			}
		}
	}
	public void getDudeStatus() {
		if (game.currScene == Scene.TWO && !stop) {
			if (reachedShip && !displayed) {
				System.out.println("\n\n YOU FOUND A MYSTERIOUS SPACE SHIP...\n\tDOES IT STILL WORK??\n\n");
				reachedShip = false;
				displayed = true;
				game.currScene = Scene.THREE;
			}
		}else{
			if (game.changed && game.fogMode == 2) {
				if (game.totalOrbs > 0) {
					game.totalOrbs -= 1;
					System.out.println("YOU HIT A TRAP!!\n\t... THE SHADOWS PULL 1 ORB FROM YOU! ...");
				}
			}else if (game.changed && game.fogMode == 1) {
				if (game.currScene == Scene.ONE) {
					System.out.println("GRABBED A SPEED ORB!!\n\t---> GIVES YOU A BOOST!! ");
				}else if (game.currScene == Scene.THREE) {
					System.out.println("FLEW THREW A WARP GATE!!\n\t---> GIVES YOU A BOOST!! ");
				}
			}
		}
	}

	public void speedBoost() {
		if (game.currScene == Scene.ONE) {
			if (!slow && yMove < 0.1f){
				for (int i = 0; i < game.numFloorObjs; i++){
					if ( xMove < game.xBoosts[i] + game.bMaxX && xMove > game.xBoosts[i] + game.bMinX){
						if ( forwardMove > game.zBoosts[i]+game.bMinY && forwardMove < game.zBoosts[i]+ game.bMaxY){
							game.orbHit[i] = 1;
							speedBoost = true;
							boostLife = 100;
						}
					}
				}
			}
		}else if (game.currScene == Scene.THREE) {
			for (int i = 0; i < game.numFloorObjs; i++){
				if ( xMove < game.xBoosts2[i] + 3f && xMove > game.xBoosts2[i] - 3f){
					if ( forwardMove > game.zBoosts2[i] - 5f && forwardMove < game.zBoosts2[i] + 5f){
						if ( yMove > game.yBoosts2[i] - 3f && yMove < game.yBoosts2[i] + 3f){
							game.orbHit[i] = 1;
							speedBoost = true;
							boostLife = 100;
						}
					}
				}
			}
		}
		// set fog mode depending on the speed of the dude
		if (speedBoost || game.currScene == Scene.THREE) {			
			if (game.fogMode != 1) {
				game.fogMode = 1;
				game.changed = true;
			}
		}else{
			if (game.fogMode != 2) {
				game.fogMode = 2;
				game.changed = true;
			}
		}
	}

	private void didHitObstacle() {
		if (forwardMove < game.boostStart) {
			// check if the dude hit anything, can jump over obstacles
			if (yMove < 0.1f) {
				for (int i = 0; i < game.numFloorObjs; i++){

					// check if dude hit a SLOW TRAP
					if (forwardMove < game.trapStart) {
						if ( xMove < game.xTraps[i]+ game.tMaxX && xMove > game.xTraps[i]+ game.tMinX){
							if ( forwardMove > game.zTraps[i]+ game.tMinY && forwardMove < game.zTraps[i]+ game.tMaxY){
								speedBoost = false;
								boostLife = 0;
								slow = true;
								slowTime = 25;
							}
						}
					}
					// check if hit a short cylinder
					if (forwardMove < game.smallCylStart) {
						if (xMove < game.xCubes[i]+0.45f && xMove > game.xCubes[i]-0.45f){
							if (forwardMove < game.zCubes[i]+0.45f && forwardMove > game.zCubes[i]-0.45f){
								game.hitWall = true;
							}
						}
					}
				}
			}
		}
	}

	public void didHitWall() {
		if (game.currScene == Scene.ONE) {
			if (forwardMove < game.bigCylStart) {
				for (int i = 0; i < game.numFloorObjs; i++){
					if (xMove < game.xWalls[i]+0.3f && xMove > game.xWalls[i]-0.3f){
						if (forwardMove > game.zWalls[i]-0.3f && forwardMove < game.zWalls[i]+0.3f){
							game.hitWall = true;
						}
					}
				}
			}
		}
		else if (game.currScene == Scene.THREE) {
			if (forwardMove < game.bigCylStart) {
				for (int i = 0; i < game.numFloorObjs*2; i++){
					if (xMove < game.xWalls2[i]+0.3f && xMove > game.xWalls2[i]-0.3f){
						if (forwardMove > game.zWalls2[i]-0.3f && forwardMove < game.zWalls2[i]+0.3f){
							game.hitWall = true;
						}
					}
				}
			}
		}

	}

	public void jump() {
		if (jumpActivate) {
			if (yMove < 1.5f && !up) {
				yMove += 0.05;
			}else{
				up = true;
				if (yMove > 0) {
					yMove -= 0.025;
				}else{
					jumpActivate = false;
					up = false;
				}
			}
		}
	}
	private void adjustRoll() {
		float speed = 3.5f;
		if (normalizeRoll) {
			if (rollAngle > 0) {
				rollAngle -= speed;
			}else if (rollAngle < 0) {
				rollAngle += speed;
			}
		}else{
			if (rollingRight) {
				rollAngle -= speed;
				if (rollAngle < -45) {
					rollingRight = false;
					normalizeRoll = true;
				}
			}else if (rollingLeft) {
				rollAngle += speed;
				if (rollAngle > 45) {
					rollingLeft = false;
					normalizeRoll = true;
				}
			}
		}
	}
	private void adjustPitch() {
		float speed = 3.5f;
		if (normalizePitch) {
			if (pitchAngle > 0) {
				pitchAngle -= speed;
			}else if (pitchAngle < 0) {
				pitchAngle += speed;
			}
		}else{
			if (pitchingDown) {
				pitchAngle -= speed;
				if (pitchAngle < -45) {
					pitchingDown = false;
					normalizePitch = true;
				}
			}else if (pitchingUp) {
				pitchAngle += speed;
				if (pitchAngle > 45) {
					pitchingUp = false;
					normalizePitch = true;
				}
			}
		}
	}
}
