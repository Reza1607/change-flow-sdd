package com.changeflow.web;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NoStoreFilterTest {

    @ParameterizedTest
    @ValueSource(ints = {200, 400, 401, 403, 404, 409, 422, 500, 503})
    void dataAndErrorResponsesPreventStorage(int status) throws Exception {
        var request = new MockHttpServletRequest();
        if (status == 500) {
            request.setDispatcherType(DispatcherType.ERROR);
        }
        var response = new MockHttpServletResponse();

        new NoStoreFilter().doFilter(request, response, (req, res) -> {
            assertEquals("no-store", response.getHeader("Cache-Control"));
            response.setStatus(status);
        });

        assertEquals(status, response.getStatus());
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }
}
