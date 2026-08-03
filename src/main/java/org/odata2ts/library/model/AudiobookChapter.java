package org.odata2ts.library.model;

import java.util.UUID;

/** The second media link entry, and the only entity type outside the hierarchy that carries one. */
public class AudiobookChapter {
  private Integer Id;
  private String Title;
  private UUID AudiobookId;
  private byte[] content = new byte[0];
  private String contentType = "audio/mpeg";

  public Integer getId() { return Id; }
  public void setId(Integer id) { Id = id; }
  public String getTitle() { return Title; }
  public void setTitle(String title) { Title = title; }

  /** Not an EDM property: the backlink used by the data source to resolve the association. */
  public UUID getAudiobookId() { return AudiobookId; }
  public void setAudiobookId(UUID audiobookId) { AudiobookId = audiobookId; }

  public byte[] getContent() { return content; }
  public void setContent(byte[] value) { content = value; }
  public String getContentType() { return contentType; }
  public void setContentType(String value) { contentType = value; }
}
