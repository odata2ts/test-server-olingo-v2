package org.odata2ts.library.model;

import java.util.Calendar;

public class MediumStats {
  private Long TotalLoanCount;
  private Calendar AverageLoanDuration;

  public MediumStats() {}

  public MediumStats(Long total, Calendar avg) {
    TotalLoanCount = total;
    AverageLoanDuration = avg;
  }

  public Long getTotalLoanCount() { return TotalLoanCount; }
  public void setTotalLoanCount(Long v) { TotalLoanCount = v; }
  public Calendar getAverageLoanDuration() { return AverageLoanDuration; }
  public void setAverageLoanDuration(Calendar v) { AverageLoanDuration = v; }
}
