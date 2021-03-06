package com.spring.service.impl;

import com.bgnt.em.BaseResultCode;
import com.google.gson.Gson;
import com.spring.response.BaseResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * User: GaoYuan
 * Date: 17/12/19
 * Time: 13:57
 */
public class TokenAuthenticationService {
    public static final long EXPIRATIONTIME = 7_200_000;     // 2小时
    public static final String SECRET = "P@ssw02d";            // JWT密码
    public static final String TOKEN_PREFIX = "Bearer";        // Token前缀
    public static final String HEADER_STRING = "Authorization";// 存放Token的Header Key

    // JWT生成方法
    public static String addAuthentication(String username) {

        // 生成JWT
        String JWT = Jwts.builder()
                // 保存权限（角色）
                .claim("authorities", "ROLE_ADMIN,AUTH_WRITE")
                // 用户名写入标题
                .setSubject(username)
                // 有效期设置
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATIONTIME))
                // 签名设置
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
        return JWT;
        // 将 JWT 写入 body
//        try {
//            response.setContentType("application/json");
//            response.setStatus(HttpServletResponse.SC_OK);
//            Gson gson = new Gson();
//
//            response.getOutputStream().println(gson.toJson(new BaseResponse(BaseResultCode.OK.getValue(), JWT)));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    // JWT验证方法，去redis中验证是否存在此token
    public static Authentication getAuthentication(HttpServletRequest request, StringRedisTemplate stringRedisTemplate) {
        // 从Header中拿到token
        String token = request.getHeader(HEADER_STRING);
        try {
            if (token != null) {
                // 解析 Token
                Claims claims = Jwts.parser()
                        // 验签
                        .setSigningKey(SECRET)
                        // 去掉 Bearer
                        .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                        .getBody();

                // 拿用户名
                String user = claims.getSubject();
                // 去redis中找到对应的用户信息进行比较
                String tokenRedis = stringRedisTemplate.opsForValue().get(user);
                if (null != tokenRedis && (TOKEN_PREFIX + " " + tokenRedis).equals(token)) {
                    // 得到 权限（角色）
                    List<GrantedAuthority> authorities = AuthorityUtils.commaSeparatedStringToAuthorityList((String) claims.get("authorities"));
                    // 返回验证令牌
                    return user != null ?
                            new UsernamePasswordAuthenticationToken(user, null, authorities) :
                            null;
                } else {
                    return null;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }
}
