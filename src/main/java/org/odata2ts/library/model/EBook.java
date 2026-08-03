package org.odata2ts.library.model;

/** Media link entry inside the inheritance hierarchy: its content is served from .../$value. */
public class EBook extends Medium {
  private String FileFormat;
  private byte[] content = new byte[0];
  private String contentType = "application/octet-stream";

  public String getFileFormat() { return FileFormat; }
  public void setFileFormat(String fileFormat) { FileFormat = fileFormat; }

  public byte[] getContent() { return content; }
  public void setContent(byte[] value) { content = value; }
  public String getContentType() { return contentType; }
  public void setContentType(String value) { contentType = value; }
}
