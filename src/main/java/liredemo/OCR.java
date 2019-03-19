package liredemo;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Arrays;

import net.semanticmetadata.lire.imageanalysis.features.GlobalFeature;
import net.semanticmetadata.lire.imageanalysis.features.LireFeature;
import net.semanticmetadata.lire.utils.SerializationUtils;
import net.sourceforge.tess4j.Tesseract1;
import net.sourceforge.tess4j.TesseractException;

import java.awt.image.FilteredImageSource;
import java.awt.image.ImageFilter;
import java.awt.image.ImageProducer;
import java.awt.Toolkit;
import javax.swing.GrayFilter;

public class OCR implements GlobalFeature {

    private String text;

    public OCR() {
    }

    @Override
    public byte[] getByteArrayRepresentation() {
        return text.getBytes();
    }

    @Override
    public double getDistance(LireFeature feature) {
        String other_text = new String(feature.getByteArrayRepresentation());

        if (text.equals(other_text)) {
            return 0;
        }

        String[] words = Arrays.stream(text.split(" ")).distinct().toArray(String[]::new);
        String[] other_words = Arrays.stream(other_text.split(" ")).distinct().toArray(String[]::new);
        int matches = 0;

        for (String word : other_words) {
            if (text.contains(word)) {
                matches += 1;
            }
        }

        int distance = other_words.length - matches;
        return distance + (words.length - distance);
    }

    @Override
    public String getFeatureName() {
        return "OCR";
    }

    @Override
    public String getFieldName() {
        return "OCR";
    }

    @Override
    public void setByteArrayRepresentation(byte[] bytes) {
        text = SerializationUtils.toString(bytes);
    }

    @Override
    public void setByteArrayRepresentation(byte[] bytes, int offset, int length) {
        text = new String(bytes.toString().substring(offset, offset + length));
    }

    @Override
    public double[] getFeatureVector() {
        byte[] byteArray = getByteArrayRepresentation();
        int times = Double.SIZE / Byte.SIZE;
        double[] doubles = new double[byteArray.length / times];
        for (int i = 0; i < doubles.length; i++) {
            doubles[i] = ByteBuffer.wrap(byteArray, i * times, times).getDouble();
        }
        return doubles;
    }

    @Override
    public void extract(BufferedImage image) {
        System.out.println("[OCR] Extracting...");

        Tesseract1 instance = new Tesseract1();
        try {
            // System.out.println("[OCR] From normal image: " + instance.doOCR(image));
            
            // ImageFilter filter = new GrayFilter(true, 50);  
            // ImageProducer producer = new FilteredImageSource(image.getSource(), filter);  
            // Image gray_image = Toolkit.getDefaultToolkit().createImage(producer);  
            // System.out.println("[OCR] From grayscaled image: " + instance.doOCR(toBufferedImage(gray_image)));

            BufferedImage bw_image = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
            text = instance.doOCR(toBufferedImage(bw_image));
            System.out.println("[OCR] From black/white image: " + text);

        } catch (TesseractException e) {

            System.out.println("[OCR] Something went wrong");
            System.out.println(e);
            e.printStackTrace();
        }
    }

    /**
     * Converts a given Image into a BufferedImage
     *
     * @param img The Image to be converted
     * @return The converted BufferedImage
     */
    private BufferedImage toBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

}