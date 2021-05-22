package dk.accel.misw.mp.model.client;

import dk.accel.misw.mp.model.server.client.wstypes.XmlToken;

public class Main {

	public static void main(String[] args) throws Exception {
		WsClient client = new WsClient(1, 10);
		XmlToken token = client.subscribe();
		for (int i = 0; i < 10; i++) {
			Thread.sleep(500L);
			client.getValues(token);
		}
		client.unsubscribe(token);
	}

}
