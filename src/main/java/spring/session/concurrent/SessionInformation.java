package spring.session.concurrent;

import org.springframework.util.Assert;

import java.io.Serializable;

/**
 * reference spring security#SessionInformation.
 * Created by hanwen on 15-7-30.
 */
public class SessionInformation implements Serializable {

	static final String PRINCIPAL_ATTR = "principal";

	static final String EXPIRED_ATTR = "expired";

	static final String LAST_REQUEST_ATTR = "lastRequest";

	static final String SESSION_ID_ATTR = "sessionId";

  private Long lastRequest;

  private String principal;

	private String sessionId;

	private boolean expired = false;

  public SessionInformation(String principal, String sessionId, Long lastRequest) {
    Assert.notNull(principal, "Principal required");
    Assert.hasText(sessionId, "SessionId required");
    Assert.notNull(lastRequest, "LastRequest required");
    this.principal = principal;
    this.sessionId = sessionId;
    this.lastRequest = lastRequest;
  }

	public SessionInformation(String principal, String sessionId, Long lastRequest, boolean expired) {
		Assert.notNull(principal, "Principal required");
		Assert.hasText(sessionId, "SessionId required");
		Assert.notNull(lastRequest, "LastRequest required");
		this.principal = principal;
		this.sessionId = sessionId;
		this.lastRequest = lastRequest;
		this.expired = expired;
	}

  public Long getLastRequest() {
      return lastRequest;
  }

  public String getPrincipal() {
      return principal;
  }

  public String getSessionId() {
      return sessionId;
  }

  public boolean isExpired() {
      return expired;
  }
}
