package com.dias.services.core;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * DetailsFilter.java
 * Date: 8 окт. 2018 г.
 * Users: vmeshkov
 * Description: Фильтр для определения данных из сессии.
 */
@Component
public class DetailsFilter implements Filter {

    private static Logger LOG = Logger.getLogger(DetailsFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig) {
        // Не требуется
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String userIdHeaderValue = httpRequest.getHeader(Details.HEADER_USER_ID);
        LOG.info(String.format("userId прочитан из заголовка запроса: %s", userIdHeaderValue));
        Details.setDetails(httpRequest.getHeader("sessionId"), userIdHeaderValue);
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Не требуется
    }
}
