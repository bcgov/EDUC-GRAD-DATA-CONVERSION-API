package ca.bc.gov.educ.api.dataconversion.controller;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.With;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

import static java.util.Optional.ofNullable;

@CrossOrigin
@RestController
@RequestMapping("/api/v1")
@OpenAPIDefinition(
        info = @Info(title = "API for Metadata", description = "API for Metadata", version = "1"),
        security = {@SecurityRequirement(name = "OAUTH2",
                scopes = {})})
class APIMetadataController {
    private final ApplicationContext context;

    @Autowired
    public APIMetadataController(ApplicationContext context) {
        this.context = context;
    }

    @GetMapping("/metadata")
    @Operation(summary = "API Metadata", description = "API Metadata", tags = {"Metadata"})
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    String generateMetadata() {
        final var controllers = new ArrayList<ControllerInfo>();
        for (String controllerName : context.getBeanNamesForAnnotation(RestController.class)) {
            if (StringUtils.equalsAnyIgnoreCase(controllerName, "OpenApiResource",
                    "SwaggerConfigResource", this.getClass().getSimpleName()))
                continue;
            final var controllerBean = context.getBean(controllerName);
            final var baseApiPath = getApiPath(
                    AnnotationUtils.findAnnotation(controllerBean.getClass(), RequestMapping.class));
            final var controllerSecurityInfo = new ControllerInfo(StringUtils.capitalize(controllerName), new ArrayList<>());
            for (Method method : controllerBean.getClass().getMethods()) {
                getMethodInfo(method)
                        .map(m -> m.withPrefixedApiPath(baseApiPath))
                        .ifPresent(m -> controllerSecurityInfo.methods().add(m));
            }
            controllers.add(controllerSecurityInfo);
        }
        String htmlTemplate = """
                <html>
                  <head>
                      <meta charset="UTF8">
                      <style>
                          body {font-family: monospace,'lato',sans-serif; font-size: 15px; color: #3b4151;} 
                          .container {max-width: 100%; margin-left: auto; margin-right: auto; padding-left: 10px; padding-right: 10px;} 
                          h2 {font-family: sans-serif; font-size: 24px; font-weight: bold;color: #036; margin: 10px 20px; small{font-size: 0.5em;}} 
                          .responsive-table { 
                              li {border-radius: 3px; padding: 10px 10px; display: flex; margin-bottom: 7px;} 
                              .table-header {background-color: #4f87dd; color: #ffffff; font-size: 18px; font-weight: bolder; text-transform: uppercase; letter-spacing: 0.03em; text-align: center;} 
                              .table-row-get {background: rgba(97,175,254,.1); border: 1px solid #61affe; border-radius: 4px; box-shadow: 0 0 3px rgba(0,0,0,.19);}
                              .table-row-post {background: rgba(73,204,144,.1); border: 1px solid #49cc90; border-radius: 4px; box-shadow: 0 0 3px rgba(0,0,0,.19);}
                              .table-row-put {background: rgba(252,161,48,.1); border: 1px solid #fca130; border-radius: 4px; box-shadow: 0 0 3px rgba(0,0,0,.19);}
                              .table-row-delete {background: rgba(249,62,62,.1); border: 1px solid #f93e3e; border-radius: 4px; box-shadow: 0 0 3px rgba(0,0,0,.19);}
                              .col-1-get {width: 80px; height: 15px;background: #61affe; border-radius: 3px; color: #fff; font-family: sans-serif; font-size: 14px;font-weight: 700;min-width: 80px; padding: 5px 0; text-align: center; text-shadow: 0 1px 0 rgba(0,0,0,.1);}
                              .col-1-post {width: 80px; height: 15px;background: #49cc90; border-radius: 3px; color: #fff; font-family: sans-serif; font-size: 14px;font-weight: 700;min-width: 80px; padding: 5px 0; text-align: center; text-shadow: 0 1px 0 rgba(0,0,0,.1);}
                              .col-1-put {width: 80px; height: 15px;background: #fca130; border-radius: 3px; color: #fff; font-family: sans-serif; font-size: 14px;font-weight: 700;min-width: 80px; padding: 5px 0; text-align: center; text-shadow: 0 1px 0 rgba(0,0,0,.1);}
                              .col-1-delete {width: 80px; height: 15px; background: #f93e3e; border-radius: 3px; color: #fff; font-family: sans-serif; font-size: 14px; font-weight: 700; min-width: 80px; padding: 5px 0; text-align: center; text-shadow: 0 1px 0 rgba(0,0,0,.1);}
                              .col { padding-left: 10px; font-weight: 700; overflow-wrap: break-word; }
                              .col-2 { width: 52%; }
                              .col-3 { width: 25%; }
                              .col-4 { width: 18%; }
                            }
                      </style>
                      <link href='https://fonts.googleapis.com/css?family=JetBrains Mono' rel='stylesheet'>
                  </head>
                  <body>
                      {EndpointDetailsHTML}
                      {DownstreamEndpointsHTML}
                  </body>
                </html>
                  """;
        return htmlTemplate.replace("{EndpointDetailsHTML}", getEndpointDetailsHTML(controllers))
                .replace("{DownstreamEndpointsHTML}", getDownstreamEndpointsHTML());
    }

    @With
    private record ControllerInfo(String name, List<MethodInfo> methods) {
    }

    @With
    private record MethodInfo(String httpMethod, String apiPath, String security, String functionName) {
        public MethodInfo withPrefixedApiPath(String prefixedApiPath) {
            return withApiPath(prefixedApiPath + this.apiPath);
        }
    }

    private static Optional<MethodInfo> getMethodInfo(Method method) {
        return Optional.<Annotation>ofNullable(AnnotationUtils.findAnnotation(method, GetMapping.class))
                .or(() -> ofNullable(AnnotationUtils.findAnnotation(method, PostMapping.class)))
                .or(() -> ofNullable(AnnotationUtils.findAnnotation(method, DeleteMapping.class)))
                .or(() -> ofNullable(AnnotationUtils.findAnnotation(method, PutMapping.class)))
                .map(annotation -> AnnotationUtils.getAnnotationAttributes(method, annotation))
                .map(attributes -> new MethodInfo(
                        attributes.annotationType()
                                .getSimpleName()
                                .replace("Mapping", "")
                                .toUpperCase(),
                        getApiPath(attributes.getStringArray("value")),
                        ofNullable(AnnotationUtils.findAnnotation(method, PreAuthorize.class))
                                .map(p -> p.value().replace("hasAuthority('", "")
                                            .replace("') and", "")
                                            .replace("')", "")
                                            .replace("SCOPE_", "")
                                    )
                                .orElse(""),
                        method.getName()
                ));
    }

    private static String getApiPath(@Nullable RequestMapping requestMapping) {
        return ofNullable(requestMapping)
                .map(RequestMapping::value)
                .map(APIMetadataController::getApiPath)
                .orElse("");
    }

    private static String getApiPath(@Nullable String... array) {
        return ofNullable(array)
                .map(arr -> arr.length > 0 ? arr[0] : null)
                .orElse("");
    }

    private static String getEndpointDetailsHTML(List<ControllerInfo> controllers) {
        HashSet<String> scopes = new HashSet<>();
        StringBuilder endpointDetailsHTML = new StringBuilder();
        endpointDetailsHTML.append("<div class=\"container\">");
        for (ControllerInfo controller : controllers) {
            endpointDetailsHTML.append("<h2>")
                    .append(controller.name())
                    .append("</h2>")
                    .append("<ul class=\"responsive-table\">")
                    .append("<li class=\"table-header\">")
                    .append("<div class=\"col col-1\"> </div>")
                    .append("<div class=\"col col-2\">Endpoint</div>")
                    .append("<div class=\"col col-3\">Scopes</div>")
                    .append("<div class=\"col col-4\">Method</div></li>");
            for (MethodInfo method : controller.methods()) {
                endpointDetailsHTML.append("<li class=\"table-row-").append(method.httpMethod().toLowerCase()).append("\">")
                        .append("<div class=\"col-1-").append(method.httpMethod().toLowerCase()).append("\" data-label=\" \">")
                        .append(method.httpMethod()).append("</div>")
                        .append("<div class=\"col col-2\" data-label=\"Endpoint\">").append(method.apiPath()).append("</div>")
                        .append("<div class=\"col col-3\" data-label=\"Scopes\">").append(method.security()).append("</div>")
                        .append("<div class=\"col col-4\" data-label=\"Method\"><em>").append(method.functionName()).append("()</em></div>")
                        .append("</li>");
                if (method.security().contains(" ")) {
                    scopes.addAll(Arrays.stream(method.security().split(" ")).toList());
                } else
                    scopes.add(method.security());
            }
            endpointDetailsHTML.append("</ul></div>");
        }
        endpointDetailsHTML.append("<h2>All Scopes</h2><div class=\"container\">");
        endpointDetailsHTML.append("<ul class=\"responsive-table\">");

        for (String scope : scopes.stream().sorted().toList()) {
            endpointDetailsHTML.append("<li class=\"table-row-get\">").append("<div class=\"col col-1\" data-label=\"Scopes\">")
                    .append(scope).append("</div>").append("</li>");
        }
        endpointDetailsHTML.append("</ul></div>");
        return endpointDetailsHTML.toString();
    }

    private String getDownstreamEndpointsHTML() {
        StringBuilder downstreamEndpointsHTML = new StringBuilder();
        downstreamEndpointsHTML.append("<h2>Downstream Api calls</h2><div class=\"container\">");
        downstreamEndpointsHTML.append("<ul class=\"responsive-table\">");

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        Properties properties = yaml.getObject();

        assert properties != null;
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            if (entry.getKey().toString().startsWith("endpoint.")) {
                downstreamEndpointsHTML.append("<li class=\"table-row-post\">")
                        .append("<div class=\"col col2-post\" data-label=\"Value\">").append(entry.getValue().toString()).append("</div>")
                        .append("</li>");
            }
        }
        downstreamEndpointsHTML.append("</ul></div>");
        return downstreamEndpointsHTML.toString();
    }
}