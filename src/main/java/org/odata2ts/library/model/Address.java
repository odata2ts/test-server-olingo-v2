package org.odata2ts.library.model;

/** Abstract complex type - V2 allows both Abstract and BaseType on complex types. */
public abstract class Address {
  private String Street;
  private String City;

  public String getStreet() { return Street; }
  public void setStreet(String street) { Street = street; }
  public String getCity() { return City; }
  public void setCity(String city) { City = city; }
}
