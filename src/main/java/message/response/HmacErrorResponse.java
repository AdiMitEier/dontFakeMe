package message.response;

import message.Response;

public class HmacErrorResponse implements Response{
	private static final long serialVersionUID = 2541562296859923361L;

	private final String message;

	public HmacErrorResponse(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public String toString() {
		return getMessage();
	}
}
