package org.odata2ts.library.model;

import java.math.BigDecimal;
import java.util.Calendar;

public class OverdueNotice {
  private String Reason;
  private BigDecimal Amount;
  private Calendar CreatedAt;

  public OverdueNotice() {}

  public OverdueNotice(String reason, BigDecimal amount, Calendar createdAt) {
    Reason = reason;
    Amount = amount;
    CreatedAt = createdAt;
  }

  public String getReason() { return Reason; }
  public void setReason(String v) { Reason = v; }
  public BigDecimal getAmount() { return Amount; }
  public void setAmount(BigDecimal v) { Amount = v; }
  public Calendar getCreatedAt() { return CreatedAt; }
  public void setCreatedAt(Calendar v) { CreatedAt = v; }
}
