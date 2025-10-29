package server;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class SessionCleanupListener implements HttpSessionListener {

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        System.out.println("[SessionCleanupListener] Session created: " + se.getSession().getId());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        String id = se.getSession().getId();
        System.out.println("[SessionCleanupListener] Session destroyed: " + id);

        EngineSessionManager.clearEngine(se.getSession());
        ServerEventManager.removeSession(se.getSession());
        SessionUserManager.clearUser(se.getSession());
    }
}
