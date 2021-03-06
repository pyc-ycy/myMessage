package com.pyc.mymessage;

import static java.lang.System.getProperty;
import com.rometools.rome.feed.synd.SyndEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.dsl.mail.Mail;
import org.springframework.integration.feed.inbound.FeedEntryMessageSource;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.dsl.file.Files;

import java.io.File;
import java.io.IOException;


@SpringBootApplication
public class MymessageApplication {

    public static void main(String[] args) {
        SpringApplication.run(MymessageApplication.class, args);
    }

    //---------------------------------------
    // Flow path of read
    // using @value annotation to gain resources from https://spring.io/blog.atom by automatic
    @Value("https://spring.io/blog.atom")
    Resource resource;

    // using Fluent API and pollers to configure acquiescent way of poll
    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerMetadata poller(){
        return Pollers.fixedRate(500).get();
    }

    // here is a position where construct adapter of inbox channel of feed and use the adapter as data input
    @Bean
    public FeedEntryMessageSource feedMessageSource() throws IOException{
        return new FeedEntryMessageSource(resource.getURL(), "news");
    }

    @Bean
    public IntegrationFlow myFlow() throws IOException{
        // flow path begin with the method called from
        return IntegrationFlows.from(feedMessageSource())
                // select route by route method,type of payload is SyndEntry,
        //the criteria type is string and the criteria value from Categroy what is classify by payload
        .<SyndEntry, String> route(payload->payload.getCategories().get(0).getName(),
                // send the different value to different message channel
                mapping->mapping.channelMapping("releases", "releasesChannel")
                .channelMapping("engineering", "engineeringChannel")
                .channelMapping("news", "newsChannel")
                ).get();    // Get the IntegrationFlow entity by get method and configure as a bean of spring
    }
    //--------------------------------------
    //-------------------------------------------
    // Releases flow path
    @Bean
    public IntegrationFlow releasesFlow(){
        // start read the data came from message channel releasesChannel
        return IntegrationFlows.from(MessageChannels.queue("releasesChannel",10))
                // Data conversion by using transform method. type of payload is SyndEntry,
                // convert it to string type and custom data format
                .<SyndEntry, String>transform(payload->"《"+
                        payload.getTitle()+"》"+payload.getLink()+ getProperty("line.separator"))
                // handling the outbound adapter of file by using handle method.
                //Files class is a Fluent API provided by Spring Integration Java DSL to construct adapter of output files
                .handle(
                        Files.outboundAdapter(new File("springblog"))
                        .fileExistsMode(FileExistsMode.APPEND)
                        .charset("UTF-8")
                        .fileNameGenerator(message -> "releases.txt")
                        .get()
                ).get();
    }
    //--------------------------------------------
    //--------------------------------------------
    // Engineering flow path
    @Bean
    public IntegrationFlow engineeringFlow(){
        return IntegrationFlows.from(MessageChannels.queue("engineeringChannel",10))
                .<SyndEntry, String>transform(
                        e->"《"+e.getTitle()+"》"+e.getLink()
                        +getProperty("line.separator")
                ).handle(
                        Files.outboundAdapter(new File("springblog"))
                        .fileExistsMode(FileExistsMode.APPEND)
                        .charset("UTF-8")
                        .fileNameGenerator(message -> "engineering.txt")
                        .get()
                ).get();
    }
    //-------------------------------------------
    @Bean
    public IntegrationFlow newsFlow(){
        return IntegrationFlows.from(MessageChannels.queue("newsChannel", 10))
                .<SyndEntry, String>transform(
                        payload->"《"+payload.getTitle()+"》"+payload.getLink()+getProperty("line.separator")
                )
                // add the information of message head by using enricherHeader
                .enrichHeaders(
                        Mail.headers()
                        .subject("A news come from Spring")
                        .to("2923616405@qq.com")
                        .from("15014366986@163.com"))
                // the information which send by mail is constructed by the method of Mail.headers provide by Spring Integration Java DSL
                .handle(
                        Mail.outboundAdapter("smtp.163.com")
                        .port(25)
                        .protocol("smtp")
                        .credentials("2923616405@qq.com", "pyc@79#61")
                        .javaMailProperties(p->p.put("mail.debug", "false")),
                        e->e.id("smtpOut")
                // define the outbound adapter of send mail by using a method called handle
                // Constructed by Mail.outboundAdapter provided from Spring Integration Java DSL
                ).get();
    }
}
