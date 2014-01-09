package secure;

import java.io.IOException;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import message.Request;
import message.Response;
import message.request.LoginRequest;

import org.bouncycastle.util.encoders.Hex;

public class AESChannel extends Base64Channel{

	private byte[] ivparam;
	private Key secretkey;
	private Cipher encrypt;
	private Cipher decrypt;
	
	public AESChannel(IChannel base64Channel) {
		super(base64Channel);
	}
	public void setIvparam(byte[] ivparam) {
		this.ivparam = ivparam;
	}
	public void setSecretkey(byte[] secretkey) {
		//this.secretkey = secretkey;
		
		/*byte[] keyBytes = new byte[1024];
		String pathToSecretKey = ...
		FileInputStream fis = new FileInputStream(pathToSecretKey);
		fis.read(keyBytes);
		fis.close();*/
		//byte[] input = Hex.decode(secretkey);
		// make sure to use the right ALGORITHM for what you want to do 
		// (see text) 
		Key key = new SecretKeySpec(secretkey,"AES");
		this.secretkey=key;
		//System.out.println("SECRET KEY AES"+key.toString());
	}
	public void initencryptChipher() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException{
		//"AES/CTR/NoPadding"
		
		this.encrypt = Cipher.getInstance("AES/CTR/NoPadding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		//SecureRandom random = new SecureRandom();
		//random.g
		encrypt.init(Cipher.ENCRYPT_MODE,this.secretkey,new IvParameterSpec(ivparam));
		//encrypt.init(Cipher.ENCRYPT_MODE,this.secretkey,ivparam);
	}
	public void initdecryptChipher() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException{
		this.decrypt = Cipher.getInstance("AES/CTR/NoPadding"); 
		// MODE is the encryption/decryption mode 
		// KEY is either a private, public or secret key 
		// IV is an init vector, needed for AES 
		//System.out.println(publickey);

		decrypt.init(Cipher.DECRYPT_MODE,this.secretkey,new IvParameterSpec((ivparam)));
	}
	//CLIENT
	public void sendMessageRequest(Request message) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException{
		if(message instanceof LoginRequest){
			//((LoginRequest)message).setChallenge(this.encodeBase64(((LoginRequest)message).getChallenge()));
		}else{
			
		}
		byte[]array = this.toByteArray(message);
		this.initencryptChipher();
		byte[] aes = encrypt.doFinal(array);
		this.sendByteArray(aes);
	}
	public Response receiveMessageResponse() throws IOException, ClassNotFoundException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException{
		byte[]rec =this.receiveByteArray();
		this.initdecryptChipher();
		byte[]aes=this.decrypt.doFinal(rec);
		Response response = (Response)this.byteArraytoObject(aes);
		
		return response;
	}
	//PROXY
	public void sendMessageResponse(Response message) throws IOException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
		byte[]array = this.toByteArray(message);
		this.initencryptChipher();
		byte[] aes = encrypt.doFinal(array);
		this.sendByteArray(aes);
	}
	public Request receiveMessageRequest() throws ClassNotFoundException, IOException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException{
		byte[]rec =this.receiveByteArray();
		this.initdecryptChipher();
		byte[]aes=this.decrypt.doFinal(rec);
		Request request = (Request)this.byteArraytoObject(aes);
		if(request instanceof LoginRequest){
			//((LoginRequest) request).setChallenge(this.decodeBase64(((LoginRequest) request).getChallenge()));
		}
		return request;
	}
	
//TODO ENCRYPT DECRYPT
	
}
