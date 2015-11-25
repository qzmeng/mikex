package mikex;

public interface MikexControlMBean {
  public void cancelAll();
  public void disconnectAll();
  public void marketModeContinuous();
  public void marketModeClosed();
  public void marketModeAuction();
  public String getName();
  public String getMarketStatus();
  public void setConnectionResponseDelay(int newResponseDelay);
  int getConnectionResponseDelay();
}
