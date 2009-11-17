package eu.jucy.testfragment;

import java.io.File;

import uc.ISlotManager;
import uc.IUser;
import uc.files.transfer.Slot;
import uc.protocols.TransferType;

public class FakeSlotManager implements ISlotManager {

	public int getCurrentSlots() {
		throw new IllegalStateException("Method in fake FakeSlotmanager called");
	}

	public int getPositionInQueue(IUser usr) {
		throw new IllegalStateException("Method in fake FakeSlotmanager called");
	}

	public Slot getSlot(IUser usr, TransferType type, File f) {
		throw new IllegalStateException("Method in fake FakeSlotmanager called");
	}

	public int getTotalSlots() {
		throw new IllegalStateException("Method in fake FakeSlotmanager called");
	}

	public void init() {
		throw new IllegalStateException("Method in fake FakeSlotmanager called");
	}

	public void returnSlot(Slot slot, IUser usr) {
		throw new IllegalStateException("Method in fake FakeSlotmanager called");
	}


}
