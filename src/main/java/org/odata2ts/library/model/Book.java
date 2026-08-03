package org.odata2ts.library.model;

public class Book extends PrintMedium {
  private Short PageCount;
  private Short AgeRating;
  private Integer PublisherId;

  public Short getPageCount() { return PageCount; }
  public void setPageCount(Short pageCount) { PageCount = pageCount; }
  public Short getAgeRating() { return AgeRating; }
  public void setAgeRating(Short ageRating) { AgeRating = ageRating; }

  /** Not an EDM property: the link to PublisherRegistry.Publisher, resolved by the data source. */
  public Integer getPublisherId() { return PublisherId; }
  public void setPublisherId(Integer publisherId) { PublisherId = publisherId; }
}
