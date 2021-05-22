package dk.accel.misw.mp.model.node;

import java.util.concurrent.Future;


public interface BroadcastClient {

	void connect(boolean retry);
	
	Future<?> connectInSeparateThread(boolean retry);
}
