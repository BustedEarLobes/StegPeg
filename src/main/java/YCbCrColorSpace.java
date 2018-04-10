import java.awt.Color;

public class YCbCrColorSpace {
    
    public static int fromRGB(int rgb) {
        int r = (rgb >> 16) & 0x0000FF;
        int g = (rgb >> 8) & 0x0000FF;
        int b = rgb & 0x0000FF;
        int y = roundClamp255(0.299 * r + 0.587 * g + 0.114 * b);
        int cb = roundClamp255(128 - 0.168736 * r - 0.331264 * g + 0.5 * b);
        int cr = roundClamp255(128 + 0.5 * r - 0.418688 * g - 0.081312 * b);
        /*int y = roundClamp255(16 + 65.738*r/256 + 129.057*g/256 + 25.064*b/256);
        int cb = roundClamp255(128 - 37.945*r/256 + 74.494*g/256 + 112.439*b/256);
        int cr = roundClamp255(128 + 112.439*r/256 + 94.154*g/256 + 18.285*b/256);
        */
        int yCbCr = y;
        yCbCr = (yCbCr << 8) + cb;
        yCbCr = (yCbCr << 8) + cr;
        return yCbCr;
        
    }
    
    public static double[] fromRGBToF(int rgb) {
        double[] ret = new double[3];
        int r = (rgb >> 16) & 0x0000FF;
        int g = (rgb >> 8) & 0x0000FF;
        int b = rgb & 0x0000FF;
        ret[0] = 0.299 * r + 0.587 * g + 0.114 * b;
        ret[1] = 128 - 0.168736 * r - 0.331264 * g + 0.5 * b;
        ret[2] = 128 + 0.5 * r - 0.418688 * g - 0.081312 * b;
        /*int y = roundClamp255(16 + 65.738*r/256 + 129.057*g/256 + 25.064*b/256);
        int cb = roundClamp255(128 - 37.945*r/256 + 74.494*g/256 + 112.439*b/256);
        int cr = roundClamp255(128 + 112.439*r/256 + 94.154*g/256 + 18.285*b/256);
        */
        return ret;
    }

    public static int toRGB(int yCbCr) {
        int y = (yCbCr >> 16) & 0x0000FF;
        int cb = (yCbCr >> 8) & 0x0000FF;
        int cr = yCbCr & 0x0000FF;
           
        int r = roundClamp255(y + 1.402f * (cr - 128f));
        int g = roundClamp255(y - 0.3441f * (cb - 128f) - 0.7141f * (cr - 128f));
        int b = roundClamp255(y + 1.772f * (cb - 128f));
        
        int rgb = r;
        rgb = (rgb << 8) + g;
        rgb = (rgb << 8) + b;
        return rgb;
    }
    
    private static int roundClamp255(double val) {
        return (int) Math.min(Math.max(Math.round(val), 0), 255);
    }
    
    public static void main(String[] args) {
        Color c = new Color(24, 234, 102);
        int rgb = c.getRGB();
        int yCbCr = fromRGB(rgb);
        System.out.println(((yCbCr >> 16) & 0x0000FF));
        System.out.println(((yCbCr >> 8) & 0x0000FF));
        System.out.println((yCbCr & 0x0000FF));

        System.out.println();

        int rgbNew = toRGB(yCbCr);
        System.out.println(((rgbNew >> 16) & 0x0000FF));
        System.out.println(((rgbNew >> 8) & 0x0000FF));
        System.out.println((rgbNew & 0x0000FF));
    }
}
