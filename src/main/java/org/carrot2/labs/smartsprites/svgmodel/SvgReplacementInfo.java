package org.carrot2.labs.smartsprites.svgmodel;

import org.carrot2.labs.smartsprites.message.Message.MessageType;

public class SvgReplacementInfo {

	private String findWhat;
	private String replaceWith;
	private boolean importantTagPresent;

	private String sourceSvgFile;
	private String sourceCssFile;

	public SvgReplacementInfo(String findWhat, String replaceWith, boolean importantTagPresent,
		String sourceSvgFile, String sourceCssFile) {
		this.findWhat = findWhat;
		this.replaceWith = replaceWith;
		this.importantTagPresent = importantTagPresent;
		this.sourceSvgFile = sourceSvgFile;
		this.sourceCssFile = sourceCssFile;
	}

	public String getFindWhat() {
		return findWhat;
	}

	public void setFindWhat(String findWhat) {
		this.findWhat = findWhat;
	}

	public String getReplaceWith() {
		return replaceWith;
	}

	public void setReplaceWith(String replaceWith) {
		this.replaceWith = replaceWith;
	}

	public String getSourceSvgFile() {
		return sourceSvgFile;
	}

	public void setSourceSvgFile(String sourceSvgFile) {
		this.sourceSvgFile = sourceSvgFile;
	}

	public boolean isImportantTagPresent() {
		return importantTagPresent;
	}

	public void setImportantTagPresent(boolean importantTagPresent) {
		this.importantTagPresent = importantTagPresent;
	}

	public String getSourceCssFile() {
		return sourceCssFile;
	}

	public void setSourceCssFile(String sourceCssFile) {
		this.sourceCssFile = sourceCssFile;
	}

	@Override
	public String toString() {
		return String.format("{ findWhat: %s, replaceWith: %s, sourceSvgFile: %s, sourceCssFile: %s", findWhat, replaceWith, sourceSvgFile, sourceCssFile);
	}
}