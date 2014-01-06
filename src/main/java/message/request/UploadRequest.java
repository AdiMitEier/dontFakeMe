package message.request;

import message.Request;

import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

/**
 * Uploads the file with the given name.
 * <p/>
 * <b>Request</b>:<br/>
 * {@code !upload &lt;filename&gt; &lt;content&gt;}<br/>
 * <b>Response:</b><br/>
 * {@code !upload &lt;message&gt;}<br/>
 */
public class UploadRequest implements Request {
	private static final long serialVersionUID = 6951706197428053894L;
	private static final Charset CHARSET = Charset.forName("ISO-8859-1");

	private final String filename;
	private int version;
	private final byte[] content;
	private byte[] base64hash;

	public UploadRequest(String filename, int version, byte[] content) {
		this.filename = filename;
		this.version = version;
		this.content = content;
	}

	public String getFilename() {
		return filename;
	}

	public int getVersion() {
		return version;
	}
	
	public void setVersion(int versionNumber){
		this.version = versionNumber;
	}

	public byte[] getContent() {
		return content;
	}
	
	//STAGE3
	public void setHmac(Key key){
		Mac hmac = null;
		try {
			hmac = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			System.err.println("NoSuchAlgorithmException (HmacSHA256 not valid): " + e.getMessage());
		}
		try {
			hmac.init(key);
		} catch (InvalidKeyException e) {
			System.err.println("InvalidKeyException: " + e.getMessage());
		}
		byte[] message = this.toString().getBytes();
		if(hmac!=null){
			hmac.update(message);
			byte[] hash = hmac.doFinal();
			base64hash = Base64.encode(hash);
		}
	}
	
	//STAGE3
	public byte[] getKey(){
		return this.base64hash;
	}

	@Override
	public String toString() {
		return String.format("!upload %s %d %s", getFilename(), getVersion(), new String(getContent(), CHARSET));
	}
}
