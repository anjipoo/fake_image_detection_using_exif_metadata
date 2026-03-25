import java.io.File;
import java.util.Locale;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;

import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.xmp.XmpDirectory;

public class MetadataFeatures {

    public static class MetaFeatureVector {

        public double hasMake;
        public double hasModel;
        public double hasDateTime;
        public double hasGPS;
        public double hasSoftware;
        public double softwareIsEditor;
        public double metadataMissing;

        public String make = "";
        public String model = "";
        public String dateTime = "";
        public String software = "";

        public double[] toNumericArray() {
            return new double[]{
                    hasMake, hasModel, hasDateTime,
                    hasGPS, hasSoftware, softwareIsEditor,metadataMissing
            };
        }

        @Override
        public String toString() {
            return "MetaFeatureVector{" +
                    "hasMake=" + hasMake +
                    ", hasModel=" + hasModel +
                    ", hasDateTime=" + hasDateTime +
                    ", hasGPS=" + hasGPS +
                    ", hasSoftware=" + hasSoftware +
                    ", softwareIsEditor=" + softwareIsEditor +
                    ", metadataMissing=" + metadataMissing +
                    ", make='" + make + '\'' +
                    ", model='" + model + '\'' +
                    ", dateTime='" + dateTime + '\'' +
                    ", software='" + software + '\'' +
                    '}';
        }
    }

    public static MetaFeatureVector extract(String imagePath) {

        MetaFeatureVector out = new MetaFeatureVector();

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(imagePath));

            // IFD0 directory (camera make/model/software)
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0 != null) {

                String make = safe(ifd0.getString(ExifIFD0Directory.TAG_MAKE));
                String model = safe(ifd0.getString(ExifIFD0Directory.TAG_MODEL));
                String software = safe(ifd0.getString(ExifIFD0Directory.TAG_SOFTWARE));
                String dateTime = safe(ifd0.getString(ExifIFD0Directory.TAG_DATETIME));

                if (!make.isEmpty()) { out.hasMake = 1; out.make = make; }
                if (!model.isEmpty()) { out.hasModel = 1; out.model = model; }
                if (!software.isEmpty()) { out.hasSoftware = 1; out.software = software; }
                if (!dateTime.isEmpty()) { out.hasDateTime = 1; out.dateTime = dateTime; }
            }

            // SubIFD (better datetime)
            ExifSubIFDDirectory sub = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (sub != null) {

                String dtOriginal = safe(sub.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL));
                String dtDigitized = safe(sub.getString(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED));

                String best = !dtOriginal.isEmpty() ? dtOriginal : dtDigitized;

                if (!best.isEmpty()) {
                    out.hasDateTime = 1;
                    out.dateTime = best;
                }
            }

            // GPS
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null) {
                out.hasGPS = 1;
            }

            // XMP software (some editors store tool name here)
if (out.hasSoftware == 0) {
    XmpDirectory xmp = metadata.getFirstDirectoryOfType(XmpDirectory.class);
    if (xmp != null) {
        // Version-safe: works without getXmpMeta()
        java.util.Map<String, String> props = xmp.getXmpProperties();

        // Common keys
        String creatorTool = safe(props.get("xmp:CreatorTool"));
        String softwareAgent = safe(props.get("xmp:SoftwareAgent"));
        String tool = !creatorTool.isEmpty() ? creatorTool : softwareAgent;

        if (!tool.isEmpty()) {
            out.hasSoftware = 1;
            out.software = tool;
        }
    }
}

            // Detect editing software
            if (out.hasSoftware == 1) {
                out.softwareIsEditor = looksLikeEditor(out.software) ? 1 : 0;
            }

        } catch (Exception e) {
            // If EXIF missing, keep all zeros
        }
// Detect if ALL metadata fields are missing
double totalMeta =
        out.hasMake +
        out.hasModel +
        out.hasDateTime +
        out.hasGPS +
        out.hasSoftware;

if (totalMeta == 0) {
    out.metadataMissing = 1;
} else {
    out.metadataMissing = 0;
}
        return out;
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static boolean looksLikeEditor(String software) {

        String s = software.toLowerCase(Locale.ROOT);

        String[] editors = {
                "photoshop", "lightroom", "snapseed",
                "gimp", "canva", "picsart",
                "adobe", "editor", "retouch"
        };

        for (String e : editors) {
            if (s.contains(e)) return true;
        }

        return false;
    }

    // Quick tester
    public static void main(String[] args) {

    String path = "images/real.jpg";   // change if needed

    File f = new File(path);
    System.out.println("Testing file: " + f.getAbsolutePath());
    System.out.println("Exists: " + f.exists());

    MetaFeatureVector meta = extract(path);

    System.out.println(meta);

    double[] features = meta.toNumericArray();

    System.out.print("Numeric features: ");

    for (double v : features) {
        System.out.print(v + " ");
    }

    System.out.println();
}
}