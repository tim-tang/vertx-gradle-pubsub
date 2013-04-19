package me.timtang.server;
import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;

/**
 * Vertx feed application server with module: mongo-persistor/auth-mgr/web-server.
 * 
 * @author tim.tang
 *
 */
public class VertxFeedApplication extends BusModBase {
    
	public void start() {
		StringBuffer sb = new StringBuffer();
		sb.append("{");
		// Normal web server stuff
		sb.append(	"\"port\": 8080,");
		sb.append(	"\"host\": \"localhost\",");
		sb.append(	"\"ssl\": false,");
		// configuration for the event bus client side bridge
		// this bridges messages from the client side to the server side event bus
		sb.append(	"\"bridge\": true,");
		
		// This defines which messages from the client we will let through
		// to the server side
		sb.append(	"\"inbound_permitted\": [ {");
		// allow calls to get static messages data from the persistor
		sb.append(		"\"address\" : \"vertx.mongopersistor\",");
		sb.append(		"\"match\" : {");
		sb.append(			"\"action\" : \"find\",");
		sb.append(			"\"collection\" : \"messages\"");
		sb.append(		"}");
		sb.append(	"}, {" );
		// allow calls to login
		sb.append(	"\"address\": \"vertx.basicauthmanager.login\"" );
		sb.append(	"}, {" );
		// and to place messages
		sb.append(		"\"address\": \"vertx.broadcaster\"," );
		sb.append(		"\"requires_auth\": true," );
		sb.append(		"\"match\" : {");
		sb.append(			"\"action\" : \"save\",");
		sb.append(			"\"collection\" : \"messages\"");
		sb.append(		"}");
		sb.append("} ],");
		// this defines which messages from the server we will let through to the client
		sb.append(	"\"outbound_permitted\": [ {} ]");
		sb.append("}");
		JsonObject webServerConf = new JsonObject(sb.toString());
		
		// now we deploy the modules that we need
		// create a handler to populate mock data when the persistor is loaded
		Handler<AsyncResult<String>> mockDataHandler = new Handler<AsyncResult<String>>() {
			public void handle(AsyncResult<String> message) {
				container.deployVerticle("me.timtang.persistor.MockDataInitializer");
			}
		};
		
		// deploy mongodb persistor module and pass in the handler.
		container.deployModule("io.vertx~mod-mongo-persistor~2.0.0-SNAPSHOT", null, 1, mockDataHandler);
		// deploy auth manager to handle the authentication
		container.deployModule("io.vertx~mod-auth-mgr~2.0.0-SNAPSHOT");
		// deploy feed broadcaster verticle.
		container.deployVerticle("me.timtang.broadcaster.VertxFeedBroadcaster");
		// deploy web server, with the configuration we defined above.
		container.deployModule("io.vertx~mod-web-server~2.0.0-SNAPSHOT", webServerConf);
    }
	
} 
