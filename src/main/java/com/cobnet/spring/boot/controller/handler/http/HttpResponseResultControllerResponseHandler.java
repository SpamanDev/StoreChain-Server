package com.cobnet.spring.boot.controller.handler.http;

import com.cobnet.interfaces.connection.web.Content;
import com.cobnet.interfaces.connection.web.ReasonableStatus;
import com.cobnet.spring.boot.core.ProjectBeanHolder;
import com.cobnet.spring.boot.dto.ObjectWrapper;
import com.cobnet.spring.boot.dto.ResponseResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class HttpResponseResultControllerResponseHandler implements ResponseBodyAdvice<ResponseResult<? extends ReasonableStatus>> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {

        return ResponseResult.class.isAssignableFrom(returnType.getMethod().getReturnType());
    }

    @Override
    public ResponseResult<? extends ReasonableStatus> beforeBodyWrite(ResponseResult<? extends ReasonableStatus> body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {

        if(body.status().getStatus().isError()) {

            ProjectBeanHolder.getSecurityService().addBadMessage(ProjectBeanHolder.getCurrentHttpRequest());
        }

        response.setStatusCode(body.status().getStatus());

        String message = body.status().message();

        ObjectWrapper<String> statusMessage = null;

        if(message != null) {

            statusMessage = new ObjectWrapper<>("message", message);
        }

        List<Content<?>> params = new ArrayList<>();

        if(statusMessage != null) {

            params.add(statusMessage);
        }

        for(Content<?> content : body.contents()) {

            params.add(content);
        }

        return body.setContents(params);
    }

}
