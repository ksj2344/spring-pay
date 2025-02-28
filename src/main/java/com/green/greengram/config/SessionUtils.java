package com.green.greengram.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SessionUtils {
    public static void addAttribute(String name, Object value) {
        Objects.requireNonNull(RequestContextHolder.getRequestAttributes()).setAttribute(name, value, RequestAttributes.SCOPE_SESSION);
    }

    public static String getStringAttribute(String name) {
        return String.valueOf(getAttribute(name));
    }

    public static Object getAttribute(String name) {
        return Objects.requireNonNull(RequestContextHolder.getRequestAttributes()).getAttribute(name, RequestAttributes.SCOPE_SESSION);
    }
}

/*
    Scope 생명주기
    - page  SSR 서버측에서 화면을 렌더링 할 때 씀

    우리가 쓰는거
    - request 요청이 생길 때 마다 새 스코프가 만들어짐
    - session 같은 브라우저 요청이 오면 session은 하나만 만들어지고 그걸 계속 사용(크롬Chrome페이지를 끄지 않았다면 계속 같은 브라우저로 여김)
    - application 전역, static
 */
