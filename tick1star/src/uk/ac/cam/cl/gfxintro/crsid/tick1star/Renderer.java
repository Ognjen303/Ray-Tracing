package uk.ac.cam.cl.gfxintro.crsid.tick1star;

import java.awt.image.BufferedImage;
import java.util.List;

import uk.ac.cam.cl.gfxintro.crsid.tick1star.Ray;

public class Renderer {
	
	// The width and height of the image in pixels
	private int width, height;
	
	// Bias factor for reflected and shadow rays
	private final double EPSILON = 0.0001;
	
	// Distributed shadow ray constants
	private final int SHADOW_RAY_COUNT = 10; // no. of spawned shadow rays
	private final double LIGHT_SIZE = 0.4; // size of spherical light source
	
	// Distributed depth-of-field constants
	private final int DOF_RAY_COUNT = 50; // no. of spawned DoF rays
	private final double DOF_FOCAL_PLANE = 3.51805; // focal lenght of camera
	private final double DOF_AMOUNT = 0.2; //amount of DoF effect

	// The number of times a ray can bounce for reflection
	private int bounces;
	
	// Background colour of the image
	private ColorRGB backgroundColor = new ColorRGB(0.001);

	public Renderer(int width, int height, int bounces) {
		this.width = width;
		this.height = height;
		this.bounces = bounces;
	}

	/*
	 * Trace the ray through the supplied scene, returning the colour to be rendered.
	 * The bouncesLeft parameter is for rendering reflective surfaces.
	 */
	protected ColorRGB trace(Scene scene, Ray ray, int bouncesLeft) {

		// Find closest intersection of ray in the scene
		RaycastHit closestHit = scene.findClosestIntersection(ray);

        // If no object has been hit, return a background colour
        SceneObject object = closestHit.getObjectHit();
        if (object == null){
            return backgroundColor;
        }
        
        // Otherwise calculate colour at intersection and return
        // Get properties of surface at intersection - location, surface normal
        Vector3 P = closestHit.getLocation();
        Vector3 N = closestHit.getNormal();
        Vector3 O = ray.getOrigin();

     	// Illuminate the surface

        // Calculate direct illumination at the point
     	ColorRGB directIllumination = this.illuminate(scene, object, P, N, O);

     	// Get reflectivity of object
     	double reflectivity = object.getReflectivity();
     	
     	if (bouncesLeft == 0 || reflectivity == 0) {
     		// Base case - if no bounces left or non-reflective surface
     		return directIllumination;
     	} else { // recursive case
     		ColorRGB reflectedIllumination;
     		
     		//TODO: Calculate the direction R of the bounced ray
     		/*Note that Vector3.reflectIn(...)
			computes a mirror-like reﬂection and we require a bounce-like reﬂection,
			so you should negate the original direction before reﬂecting it. Ensure too
			that r and n are unit vectors.*/
     		
     		Vector3 D = ray.getDirection();
     		Vector3 R = D.scale(-1.0).reflectIn(N).normalised();
     		
     		//TODO: Spawn a reflectedRay with bias
     		Ray reflectedRay = new Ray(P.add(N.scale(EPSILON)), R);
     		
     		//TODO: Calculate reflectedIllumination by tracing reflectedRay
     		//Recursive call
     		bouncesLeft -= 1;
     		reflectedIllumination = this.trace(scene, reflectedRay, bouncesLeft);
     		
     		// Scale direct and reflective illumination to conserve light
     		directIllumination = directIllumination.scale(1.0 - reflectivity);
     		reflectedIllumination = reflectedIllumination.scale(reflectivity);
     		
     		// Return total illumination
     		return directIllumination.add(reflectedIllumination);
     	}
	}

	/*
	 * Illuminate a surface on and object in the scene at a given position P and surface normal N,
	 * relative to ray originating at O
	 */
	private ColorRGB illuminate(Scene scene, SceneObject object, Vector3 P, Vector3 N, Vector3 O) {
	   
		ColorRGB colourToReturn = new ColorRGB(0);

		ColorRGB I_a = scene.getAmbientLighting(); // Ambient illumination intensity

		ColorRGB C_diff = object.getColour(); // Diffuse colour defined by the object
		
		// Get Phong coefficients
		double k_d = object.getPhong_kD();
		double k_s = object.getPhong_kS();
		double alpha = object.getPhong_alpha();

		// TODO: Add ambient light term to start with
		
		ColorRGB ambient_light = C_diff.scale(I_a);
		colourToReturn = colourToReturn.add(ambient_light);
		

		// Loop over each point light source
		List<PointLight> pointLights = scene.getPointLights();
		for (int i = 0; i < pointLights.size(); i++) {
			PointLight light = pointLights.get(i); // Select point light
			
			// Calculate point light constants
			double distanceToLight = light.getPosition().subtract(P).magnitude();
			ColorRGB C_spec = light.getColour();
			ColorRGB I = light.getIlluminationAt(distanceToLight);
			
			
			// TODO: Calculate L, V, R
			Vector3 L = light.getPosition().subtract(P).normalised();
			Vector3 V = P.scale(-1.0).normalised(); // Camera is at origin (0, 0, 0), hence V = origin - P
			Vector3 R = L.reflectIn(N).normalised();
			
			// TODO: Calculate ColorRGB diffuse and ColorRGB specular terms
			ColorRGB diffuse = C_diff.scale(k_d * Math.max(0, N.dot(L))).scale(I);
			ColorRGB specular = C_spec.scale(k_s * Math.pow(Math.max(0, R.dot(V)), alpha)).scale(I);
			
			// Cast shadow rays
			int notInShadowCount = 0; // number of cast shadow rays which are in shadow
			for (int j = 0; j < SHADOW_RAY_COUNT; j++) {
				
				// Offset point light source to simulate spherical light source
				Vector3 light_offset = Vector3.randomInsideUnitSphere();
				light_offset = light_offset.scale(LIGHT_SIZE);
				Vector3 L_offset = light.getPosition().subtract(P).add(light_offset).normalised();
				
				// Cast shadow
				Ray shadowRay = new Ray(P.add(N.scale(EPSILON)), L_offset);
				
				//TODO: Determine if shadowRay intersects with an object
				//TODO: If it does not, add diffuse/specular components
				RaycastHit closestHit = scene.findClosestIntersection(shadowRay);
				double distanceToHit = closestHit.getDistance();
				
				if (distanceToHit == Double.POSITIVE_INFINITY || (distanceToHit > distanceToLight)) {
					
					// no object was hit, add the specular and diffuse components
					// First we reached PointLight and then hit object
					// Hence point P is not in shadow of object
					notInShadowCount++;
				} else {
					// point P is in shadow, do nothing
				}
			}
			
			// add specular and diffuse components
			if (notInShadowCount != 0) {
				colourToReturn = colourToReturn.add(diffuse.scale(1.0 * notInShadowCount / SHADOW_RAY_COUNT));
				colourToReturn = colourToReturn.add(specular.scale(1.0 * notInShadowCount / SHADOW_RAY_COUNT));
			}
		}
		return colourToReturn;
	}

	// Render image from scene, with camera at origin
	public BufferedImage render(Scene scene) {
		
		// Set up image
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		
		// Set up camera
		Camera camera = new Camera(width, height);

		// Loop over all pixels
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				Ray ray = camera.castRay(x, y); // Cast ray through pixel
				double s = DOF_FOCAL_PLANE  / (new Vector3(0, 0, 1).dot(ray.getDirection()));
				Vector3 F = ray.evaluateAt(s); // intersection of point-pin-hole ray with focal plane
				
				double r_avg = 0; // use these to average out all DOF_RAY_COUNT contributions
				double g_avg = 0;
				double b_avg = 0;
				
				for (int i = 0; i < DOF_RAY_COUNT; i++) {
					double xRandom = Math.random(); // generate number in range [-1, 1]
					double yRandom = Math.random();
					xRandom *= DOF_AMOUNT;
					yRandom *= DOF_AMOUNT;
					Vector3 O = new Vector3(xRandom, yRandom, 0.0); // Point on aperature of camera
					
					Ray castRay = new Ray(O, F.subtract(O).normalised()); //O_dash, F - O_dash.normalised()
					ColorRGB linearRGBTraced = trace(scene, castRay, bounces); // Trace path of castRay and determine colour
					
					r_avg += linearRGBTraced.r;
					g_avg += linearRGBTraced.g;
					b_avg += linearRGBTraced.b;
				}
				
				r_avg /= DOF_RAY_COUNT;
				g_avg /= DOF_RAY_COUNT;
				b_avg /= DOF_RAY_COUNT;
				
				ColorRGB linearRGB = new ColorRGB(r_avg, g_avg, b_avg); 
				ColorRGB gammaRGB = tonemap( linearRGB );
				image.setRGB(x, y, gammaRGB.toRGB()); // Set image colour to traced colour
			}
			// Display progress every 10 lines
            if( y % 10 == 9 | y==(height-1) )
			    System.out.println(String.format("%.2f", 100 * y / (float) (height - 1)) + "% completed");
		}
		return image;
	}


	// Combined tone mapping and display encoding
	public ColorRGB tonemap( ColorRGB linearRGB ) {
		double invGamma = 1./2.2;
		double a = 2;  // controls brightness
		double b = 1.3; // controls contrast

		// Sigmoidal tone mapping
		ColorRGB powRGB = linearRGB.power(b);
		ColorRGB displayRGB = powRGB.scale( powRGB.add(Math.pow(0.5/a,b)).inv() );

		// Display encoding - gamma
		ColorRGB gammaRGB = displayRGB.power( invGamma );

		return gammaRGB;
	}


}
