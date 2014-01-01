package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;

public class FileUtils {
	public static byte[] readFile(String dir, String filename)
	{
		FileInputStream fileInputStream=null;
		 
        File file = new File(dir+"/"+filename);
 
        byte[] bFile = new byte[(int) file.length()];
 
        try {
        	fileInputStream = new FileInputStream(file);
        	fileInputStream.read(bFile);
        	fileInputStream.close();
	    
	    	return bFile;
        }catch(IOException e){
        	return null;
        }
	}
	
	public static boolean writeFile(String dir, String filename, byte[] bFile)
	{
		
		FileOutputStream fileOuputStream = null;
 
        try {
        	fileOuputStream = new FileOutputStream(dir+"/"+filename); 
        	fileOuputStream.write(bFile);
        	fileOuputStream.close();
	    
	    	return true;
        }catch(IOException e){
        	return false;
        }
	}
	
	public static Key readKeyFromFile(String dir){
		FileInputStream fileInputStream=null;
		 
        File file = new File(dir);
 
        byte[] keyBytes = new byte[(int) file.length()];
 
        try {
        	fileInputStream = new FileInputStream(file);
        	fileInputStream.read(keyBytes);
        	fileInputStream.close();
        	
        	byte[] input = Hex.decode(keyBytes);
        	
	    	return new SecretKeySpec(input,"HmacSHA256");
        }catch(IOException e){
        	return null;
        }
	}
}
