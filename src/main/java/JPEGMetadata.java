import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

public class JPEGMetadata {
    
    public enum JPEGFrameMarkers {
        SOI("Start of image",
                new byte[] {(byte)0xD8}),
        SOF0("Start of frame (baseline DCT)",
                new byte[] {(byte)0xC0}),
        SOF2("Start of frame (progressive DCT)",
                new byte[] {(byte)0xC2}),
        DHT("Define Huffman table(s)",
                new byte[] {(byte)0xC4}),
        DQT("Define quantization table(s)",
                new byte[] {(byte)0xDB}),
        DRI("Define restart interval",
                new byte[] {(byte)0xDD}),
        SOS("Start of scan",
                new byte[] {(byte)0xDA}),
        RST_N("Restart",
                new byte[] {
                    (byte)0xD0,
                    (byte)0xD1,
                    (byte)0xD2,
                    (byte)0xD3,
                    (byte)0xD4,
                    (byte)0xD5,
                    (byte)0xD6,
                    (byte)0xD7}),
        APP_N("Application specific",
                new byte[] {
                    (byte)0xE0,
                    (byte)0xE1,
                    (byte)0xE2,
                    (byte)0xE3,
                    (byte)0xE4,
                    (byte)0xE5,
                    (byte)0xE6,
                    (byte)0xE7,
                    (byte)0xE8,
                    (byte)0xE9,
                    (byte)0xEA,
                    (byte)0xEB,
                    (byte)0xEC,
                    (byte)0xED,
                    (byte)0xEE,
                    (byte)0xEF}),
        COM("Comment",
                new byte[] {(byte)0xFE}),
        EOI("End of image",
                new byte[] {(byte)0xD9}),
        UNKNOWN("Unknown frame",
                new byte[] {});
        
        String description;
        byte[] matches;
        
        private JPEGFrameMarkers(String description, byte[] matches) {
            this.description = description;
            this.matches = matches;
        }
        
        public static JPEGFrameMarkers find(byte b) {
            for(JPEGFrameMarkers f : values()) {
                for(byte m : f.matches) {
                    if(b == m) {
                        return f;
                    }
                }
            }
            return JPEGFrameMarkers.UNKNOWN;
        }
        
        public String getDescription() {
            return description;
        }
            
    }
    
    public String hexString(byte b) {
        return "0x" + DatatypeConverter.printHexBinary(new byte[] {b});
    }
    
    public void run() {
        try {
            File f = new File("download.jpg");
            byte[] data = Files.readAllBytes(f.toPath());
            
            boolean frameMarker = false;
            int count = 0;
            int n = -1;
            JPEGFrameMarkers currentMarker = JPEGFrameMarkers.UNKNOWN;
            
            for(int i = 0; i < data.length; i ++) {
                int ib = data[i] & 0xFF;
                if(ib == 0xFF) {
                    System.out.println();
                    System.out.println("----------END-----------");
                    frameMarker = true;
                    count = 0;
                } else if(frameMarker) {
                    currentMarker = JPEGFrameMarkers.find(data[i]);
                    System.out.println("BEGIN: " + currentMarker.getDescription() + "");
                    frameMarker = false;
                    count = 0;
                    if(currentMarker == JPEGFrameMarkers.APP_N) {
                        n = ib & 0x0F;
                    }
                } else {
                    if(currentMarker == JPEGFrameMarkers.APP_N) {
                        int j = i + 1;
                        List<Byte> bytes = new ArrayList<Byte>();
                        for(; j < data.length; j ++) {
                            int jb = data[j] & 0xFF;
                            if(jb == 0x00) {
                                break;
                            } else {
                                bytes.add(data[j]);
                            }
                        }
                        byte[] byteArr = new byte[bytes.size()];
                        for(int x = 0; x < byteArr.length; x ++) {
                            byteArr[x] = bytes.get(x);
                        }
                        System.out.println(new String(byteArr));
                        i = j + 3;
                    } else {
                        System.out.print(hexString(data[i]) + " ");
                        count ++;
                        if(count > 15) {
                            count = 0;
                            System.out.println();
                        }
                    }
                }
            }
            
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(JPEGMetadata.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(JPEGMetadata.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /** 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new JPEGMetadata().run();
    }
    
}


