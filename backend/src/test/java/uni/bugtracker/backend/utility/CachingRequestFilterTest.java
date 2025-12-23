package uni.bugtracker.backend.utility;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CachingRequestFilterTest {

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterChain mockFilterChain;

    @Captor
    private ArgumentCaptor<ContentCachingRequestWrapper> wrapperCaptor;

    private final CachingRequestFilter filter = new CachingRequestFilter();

    @Test
    void doFilter_shouldWrapRequestInContentCachingRequestWrapper() throws ServletException, IOException {
        // When
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Then
        verify(mockFilterChain).doFilter(wrapperCaptor.capture(), eq(mockResponse));
        ContentCachingRequestWrapper wrapper = wrapperCaptor.getValue();
        assertNotNull(wrapper);
        assertInstanceOf(ContentCachingRequestWrapper.class, wrapper);
    }

    @Test
    void doFilter_shouldPassWrapperToNextFilter() throws ServletException, IOException {
        // When
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Then
        verify(mockFilterChain, times(1)).doFilter(any(ContentCachingRequestWrapper.class), eq(mockResponse));
        verify(mockFilterChain, never()).doFilter(eq(mockRequest), any());
    }

    @Test
    void doFilter_shouldPropagateIOException() throws ServletException, IOException {
        // Given
        IOException expectedException = new IOException("Test exception");
        doThrow(expectedException).when(mockFilterChain).doFilter(any(), any());

        // When & Then
        IOException thrown = assertThrows(IOException.class, () ->
                filter.doFilter(mockRequest, mockResponse, mockFilterChain)
        );
        assertEquals(expectedException, thrown);
    }

    @Test
    void doFilter_shouldPropagateServletException() throws ServletException, IOException {
        // Given
        ServletException expectedException = new ServletException("Test exception");
        doThrow(expectedException).when(mockFilterChain).doFilter(any(), any());

        // When & Then
        ServletException thrown = assertThrows(ServletException.class, () ->
                filter.doFilter(mockRequest, mockResponse, mockFilterChain)
        );
        assertEquals(expectedException, thrown);
    }

    @Test
    void doFilter_shouldWrapRequestEvenWhenExceptionThrown() throws ServletException, IOException {
        // Given
        ServletException expectedException = new ServletException("Test exception");
        doThrow(expectedException).when(mockFilterChain).doFilter(any(), any());

        // When
        try {
            filter.doFilter(mockRequest, mockResponse, mockFilterChain);
        } catch (ServletException e) {
            // Expected
        }

        // Then
        verify(mockFilterChain).doFilter(wrapperCaptor.capture(), eq(mockResponse));
        assertNotNull(wrapperCaptor.getValue());
    }

    @Test
    void doFilter_shouldCreateWrapper() throws ServletException, IOException {
        // When
        filter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Then
        verify(mockFilterChain).doFilter(wrapperCaptor.capture(), eq(mockResponse));
        ContentCachingRequestWrapper wrapper = wrapperCaptor.getValue();
        assertNotNull(wrapper);
        assertNotSame(mockRequest, wrapper);
    }
}