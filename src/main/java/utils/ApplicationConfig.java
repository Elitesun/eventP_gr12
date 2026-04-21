package utils;

/**
 * Configuration globale de l'application
 */
public class ApplicationConfig {

    public static String getBaseUrl() {
        try {
            jakarta.faces.context.FacesContext context = jakarta.faces.context.FacesContext.getCurrentInstance();
            if (context != null) {
                // Récupère l'IP/host et le contextPath dynamiquement depuis la requête
                jakarta.servlet.http.HttpServletRequest request = (jakarta.servlet.http.HttpServletRequest) context
                        .getExternalContext().getRequest();
                String contextPath = request.getContextPath();
                String scheme = request.getScheme();
                String host = request.getServerName();
                int port = request.getServerPort();
                return scheme + "://" + host + ":" + port + contextPath;
            }
        } catch (Exception e) {
            // Fallback
        }
        // Fallback local Docker
        return "http://localhost:8080/eventProjectGlsi";
    }

    public static String getScanUrl(String codeQr) {
        return getBaseUrl() + "/scan.xhtml?code=" + codeQr;
    }
}
