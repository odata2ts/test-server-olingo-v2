package org.odata2ts.library.model;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.UUID;

public class Member {
  private Integer Id;
  private String Name;
  private Calendar DateOfBirth;
  private PostalAddress Address;
  private Calendar ActiveSince;
  private BigDecimal Balance;
  private UUID IdDocumentId;

  public Integer getId() { return Id; }
  public void setId(Integer id) { Id = id; }
  public String getName() { return Name; }
  public void setName(String name) { Name = name; }
  public Calendar getDateOfBirth() { return DateOfBirth; }
  public void setDateOfBirth(Calendar dateOfBirth) { DateOfBirth = dateOfBirth; }
  public PostalAddress getAddress() { return Address; }
  public void setAddress(PostalAddress address) { Address = address; }
  public Calendar getActiveSince() { return ActiveSince; }
  public void setActiveSince(Calendar activeSince) { ActiveSince = activeSince; }
  public BigDecimal getBalance() { return Balance; }
  public void setBalance(BigDecimal balance) { Balance = balance; }

  /** Not an EDM property: resolves the Member_IdDocument association. */
  public UUID getIdDocumentId() { return IdDocumentId; }
  public void setIdDocumentId(UUID idDocumentId) { IdDocumentId = idDocumentId; }
}
