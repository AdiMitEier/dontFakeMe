package message.request;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;

import org.bouncycastle.util.encoders.Base64;

import message.Request;

/**
 * Lists all files available on all file servers.
 * <p/>
 * <b>Request</b>:<br/>
 * {@code !list}<br/>
 * <b>Response:</b><br/>
 * {@code No files found.}<br/>
 * or<br/>
 * {@code &lt;filename1&gt;}<br/>
 * {@code &lt;filename2&gt;}<br/>
 * {@code ...}<br/>
 *
 * @see message.response.ListResponse
 */
public class ListRequest implements Request {
	private static final long serialVersionUID = -3772629665574053670L;
	byte[] base64hash;
	
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
		return "!list";
	}
}
