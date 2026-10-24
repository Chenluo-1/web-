package cn.ecom.config;

import cn.ecom.filter.JwtAuthenticationTokenFilter;
import cn.ecom.handler.security.AccessDeniedHandlerImpl;
import cn.ecom.handler.security.AuthenticationEntryPointImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
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
 * - 白名单（/login、/health、swagger、游客商品只读）permitAll；其余请求先要求登录，
 *   再经自定义权限管理器（路径级）与方法注解 @PreAuthorize（方法级）两层授权
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

    /** 路径级授权：白名单之外的全部请求交由此管理器判断（实验4 任务二·步骤2） */
    @Autowired
    private CustomAuthorizationManager customAuthorizationManager;

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
                // 路径级授权：白名单放行，其余全部交给自定义权限管理器（5.5 的 authorizeHttpRequests 只有 Customizer 重载）
                .authorizeHttpRequests(auth -> auth
                        // 登录/注册接口允许匿名（已登录用户重新登录也放行）
                        .antMatchers("/login", "/register").permitAll()
                        .antMatchers("/health").permitAll()
                        // swagger 放行
                        .antMatchers("/v2/api-docs", "/swagger-resources/**", "/swagger-ui.html",
                                "/webjars/**", "/doc.html", "/favicon.ico").permitAll()
                        // 错误页转发放行（兜底；AuthorizationFilter 默认跳过内部 ERROR dispatch）
                        .antMatchers("/error").permitAll()
                        // 预检请求不携带令牌，放行避免跨域失败
                        .antMatchers(HttpMethod.OPTIONS).permitAll()
                        .antMatchers("/behavior/report").permitAll()
                        .antMatchers("/search/**").permitAll()
                        // 游客浏览：商品列表/详情、商品评价只读（顺序别动：/product/category 靠 /product/* 放行）
                        .antMatchers(HttpMethod.GET, "/product", "/product/*").permitAll()
                        .antMatchers(HttpMethod.GET, "/review/product/**").permitAll()
                        // 白名单内不得出现带 @PreAuthorize 的接口，否则未登录会从 401 变 403
                        // 其余请求：先要求登录（未登录由 entrypoint 产 401）；已登录再按路径权限表比对（无权限由 AccessDeniedHandler 产 403）
                        .anyRequest().access(customAuthorizationManager));

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
