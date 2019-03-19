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
import com.drew.lang.Rational;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.utils.SerializationUtils;

public class GPS implements net.semanticmetadata.lire.imageanalysis.features.GlobalFeature {

    private double[] coordinates;
    final private String separator = ":";

    @Override
    public byte[] getByteArrayRepresentation() {
        return SerializationUtils.toByteArray(this.coordinates);
    }

    @Override
    public double getDistance(LireFeature feature) {
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
        return this.coordinates;
    }

    @Override
    public void extract(BufferedImage image) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Metadata metadata;
        try {
            System.out.println("GPS ------------------------------");
            ImageIO.write(image, "png", output);
            metadata = ImageMetadataReader
                    .readMetadata(new ByteArrayInputStream(output.toByteArray(), 0, output.size()));
            final GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);

            final Rational[] lat = gps.getRationalArray(GpsDirectory.TAG_LATITUDE);
            final double latitude = lat[0].doubleValue() + lat[1].doubleValue() / 60 + lat[2].doubleValue() / 3600;
            final Rational[] lng = gps.getRationalArray(GpsDirectory.TAG_LONGITUDE);
            final double longitude = lng[0].doubleValue() + lng[1].doubleValue() / 60 + lng[2].doubleValue() / 3600;

            System.out.println("Extracting GPS...");
            System.out.println("Latitude: " + latitude + ", Longitude: " + longitude);
            System.out.println("GPS ------------------------------");

            this.coordinates = new double[] { latitude, longitude };

            System.out.println(SerializationUtils.toString(this.coordinates));
            System.out.println("GPS ------------------------------");

        } catch (ImageProcessingException | IOException e) {
            e.printStackTrace();
        }
    }

}