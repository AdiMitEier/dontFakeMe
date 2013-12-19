package message.response;

import message.Response;
import model.FileModel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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
 * @see message.request.ListRequest
 */
public class ListResponse implements Response {
	private static final long serialVersionUID = -7319020129445822795L;

	private final Set<FileModel> fileNames; //STAGE 1 changed generic from String to FileModel

	public ListResponse(Set<FileModel> fileNames) {
		this.fileNames = Collections.unmodifiableSet(new LinkedHashSet<FileModel>(fileNames));
	}

	public Set<FileModel> getFileNames() {
		return fileNames;
	}

	@Override
	public String toString() {
		if (getFileNames().isEmpty()) {
			return "No files found.";
		}

		StringBuilder sb = new StringBuilder();
		for (FileModel fileName : getFileNames()) {
			sb.append(fileName).append("\n");
		}
		return sb.toString();
	}
}
