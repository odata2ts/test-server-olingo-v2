package org.odata2ts.library.model;

import java.util.Calendar;

public abstract class AudioMedium extends Medium {
  private Calendar Duration;

  public Calendar getDuration() { return Duration; }
  public void setDuration(Calendar duration) { Duration = duration; }
}
