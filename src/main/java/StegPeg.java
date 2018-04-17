import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;

public class StegPeg {
    private static final double STEG_COEFF = 0.98;
    
	int[][] quantizationMatrix = new int[][] {
        {16, 12, 14, 14, 18, 24, 49, 72},
        {11, 12, 13, 17, 22, 35, 64, 92},
        {10, 14, 16, 22, 37, 55, 78, 95},
        {16, 19, 24, 29, 56, 64, 87, 98},
        {24, 26, 40, 51, 68, 81, 103, 112},
        {40, 58, 57, 87, 109, 104, 121, 100},
        {51, 60, 69, 80, 103, 113, 120, 103},
        {61, 55, 56, 62, 77, 92, 101, 99}
    };
    
    
    public void stegHide(byte[] data, String fileInName, String fileOutName) {
        try {
            BufferedImage imIn = ImageIO.read(getClass().getClassLoader().getResource(fileInName));
            BufferedImage imOut = new BufferedImage(imIn.getWidth(), imIn.getHeight(), BufferedImage.TYPE_INT_RGB);
            int currentByteIndex = 0;
            int currentBitIndex = 0;
            for(int y = 0; y < imIn.getHeight(); y += 8) {
                for(int x = 0; x < imIn.getWidth(); x += 8) {
                    int[][] rgbBlock = getBlock(imIn, x, y);
                    int[][] yBlock = new int[8][8];
                    
                    for(int bx = 0; bx < 8; bx ++) {
                        for(int by = 0; by < 8; by ++) {
                            int yCbCr = YCbCrColorSpace.fromRGB(rgbBlock[bx][by]);
                            yBlock[bx][by] = ((yCbCr >> 16) & 0x0000FF) - 128;
                        }
                    }
                    
                    // DCT
                    
                    float[][] dctCoeffBlock = type2DCT(yBlock);
                    
                    //dctCoeffBlock[0][0] *= .99;
                    //dctCoeffBlock[1][1] *= .99;
                    byte currentByte = data[currentByteIndex];
                    int bit = getBit(currentByte, currentBitIndex);
                    if(bit == 1) {
                        for(int u = 0; u < 8; u ++) {
                            for(int v = 0; v < 8; v ++) {
                                dctCoeffBlock[u][v] *= STEG_COEFF;
                            }
                        }
                    }
                    
                    currentBitIndex ++;
                    if(currentBitIndex > 7) {
                        currentBitIndex = 0;
                        currentByteIndex ++;
                        if(currentByteIndex >= data.length) {
                            currentByteIndex = 0;
                        }
                    }
                    
                    float[][] yBlockS = type3DCT(dctCoeffBlock);
                    
                    for(int bx = 0; bx < 8; bx ++) {
                        for(int by = 0; by < 8; by ++) {
                            int newY = clamp255((int) (Math.round(yBlockS[bx][by])) + 128);
                            int oldYCbCr = YCbCrColorSpace.fromRGB(rgbBlock[bx][by]);
                            int newYCbCr = ((newY << 16) & 0xFF0000) + (oldYCbCr & 0x0000FFFF);
                            int newRGB = YCbCrColorSpace.toRGB(newYCbCr);
                            rgbBlock[bx][by] = newRGB;
                        }
                    }
                    
                    setBlock(imOut, x, y, rgbBlock);
                    
                }
            }
            
            ImageIO.write(imOut, "png", new File(fileOutName));
        } catch(IOException e) {
            System.out.println("Exception occured.");
            e.printStackTrace();
        }
    }
    
    public byte[] stegExtract(String stegFileName, String origFileName) {
        int currentBitIndex = 0;
        byte currentByte = 0;
        List<Byte> bytes = new LinkedList<Byte>();
        try {
            //boolean firstPrint = true;
            
            BufferedImage imIn = ImageIO.read(new File(stegFileName));
            BufferedImage orig = ImageIO.read(getClass().getClassLoader().getResource(origFileName));
            for(int y = 0; y < imIn.getHeight(); y += 8) {
                for(int x = 0; x < imIn.getWidth(); x += 8) {
                    int[][] rgbBlock1 = getBlock(imIn, x, y);
                    int[][] rgbBlock2 = getBlock(orig, x, y);
                    int[][] yBlock1 = new int[8][8];
                    int[][] yBlock2 = new int[8][8];

                    for(int bx = 0; bx < 8; bx ++) {
                        for(int by = 0; by < 8; by ++) {
                            int yCbCr1 = YCbCrColorSpace.fromRGB(rgbBlock1[bx][by]);
                            yBlock1[bx][by] = ((yCbCr1 >> 16) & 0x0000FF) - 128;
                            int yCbCr2 = YCbCrColorSpace.fromRGB(rgbBlock2[bx][by]);
                            yBlock2[bx][by] = ((yCbCr2 >> 16) & 0x0000FF) - 128;
                        }
                    }
                    
                    // DCT
                    
                    float[][] dctCoeffBlock1 = type2DCT(yBlock1);
                    float[][] dctCoeffBlock2 = type2DCT(yBlock2);
                    //System.out.println("Orig block: ");
                    //printBlock(dctCoeffBlock2);
                    //System.out.println("Steg block: ");
                    //printBlock(dctCoeffBlock1);
                    //float test = dctCoeffBlock[0][0] * quantizationMatrix[0][0];
                    
                    int bit;
                    //float bsum = Math.abs(dctCoeffBlock1[0][0] - dctCoeffBlock2[0][0]) > .01 ? 1 : 0;
                    //bsum += Math.abs(dctCoeffBlock1[0][0] - dctCoeffBlock2[0][0]) > .01 ? 1 : 0;
                    //bsum += Math.abs(dctCoeffBlock1[0][0] - dctCoeffBlock2[0][0]) > .01 ? 1 : 0;
                    //if(bsum > 1) {
                    //    bit = 1;
                    //} else {
                    //    bit = 0;
                    //}
                    //for(int i = 0 )
                    bit = Math.abs(dctCoeffBlock1[0][0] - dctCoeffBlock2[0][0]) >= (1 - STEG_COEFF) * dctCoeffBlock2[0][0] ? 1 : 0;
                    
                    
                    currentByte = setBit(currentByte, bit, currentBitIndex);
                    
                    currentBitIndex ++;
                    if(currentBitIndex > 7) {
                        currentBitIndex = 0;
                        bytes.add(currentByte);
                        currentByte = 0;
                    }
                    
                }
            }
        } catch(IOException e) {
            System.out.println("Exception occured.");
            e.printStackTrace();
        }
        byte[] bytesArr = new byte[bytes.size()];
        for(int i = 0; i < bytesArr.length; i ++) {
            bytesArr[i] = bytes.get(i);
        }
        return bytesArr;
    }
    
    static String stripExtension (String str) {
        if (str == null) return null;
        int pos = str.lastIndexOf(".");
        if (pos == -1) return str;
        return str.substring(0, pos);
    }
    
    public void compress(String fileName) {
        try {
            BufferedImage imIn = ImageIO.read(new File(fileName));
            BufferedImage noAlpha = new BufferedImage(imIn.getWidth(), imIn.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics g = noAlpha.createGraphics();
            g.drawImage(imIn, 0, 0, imIn.getWidth(), imIn.getHeight(), null);
            g.dispose();
            ImageIO.write(noAlpha, "jpg", new File(stripExtension(fileName) + ".jpg"));
        } catch(IOException e) {
            System.out.println("Exception occured.");
            e.printStackTrace();
        }
    }
    
    
    public void run() {
        System.out.println("Hiding...");
        stegHide("Cameron Hessler 123 Test".getBytes(), "space.png", "space_out.png");
        System.out.println("Compressing...");
        compress("space_out.png");
        System.out.println("Extracting...");
        String recovered = new String(stegExtract("space_out.jpg", "space.png"));
        System.out.println(recovered);
    }
    
    public int getBit(byte b, int index) {
        if(index < 0 || index > 7) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds [0-7]");
        }
        return (b >> index) & 0x01;
    }
    
    public byte setBit(byte b, int bit, int index) {
        if(index < 0 || index > 7) {
            throw new IndexOutOfBoundsException("Index " + index + " is out of bounds [0-7]");
        }
        if(bit == 1) {
            b |= 1 << index;
        } else {
            b &= ~(1 << index);
        }
        return b;
    }
    
    /**
     * Returns 8x8 block for DCT. Fills the edge of the block by repeating edge values
     * @param xStart - top left corner of block
     * @param yStart - top left corner of block
     * @return
     */
    public int[][] getBlock(BufferedImage im, int xStart, int yStart) {
        int[][] block = new int[8][8];
        for(int x = 0; x < 8; x ++) {
            for(int y = 0; y < 8; y ++) {
                int imX = Math.min(xStart + x, im.getWidth() - 1);
                int imY = Math.min(yStart + y, im.getHeight() - 1);
                imX = Math.max(0, imX);
                imY = Math.max(0, imY);
                block[x][y] = im.getRGB(imX, imY);
            }
        }
        return block;
    }
    
        /**
     * Sets 8x8 block in image. Ignores points past image edge
     * @param xStart - top left corner of block
     * @param yStart - top left corner of block
     * @return
     */
    public void setBlock(BufferedImage im, int xStart, int yStart, int[][] block) {
        for(int x = 0; x < 8; x ++) {
            for(int y = 0; y < 8; y ++) {
                if(x + xStart < im.getWidth() && y + yStart < im.getHeight() && yStart >= 0 && xStart >= 0) {
                    im.setRGB(x + xStart, y + yStart, block[x][y]);
                }
            }
        }
    }
    
    /**
     * Performs DCT coordinate transformation on a given 8x8 block of data.
     */
    public float[][] type2DCT(int[][] block) {
        float[][] dctCoefBlock  = new float[8][8];
        
        for(int u = 0; u < 8; u ++) {
            for(int v = 0; v < 8; v ++) {
                float alphaOfU = (float) (u == 0 ? 1/Math.sqrt(2) : 1);
                float alphaOfV = (float) (v == 0 ? 1/Math.sqrt(2) : 1);
                float dctSum = 0;
                for(int x = 0; x < 8; x ++) {
                    for(int y = 0; y < 8; y ++) {
                        dctSum += block[x][y] * Math.cos(((2*x + 1) * u * Math.PI)/16) * Math.cos(((2*y + 1) * v * Math.PI)/16);
                    }
                }
                
                float value = 0.25f * alphaOfU * alphaOfV * dctSum;
                dctCoefBlock [u][v] = value;
            }
        }
        
        return dctCoefBlock ;
    }
    
    //DCT Inverse
    public float[][] type3DCT(float[][] dctBlock) {
        float[][] block = new float[8][8];
        
        for(int x = 0; x < 8; x ++) {
            for(int y = 0; y < 8; y ++) {
                float blockSum = 0;
                for(int u = 0; u < 8; u ++) {
                    for(int v = 0; v < 8; v ++) {
                        float alphaOfU = (float) (u == 0 ? 1/Math.sqrt(2) : 1);
                        float alphaOfV = (float) (v == 0 ? 1/Math.sqrt(2) : 1);
                        blockSum += alphaOfU * alphaOfV * dctBlock[u][v] * Math.cos(((2*x  + 1) * u * Math.PI)/16) * Math.cos(((2*y  + 1) * v * Math.PI)/16);
                    }
                }
                
                float value = 0.25f * blockSum;
                block[x][y] = value;
            }
        }
        
        return block;
    }
    
    public int clamp255(int val) {
        return Math.min(Math.max(val, 0), 255);
    }
    
    
    public void printBlock(float[][] dctBlock) {
        for(int v = 0; v < 8; v ++) {
            for(int u = 0; u < 8; u ++) {
                System.out.printf("%8.2f", dctBlock[u][v]);
            }
            System.out.println();
        }
    }
    
    public static void main(String[] args) {
        StegPeg stegPeg = new StegPeg();
        //stegPeg.run();

        float start1 = 128;
        float start2 = 63;
        float const1 = 16;
        float const2 = 24;
        
        for(float fact = .5f; fact < 1.5; fact += .125f) {
        	float quant1 = (float) Math.floor(start1/(float)(fact * const1));
        	float quant2 = (float) Math.floor(start2/(float)(fact * const2));
        	System.out.print(start1 + "," + start2);
        	System.out.print(" | " + (fact * const1) + "," + (fact * const2));
        	System.out.print(" | " + quant1 + "," + quant2);
        	start1 = (float) (quant1 * (fact * const1));
        	start2 = (float) (quant2 * (fact * const2));
        	System.out.println(" | " + start1 + "," + start2);
        }
        
        /*
        int[][] testDCT = new int[][] {
            {52, 63, 62, 63, 67, 79, 85, 87},
            {55, 59, 59, 58, 61, 65, 71, 79},
            {61, 55, 68, 71, 68, 60, 64, 69},
            {66, 90, 113, 122, 104, 70, 59, 68},
            {70, 109, 144, 154, 126, 77, 55, 65},
            {61, 85, 104, 106, 88, 68, 61, 76},
            {64, 69, 66, 70, 68, 58, 65, 78},
            {73, 72, 73, 69, 70, 75, 83, 94} 
        };
        
        for(int v = 0; v < 8; v ++) {
            for(int u = 0; u < 8; u ++) {
                testDCT[u][v] -= 128;
            }
        }
        
        float[][] out = stegPeg.type2DCT(testDCT);
        
        int[][] testInv = new int[8][8];
        
        for(int v = 0; v < 8; v ++) {
            for(int u = 0; u < 8; u ++) {
                testInv[u][v] = (int)Math.round(out[u][v]);
                System.out.printf("%8.2f", out[u][v]);
            }
            System.out.println();
        }
        
        float[][] out2 = stegPeg.type3DCT(out);
        
        System.out.println("Inverse");
        for(int v = 0; v < 8; v ++) {
            for(int u = 0; u < 8; u ++) {
                System.out.printf("%8.2f ", out2[u][v]);
            }
            System.out.println();
        }
        */
        
    }
}
