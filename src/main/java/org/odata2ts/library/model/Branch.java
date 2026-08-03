package org.odata2ts.library.model;

import java.util.Calendar;

public class Branch {
  private Integer Id;
  private String Name;
  private PostalAddress Address;
  private Byte LowestFloor;
  private Calendar OpensAt;
  private Calendar ClosesAt;
  private Integer Amenities;
  private Long Population;

  public Integer getId() { return Id; }
  public void setId(Integer id) { Id = id; }
  public String getName() { return Name; }
  public void setName(String name) { Name = name; }
  public PostalAddress getAddress() { return Address; }
  public void setAddress(PostalAddress address) { Address = address; }
  public Byte getLowestFloor() { return LowestFloor; }
  public void setLowestFloor(Byte lowestFloor) { LowestFloor = lowestFloor; }
  public Calendar getOpensAt() { return OpensAt; }
  public void setOpensAt(Calendar opensAt) { OpensAt = opensAt; }
  public Calendar getClosesAt() { return ClosesAt; }
  public void setClosesAt(Calendar closesAt) { ClosesAt = closesAt; }
  public Integer getAmenities() { return Amenities; }
  public void setAmenities(Integer amenities) { Amenities = amenities; }
  public Long getPopulation() { return Population; }
  public void setPopulation(Long population) { Population = population; }
}
