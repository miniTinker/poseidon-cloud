package com.muggle.poseidon.config.security.filter;

import com.muggle.poseidon.config.security.properties.SecurityMessageProperties;
import com.muggle.poseidon.config.security.properties.UserSecurityProperties;
import com.muggle.poseidon.store.SecurityStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: poseidon-cloud-starter
 * @description: 登陆过滤器
 * @author: muggle
 * @create: 2019-11-07
 **/

@Slf4j
public class SecurityLoginFilter extends UsernamePasswordAuthenticationFilter {

    private SecurityStore securityStore;

    private UserSecurityProperties properties;

    public SecurityLoginFilter(SecurityStore securityStore, UserSecurityProperties properties) {
        this.securityStore = securityStore;
        this.properties = properties;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        String method = request.getMethod();
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            throw new AuthenticationServiceException("请求非法 ");
        }


        String username = this.obtainUsername(request);
        String password = this.obtainPassword(request);
        String loginType = request.getParameter(SecurityMessageProperties.LOGIN_TYPE);
        String verification = request.getParameter(SecurityMessageProperties.VERIFICATION);

        if (username == null||password==null||loginType==null||verification==null) {
            throw new AuthenticationServiceException("缺少参数，请填写用户名，密码，验证码等信息");
        }

        try {
            /** 1. 校验验证码 **/
            volidate(verification, username);
            /** 2. 验证登录信息 **/
            Map<String, String> principal = new HashMap<>();
            principal.put("username", username);
            principal.put("loginType", loginType);
            UsernamePasswordAuthenticationToken authenticationtoken = new UsernamePasswordAuthenticationToken(principal, password);
            Authentication authenticate = this.getAuthenticationManager().authenticate(authenticationtoken);
            /** 3. 登录信息写入token**/
            UserDetails details = (UserDetails) authenticate.getDetails();
            String key = SecurityMessageProperties.USER_NAME  + username;
            // 生成token 返回给前端
            /** todo 先不做 记住我的功能 4. 设置token过期时间**/
            String token = securityStore.saveUser(details, properties.getExperTime(), key);
            UsernamePasswordAuthenticationToken result = new UsernamePasswordAuthenticationToken(token, "");
            return result;
        }catch (AuthenticationException e){
            log.error("登陆异常：", e);
            throw new AuthenticationServiceException(e.getMessage());
        } catch (Exception e) {
            log.error("系统异常：", e);
            throw new AuthenticationServiceException("系统异常");
        }
    }

    /**
     * 校验验证码
     *
     * @param
     * @param verification
     */
    private void volidate(String verification, String username) throws AuthenticationException {
        if (verification==null){
            throw new BadCredentialsException("请输入验证码");
        }
    }
}