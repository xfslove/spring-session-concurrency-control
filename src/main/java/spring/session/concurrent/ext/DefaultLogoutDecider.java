package spring.session.concurrent.ext;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hanwen on 15-9-24.
 */
public class DefaultLogoutDecider implements LogoutDecider {

  @Override
  public void doLogout(HttpServletRequest request) {
    request.getSession().invalidate();
  }

}
