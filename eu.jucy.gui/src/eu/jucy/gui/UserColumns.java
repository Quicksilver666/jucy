package eu.jucy.gui;

import helpers.SizeEnum;

import java.net.InetAddress;
import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.plugin.AbstractUIPlugin;


import uc.IHasUser;
import uc.IHub;
import uc.IUser;
import uc.IUser.Mode;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;


/**
 * Holds all columns for Users 
 * 
 * @author Quicksilver
 *
 */
public abstract class UserColumns  extends ColumnDescriptor<IUser> {

	
	protected UserColumns(int defaultColumnSize, String columnName, int style) {
		super(defaultColumnSize, columnName, style);
	}

	@Override
	public Image getImage(IUser cur) {
		return null;
	}
	

	
	public static class NameUserCol<X extends IHasUser> extends  ColumnDescriptor<X> {

		public static <T extends IHasUser> ColumnDescriptor<T> get() {
			return new NameUserCol<T>();
		}
		
		public NameUserCol() {
			super(150, Lang.User);
		}

		@Override
		public String getText(X x) {
			return x.getUser().getNick();
		}
		
		@Override
		public Image getImage(X x) {
			return Nick.GetUserImage(x.getUser());
		}
		
	}

	public static class Nick extends UserColumns {
		
		private static final Image NORM_ACTIVE;
		private static final Image NORM_PASSIVE;
		private static final Image NORM_OFFLINE;
		
		private static final Image OP_ACTIVE;
		private static final Image OP_PASSIVE;
		private static final Image OP_OFFLINE;
		
		
		static {
			NORM_ACTIVE  =	AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.USER_ACTIVE).createImage();
			NORM_PASSIVE =	AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.USER_PASSIVE).createImage();
			NORM_OFFLINE = AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.USER_OFFLINE).createImage();
			
			Image key = AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.USER_OPKEY).createImage();
			
			
			OP_ACTIVE = copyWithKey(NORM_ACTIVE,key);
			OP_PASSIVE = copyWithKey(NORM_PASSIVE, key);
			OP_OFFLINE = copyWithKey(NORM_OFFLINE, key);
			
//			Image normCop = new Image(NORM_ACTIVE.getDevice(),NORM_ACTIVE,SWT.IMAGE_COPY);
//			GC gc = new GC(normCop);
//			gc.drawImage(key, 0 , 0);
//			gc.dispose();
//			ImageData id = normCop.getImageData();
//			id.transparentPixel = id.palette.getPixel(new RGB(255,255,255));
//			normCop.dispose();
//			OP_ACTIVE = new Image(NORM_ACTIVE.getDevice(),id);
//			
//			Image pasCop = new Image(NORM_PASSIVE.getDevice(),NORM_PASSIVE,SWT.IMAGE_COPY);
//			GC gc2 = new GC(pasCop);
//			gc2.drawImage(key, 0 , 0);
//			gc2.dispose();
//			OP_PASSIVE = pasCop;
//			
//			Image offlineOp = new Image(NORM_OFFLINE.getDevice(),NORM_OFFLINE,SWT.IMAGE_COPY);
//			GC gc3 = new GC(offlineOp);
//			gc3.drawImage(key, 0 , 0);
//			gc3.dispose();
//			OP_OFFLINE = offlineOp;
			
			key.dispose();
		}
		
		private static Image copyWithKey(Image original,Image addOn) {
			Image normCop = new Image(original.getDevice(),original,SWT.IMAGE_COPY);
			GC gc = new GC(normCop);
			gc.setAdvanced(true);
			gc.setInterpolation(SWT.HIGH);
			gc.setAntialias(SWT.ON);
			gc.drawImage(addOn, 0 , 0);
			gc.dispose();
			ImageData id = normCop.getImageData();
			id.transparentPixel = id.palette.getPixel(new RGB(255,255,255));
			normCop.dispose();
			return new Image(NORM_ACTIVE.getDevice(),id);
		}
		
		public Nick() {
			super(100,Lang.Nick,SWT.LEAD);
		}

		@Override
		public Comparator<IUser> getComparator() {
			 return new Comparator<IUser>(){
					private final Collator mine =Collator.getInstance();
					public int compare(IUser a, IUser b) {
						if (a.isOp() ^ b.isOp()  ){
							return a.isOp()? -1 : 1 ;
						}
						return mine.compare(a.getNick() , b.getNick());
					}
				};
		}
		
		public static Image GetUserImage(IUser usr) {
			if (usr.isOnline()) {
				if (usr.getModechar() == Mode.ACTIVE) {
					if (usr.isOp()) {
						return OP_ACTIVE;
					} else {
						return NORM_ACTIVE;
					}
				} else {
					if (usr.isOp()) {
						return OP_PASSIVE;
					} else {
						return NORM_PASSIVE;	
					}
				}
			} else {
				if (usr.isOp()) {
					return OP_OFFLINE;
				} else {
					return NORM_OFFLINE;
				}
			}
		}
		@Override
		public Image getImage(IUser cur) {
			return GetUserImage(cur);
		}

		@Override
		public String getText(IUser x) {
			return x.getNick();
		}

		@Override
		public Color getForeground(IUser x) {
			
			return super.getForeground(x);
		}
		
		
		
	}
	
	public static class Shared extends UserColumns {
		


		public Shared() {
			super(70,Lang.Shared,SWT.TRAIL);
		}
		
		@Override
		public Comparator<IUser> getComparator() {
			return new Comparator<IUser>(){
				public int compare(IUser a, IUser b) {
					return Long.valueOf(a.getShared()).compareTo(b.getShared());
				}
			};
		}

		@Override
		public String getText(IUser usr) {
			return SizeEnum.getReadableSize(usr.getShared());
		}
	}
	
	public static class Description extends UserColumns {
		
		public Description() {
			super(70,Lang.Description, SWT.LEAD);
		}

		@Override
		public Comparator<IUser> getComparator() {
			return new Comparator<IUser>(){
				private final Collator mine = Collator.getInstance();
				public int compare(IUser a, IUser b){
					return mine.compare(a.getDescription() , b.getDescription());
				}
			};
		}

		@Override
		public String getText(IUser x) {
			return x.getDescription();
		}
		
		
	}
	
	public static class Tag extends UserColumns {
		
		public Tag() {
			super(90,Lang.Tag,SWT.LEAD);
		}


		@Override
		public String getText(IUser x) {
			return x.getTag();
		}
		
	}
	
	public static class Connection extends UserColumns {
		
		public Connection() {
			super(50,Lang.Connection,SWT.LEAD);
		}


		@Override
		public String getText(IUser x) {
			return x.getConnection();
		}
	}
	
	public static class Email extends UserColumns {
		public Email() {
			super(50,Lang.EMail,SWT.LEAD);
		}


		@Override
		public String getText(IUser usr) {
			return usr.getEMail();
		}
		
	}
	
	public static class HubName extends UserColumns {

		public HubName() {
			super(80,Lang.Hub, SWT.LEAD);
		}

		@Override
		public String getText(IUser x) {
			IHub hub = x.getHub();
			if (hub !=null) {
				return hub.getName();
			} else {
				return "";
			}
		}


	}
	
	public static class IPColumn extends UserColumns {

		public IPColumn() {
			super(100, Lang.IP, SWT.LEAD);
		}

		@Override
		public String getText(IUser x) {
			InetAddress a = x.getIp();
			return a == null? "": a.getHostAddress();
		}

	}
	
	public static class LastSeen extends UserColumns {
		
		private final SimpleDateFormat sdf = new SimpleDateFormat();
		
		public LastSeen() {
			super(80,Lang.TimeLastSeen,SWT.LEAD);
		}

		@Override
		public String getText(IUser usr) {
			if (usr.isOnline()) {
				return Lang.Online;
			} else {
				if (usr.getLastseen() != 0) {
					return sdf.format(new Date(usr.getLastseen()));
				} else {
					return "";
				}
			}
		}

		@Override
		public Comparator<IUser> getComparator() {
			return new Comparator<IUser>() {
				public int compare(IUser o1, IUser o2) {
					int i= Boolean.valueOf(o1.isOnline()).compareTo(o2.isOnline());
					if (i != 0) {
						return i;
					} else {
						return Long.valueOf(o1.getLastseen()).compareTo(o2.getLastseen());
					}
				}
				
			};
		}
		
	}
	
/*	public static class IsFavUser extends UserColumns {
		
		public IsFavUser() {
			super(60, Lang.IsFavoriteUser,SWT.LEAD);
		}

		@Override
		public String getText(IUser usr) {
			return (usr.isFavUser() ? Lang.Yes : Lang.No );
		}
		
	} */
	
	public static class SlotUntil extends UserColumns {
		
		public SlotUntil() {
			super(60,Lang.HasExtraSlotUntil,SWT.LEAD);
		}

		@Override
		public Comparator<IUser> getComparator() {
			return new Comparator<IUser>() {
				public int compare(IUser o1, IUser o2) {
					return Long.valueOf(o1.getAutograntSlot()).compareTo(o2.getAutograntSlot());
				}
			};
		}

		@Override
		public String getText(IUser usr) {
			if (usr.hasCurrentlyAutogrant()) {
				if (usr.getAutograntSlot() == IUser.UNTILFOREVER) {
					return SizeEnum.infinity;
				} else {
					SimpleDateFormat format;
					if (usr.getAutograntSlot() < System.currentTimeMillis() + 12L*3600L*1000L) {
						format = new SimpleDateFormat("HH:mm:ss");
					} else {
						format = new SimpleDateFormat("HH:mm EEE, d MMM yyyy");
					}
					return format.format(new Date(usr.getAutograntSlot()));
					
				}
			} else {
				return Lang.No;
			}
		}	
	}
	
}
