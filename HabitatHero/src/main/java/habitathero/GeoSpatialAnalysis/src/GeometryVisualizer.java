package habitathero.GeoSpatialAnalysis.src;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeometryVisualizer {
    private static GeometryVisualizer instance;

    private GeometryVisualizer() {
    }

    public static GeometryVisualizer getInstance() {
        if (instance == null) {
            instance = new GeometryVisualizer();
        }
        return instance;
    }

    // Method to visualize geometry from JSONObject
    public static void visualize(JSONObject geomJson, String outputFileName) {
        if (geomJson == null || geomJson.isEmpty()) {
            System.out.println("[ERROR] No geometry provided.");
            return;
        }

        JSONArray coordinates = geomJson.optJSONArray("coordinates");
        if (coordinates == null || coordinates.length() == 0) {
            System.out.println("[ERROR] No coordinates found.");
            return;
        }

        JSONArray ring = getOuterRing(coordinates);
        if (ring == null || ring.length() < 4) {
            System.out.println("[ERROR] Invalid outer ring.");
            return;
        }

        // Find bounds for scaling
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (int i = 0; i < ring.length(); i++) {
            JSONArray point = ring.getJSONArray(i);
            double x = point.getDouble(0);
            double y = point.getDouble(1);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        double width = maxX - minX;
        double height = maxY - minY;
        if (width <= 0 || height <= 0) {
            System.out.println("[ERROR] Invalid geometry bounds.");
            return;
        }

        // Create image
        int imgWidth = 800;
        int imgHeight = 600;
        BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, imgWidth, imgHeight);
        g2d.setColor(Color.BLUE);

        // Scale and draw polygon
        int[] xPoints = new int[ring.length()];
        int[] yPoints = new int[ring.length()];
        for (int i = 0; i < ring.length(); i++) {
            JSONArray point = ring.getJSONArray(i);
            double x = point.getDouble(0);
            double y = point.getDouble(1);
            xPoints[i] = (int) ((x - minX) / width * (imgWidth - 20) + 10);
            yPoints[i] = imgHeight - (int) ((y - minY) / height * (imgHeight - 20) + 10); // Flip Y
        }
        g2d.drawPolygon(xPoints, yPoints, ring.length());

        g2d.dispose();

        // Save image
        try {
            File outputFile = new File(outputFileName + ".png");
            ImageIO.write(image, "png", outputFile);
            System.out.println("[RESULT] Saved: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to save visualization: " + e.getMessage());
        }
    }

    private static JSONArray getOuterRing(JSONArray coordinates) {
        // Either Polygon format [ [ring], ... ] or MultiPolygon [[ [ring], ...], ...]
        if (coordinates.length() == 0) {
            return null;
        }
        if (coordinates.get(0) instanceof JSONArray && ((JSONArray) coordinates.get(0)).length() > 0
                && ((JSONArray) coordinates.get(0)).get(0) instanceof JSONArray) {
            // likely Polygon: coordinates = [ ring, ...]
            return coordinates.getJSONArray(0);
        }
        // MultiPolygon: coordinates = [ [ ring, ... ], ... ]
        JSONArray first = coordinates.optJSONArray(0);
        if (first != null && first.length() > 0) {
            return first.getJSONArray(0);
        }

        return null;
    }

    // Method to visualize outer ring from JSONArray
    public static void visualizeOuterRing(JSONArray ring, String outputFileName) {
        if (ring == null || ring.length() < 4) {
            System.out.println("[ERROR] Invalid outer ring.");
            return;
        }

        // Find bounds for scaling
        double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        for (int i = 0; i < ring.length(); i++) {
            JSONArray point = ring.getJSONArray(i);
            double x = point.getDouble(0);
            double y = point.getDouble(1);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        double width = maxX - minX;
        double height = maxY - minY;
        if (width <= 0 || height <= 0) {
            System.out.println("[ERROR] Invalid geometry bounds.");
            return;
        }

        // Create image
        int imgWidth = 800;
        int imgHeight = 600;
        BufferedImage image = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, imgWidth, imgHeight);
        g2d.setColor(Color.RED); // Use RED to distinguish outer ring from full geometry

        // Scale and draw polygon
        int[] xPoints = new int[ring.length()];
        int[] yPoints = new int[ring.length()];
        for (int i = 0; i < ring.length(); i++) {
            JSONArray point = ring.getJSONArray(i);
            double x = point.getDouble(0);
            double y = point.getDouble(1);
            xPoints[i] = (int) ((x - minX) / width * (imgWidth - 20) + 10);
            yPoints[i] = imgHeight - (int) ((y - minY) / height * (imgHeight - 20) + 10); // Flip Y
        }
        g2d.drawPolygon(xPoints, yPoints, ring.length());

        g2d.dispose();

        // Save image
        try {
            File outputFile = new File(outputFileName + "_outer_ring.png");
            ImageIO.write(image, "png", outputFile);
            System.out.println("[RESULT] Saved: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            System.out.println("[ERROR] Failed to save visualization: " + e.getMessage());
        }
    }
}