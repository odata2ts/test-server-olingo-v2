package org.odata2ts.library.model;

import java.util.Calendar;
import java.util.UUID;

/** Composite key (MediumId + InventoryNumber), and the only entity with a concurrency token. */
public class Copy {
  private UUID MediumId;
  private Integer InventoryNumber;
  private Short Condition;
  private Boolean IsLoanable;
  private Short Status;
  private Calendar AcquisitionDate;
  private Float WeightKg;
  private String ShelfCode;
  private Integer LocationId;

  public UUID getMediumId() { return MediumId; }
  public void setMediumId(UUID mediumId) { MediumId = mediumId; }
  public Integer getInventoryNumber() { return InventoryNumber; }
  public void setInventoryNumber(Integer inventoryNumber) { InventoryNumber = inventoryNumber; }
  public Short getCondition() { return Condition; }
  public void setCondition(Short condition) { Condition = condition; }
  public Boolean getIsLoanable() { return IsLoanable; }
  public void setIsLoanable(Boolean isLoanable) { IsLoanable = isLoanable; }
  public Short getStatus() { return Status; }
  public void setStatus(Short status) { Status = status; }
  public Calendar getAcquisitionDate() { return AcquisitionDate; }
  public void setAcquisitionDate(Calendar acquisitionDate) { AcquisitionDate = acquisitionDate; }
  public Float getWeightKg() { return WeightKg; }
  public void setWeightKg(Float weightKg) { WeightKg = weightKg; }

  /**
   * The EDM property is named `Location_` - a trailing underscore that collides with the navigation
   * property `Location` under a client renaming strategy (odata2ts#142). The EDM mapping points at
   * this getter, so the Java side keeps a readable name.
   */
  public String getShelfCode() { return ShelfCode; }
  public void setShelfCode(String shelfCode) { ShelfCode = shelfCode; }

  /** Not an EDM property: resolves the Copy_Location association. */
  public Integer getLocationId() { return LocationId; }
  public void setLocationId(Integer locationId) { LocationId = locationId; }
}
