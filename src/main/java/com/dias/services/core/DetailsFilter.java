package com.dias.services.core;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * DetailsFilter.java
 * Date: 8 окт. 2018 г.
 * Users: vmeshkov
 * Description: Фильтр для определения данных из сессии.
 */
@Component
public class DetailsFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // Не требуется
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        Details.setDetails(httpRequest.getHeader("sessionId"), httpRequest.getHeader(Details.HEADER_USER_ID));
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Не требуется
    }
}
