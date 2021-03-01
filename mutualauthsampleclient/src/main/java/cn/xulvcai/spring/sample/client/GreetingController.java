package cn.xulvcai.spring.sample.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class GreetingController {

    @Value("${client.url}")
    private String clientUrl;

    @Autowired
    private RestTemplate restTemplate;

    @GetMapping("/greeting")
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "World") String name) {
        String url = String.format("%s/greeting?name=%s", clientUrl, name);
        ResponseEntity<Greeting> response = restTemplate.getForEntity(url, Greeting.class);
        return response.getBody();
    }


}
