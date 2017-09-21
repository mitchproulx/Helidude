package animation;

import objects.ComplexShape;
import models.Dude;
import models.Shape;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class GameRenderer implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {

	public boolean TEST = false;
	public Scene startScene = Scene.ONE;

	/* INITIAL SETTINGS */
	public static final boolean TRACE = false;
	public static final boolean MIPMAPPING = false;
	public static final String[] TEXTURE_RESOURCES = {
		"resources/brick.jpg", 		// 0 - http://www.textures.com/system/gallery/photos/Brick/Medieval/Bricks/61536/BrickOldRounded0134_1_600.jpg?v=4
		"resources/wormhole.jpg", 	// 1 - http://forums.techgage.com/attachment.php?attachmentid=1540&d=1355606092
		"resources/circle.png", 	// 2 - http://www.combinatorics.org/files/Surveys/ds5/gifs/5-VD-ellipses-colour.gif
		"resources/skull.png",		// 3 - http://onepiece.wikia.com/wiki/Jolly_Roger
		"resources/space1.jpg", 	// 4 - http://universe-beauty.com/albums/userpics/2011y/05/23/1/10/amazing-space-photo-img126-JPG.jpg
		"resources/space3.jpg"		// 5 - https://wallpaperscraft.com/image/space_stars_blue_background_80369_1600x900.jpg
	};

	public static final String WINDOW_TITLE = "Helidude Graphics Project by Mitchell Proulx";
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 640;

	// FOG
	public float[] colour = new float[] { 0.2f, 0.2f, 0.2f, 0 };
	public int fogMode = 1;
	public float density = 0.35f;
	public boolean changed = true, enabled = true;

	// CAMERA
	public float ar;
	public boolean changeView = false;
	public float startCameraZ = -1.5f, startCameraY = -1, angleUp = 0, cMove = 0, cFollowMove = 0;
	public boolean thirdPerson = false, freeLook = false;
	public float xClick = 0, yClick = 0;
	public float[] lastDragPos = null;

	// ANIMATION
	public float INTERVAL = 0.005f; // inc/dec t by this amount 60 times a second
	public boolean reverse = false, dReverse = false;
	public float t = 0.0f;
	public Scene currScene = null;

	// WORLD
	public final int MAX_DIST = 246;
	public Texture[] textures;
	public int sc3_texture = 4;
	public float maxAnisotropic = 1.0f, scale = 1.0f, distance = 25;
	public boolean aniso, anisoChanged;
	public boolean hitWall = false;
	public int numFloorObjs = 35, totalOrbs = 0, bigCylStart = -25, smallCylStart = -15, trapStart = -15, boostStart = -10;
	public float zBoosts[], xBoosts[];
	public float zBoosts2[], xBoosts2[], yBoosts2[];
	public float zTraps[], xTraps[];
	public float zWalls[], zWalls2[], xWalls[], xWalls2[];
	public float zCubes[], xCubes[];
	public static final GLU glu = new GLU();
	public int orbHit[] = new int[numFloorObjs];
	GLUquadric orb, cylinder;
	public float wallWidth = 0.4f, wallHeight = 0.4f;
	public float shortWallWidth = 0.45f, shortWallHeight = 0.45f;
	ComplexShape diamond = null, spaceShip = null;	// (.obj) files with more complex vertices
	public float shipDist = -45f;	// slot distance away from the dude in scene 2

	public float dPos = -2.8f, dINTERVAL = 0.02f;
	public static final String INPUT_PATH_NAME = "resources/";

	// ORB
	final float radius = 0.25f;
	final int slices = 16, stacks = 16;

	// DUDE
	public  String direction;
	public Dude dude = null;

	// WORLD
	public float bMinX = -0.8f, bMinY = -0.8f, bMaxX = 0.8f, bMaxY = 0.8f;
	public float tMinX = -1f, tMinY = -0.5f, tMaxX = 1f, tMaxY = 0.5f;
	public float minX = -5f, minY = -5f, maxX = 5f, maxY = 5f, rightBound = 5, leftBound = -5;

	public static void main(String[] args) {

		final JFrame frame = new JFrame(WINDOW_TITLE);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (TRACE)
					System.out.println("closing window '" + ((JFrame)e.getWindow()).getTitle() + "'");
				System.exit(0);
			}
		});

		final GLProfile profile = GLProfile.get(GLProfile.GL2);
		final GLCapabilities capabilities = new GLCapabilities(profile);
		final GLCanvas canvas = new GLCanvas(capabilities);
		try {
			Object self = self().getConstructor().newInstance();
			self.getClass().getMethod("setup", new Class[] { GLCanvas.class }).invoke(self, canvas);
			canvas.addGLEventListener((GLEventListener)self);
			canvas.addKeyListener((KeyListener)self);
			canvas.addMouseListener((MouseListener)self);
			canvas.addMouseMotionListener((MouseMotionListener)self);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);

		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);

		canvas.requestFocusInWindow();

		if (TRACE)
			System.out.println("-> end of main().");
	}

	private static Class<?> self() {
		// get containing class of a static method 
		return new Object() { }.getClass().getEnclosingClass();
	}

	public void setup(final GLCanvas canvas) {
		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (t >= 1) {
					reverse = !reverse;
					t = 0;
				} else {
					t += INTERVAL;
				}
				if (dPos >= 2.8f) {
					dReverse = !dReverse;
					dPos = -2.8f;
				} else {
					dPos += dINTERVAL;
				}
				canvas.repaint();
			}
		}, 1000, 1000/60);

		dude = new Dude(this);
	}

	public enum Scene {
		ONE, TWO, THREE 
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		final GL2 gl = drawable.getGL().getGL2();

		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

		textures = new Texture[TEXTURE_RESOURCES.length];
		try {
			for (int i = 0; i < TEXTURE_RESOURCES.length; i++) {
				File infile = new File(TEXTURE_RESOURCES[i]);
				BufferedImage image = ImageIO.read(infile);
				ImageUtil.flipImageVertically(image);
				textures[i] = TextureIO.newTexture(AWTTextureIO.newTextureData(gl.getGLProfile(), image, MIPMAPPING));
				textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_REPEAT);
				textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_REPEAT);

				if (MIPMAPPING) {
					textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
					textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR_MIPMAP_LINEAR);
				} else {
					textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR); // GL_LINEAR or GL_NEAREST
					textures[i].setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_FASTEST);
		gl.glTexEnvf(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_REPLACE); 
		if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic")) {
			float max[] = new float[1];
			gl.glGetFloatv(GL.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, max, 0);
			maxAnisotropic = max[0];
		}

		zBoosts = new float[numFloorObjs];
		xBoosts = new float[numFloorObjs];

		zBoosts2 = new float[numFloorObjs];
		xBoosts2 = new float[numFloorObjs];
		yBoosts2 = new float[numFloorObjs];

		zTraps = new float[numFloorObjs];
		xTraps = new float[numFloorObjs];

		zWalls = new float[numFloorObjs];
		zWalls2 = new float[numFloorObjs * 2];

		xWalls = new float[numFloorObjs];
		xWalls2 = new float[numFloorObjs * 2];

		zCubes = new float[numFloorObjs];
		xCubes = new float[numFloorObjs];

		initializeObstacles();

		orb = glu.gluNewQuadric();
		cylinder = glu.gluNewQuadric();
		diamond = new ComplexShape(new Shape[] { new Shape(INPUT_PATH_NAME + "dodecahedron.obj") }, new float[][] {{0.0f, 2.0f, 0.0f}});
		spaceShip = new ComplexShape(new Shape[] { new Shape(INPUT_PATH_NAME + "shuttle.obj") }, new float[][] {{0.0f, 2.0f, 0.0f}});

		dude.stop = false;
		currScene = startScene;
	}

	public void initializeObstacles() {

		// FOR SCENE 1
		for (int j = 0; j < numFloorObjs; j++) {
			// RANDOMLY CREATE THE SPEED BOOSTS
			xBoosts[j] = (float) (3f*(Math.random()));
			if (Math.random() > 0.5) {
				xBoosts[j] *= -1;
			}
			zBoosts[j] = -((float) (distance*(Math.random()*10)));

			// RANDOMLY CREATE THE TRAPS
			xTraps[j] = (float) (3f*(Math.random()));
			if (Math.random() > 0.5) {
				xTraps[j] *= -1;
			}
			zTraps[j] = -((float) (distance*(Math.random()*10)));

			// RANDOMLY CREATE THE CLYINDERS
			xWalls[j] = (float) (3f*(Math.random()));
			if (Math.random() > 0.5) {
				xWalls[j] *= -1;
			}
			zWalls[j] = -((float) (distance*(Math.random()*10)));

			// RANDOMLY CREATE THE CUBES
			xCubes[j] = (float) (3f*(Math.random()));
			if (Math.random() > 0.5) {
				xCubes[j] *= -1;
			}
			zCubes[j] = -((float) (distance*(Math.random()*10)));
		}

		// FOR SCENE 2
		for (int i = 0; i < numFloorObjs; i++) {
			yBoosts2[i] = (float) (5f*(Math.random()));
			xBoosts2[i] = (float) (3f*(Math.random()));
			if (Math.random() > 0.5) {
				xBoosts2[i] *= -1;
			}
			zBoosts2[i] = -((float) (distance*(Math.random()*10)));
		}
		for (int n = 0; n < (numFloorObjs * 2); n++) {
			zWalls2[n] = -((float) (distance*2*(Math.random()*10)));
			xWalls2[n] = (float) (5f*(Math.random()));
			if (Math.random() > 0.5) {
				xWalls2[n] *= -1;
			}
		}
	}

	@Override
	public void display(GLAutoDrawable drawable) {

		final GL2 gl = drawable.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		switch(currScene){
		case ONE:
			drawSceneOne(gl);
			break;
		case TWO:
			drawSceneTwo(gl);
			break;
		case THREE:
			drawSceneThree(gl);
			break;
		}
		updateCamera(gl);
	}

	private void drawSceneThree(GL2 gl) {
		drawFog(gl);
		drawEnvironmentObjects(gl);
		drawGroundRoof(gl);
		drawSideWalls(gl);
		drawEndWall(gl);

		dude.stop = false;
		dude.run(gl);
	}

	private void drawSceneTwo(GL2 gl) {
		drawFog(gl);
		drawGroundRoof(gl);
		drawSideWalls(gl);

		System.out.println("\n\n PRESS THE UP ARROW KEY TO MOVE TOWARDS THE SPACE SHIP \n\n");
		
		dude.stop = true;
		dude.turnAngle = 90;
		dude.yMove = 0;
		dude.xMove = 0;
		drawShip(gl);
		dude.run(gl);
	}

	private void drawSceneOne(GL2 gl) {
		drawFog(gl);
		drawGhosts(gl);
		drawEnvironmentObjects(gl);
		drawGroundRoof(gl);
		drawSideWalls(gl);
		drawEndWall(gl);

		dude.run(gl);
	}

	public void drawFog(GL2 gl) {
		if (changed) {
			if (enabled) {
				gl.glEnable(GL2.GL_FOG);
				gl.glFogfv(GL2.GL_FOG_COLOR, colour, 0);
				if (fogMode == 1) {
					gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP);
				} else {
					gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_EXP2);
				}
				//System.out.println("Fog desnity is " + density);
				gl.glFogf(GL2.GL_FOG_DENSITY, density);
			}else{
				gl.glDisable(GL2.GL_FOG);
			}
			changed = false;
		}
	}

	public void drawGhosts(GL2 gl) {
		for (int m = 0; m < 10; m++) {
			gl.glPushMatrix();
			gl.glTranslatef(dPos, 1f, -24*(m+1));
			gl.glRotatef(dude.boostColorPos, 0, 1, 0);
			gl.glScalef(0.5f, 0.5f, 0.5f);
			if (dude.forwardMove > -24*(m+1)) {
				diamond.draw(gl);
			}
			gl.glPopMatrix();
		}
	}

	public void drawEnvironmentObjects(GL2 gl) {

		// first draw the magical orbs
		float[][] rainbow = dude.boostArray;

		if (currScene == Scene.ONE) {				// MAGICAL ORBS
			for (int z = 0; z < numFloorObjs; z++){


				gl.glPushMatrix();
				gl.glColor3f(rainbow[z%rainbow.length][0], rainbow[z%rainbow.length][1], rainbow[z%rainbow.length][2]);
				if (zBoosts[z] > -MAX_DIST && zBoosts[z] < boostStart) {
					if (orbHit[z]==0) {
						gl.glTranslatef(xBoosts[z], 0.5f, zBoosts[z]);
						glu.gluQuadricDrawStyle(orb, GLU.GLU_FILL);
						glu.gluSphere(orb, radius, slices, stacks);
						glu.gluDeleteQuadric(orb);
					}
				}
				gl.glPopMatrix();

				// SPEED BOOSTS, TRAPS, SHORT/LONG WALLS
				drawSpeedTiles(gl, z);
				drawSlowTraps(gl, z);
				drawShortCylinders(gl, z);
				drawRegularCylinders(gl, z);
			}
		}
		else if (currScene == Scene.THREE) {		// WARP GATES

			for (int z = 0; z < numFloorObjs; z++){
				gl.glPushMatrix();
				gl.glColor3f(rainbow[z%rainbow.length][0], rainbow[z%rainbow.length][1], rainbow[z%rainbow.length][2]);
				if (zBoosts2[z] > -MAX_DIST && zBoosts2[z] < boostStart) {
					if (orbHit[z]==0) {
						if (yBoosts2[z] >= 4.9f) {
							yBoosts2[z] = 4.5f;
						}
						if (yBoosts2[z] < 0.5f) {
							yBoosts2[z] = 0.5f;
						}
						gl.glTranslatef(xBoosts2[z], yBoosts2[z], zBoosts2[z]);
						glu.gluDisk(cylinder, 0, 0, 8, 1);
						glu.gluCylinder(cylinder, 0.65f, 0.65f, 2.5f, 8, 8);
						glu.gluDeleteQuadric(cylinder);
					}
				}
				gl.glPopMatrix();

				// LONG WALLS ONLY
				drawRegularCylinders(gl, z);
			}
		}
	}

	public void drawGroundRoof(GL2 gl) {

		final int shipDist = 3;
		int direction = -1;
		int range = ((int)distance*2)+5;
		if (currScene == Scene.TWO) {
			range = 20;
		}else if (currScene == Scene.THREE) {
			range = ((int)distance*4);
		}

		for (int tile = 0; tile < range && tile > dude.forwardMove-1; tile++) {
			for (int r = 0; r < 4; r++){
				gl.glPushMatrix();
				gl.glColor3f(1.0f, 1.0f, 1.0f);
				if (r == 0) {
					gl.glTranslatef(0, 0, maxX * tile * direction);			// bottom right
				}else if (r == 1){
					gl.glTranslatef(0, maxY, maxX * tile * direction);			// top right
				}else if(r == 2) {
					gl.glTranslatef(-maxX, 0, maxX * tile * direction);		// bottom left
				}else{
					gl.glTranslatef(-maxX, maxY, maxX * tile * direction);		// bottom right
				}

				if (anisoChanged) {
					for (Texture texture: textures)
						texture.setTexParameterf(gl, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, aniso ? maxAnisotropic : 1);
					gl.glTexParameterf(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, aniso ? maxAnisotropic : 1);
					anisoChanged = false;
				}

				// position and draw the texture
				gl.glRotatef(90, 1, 0, 0);
				if (currScene == Scene.ONE) {
					textures[0].enable(gl);
					textures[0].bind(gl);
					scale = 1.25f;				// brick density
				}
				else if (currScene == Scene.TWO) {
					if (tile < shipDist) {
						textures[0].enable(gl);
						textures[0].bind(gl);
					}else {
						textures[sc3_texture].enable(gl);
						textures[sc3_texture].bind(gl);
					}
					scale = 1.25f;				// brick density
				}
				else if (currScene == Scene.THREE) {
					textures[sc3_texture].enable(gl);
					textures[sc3_texture].bind(gl);
					scale = 1f;				// brick density
				}
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(0.0f, 0.0f);
				gl.glVertex3f(minX, minY, 0);
				gl.glTexCoord2f(scale, 0.0f);
				gl.glVertex3f(maxX, minY, 0);
				gl.glTexCoord2f(scale, scale);
				gl.glVertex3f(maxX, maxY, 0);
				gl.glTexCoord2f(0.0f, scale);
				gl.glVertex3f(minX, maxY, 0);
				gl.glEnd();
				if (currScene == Scene.ONE) {
					textures[0].disable(gl);
				}
				else if (currScene == Scene.TWO) {
					if (tile < shipDist) {
						textures[0].disable(gl);
					}else {
						textures[sc3_texture].disable(gl);
					}
				}
				else if (currScene == Scene.THREE) {
					textures[sc3_texture].disable(gl);
				}
				gl.glPopMatrix();
			}
		}
	}

	public void drawSideWalls(GL2 gl) {

		final int shipDist = 2;
		int direction = -1;
		int range = ((int)distance*2)+5;
		if (currScene == Scene.TWO) {
			range = 20;
		}else if (currScene == Scene.THREE) {
			range = ((int)distance*4);
		}

		for (int n = 0; n < range && n > dude.forwardMove-1; n++) {
			for (int w = 0; w < 2; w++){
				gl.glPushMatrix();
				gl.glColor3f(1.0f, 1.0f, 1.0f);
				if (w == 0) {
					gl.glTranslatef(rightBound,0,maxX*n*direction);
				}else{
					gl.glTranslatef(leftBound,0,maxX*n*direction);
				}
				gl.glRotatef(90, 0, 1, 0);
				if (currScene == Scene.THREE) {
					textures[sc3_texture].enable(gl);
					textures[sc3_texture].bind(gl);
					minX = 0;
					minY = 0;
					scale = 1f;					// brick density
				}else if (currScene == Scene.TWO){
					if (n < shipDist) {
						textures[0].enable(gl);
						textures[0].bind(gl);
					}else{
						textures[sc3_texture].enable(gl);
						textures[sc3_texture].bind(gl);	
					}
					minX = 0;
					minY = 0;
					scale = 1f;
				}else if (currScene == Scene.ONE){
					textures[0].enable(gl);
					textures[0].bind(gl);
					minX = 0;
					minY = 0;
					scale = 2.5f;					// brick density
				}
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(0.0f, 0.0f);
				gl.glVertex3f(minX, minY, 0);
				gl.glTexCoord2f(scale, 0.0f);
				gl.glVertex3f(maxX, minY, 0);
				gl.glTexCoord2f(scale, scale);
				gl.glVertex3f(maxX, maxY, 0);
				gl.glTexCoord2f(0.0f, scale);
				gl.glVertex3f(minX, maxY, 0);
				gl.glEnd();
				if (currScene == Scene.THREE) {
					textures[sc3_texture].disable(gl);
				}else if (currScene == Scene.TWO){
					if (n < shipDist) {
						textures[0].disable(gl);
					}else{
						textures[sc3_texture].disable(gl);
					}
				}else if (currScene == Scene.ONE){
					textures[0].disable(gl);
				}
				gl.glPopMatrix();
			}
		}
	}

	public void drawEndWall(GL2 gl) {

		int startRender = -MAX_DIST + 75;
		if (currScene == Scene.THREE) {
			startRender = -(2*MAX_DIST) + 75;
		}

		if (dude.forwardMove < startRender) {
			for (int p = 0; p < 2; p++){
				gl.glPushMatrix();
				gl.glColor3f(1.0f, 1.0f, 1.0f);

				if (currScene == Scene.ONE){
					if (p==0){
						gl.glTranslatef(-5,0,-MAX_DIST-8);
					}else{
						gl.glTranslatef(0,0,-MAX_DIST-8);
					}
					textures[1].enable(gl);
					textures[1].bind(gl);
					scale = 1f;		// brick density
				}else if (currScene == Scene.THREE) {
					if (p==0){
						gl.glTranslatef(-5,0,(-MAX_DIST*2));
					}else{
						gl.glTranslatef(0,0,(-MAX_DIST*2));
					}
					textures[1].enable(gl);
					textures[1].bind(gl);
					scale = 1f;		// texture density
				}
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(0.0f, 0.0f);
				gl.glVertex3f(minX, minY, 0);
				gl.glTexCoord2f(scale, 0.0f);
				gl.glVertex3f(maxX, minY, 0);
				gl.glTexCoord2f(scale, scale);
				gl.glVertex3f(maxX, maxY, 0);
				gl.glTexCoord2f(0.0f, scale);
				gl.glVertex3f(minX, maxY, 0);
				gl.glEnd();
				if (currScene == Scene.ONE){
					textures[1].disable(gl);
				}else if (currScene == Scene.THREE) {
					textures[1].disable(gl);
				}
				gl.glPopMatrix();
			}
		}
	}

	public void updateCamera(GL2 gl) {
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glFrustumf(ar < 1 ? -1.0f : -ar, ar < 1 ? 1.0f : ar, ar > 1 ? -1.0f : -1/ar, ar > 1 ? 1.0f : 1/ar, 1.0f, distance);

		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

		if (changeView) {	// first person      
			thirdPerson = false;
			if (freeLook) {
				gl.glTranslatef(0, startCameraY-dude.yMove, -dude.forwardMove/2);
				gl.glRotatef(xClick*0.0025f, 0, 1, 0);
				gl.glRotatef(-(yClick*0.0025f), 1, 0, 0);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}else{
				gl.glTranslatef(-1*dude.xMove, startCameraY-dude.yMove, -dude.forwardMove/2);
				gl.glRotatef(cFollowMove, 0, 1, 0);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}
		}else{				// third person		( DEFAULT )
			thirdPerson = true;
			gl.glRotatef(angleUp, 1, 0, 0);
			gl.glTranslatef(0, startCameraY, startCameraZ-dude.forwardMove/2);
			gl.glRotatef(cFollowMove, 0, 1, 0);
			gl.glScalef(0.5f, 0.5f, 0.5f);
		}
	}

	public void drawRegularCylinders(GL2 gl, int z) {
		if (currScene == Scene.ONE) {
			gl.glPushMatrix();
			gl.glColor3f(0.25f, 0.25f, 0.25f);
			if (zWalls[z] > -MAX_DIST && zWalls[z] < bigCylStart) {
				gl.glTranslatef(xWalls[z], 0, zWalls[z]);
				gl.glRotatef(90,0,0,1);
				gl.glRotatef(90,0,1,0);
				glu.gluDisk(cylinder, 0, 0, 8, 1);
				glu.gluCylinder(cylinder, wallWidth, wallHeight, 5f, 8, 8);
				glu.gluDeleteQuadric(cylinder);
			}
			gl.glPopMatrix();	
		}
		else if (currScene == Scene.THREE) {
			gl.glPushMatrix();
			gl.glColor3f(0.25f, 0.85f, 0.25f);
			if (zWalls2[z] > -(2*MAX_DIST) && zWalls2[z] < bigCylStart) {
				gl.glTranslatef(xWalls2[z], 0, zWalls2[z]);
				gl.glRotatef(90,0,0,1);
				gl.glRotatef(90,0,1,0);
				glu.gluDisk(cylinder, 0, 0, 8, 1);
				glu.gluCylinder(cylinder, wallWidth-0.2f, wallWidth-0.2f, 5f, 8, 8);
				glu.gluDeleteQuadric(cylinder);

				gl.glTranslatef(xWalls2[z]+0.4f, 0, 0);
				glu.gluDisk(cylinder, 0, 0, 8, 1);
				glu.gluCylinder(cylinder, wallWidth-0.2f, wallWidth-0.2f, 5f, 8, 8);
				glu.gluDeleteQuadric(cylinder);

				gl.glTranslatef(xWalls2[z]-0.4f, 0, 0);
				glu.gluDisk(cylinder, 0, 0, 8, 1);
				glu.gluCylinder(cylinder, wallWidth-0.2f, wallWidth-0.2f, 5f, 8, 8);
				glu.gluDeleteQuadric(cylinder);

				gl.glTranslatef(xWalls2[z]-0.2f, 0, zWalls2[z]+0.2f);
				glu.gluDisk(cylinder, 0, 0, 8, 1);
				glu.gluCylinder(cylinder, wallWidth-0.2f, wallWidth-0.2f, 5f, 8, 8);
				glu.gluDeleteQuadric(cylinder);

				gl.glTranslatef(xWalls2[z]-0.4f, 0, zWalls2[z]+0.4f);
				glu.gluDisk(cylinder, 0, 0, 8, 1);
				glu.gluCylinder(cylinder, wallWidth-0.2f, wallWidth-0.2f, 5f, 8, 8);
				glu.gluDeleteQuadric(cylinder);

				gl.glTranslatef(xWalls2[z]-0.2f, 0, zWalls2[z]+0.4f);
				glu.gluDisk(cylinder, 0, 0, 8, 1);
				glu.gluCylinder(cylinder, wallWidth-0.2f, wallWidth-0.2f, 5f, 8, 8);
				glu.gluDeleteQuadric(cylinder);

				gl.glTranslatef(xWalls2[z]-0.4f, 0, zWalls2[z]+0.2f);
				glu.gluDisk(cylinder, 0, 0, 8, 1);
				glu.gluCylinder(cylinder, wallWidth-0.2f, wallWidth-0.2f, 5f, 8, 8);
				glu.gluDeleteQuadric(cylinder);
				
				if (z < numFloorObjs) {
					gl.glTranslatef(xWalls2[z*2], 0, zWalls2[z*2]);
					gl.glRotatef(90,0,0,1);
					gl.glRotatef(90,0,1,0);
					glu.gluDisk(cylinder, 0, 0, 8, 1);
					glu.gluCylinder(cylinder, wallWidth-0.2f, wallWidth-0.2f, 5f, 8, 8);
					glu.gluDeleteQuadric(cylinder);
				}
			}
			gl.glPopMatrix();
		}
	}

	public void drawShortCylinders(GL2 gl, int z) {
		gl.glPushMatrix();
		gl.glColor3f(0.55f, 0.5f, 0.5f);
		if (zCubes[z] > -MAX_DIST && zCubes[z] < smallCylStart) {
			for (int cl = 0; cl < 2; cl++){
				gl.glTranslatef(xCubes[z], 0, zCubes[z]);
				gl.glRotatef(90,0,0,1);
				gl.glRotatef(90,0,1,0);
				glu.gluDisk(cylinder, 0, 0, 8, 1);
				glu.gluCylinder(cylinder, shortWallWidth, shortWallHeight, 0.9f, 8, 8);
				glu.gluDeleteQuadric(cylinder);
			}
		}
		gl.glPopMatrix();
	}

	public void drawSlowTraps(GL2 gl, int z) {
		gl.glPushMatrix();
		gl.glColor3f(0.0f, 0.0f, 0.0f);
		if (zTraps[z] > -MAX_DIST && zTraps[z] < trapStart) {		// dont let traps appear right at the beginning
			gl.glTranslatef(xTraps[z], 0, zTraps[z]);
			gl.glRotatef(90, 1, 0, 0);
			gl.glScalef(1,-1,1);
			textures[3].enable(gl);
			textures[3].bind(gl);
			scale = 2f;		// brick density

			gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(0.0f, 0.0f);
			gl.glVertex3f(tMinX, tMinY, -0.011f);
			gl.glTexCoord2f(scale, 0.0f);
			gl.glVertex3f(tMaxX, tMinY, -0.011f);
			gl.glTexCoord2f(scale, scale);
			gl.glVertex3f(tMaxX, tMaxY, -0.011f);
			gl.glTexCoord2f(0.0f, scale);
			gl.glVertex3f(tMinX, tMaxY, -0.011f);
			gl.glEnd();
			textures[3].disable(gl);
		}
		gl.glPopMatrix();	
	}

	public void drawSpeedTiles(GL2 gl, int z) {
		gl.glPushMatrix();
		gl.glColor3f(0.0f, 0.0f, 0.0f);
		if (zBoosts[z] > -MAX_DIST && zBoosts[z] < boostStart) {		// dont let boosts appear right at the beginning				
			gl.glTranslatef(xBoosts[z], 0, zBoosts[z]);
			gl.glRotatef(90, 1, 0, 0);
			gl.glScalef(1,-1,1);
			textures[2].enable(gl);
			textures[2].bind(gl);
			scale = 1f;// brick density

			gl.glBegin(GL2.GL_QUADS);
			gl.glTexCoord2f(0.0f, 0.0f);
			gl.glVertex3f(bMinX, bMinY, -0.01f);
			gl.glTexCoord2f(scale, 0.0f);
			gl.glVertex3f(bMaxX, bMinY, -0.01f);
			gl.glTexCoord2f(scale, scale);
			gl.glVertex3f(bMaxX, bMaxY, -0.01f);
			gl.glTexCoord2f(0.0f, scale);
			gl.glVertex3f(bMinX, bMaxY, -0.01f);
			gl.glEnd();
			textures[2].disable(gl);
		}
		gl.glPopMatrix();
	}

	public void drawShip(GL2 gl) {
		gl.glPushMatrix();
		gl.glScalef(0.25f, 0.25f, 0.25f);
		gl.glRotatef(-90, 0, 0, 1);
		gl.glTranslatef(-8f, -2f, shipDist);
		spaceShip.draw(gl);
		gl.glPopMatrix();
	}

	@Override
	public void dispose(GLAutoDrawable drawable) { }

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		//final GL2 gl = drawable.getGL().getGL2();
		ar = (float)width / (height == 0 ? 1 : height);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		direction = null;
		if (e.getKeyChar() == 'a' || e.getKeyChar() == 'A' || e.getKeyCode() == KeyEvent.VK_LEFT){ // strafe left
			direction = "left";
			if (currScene != Scene.TWO) {
				dude.turnLeft = true;
			}
		}else if (e.getKeyChar() == 'd' || e.getKeyChar() == 'D' || e.getKeyCode() == KeyEvent.VK_RIGHT) { // strafe right
			direction = "right";
			if (currScene != Scene.TWO) {
				dude.turnRight = true;
			}
		}else if (e.getKeyChar() == 'w' || e.getKeyChar() == 'W' || e.getKeyCode() == KeyEvent.VK_UP ){ // move forward
			direction = "up";
			dude.speedUp = true;
		}else if (e.getKeyChar() == 's' || e.getKeyChar() == 'S' || e.getKeyCode() == KeyEvent.VK_DOWN){ // move backwards
			direction = "down";
			dude.backUp = true;
		} else if ((e.getKeyChar() == 'z' || e.getKeyChar() == 'Z') && maxAnisotropic > 0) { // anisotropic filtering
			aniso = !aniso;
			anisoChanged = true;
			System.out.println("Turning anisotropic filtering " + (aniso ? "ON" : "OFF"));
		} else if (e.getKeyChar() == 'm') {			// fog mode
			fogMode++;
			if (fogMode > 2){
				fogMode = 1;
			}
			changed = true;
		}else if (e.getKeyChar() == 't') {	// dude default run speed
			if (dude.slow){
				dude.slow = false;
			}else if (!dude.slow) {
				dude.slow = true;
			}
		}
		if (direction != null) {
			System.out.println("Direction: " + direction);
			((GLCanvas)e.getSource()).repaint();
		}
		if (e.getKeyChar() == ' ') {
			System.out.println("Enter: JUMP!");
			dude.jump = true;
		} else if (e.getKeyChar() == '\n') {
			if (currScene == Scene.ONE) {
				System.out.println("Space bar: Perspective changed!");
				if (!changeView) {
					changeView = true;
				}else if (changeView){
					changeView = false;
				}
			}
		}
	}
	@Override
	public void mousePressed(MouseEvent e) {
		if (!thirdPerson) {
			lastDragPos = new float[] {e.getX(), e.getY()};
			freeLook = true;
		}
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		freeLook = false;
		lastDragPos = null;
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		if (!thirdPerson) {
			xClick += e.getX() - lastDragPos[0];
			yClick -= e.getY() - lastDragPos[1];
		}
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		lastDragPos = new float[] {e.getX(), e.getY()};
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		xClick = e.getX();
		yClick = INITIAL_HEIGHT - e.getY();
	}
	@Override
	public void mouseEntered(MouseEvent e) { }
	@Override
	public void mouseExited(MouseEvent e) { }
	@Override
	public void keyTyped(KeyEvent e) { }
	@Override
	public void keyReleased(KeyEvent e) { }
}

