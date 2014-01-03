package message.response;

import message.Response;

public class TopThreeDownloadsResponse implements Response {
	private static final long serialVersionUID = 4550680230065708876L;

	private final String message;

	public TopThreeDownloadsResponse(String message) {
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
