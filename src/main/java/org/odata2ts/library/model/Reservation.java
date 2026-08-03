package org.odata2ts.library.model;

import java.util.Calendar;
import java.util.UUID;

public class Reservation {
  private UUID Id;
  private Calendar ReservedAt;
  private Integer MemberId;

  public UUID getId() { return Id; }
  public void setId(UUID id) { Id = id; }
  public Calendar getReservedAt() { return ReservedAt; }
  public void setReservedAt(Calendar reservedAt) { ReservedAt = reservedAt; }
  public Integer getMemberId() { return MemberId; }
  public void setMemberId(Integer memberId) { MemberId = memberId; }
}
