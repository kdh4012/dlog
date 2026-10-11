package com.dlog.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * 운영 배포 시 React 빌드 결과물(static/)을 함께 서빙한다.
 * 존재하지 않는 경로는 index.html 로 넘겨 React Router 가 처리하게 한다.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    private static final String STATIC_ROOT = "static/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/" + STATIC_ROOT)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // API 및 확장자가 있는 파일 요청은 fallback 하지 않음 (404 유지)
                        if (resourcePath.startsWith("api/") || resourcePath.contains(".")) {
                            return null;
                        }
                        Resource index = new ClassPathResource(STATIC_ROOT + "index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
