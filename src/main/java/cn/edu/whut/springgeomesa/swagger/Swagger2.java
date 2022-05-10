package cn.edu.whut.springgeomesa.swagger;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @ClassName Swagger2
 * @Description 对swagger进行配置
 * @Author sheldon
 * @Date 2022/5/8 10:08
 * @Version 1.0
 **/
@Configuration // 表明是配置类
@EnableSwagger2  // 开启 swagger 功能
public class Swagger2 {
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())  // 用于生成 API 信息
                .select()  // 返回一个 ApiSelectBuilder 实例，用来控制接口被 swagger 做成文档
                .apis(RequestHandlerSelectors.basePackage("cn.edu.whut.springgeomesa.controller"))  // 用于指定扫描哪个包下的接口
                .paths(PathSelectors.any())  // 选择所有的 API
                .build();
    }

    /*
     * @author sheldon
     * @date 2022/5/8
     * @description 用于定义API主界面的信息，比如可以声明所有的API的总标题、描述、版本
     * @param:
     * @return springfox.documentation.service.ApiInfo
     **/
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("Spring Boot 中使用 Swagger2 构建 RESTful APIs")
                .description("spring-geomesa RestAPI 测试")
                .termsOfServiceUrl("https://baidu.com")  // 定义服务的域名
                .version("1.0")
                .build();
    }
}
