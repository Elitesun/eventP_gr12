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
                // On force l'IP 10.214.140.247 pour que le téléphone puisse accéder
                return "https://10.214.140.247:8181" + contextPath;
            }
        } catch (Exception e) {
            // Fallback
        }
        // Fallback avec le bon context path GlassFish fixe défini dans glassfish-web.xml
        return "https://10.214.140.247:8181/eventProjectGlsi";
    }

    public static String getScanUrl(String codeQr) {
        return getBaseUrl() + "/scan.xhtml?code=" + codeQr;
    }
}
