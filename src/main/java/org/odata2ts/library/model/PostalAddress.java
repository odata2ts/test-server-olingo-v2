package org.odata2ts.library.model;

public class PostalAddress extends Address {
  private String PostalCode;
  private String Country;

  public PostalAddress() {}

  public PostalAddress(String street, String city, String postalCode, String country) {
    setStreet(street);
    setCity(city);
    PostalCode = postalCode;
    Country = country;
  }

  public String getPostalCode() { return PostalCode; }
  public void setPostalCode(String postalCode) { PostalCode = postalCode; }
  public String getCountry() { return Country; }
  public void setCountry(String country) { Country = country; }
}
