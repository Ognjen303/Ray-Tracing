package uk.ac.cam.cl.gfxintro.crsid.tick1;

import java.awt.image.BufferedImage;
import java.util.List;

public class Renderer {
	
	// The width and height of the image in pixels
	private int width, height;
	
	// Bias factor for reflected and shadow rays
	private final double EPSILON = 0.0001;

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
			
			// Cast shadow ray
			Ray shadowRay = new Ray(P.add(N.scale(EPSILON)), L);
			
			//TODO: Determine if shadowRay intersects with an object
			//TODO: If it does not, add diffuse/specular components
			RaycastHit closestHit = scene.findClosestIntersection(shadowRay);
			double distanceToHit = closestHit.getDistance();
			
			if (distanceToHit == Double.POSITIVE_INFINITY) {
				
				// no object was hit, add the specular and diffuse components
				colourToReturn = colourToReturn.add(diffuse);
				colourToReturn = colourToReturn.add(specular);
			} else if (distanceToHit > distanceToLight) {
				
				// First we reached PointLight and then hit object
				// Hence point P is not in shadow of object
				colourToReturn = colourToReturn.add(diffuse);
				colourToReturn = colourToReturn.add(specular);
			} else {
				// point P is in shadow, do nothing
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
				ColorRGB linearRGB = trace(scene, ray, bounces); // Trace path of cast ray and determine colour
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
