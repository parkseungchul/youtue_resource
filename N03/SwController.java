package com.psc.sw.website.controller.sw;

import com.facebook.ads.sdk.APIContext;
import com.facebook.ads.sdk.APIException;
import com.facebook.ads.sdk.serverside.*;
import com.psc.sw.website.component.ProfileComponent;
import com.psc.sw.website.dto.AppId;
import com.psc.sw.website.dto.PixelDto;
import com.psc.sw.website.entity.Members;
import com.psc.sw.website.repository.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;


@Slf4j
@Controller
@AllArgsConstructor
@RequestMapping("/sw")
public class SwController {

    private final ProfileComponent profileComponent;
    private final MemberRepository memberRepository;

    @GetMapping("/typo")
    public String typoHandler(OAuth2AuthenticationToken auth, Model model) {
        if (auth != null) {
            Map<String, Object> attrs = auth.getPrincipal().getAttributes();
            String email = (String) attrs.get("email");
            Members member = memberRepository.findFirstByAppidAndEmail(AppId.TYPO, email);

            if (member != null) {
                return "redirect:/typo/v1/" + member.getDocId();
            }else{
                model.addAttribute("msg", email +" 허용되지 않은 이메일");
                model.addAttribute("sheetId", "");
                return profileComponent.getUrlPrxFix()+"main";
            }
        } else {
            return "redirect:/sw";
        }
    }

    @GetMapping("/meta")
    public String pixel(Model model) {
        return "sw/meta/index";
    }

    @PostMapping("/meta")
    @ResponseBody  // 이 메서드는 JSON 응답을 반환합니다.
    public PixelDto postPixel(HttpServletRequest request, @RequestBody PixelDto pixelDto, Model model){

        String clientIpAddress = request.getRemoteAddr();
        String clientUserAgent = request.getHeader("User-Agent");

        log.debug(pixelDto.toString());
        String ACCESS_TOKEN = pixelDto.getTokenId();
        String PIXEL_ID = pixelDto.getPixelId();
        String URL_ID = pixelDto.getUrlId();
        String EMAIL = pixelDto.getEmail();
        String PHONE = pixelDto.getPhone();
        String EVENT_ID = pixelDto.getEventId();
        String PRODUCT_ID = pixelDto.getProductId();
        String FBP = pixelDto.getFbp();
        String FBC = pixelDto.getFbc();

        APIContext context = new APIContext(ACCESS_TOKEN).enableDebug(true);
        context.setLogger(System.out);

        UserData userData = new UserData()
                .emails(Arrays.asList(EMAIL))
                .phones(Arrays.asList(PHONE))
                .clientIpAddress(clientIpAddress)
                .clientUserAgent(clientUserAgent)
                .fbc(FBP)
                .fbp(FBC);

        Content content = new Content()
                .productId(PRODUCT_ID)
                .quantity(1L)
                .deliveryCategory(DeliveryCategory.home_delivery);

        CustomData customData = new CustomData()
                .addContent(content)
                .currency("krw")
                .value(pixelDto.getProductValue());

        Event purchaseEvent = new Event();
        purchaseEvent.eventName("Purchase")
                .eventTime(System.currentTimeMillis() / 1000L)
                .eventId(EVENT_ID)
                .userData(userData)
                .customData(customData)
                .eventSourceUrl(URL_ID)
                .actionSource(ActionSource.website);

        EventRequest eventRequest = new EventRequest(PIXEL_ID, context);
        eventRequest.addDataItem(purchaseEvent);


        String result = "Success";
        pixelDto.setResult(result);
        try {
            EventResponse response = eventRequest.execute();
            log.debug(String.format("Standard API response : %s ", response));
        } catch (APIException e) {

            e.printStackTrace();
            pixelDto.setResult(e.toString());

        }
        return pixelDto;
    }
}
