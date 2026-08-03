package org.odata2ts.library.model;

import java.util.Calendar;
import java.util.UUID;

/**
 * Root of the media hierarchy - abstract in the EDM, and never served through an entity set of its own.
 * See {@link org.odata2ts.library.edm.LibraryEdmProvider} for why the concrete types get one each.
 */
public abstract class Medium {
  private UUID Id;
  private String Title;
  private String Language;
  private Calendar PublicationDate;
  private Double PopularityScore;

  public UUID getId() { return Id; }
  public void setId(UUID id) { Id = id; }
  public String getTitle() { return Title; }
  public void setTitle(String title) { Title = title; }
  public String getLanguage() { return Language; }
  public void setLanguage(String language) { Language = language; }
  public Calendar getPublicationDate() { return PublicationDate; }
  public void setPublicationDate(Calendar publicationDate) { PublicationDate = publicationDate; }
  public Double getPopularityScore() { return PopularityScore; }
  public void setPopularityScore(Double popularityScore) { PopularityScore = popularityScore; }
}
