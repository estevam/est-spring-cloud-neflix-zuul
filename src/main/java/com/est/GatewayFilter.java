package com.est;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.Route;
import org.springframework.cloud.netflix.zuul.filters.SimpleRouteLocator;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StreamUtils;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.http.HttpMethod;

/**
 * 
 * @author estevam.meneses
 *
 */
@Component
public class GatewayFilter extends ZuulFilter {
	@Autowired
	private SimpleRouteLocator simpleRouteLocator;

	private static Logger log = LoggerFactory.getLogger(GatewayFilter.class);

	private ProxyRequestHelper helper;
	public ZuulProperties.Host hostProperties;

	public GatewayFilter(ProxyRequestHelper helper, ZuulProperties properties) {
		this.helper = helper;
		this.hostProperties = properties.getHost();

	}

	@Override
	public boolean shouldFilter() {
		/*
		 * RequestContext requestContext = getCurrentContext(); HttpServletRequest
		 * request = requestContext.getRequest(); String actualPath =
		 * getUri(request.getRequestURI()); String targetLocation =
		 * getTargetLocation(request.getRequestURI()); if
		 * (targetLocation.equals(gatewayHost) && actualPath.equals(tokenUri)) { return
		 * false; }
		 */
		return true;
	}

	@Override
	public Object run() throws ZuulException {
		RequestContext rc = RequestContext.getCurrentContext();
		HttpServletRequest request = rc.getRequest();
		/*
		 * boolean token = request.getParameterMap().containsKey("token"); if(!token){
		 * rc.setResponseStatusCode(401); rc.setSendZuulResponse(false);
		 * rc.setResponseBody("no token"); }
		 */
		OkHttpClient httpClient = new OkHttpClient.Builder().build();
		RequestContext context = RequestContext.getCurrentContext();

		String body = "";
		InputStream stream = null;

		context.setSendZuulResponse(false); //
		String method = request.getMethod();

		/// String uri = this.helper.buildZuulRequestURI(request);

		Headers.Builder headers = new Headers.Builder();
		Enumeration<String> headerNames = request.getHeaderNames();

		while (headerNames.hasMoreElements()) {
			String name = headerNames.nextElement();
			Enumeration<String> values = request.getHeaders(name);
			while (values.hasMoreElements()) {
				String value = values.nextElement();
				headers.add(name, value);
			}
		}

		String uri = headers.get("X-CF-Forwarded-Url");

		InputStream inputStream = null;
		try {
			inputStream = request.getInputStream();
		} catch (IOException e) {
			// e.printStackTrace();
			log.error("{}", e.toString());
			return null;
		}

		RequestBody requestBody = null;
		if (inputStream != null && HttpMethod.permitsRequestBody(method)) {
			MediaType mediaType = null;
			if (headers.get("Content-Type") != null) {
				mediaType = MediaType.parse(headers.get("Content-Type"));
			}
			try {
				requestBody = RequestBody.create(mediaType, StreamUtils.copyToByteArray(inputStream));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				log.error("{}", e.toString());
				return null;
			}
		}

		Request.Builder builder = new Request.Builder().headers(headers.build()).url(uri).method(method, requestBody);

		Response response = null;
		try {
			response = httpClient.newCall(builder.build()).execute();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			// e.printStackTrace();
			log.error(e.toString());
			return null;
		}

		LinkedMultiValueMap<String, String> responseHeaders = new LinkedMultiValueMap<>();

		for (Map.Entry<String, List<String>> entry : response.headers().toMultimap().entrySet()) {
			responseHeaders.put(entry.getKey(), entry.getValue());
		}

		try {
			body = response.body().string();
			stream = IOUtils.toInputStream(body, "UTF-8");
			this.helper.setResponse(response.code(), stream, responseHeaders);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			log.error("Error:{} ", e.getMessage());
			// e.printStackTrace();
			return null;
		}

		// prevent SimpleHostRoutingFilter from running
		context.setRouteHost(null);

		return null;
	}

	@Override
	public String filterType() {
		// TODO Auto-generated method stub
		return "pre";
	}

	@Override
	public int filterOrder() {
		// TODO Auto-generated method stub
		return 1;
	}

	public RequestContext getCurrentContext() {
		return RequestContext.getCurrentContext();
	}

	public String getUri(String requestUri) {
		Route route = simpleRouteLocator.getMatchingRoute(requestUri);
		return route.getPath();
	}

	public String getTargetLocation(String requestUri) {
		Route route = simpleRouteLocator.getMatchingRoute(requestUri);
		return route.getLocation();
	}
}
