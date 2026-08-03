package org.odata2ts.library.model;

/** Fourth level of the hierarchy: TradeJournal -> Magazine -> PrintMedium -> Medium. */
public class TradeJournal extends Magazine {
  private String Field;

  public String getField() { return Field; }
  public void setField(String field) { Field = field; }
}
