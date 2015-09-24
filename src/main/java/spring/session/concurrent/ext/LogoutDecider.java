package spring.session.concurrent.ext;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by hanwen on 15-9-24.
 */
public interface LogoutDecider {

  void doLogout(HttpServletRequest request);

}
