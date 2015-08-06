package spring.session.concurrent;

/**
 * Created by hanwen on 15-8-6.
 */
public class ConfigDataProvider {

	private String principalAttr = "principal";

	private int maximumSessions = 1;

	private String targetUrl;

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public String getPrincipalAttr() {
		return principalAttr;
	}

	public void setPrincipalAttr(String principalAttr) {
		this.principalAttr = principalAttr;
	}

	public int getMaximumSessions() {
		return maximumSessions;
	}

	public void setMaximumSessions(int maximumSessions) {
		this.maximumSessions = maximumSessions;
	}
}
