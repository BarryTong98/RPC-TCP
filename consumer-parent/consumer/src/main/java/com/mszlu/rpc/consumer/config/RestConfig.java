package com.mszlu.rpc.consumer.config;

import com.mszlu.rpc.annontation.EnableHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableHttpClient(basePackage = "com.mszlu.rpc.consumer.rpc")
public class RestConfig {

    //Define RestTemple which provided by spring
    //Initiate the HTTP request, pass the parameters, and parse the return value(Class<T> responseType)
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

}
