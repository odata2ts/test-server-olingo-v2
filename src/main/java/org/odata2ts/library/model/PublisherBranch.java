package org.odata2ts.library.model;

/** PublisherRegistry.Branch - deliberately the same type name as Library.Circulation.Branch. */
public class PublisherBranch {
  private Integer Id;
  private String City;
  private String Country;

  public Integer getId() { return Id; }
  public void setId(Integer id) { Id = id; }
  public String getCity() { return City; }
  public void setCity(String city) { City = city; }
  public String getCountry() { return Country; }
  public void setCountry(String country) { Country = country; }
}
