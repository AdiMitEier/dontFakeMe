package message.request;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

import message.Request;

/**
 * Retrieves the highest available version number of a particular file on a certain server.
 * <p/>
 * <b>Request</b>:<br/>
 * {@code !version &lt;filename&gt;}<br/>
 * <b>Response:</b><br/>
 * {@code !version &lt;filename&gt; &lt;version&gt;}<br/>
 *
 * @see message.response.VersionResponse
 */
public class VersionRequest implements Request {
	private static final long serialVersionUID = 3995314039957433479L;

	private final String filename;
	private byte[] base64hash;

	public VersionRequest(String filename) {
		this.filename = filename;
	}

	public String getFilename() {
		return filename;
	}
	
	//STAGE3
	public void setHmac(Key key){
		Mac hmac = null;
		try {
			hmac = Mac.getInstance("HmacSHA256");
		} catch (NoSuchAlgorithmException e) {
			// TODO vernuenftiges handling
			e.printStackTrace();
		}
		try {
			hmac.init(key);
		} catch (InvalidKeyException e) {
			// TODO vernuenftiges handling
			e.printStackTrace();
		}
		byte[] message = this.toString().getBytes();
		hmac.update(message);
		byte[] hash = hmac.doFinal();
		base64hash = Base64.encode(hash);
	}
	
	//STAGE3
	public byte[] getKey(){
		return this.base64hash;
	}
	
	@Override
	public String toString() {
		return "!version " + getFilename();
	}
}
