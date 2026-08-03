package org.odata2ts.library.model;

import java.util.Calendar;
import java.util.UUID;

/** Carries Edm.Binary as an ordinary property - not a media link entry. */
public class IdDocument {
  private UUID Id;
  private byte[] Scan;
  private Calendar UploadedAt;

  public UUID getId() { return Id; }
  public void setId(UUID id) { Id = id; }
  public byte[] getScan() { return Scan; }
  public void setScan(byte[] scan) { Scan = scan; }
  public Calendar getUploadedAt() { return UploadedAt; }
  public void setUploadedAt(Calendar uploadedAt) { UploadedAt = uploadedAt; }
}
