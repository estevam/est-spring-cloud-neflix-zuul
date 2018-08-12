package com.est;

import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 
 * @author estevam.meneses
 *
 */
@Configuration
public class ConfigZuul {
	
	/**
	 * Start Zuul filter 
	 * @param helper
	 * @param zuulProperties
	 * @return
	 */
	@Bean
	public GatewayFilter shengRoutingFilter(ProxyRequestHelper helper, ZuulProperties zuulProperties) {
		return new GatewayFilter(helper, zuulProperties);
	}
}
