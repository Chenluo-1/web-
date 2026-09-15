package cn.ecom.config;

import cn.ecom.filter.JwtAuthenticationTokenFilter;
import cn.ecom.handler.security.AccessDeniedHandlerImpl;
import cn.ecom.handler.security.AuthenticationEntryPointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置（照 seig）：
 * - 无状态会话，token 校验过滤器在 UsernamePasswordAuthenticationFilter 之前
 * - /login、/health、swagger 许可匿名；/sys/**、/getInfo、/getRouters、/logout 需登录
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;

    @Autowired
    private AuthenticationEntryPointImpl authenticationEntryPoint;

    @Autowired
    private AccessDeniedHandlerImpl accessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                // 关闭 csrf
                .csrf().disable()
                // 不通过 Session 获取 SecurityContext
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                // 登录接口允许匿名（已登录用户重新登录也放行）
                .antMatchers("/login").permitAll()
                .antMatchers("/health").permitAll()
                // swagger 放行
                .antMatchers("/v2/api-docs", "/swagger-resources/**", "/swagger-ui.html",
                        "/webjars/**", "/doc.html", "/favicon.ico").permitAll()
                // 登录态相关与系统管理接口必须登录
                .antMatchers("/behavior/report").permitAll()
                .antMatchers("/search/**").permitAll()
                .antMatchers("/getInfo", "/getRouters", "/logout", "/sys/**", "/product/**", "/file/**",
                        "/cart/**", "/address/**", "/order/**", "/coupon/**", "/logistics/**", "/stock/**", "/dashboard/**",
                        "/chat/**", "/knowledge/**").authenticated()
                .anyRequest().permitAll();

        http.exceptionHandling()
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler);

        // token 校验过滤器
        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        // 关闭默认退出
        http.logout().disable();

        // 允许跨域
        http.cors();
    }
}
