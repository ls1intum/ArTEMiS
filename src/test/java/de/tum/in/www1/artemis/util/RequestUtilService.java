package de.tum.in.www1.artemis.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.List;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class RequestUtilService {

    private MockMvc mvc;

    private ObjectMapper mapper;

    public RequestUtilService(MockMvc mvc, ObjectMapper mapper) {
        this.mvc = mvc;
        this.mapper = mapper;
    }

    public <T> URI post(String path, T body, HttpStatus expectedStatus) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);
        MvcResult res = mvc.perform(MockMvcRequestBuilders.post(new URI(path)).contentType(MediaType.APPLICATION_JSON).content(jsonBody).with(csrf()))
                .andExpect(status().is(expectedStatus.value())).andReturn();
        if (!expectedStatus.is2xxSuccessful()) {
            assertThat(res.getResponse().containsHeader("location")).as("no location header on failed request").isFalse();
            return null;
        }
        assertThat(res.getResponse().containsHeader("location")).isTrue();
        return new URI(res.getResponse().getHeader("location"));
    }

    public <T, R> R postWithResponseBody(String path, T body, Class<R> responseType, HttpStatus expectedStatus) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);
        MvcResult res = mvc.perform(MockMvcRequestBuilders.post(new URI(path)).contentType(MediaType.APPLICATION_JSON).content(jsonBody).with(csrf()))
                .andExpect(status().is(expectedStatus.value())).andReturn();
        if (!expectedStatus.is2xxSuccessful()) {
            assertThat(res.getResponse().containsHeader("location")).as("no location header on failed request").isFalse();
            return null;
        }
        return mapper.readValue(res.getResponse().getContentAsString(), responseType);
    }

    public <T, R> R postWithResponseBody(String path, T body, Class<R> responseType) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);
        MvcResult res = mvc.perform(MockMvcRequestBuilders.post(new URI(path)).contentType(MediaType.APPLICATION_JSON).content(jsonBody).with(csrf())).andReturn();
        if (res.getResponse().getStatus() >= 299) {
            return null;
        }
        return mapper.readValue(res.getResponse().getContentAsString(), responseType);
    }

    public <T, R> R putWithResponseBody(String path, T body, Class<R> responseType, HttpStatus expectedStatus) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);
        MvcResult res = mvc.perform(MockMvcRequestBuilders.put(new URI(path)).contentType(MediaType.APPLICATION_JSON).content(jsonBody).with(csrf()))
                .andExpect(status().is(expectedStatus.value())).andReturn();

        if (res.getResponse().getStatus() >= 299) {
            return null;
        }

        return mapper.readValue(res.getResponse().getContentAsString(), responseType);
    }

    public <T, R> List<R> putWithResponseBodyList(String path, T body, Class<R> listElementType, HttpStatus expectedStatus) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);
        MvcResult res = mvc.perform(MockMvcRequestBuilders.put(new URI(path)).contentType(MediaType.APPLICATION_JSON).content(jsonBody).with(csrf()))
                .andExpect(status().is(expectedStatus.value())).andReturn();

        return mapper.readValue(res.getResponse().getContentAsString(), mapper.getTypeFactory().constructCollectionType(List.class, listElementType));
    }

    public <T> void put(String path, T body, HttpStatus expectedStatus) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);
        mvc.perform(MockMvcRequestBuilders.put(new URI(path)).contentType(MediaType.APPLICATION_JSON).content(jsonBody).with(csrf()))
                .andExpect(status().is(expectedStatus.value()));
    }

    public <T> T get(String path, HttpStatus expectedStatus, Class<T> responseType) throws Exception {
        return get(path, expectedStatus, responseType, new LinkedMultiValueMap<>());
    }

    public <T> T get(String path, HttpStatus expectedStatus, Class<T> responseType, MultiValueMap<String, String> params) throws Exception {
        MvcResult res = mvc.perform(MockMvcRequestBuilders.get(new URI(path)).params(params).with(csrf())).andExpect(status().is(expectedStatus.value())).andReturn();
        if (!expectedStatus.is2xxSuccessful()) {
            if (res.getResponse().getContentType() != null && !res.getResponse().getContentType().equals("application/problem+json")) {
                assertThat(res.getResponse().getContentAsString()).isNullOrEmpty();
            }
            return null;
        }
        return mapper.readValue(res.getResponse().getContentAsString(), responseType);
    }

    public <T> List<T> getList(String path, HttpStatus expectedStatus, Class<T> listElementType) throws Exception {
        return getList(path, expectedStatus, listElementType, new LinkedMultiValueMap<>());
    }

    public <T> List<T> getList(String path, HttpStatus expectedStatus, Class<T> listElementType, MultiValueMap<String, String> params) throws Exception {
        MvcResult res = mvc.perform(MockMvcRequestBuilders.get(new URI(path)).with(csrf()).params(params)).andExpect(status().is(expectedStatus.value())).andReturn();
        if (!expectedStatus.is2xxSuccessful()) {
            if (res.getResponse().getContentType() != null && !res.getResponse().getContentType().equals("application/problem+json")) {
                assertThat(res.getResponse().getContentAsString()).isNullOrEmpty();
            }
            return null;
        }

        return mapper.readValue(res.getResponse().getContentAsString(), mapper.getTypeFactory().constructCollectionType(List.class, listElementType));
    }

    public void delete(String path, HttpStatus expectedStatus) throws Exception {
        mvc.perform(MockMvcRequestBuilders.delete(new URI(path)).with(csrf())).andExpect(status().is(expectedStatus.value())).andReturn();
    }
}
