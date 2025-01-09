package com.psc.sw.website.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;

    /**
     * 메인 페이지
     * - 로그인 안 된 상태면 email=null
     * - 로그인 된 상태면 구글 프로필에서 email 추출
     */
    @GetMapping("/")
    public String index(OAuth2AuthenticationToken auth, Model model) {
        /**
        if (auth != null) {
            Map<String, Object> attrs = auth.getPrincipal().getAttributes();
            String email = (String) attrs.get("email"); // null일 수도 있음
            model.addAttribute("email", email);
        }
        **/
        return "index";
    }

    // 웹툰 페이지
    @GetMapping("/wetoon")
    public String wetoon() {
        return "wetoon"; // wetoon.html
    }



    @GetMapping("/revoke")
    public String revokeToken(OAuth2AuthenticationToken authentication, HttpServletRequest request) {
        // (1) 구글 토큰 Revoke
        if (authentication != null) {
            String registrationId = authentication.getAuthorizedClientRegistrationId();
            String principalName = authentication.getName();
            OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(registrationId, principalName);

            if (client != null) {
                // AccessToken/RefreshToken Revoke
                if (client.getAccessToken() != null) {
                    revokeGoogleToken(client.getAccessToken().getTokenValue());
                }
                if (client.getRefreshToken() != null) {
                    revokeGoogleToken(client.getRefreshToken().getTokenValue());
                }
                authorizedClientService.removeAuthorizedClient(registrationId, principalName);
            }
        }

        // (2) 세션 무효화 → SecurityContext 제거
        request.getSession().invalidate();

        // (3) 홈으로 리다이렉트
        return "redirect:/";
    }

    /**
     * 실제 구글 Revoke API (https://oauth2.googleapis.com/revoke?token=xxx) POST 호출
     */
    private void revokeGoogleToken(String tokenValue) {
        String revokeUrl = "https://oauth2.googleapis.com/revoke?token=" + tokenValue;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 빈 바디 + headers
        HttpEntity<String> request = new HttpEntity<>("", headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    revokeUrl,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            System.out.println("Revoke response: " + response.getStatusCode() + " " + response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
