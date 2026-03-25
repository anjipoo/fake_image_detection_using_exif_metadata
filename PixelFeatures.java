import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class PixelFeatures {

    public double entropy;
    public double laplacianSharpness;
    public double edgeDensity;

    public double meanR;
    public double meanG;
    public double meanB;

    public double varR;
    public double varG;
    public double varB;

    public static BufferedImage loadImage(String path) throws Exception {
        return ImageIO.read(new File(path));
    }

    public static PixelFeatures extract(String imagePath) {

        PixelFeatures pf = new PixelFeatures();

        try {

            BufferedImage img = loadImage(imagePath);

            int width = img.getWidth();
            int height = img.getHeight();

            int[] histogram = new int[256];

            double sumR = 0;
            double sumG = 0;
            double sumB = 0;

            for(int y=0;y<height;y++){
                for(int x=0;x<width;x++){

                    int pixel = img.getRGB(x,y);

                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;

                    sumR += r;
                    sumG += g;
                    sumB += b;

                    int gray = (r + g + b) / 3;

                    histogram[gray]++;
                }
            }

            double total = width * height;

            pf.meanR = sumR / total;
            pf.meanG = sumG / total;
            pf.meanB = sumB / total;

            double vr = 0;
            double vg = 0;
            double vb = 0;

            for(int y=0;y<height;y++){
                for(int x=0;x<width;x++){

                    int pixel = img.getRGB(x,y);

                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;

                    vr += Math.pow(r - pf.meanR,2);
                    vg += Math.pow(g - pf.meanG,2);
                    vb += Math.pow(b - pf.meanB,2);
                }
            }

            pf.varR = vr / total;
            pf.varG = vg / total;
            pf.varB = vb / total;

            double entropy = 0;

            for(int i=0;i<256;i++){

                if(histogram[i]==0) continue;

                double p = histogram[i] / total;

                entropy -= p * (Math.log(p) / Math.log(2));
            }

            pf.entropy = entropy;

            int[][] laplacianKernel = {
                    {0,-1,0},
                    {-1,4,-1},
                    {0,-1,0}
            };

            double sharpnessSum = 0;

            for(int y=1;y<height-1;y++){
                for(int x=1;x<width-1;x++){

                    double value = 0;

                    for(int ky=-1;ky<=1;ky++){
                        for(int kx=-1;kx<=1;kx++){

                            int pixel = img.getRGB(x+kx,y+ky);

                            int r = (pixel >> 16) & 0xff;
                            int g = (pixel >> 8) & 0xff;
                            int b = pixel & 0xff;

                            int gray = (r + g + b) / 3;

                            value += gray * laplacianKernel[ky+1][kx+1];
                        }
                    }

                    sharpnessSum += Math.abs(value);
                }
            }

            pf.laplacianSharpness = sharpnessSum / total;

            int[][] sobelX = {
                    {-1,0,1},
                    {-2,0,2},
                    {-1,0,1}
            };

            int[][] sobelY = {
                    {-1,-2,-1},
                    {0,0,0},
                    {1,2,1}
            };

            int edgeCount = 0;

            for(int y=1;y<height-1;y++){
                for(int x=1;x<width-1;x++){

                    double gx = 0;
                    double gy = 0;

                    for(int ky=-1;ky<=1;ky++){
                        for(int kx=-1;kx<=1;kx++){

                            int pixel = img.getRGB(x+kx,y+ky);

                            int r = (pixel >> 16) & 0xff;
                            int g = (pixel >> 8) & 0xff;
                            int b = pixel & 0xff;

                            int gray = (r + g + b) / 3;

                            gx += gray * sobelX[ky+1][kx+1];
                            gy += gray * sobelY[ky+1][kx+1];
                        }
                    }

                    double magnitude = Math.sqrt(gx*gx + gy*gy);

                    if(magnitude > 100){
                        edgeCount++;
                    }
                }
            }

            pf.edgeDensity = (double)edgeCount / total;

        }
        catch(Exception e){
            System.out.println("Pixel feature extraction failed: " + e.getMessage());
        }

        return pf;
    }

    public String toString() {

        return "PixelFeatures{" +
                "entropy=" + entropy +
                ", laplacianSharpness=" + laplacianSharpness +
                ", edgeDensity=" + edgeDensity +
                ", meanR=" + meanR +
                ", meanG=" + meanG +
                ", meanB=" + meanB +
                ", varR=" + varR +
                ", varG=" + varG +
                ", varB=" + varB +
                '}';
    }

    public static void main(String[] args) {

        String path = "images/real.jpg";

        PixelFeatures pf = extract(path);

        System.out.println(pf);

    }
}