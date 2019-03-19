package liredemo;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;

import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.utils.SerializationUtils;

public class GPS implements GlobalFeature {

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

        double[] coords = SerializationUtils.toDoubleArray(feature.getByteArrayRepresentation());
        double R = 6371e3; // metres
        double l1 = Math.toRadians(this.coordinates[0]);
        double l2 = Math.toRadians(coords[0]);
        double Ao = Math.toRadians(coords[0] - this.coordinates[0]);
        double Aa = Math.toRadians(coords[1] - this.coordinates[1]);

        double a = Math.sin(Ao / 2) * Math.sin(Ao / 2) + Math.cos(l1) * Math.cos(l2) * Math.sin(Aa / 2) * Math.sin(Aa / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double d = R * c;
        double km = 2000;

        return Math.floor(d / 2000);
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
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Metadata metadata;

            System.out.println("[GPS] Extracting...");

            if (ImageIO.write(image, "jpeg", output)) {
                ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray(), 0, output.size());
                metadata = ImageMetadataReader.readMetadata(inputStream);

                Iterable<Directory> directories = metadata.getDirectories();
                for (Directory dir : directories) {
                    Collection<Tag> tags = dir.getTags();
                    for (Tag tag : tags) {
                        System.out.println("[GPS] Directory: " + tag.getDirectoryName() + ", Tag: " + tag.getTagName()
                                + ", Value: " + tag.getDescription());
                    }
                }

                final GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
                final Collection<GpsDirectory> gpss = metadata.getDirectoriesOfType(GpsDirectory.class);

                if (gps == null) {
                    System.out.println("[GPS] Tags not found");
                    this.coordinates = new double[] { 0, 0 };
                } else {
                    System.out.println("[GPS] Getting location...");
                    GeoLocation location = gps.getGeoLocation();
                    this.coordinates = new double[] { location.getLatitude(), location.getLongitude() };
                    System.out.println("[GPS] Latitude: " + coordinates[0] + ", Longitude: " + coordinates[1]);
                }

                if (gpss == null || gpss.size() == 0) {
                    System.out.println("[GPS] Tags not found");
                    this.coordinates = new double[] { 0, 0 };
                } else {
                    for (GpsDirectory gp : gpss) {
                        System.out.println("[GPS] Getting location...");
                        GeoLocation location = gp.getGeoLocation();
                        this.coordinates = new double[] { location.getLatitude(), location.getLongitude() };
                        System.out.println("[GPS] Latitude: " + coordinates[0] + ", Longitude: " + coordinates[1]);
                    }
                }
            } else {
                System.out.println("[GPS] Tags not found");
                this.coordinates = new double[] { 0, 0 };
            }
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