package mikex;

public interface MikexControlMBean {
  public void cancelAll();
  public void disconnectAll();
  public void marketModeContinuous();
  public void marketModeContinuousNonCxl();
  public void marketModeClosed();
  public void marketModeAuction();
  public void marketModeAuctionNonCxl();
  public void ackInhibit();
  public void ackGenerate();
  public void autoFillDisable();
  public void autoFillEnable();
  public String getName();
  public String getMarketStatus();
  public void setConnectionResponseDelay(int newResponseDelay);
  int getConnectionResponseDelay();
}
