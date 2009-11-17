package uc.protocols.hub;

import uc.listener.IPMReceivedListener;
import uc.listener.IUserChangedListener;
import uc.protocols.IProtocolStatusChangedListener;

/**
 * collection of all listeners needed 
 * to watch the hub .. used by UI 
 * @author Quicksilver
 *
 */
public interface IHubListener extends IProtocolStatusChangedListener,
		IMCReceivedListener,IUserChangedListener, IPMReceivedListener,
		IHubnameChangedListener, IFeedListener {

}
