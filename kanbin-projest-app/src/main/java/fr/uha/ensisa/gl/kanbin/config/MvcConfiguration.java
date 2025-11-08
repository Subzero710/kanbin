package fr.uha.ensisa.gl.kanbin.config;

import java.util.concurrent.TimeUnit;

import fr.uha.ensisa.gl.kanbin.projest.repo.BoardRepo;
import fr.uha.ensisa.gl.kanbin.projest.repo.RepoFactory;
import fr.uha.ensisa.gl.kanbin.projest.repo.mem.RepoFactoryMem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.EncodedResourceResolver;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

@Configuration
@ComponentScan(basePackages="fr.uha.ensisa.gl.kanbin")
@EnableWebMvc
public class MvcConfiguration implements WebMvcConfigurer {
	@Autowired
	private ApplicationContext applicationContext;

	@Bean
    public ViewResolver viewResolver() {
		ThymeleafViewResolver resolver = new ThymeleafViewResolver();
		resolver.setTemplateEngine(springTemplateEngine());
		return resolver;
	}

	@Bean
	public SpringTemplateEngine springTemplateEngine() {
	  SpringTemplateEngine engine = new SpringTemplateEngine();
	  engine.setEnableSpringELCompiler(true);
	  engine.setTemplateResolver(templateResolver());
	  return engine;
	}

	@Bean
    public SpringResourceTemplateResolver templateResolver() {
	  SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
	  resolver.setApplicationContext(this.applicationContext);
	  resolver.setPrefix("/WEB-INF/views/");
	  resolver.setSuffix(".html");
	  resolver.setTemplateMode(TemplateMode.HTML);
	  return resolver;
	}
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry
			.addResourceHandler("/resources/**")
			.addResourceLocations("/resources/")
			.setCachePeriod(0); // for development
		registry
			.addResourceHandler("/libs/**")
			.addResourceLocations("/libs/bootstrap/")
			.setCachePeriod((int)TimeUnit.DAYS.toSeconds(365))
			.resourceChain(true)
			.addResolver(new EncodedResourceResolver())
      		.addResolver(new PathResourceResolver());
	}

	@Bean
	public MultipartResolver multipartResolver(){
		return new StandardServletMultipartResolver();
	}

    @Bean
    public RepoFactory getRepoFactory() { return new RepoFactoryMem(); } // doc GL

    @Bean
    public BoardRepo boardRepo(RepoFactory f) { return f.getBoardRepo(); }
}
