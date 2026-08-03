package org.odata2ts.library.model;

import java.util.Calendar;

public class LoanStats {
  private Long TotalLoans;
  private Calendar AverageLoanDuration;

  public LoanStats() {}

  public LoanStats(Long total, Calendar avg) {
    TotalLoans = total;
    AverageLoanDuration = avg;
  }

  public Long getTotalLoans() { return TotalLoans; }
  public void setTotalLoans(Long v) { TotalLoans = v; }
  public Calendar getAverageLoanDuration() { return AverageLoanDuration; }
  public void setAverageLoanDuration(Calendar v) { AverageLoanDuration = v; }
}
