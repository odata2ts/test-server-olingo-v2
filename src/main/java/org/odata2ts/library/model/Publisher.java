package org.odata2ts.library.model;

import java.util.Calendar;

public class Publisher {
  private Integer Id;
  private String Name;
  private String Country;
  private Calendar Founded;

  public Integer getId() { return Id; }
  public void setId(Integer id) { Id = id; }
  public String getName() { return Name; }
  public void setName(String name) { Name = name; }
  public String getCountry() { return Country; }
  public void setCountry(String country) { Country = country; }
  public Calendar getFounded() { return Founded; }
  public void setFounded(Calendar founded) { Founded = founded; }
}
