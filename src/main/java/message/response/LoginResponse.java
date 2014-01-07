package message.response;

import java.security.Key;

import message.Response;

/**
 * Authenticates the client with the provided username and password.
 * <p/>
 * <b>Request</b>:<br/>
 * {@code !login &lt;username&gt; &lt;password&gt;}<br/>
 * <b>Response:</b><br/>
 * {@code !login success}<br/>
 * or<br/>
 * {@code !login wrong_credentials}
 *
 * @see message.request.LoginRequest
 */
public class LoginResponse implements Response {
	private static final long serialVersionUID = 3134831924072300109L;
	private byte[] clientchallenge;
	private byte[] proxychallenge;
	private byte[] secretkey;
	private byte[] ivparameter;
	
	public enum Type {
		SUCCESS("Successfully logged in."),
		WRONG_CREDENTIALS("Wrong username or password."),
		OK("!OK");
		String message;

		Type(String message) {
			this.message = message;
		}
	}

	private final Type type;

	public LoginResponse(Type type) {
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	@Override
	public String toString() {
		return "!login " + getType().name().toLowerCase();
	}

	public void setClientchallenge(byte[] clientchallenge) {
		this.clientchallenge = clientchallenge;
	}

	public byte[] getClientchallenge() {
		return clientchallenge;
	}

	public void setProxychallenge(byte[] proxychallenge) {
		this.proxychallenge = proxychallenge;
	}

	public byte[] getProxychallenge() {
		return proxychallenge;
	}

	public void setSecretkey(byte[] secretkey) {
		this.secretkey = secretkey;
	}

	public byte[] getSecretkey() {
		return secretkey;
	}

	public void setIvparameter(byte[] ivparameter) {
		this.ivparameter = ivparameter;
	}

	public byte[] getIvparameter() {
		return ivparameter;
	}
}
