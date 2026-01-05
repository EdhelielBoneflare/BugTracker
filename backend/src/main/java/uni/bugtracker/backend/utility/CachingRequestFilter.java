package uni.bugtracker.backend.utility;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

@Component
public class CachingRequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        ContentCachingRequestWrapper wrappedRequest =
                new ContentCachingRequestWrapper(httpRequest, 0);

        chain.doFilter(wrappedRequest, response);
    }
}