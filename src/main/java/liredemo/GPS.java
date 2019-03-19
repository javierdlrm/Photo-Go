package liredemo;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.utils.SerializationUtils;

public class GPS implements net.semanticmetadata.lire.imageanalysis.features.GlobalFeature {

    private double[] coordinates;
    final private String separator = ":";

    @Override
    public byte[] getByteArrayRepresentation() {
        if (this.coordinates == null)
            return null;
        return SerializationUtils.toByteArray(this.coordinates);
    }

    @Override
    public double getDistance(LireFeature feature) {
        if (this.coordinates == null)
            return Double.MAX_VALUE;

        List<Double> coordinates = Arrays.stream(this.coordinates).boxed().collect(Collectors.toList());
        List<Double> coordinates_to_compare = Arrays
                .stream(SerializationUtils.toDoubleArray(feature.getByteArrayRepresentation())).boxed()
                .collect(Collectors.toList());
        coordinates.retainAll(coordinates_to_compare);

        return this.coordinates.length - coordinates.size();
    }

    @Override
    public String getFeatureName() {
        return "GPS";
    }

    @Override
    public String getFieldName() {
        return "GPS";
    }

    @Override
    public void setByteArrayRepresentation(byte[] bytes) {
        String representation = new String(bytes);
        int semicolon = representation.indexOf(separator);
        double latitude = Double.parseDouble(representation.substring(0, semicolon));
        double longitude = Double.parseDouble(representation.substring(semicolon + 1));

        this.coordinates = new double[] { latitude, longitude };
    }

    @Override
    public void setByteArrayRepresentation(byte[] bytes, int offset, int length) {
        String representation = new String(bytes);
        int semicolon = representation.indexOf(separator);
        double latitude = Double.parseDouble(representation.substring(0, semicolon));
        double longitude = Double.parseDouble(representation.substring(semicolon + 1));

        double[] coordinates = new double[] { latitude, longitude };

        if (this.coordinates == null) {
            this.coordinates = coordinates;
            return;
        }

        if (offset == 0 && length > 0) {
            this.coordinates[offset] = coordinates[offset];

            if (length >= 1) {
                this.coordinates[offset + 1] = coordinates[offset + 1];
            }
        } else if (offset == 1 && length > 0) {
            this.coordinates[offset] = coordinates[offset];
        }
    }

    @Override
    public double[] getFeatureVector() {
        return this.coordinates != null ? this.coordinates : null;
    }

    @Override
    public void extract(BufferedImage image) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Metadata metadata;
        try {
            System.out.println("[GPS] Extracting...");
            ImageIO.write(image, "png", output);
            metadata = ImageMetadataReader
                    .readMetadata(new ByteArrayInputStream(output.toByteArray(), 0, output.size()));
            final GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);

            if (gps == null) {
                System.out.println("[GPS] Tags not found");
                this.coordinates = null;
                return;
            }

            System.out.println("[GPS] Getting location...");
            GeoLocation location = gps.getGeoLocation();
            this.coordinates = new double[] { location.getLatitude(), location.getLongitude() };

            System.out.println("[GPS] Latitude: " + coordinates[0] + ", Longitude: " + coordinates[1]);
            System.out.println("[GPS] Double[]: ");
            System.out.println(SerializationUtils.toString(this.coordinates));

        } catch (ImageProcessingException | IOException e) {
            System.out.println("[GPS] Something went wrong");
            System.out.println(e);
            e.printStackTrace();
        }
    }

}