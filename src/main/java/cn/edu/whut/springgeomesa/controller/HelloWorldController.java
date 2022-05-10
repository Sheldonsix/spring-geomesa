package cn.edu.whut.springgeomesa.controller;

import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @ClassName HelloWorldController
 * @Description TODO
 * @Author sheldon
 * @Date 2022/5/8 10:38
 * @Version 1.0
 **/
@Api(tags = "helloworld")
@RestController
public class HelloWorldController {
    @GetMapping("/hello")
    public String hello(String id) {
//        logger.info("helloworld");
        return "Hello, world.";
    }
}
