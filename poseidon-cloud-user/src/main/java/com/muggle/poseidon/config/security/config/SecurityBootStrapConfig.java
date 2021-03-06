package com.muggle.poseidon.config.security.config;

import com.muggle.poseidon.auto.PoseidonSecurityProperties;
import com.muggle.poseidon.entity.AuthUrlPathDO;
import com.muggle.poseidon.service.TokenService;
import com.muggle.poseidon.store.SecurityStore;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: poseidon
 * @description: security配置辅助类
 * @author: muggle
 * @create: 2019-05-21
 **/
@Configuration
@Slf4j
public class SecurityBootStrapConfig {
    @Autowired
    private WebApplicationContext applicationContext;
    @Value("${spring.application.name}")
    private String appName;


    @Bean
    @RefreshScope
    @ConfigurationProperties(prefix = "poseidon")
    public PoseidonSecurityProperties getModel() {
        return new PoseidonSecurityProperties();
    }

    @Autowired
    public void saveIgnorePath(){
        SecurityStore.ACCESS_PATHS.add("/registry/machine");
        SecurityStore.ACCESS_PATHS.add("/error");
    }
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Profile({"prod","test"})
    public CommandLineRunner initUrl(final TokenService tokenService) {
        return new CommandLineRunner() {
            @Override
            public void run(String... strings) throws Exception {
                log.debug("》》》》 权限系统开机任务执行");
                List<AuthUrlPathDO> allURL = getAllURL();
                tokenService.saveUrlInfo(allURL);
            }
        };
    }

    /** 读取所有的url 并交给tokenService.saveUrlInfo处理 **/
    public  List<AuthUrlPathDO> getAllURL() {
        List<AuthUrlPathDO> resultList = new ArrayList<>();

        RequestMappingHandlerMapping requestMappingHandlerMapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
        // 获取url与类和方法的对应信息
        Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();

        for (Map.Entry<RequestMappingInfo, HandlerMethod> mappingInfoHandlerMethodEntry : map.entrySet()) {

            RequestMappingInfo requestMappingInfo = mappingInfoHandlerMethodEntry.getKey();
            HandlerMethod handlerMethod = mappingInfoHandlerMethodEntry.getValue();
            AuthUrlPathDO authUrlPathDO = new AuthUrlPathDO();
            if (handlerMethod.getMethod().getDeclaringClass().getName().equals("com.muggle.poseidon.handler.PoseidonWebHandler")){
                continue;
            }
            // 类名
            authUrlPathDO.setClassName(handlerMethod.getMethod().getDeclaringClass().getName());

            Annotation[] parentAnnotations = handlerMethod.getBeanType().getAnnotations();
            for (Annotation annotation : parentAnnotations) {
                if (annotation instanceof Api) {
                    Api api = (Api) annotation;
                    authUrlPathDO.setClassDesc(api.value());
                } else if (annotation instanceof RequestMapping) {
                    RequestMapping requestMapping = (RequestMapping) annotation;
                    if (null != requestMapping.value() && requestMapping.value().length > 0) {
                        //类URL
                        authUrlPathDO.setClassUrl(requestMapping.value()[0]);
                    }
                }
            }
            // 方法名
            authUrlPathDO.setMethodName(handlerMethod.getMethod().getName());
            Annotation[] annotations = handlerMethod.getMethod().getDeclaredAnnotations();
            if (annotations != null) {
                // 处理具体的方法信息
                for (Annotation annotation : annotations) {
                    if (annotation instanceof ApiOperation) {
                        ApiOperation methodDesc = (ApiOperation) annotation;
                        String desc = methodDesc.value();
                        //接口描述
                        authUrlPathDO.setMethodDesc(desc);
                    }
                }
            }
            PatternsRequestCondition p = requestMappingInfo.getPatternsCondition();
            for (String url : p.getPatterns()) {
                //请求URL
                authUrlPathDO.setMethodURL(url);
            }
            RequestMethodsRequestCondition methodsCondition = requestMappingInfo.getMethodsCondition();
            for (RequestMethod requestMethod : methodsCondition.getMethods()) {
                //请求方式：POST/PUT/GET/DELETE
                authUrlPathDO.setRequestType(requestMethod.toString());
            }
            authUrlPathDO.setApplication(appName);
            resultList.add(authUrlPathDO);
        }
        return resultList;
    }
}
