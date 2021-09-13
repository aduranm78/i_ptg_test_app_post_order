package com.example;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import org.apache.camel.model.rest.RestBindingMode;
import java.net.URLEncoder;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

/**
 * A simple Camel route that triggers from a timer and calls a bean and prints to system out.
 * <p/>
 * Use <tt>@Component</tt> to make Camel auto detect this route when starting.
 */
@Component
public class MySpringBootRouter extends RouteBuilder {
	
	@Autowired
	private Environment env;

    @Override
    public void configure() throws Exception {
		restConfiguration()
		.component("netty-http")
		.port("8080")
		.bindingMode(RestBindingMode.auto);
    	
    	String erpUri = "https://5298967-sb1.restlets.api.netsuite.com/app/site/hosting/restlet.nl?script=575&deploy=1";
    	
    	onException(HttpOperationFailedException.class)
    		.handled(true)
    		.process(exchange -> {
    			System.out.println("Error al enviar información en Netsuite");
    			System.out.println(exchange.getProperties());
    		});
    		// .continued(true); // Para continuar con la ruta

    	
    	//from("timer:poll?period={{timer.period}}").routeId("{{route.id}}")
		//from("")
    		/*.process(exchange -> {
    			String wmsUri = env.getProperty("wms.uri");
				System.out.println("URL WMS: " + wmsUri);
    			// String dateRange = WmsParams.getDateRange(60 * 60 * 24 * 90); // Poll interval in seconds (3 months)
    			// String dateRange = WmsParams.getDateRange(30); // Poll interval in seconds (30 seconds)
    			String dateRange = WmsParams.getDateRange(60 * 60); // Poll interval in seconds (1 hour)
    			System.out.println();
    			System.out.println();
    			System.out.println("Periodo de consulta: " + dateRange);
    			String encodedDateRange = URLEncoder.encode(dateRange, "UTF-8");
    	    	exchange.getMessage().setHeader(Exchange.HTTP_QUERY, "warehouse=28002&between=" + encodedDateRange);
    	    	exchange.getMessage().setHeader(Exchange.HTTP_URI, wmsUri);
    		})
    		.to("log:DEBUG?showBody=true&showHeaders=true")
    		//.to("https://test?throwExceptionOnFailure=false") // Para no lanzar errores
    		.to("https://wms")
        	.to("log:DEBUG?showBody=true&showHeaders=true")

		/*rest()
			.path("/").consumes("application/json").produces("application/json")
			  .put("/order")
	  //          .type(Customer.class).outType(CustomerSuccess.class)
				.to("direct:put-customer")
			  .post("/order")
	  //          .type(Customer.class).outType(CustomerSuccess.class)
				.to("direct:post-customer");
		  
		from("direct:post-customer")
			.setHeader("CamelHttpMethod", constant("POST"))
			.to("direct:request");
		from("direct:put-customer")
			.setHeader("CamelHttpMethod", constant("PUT"))
			.to("direct:request");*/
	  
		//from("direct:post-customer")
		from("direct:order")
			//.put("/post-order")
			//.type(Customer.class).outType(CustomerSuccess.class)
        	//.removeHeaders("*")
        	.setHeader("CamelHttpMethod", constant("POST"))
        	.setHeader(Exchange.HTTP_URI, constant(erpUri))
        	.process(new Processor() {
                @Override
                public void process(Exchange exchange) throws Exception {
                	String authHeader = OAuthSign.getAuthHeader(erpUri);
                    exchange.getMessage().setHeader("Authorization", authHeader);
                }
        	})
        	.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
        	.to("log:DEBUG?showBody=true&showHeaders=true")
        	.to("https://netsuite")
        	.to("log:DEBUG?showBody=true&showHeaders=true")
        	.to("stream:out");
    }

}
