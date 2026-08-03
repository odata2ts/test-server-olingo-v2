package org.odata2ts.library.model;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.UUID;

public class Loan {
  private UUID Id;
  private Calendar LoanedAt;
  private Calendar DueDate;
  private Calendar ReturnedAt;
  private BigDecimal LateFee;
  private Integer MemberId;
  private UUID CopyMediumId;
  private Integer CopyInventoryNumber;

  public UUID getId() { return Id; }
  public void setId(UUID id) { Id = id; }
  public Calendar getLoanedAt() { return LoanedAt; }
  public void setLoanedAt(Calendar loanedAt) { LoanedAt = loanedAt; }
  public Calendar getDueDate() { return DueDate; }
  public void setDueDate(Calendar dueDate) { DueDate = dueDate; }
  public Calendar getReturnedAt() { return ReturnedAt; }
  public void setReturnedAt(Calendar returnedAt) { ReturnedAt = returnedAt; }
  public BigDecimal getLateFee() { return LateFee; }
  public void setLateFee(BigDecimal lateFee) { LateFee = lateFee; }

  public Integer getMemberId() { return MemberId; }
  public void setMemberId(Integer memberId) { MemberId = memberId; }
  public UUID getCopyMediumId() { return CopyMediumId; }
  public void setCopyMediumId(UUID copyMediumId) { CopyMediumId = copyMediumId; }
  public Integer getCopyInventoryNumber() { return CopyInventoryNumber; }
  public void setCopyInventoryNumber(Integer copyInventoryNumber) { CopyInventoryNumber = copyInventoryNumber; }
}
