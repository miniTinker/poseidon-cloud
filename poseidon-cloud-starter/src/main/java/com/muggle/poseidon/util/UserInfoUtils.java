package com.muggle.poseidon.util;

import com.muggle.poseidon.base.exception.BasePoseidonCheckException;
import com.muggle.poseidon.base.exception.SimplePoseidonCheckException;
import com.muggle.poseidon.base.exception.SimplePoseidonException;
import com.muggle.poseidon.entity.SimpleUserDO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * @program: poseidon-cloud-starter
 * @description: 获取用户信息
 * @author: muggle
 * @create: 2019-11-06
 **/

public class UserInfoUtils {

    private UserInfoUtils(){
        throw new SimplePoseidonException("禁止实例化");
    }

    public static SimpleUserDO getUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication==null||authentication.getDetails()==null){
           return null;
        }
//          if (authentication==null||"anonymousUser".equals(principal)|| SecurityMessageProperties.BAD_TOKEN.equals(principal))
        if (!(authentication instanceof SimpleUserDO )){
//            throw new SimplePoseidonCheckException("用户登陆信息异常");
        }
        return (SimpleUserDO)authentication;
    }
}
