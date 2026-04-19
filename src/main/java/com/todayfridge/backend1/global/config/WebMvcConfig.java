package com.todayfridge.backend1.global.config;

import com.todayfridge.backend1.global.auth.CustomUserPrincipal;
import com.todayfridge.backend1.global.security.CurrentUser;
import com.todayfridge.backend1.global.security.LoginUser;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginUserResolver());
    }

    static class LoginUserResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class)
                    && parameter.getParameterType().equals(LoginUser.class);
        }

        @Override
        public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) {
            Authentication authentication = (Authentication) webRequest.getUserPrincipal();
            if (authentication == null || authentication.getPrincipal() == null) return null;
            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserPrincipal p) {
                return new LoginUser(p.getUserId(), p.getUsername(), p.getNickname(), p.getRole());
            }
            return null;
        }
    }
}
