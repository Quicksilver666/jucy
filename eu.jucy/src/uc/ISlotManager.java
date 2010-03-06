package uc;



import uc.files.filelist.FileListFile;
import uc.files.transfer.Slot;
import uc.protocols.TransferType;

public interface ISlotManager {

	void init();

	/**
	 * with this function an upload can request a slot
	 * 
	 * @param usr - the user that wants something from us
	 * @param type - what we want to upload..
	 * @param f if its a file the file is provided
	 * @return a slot if available  null if none
	 */
	Slot getSlot(IUser usr, TransferType type, FileListFile f);

	/**
	 * returns a slot after use to the client so someone else can reuse it..
	 * 
	 * @param slot - the slot that is returned..
	 */
	void returnSlot(Slot slot, IUser usr);

	/**
	 * 
	 * @return value= totalslots - currently in use slots.
	 * so it returns the value usually sent in search messages.  
	 */
	int getCurrentSlots();

	/**
	 * 
	 * @return totalSlots  - the current maximum of slots
	 */
	int getTotalSlots();

	/**
	 * 
	 * @param usr - which user
	 * @return position of the user in the Queue.
	 * -1 if he is not in the queue
	 *  0 based..
	 */
	int getPositionInQueue(IUser usr);

}