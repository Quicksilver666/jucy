package eu.jucy.gui;

import helpers.GH;
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
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.ui.plugin.AbstractUIPlugin;


import uc.IHasUser;
import uc.IHub;
import uc.IUser;
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
			return Nick.getUserImage(x.getUser(),false);
		}
		
	}

	public static class Nick extends UserColumns {
		
//		private static final Image NORM_ACTIVE;
//		private static final Image NORM_PASSIVE;
//		private static final Image NORM_OFFLINE;
//		
//		private static final Image OP_ACTIVE;
//		private static final Image OP_PASSIVE;
//		private static final Image OP_OFFLINE;
		
		private static final Image[] USERIMAGES = new Image[12];
		
		
		
		static {
			Image norm = AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.USER_ACTIVE2).createImage();
			Image passive = AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.USER_PASSIVE2).createImage();
			Image offline = new Image(null,norm,SWT.IMAGE_GRAY);//AbstractUIPlugin.imageDescriptorFromPlugin(
					//Application.PLUGIN_ID, IImageKeys.USER_OFFLINE2).createImage();
			Image key = AbstractUIPlugin.imageDescriptorFromPlugin(
					Application.PLUGIN_ID, IImageKeys.USER_OPKEY2).createImage();
			

			
			for (int i = 0; i < USERIMAGES.length;i++) {
				Image baseImage = i % 3 == 0? norm: (i% 3 == 1?passive:offline);
				Image keyUsed = i%6 < 3 ?null:key;
				int size =i<6?16:22;
				Image cop = copyWithKey(baseImage,keyUsed, size);
				USERIMAGES[i] = cop;
			}

			
			norm.dispose();
			passive.dispose();
			offline.dispose();
			key.dispose();
		}
		
		public static Image copyWithKey(Image original,Image addOn,int scale) {
			ImageData id;
			Rectangle r = original.getBounds();
			Image normCop = new Image(null,scale,scale);
			GC gc = new GC(normCop);
			gc.setAdvanced(true);
			gc.setInterpolation(SWT.HIGH);
			gc.setAntialias(SWT.ON);
			gc.drawImage(original, 0, 0,r.width,r.height,0,0,scale,scale);
			if (addOn != null) {
				gc.drawImage(addOn, 0 , 0,r.width,r.height,0,0,scale,scale);
			}
			gc.dispose();
			id = normCop.getImageData();
			id = id.scaledTo(scale, scale);
			id.transparentPixel = id.palette.getPixel(new RGB(255,255,255));
			
			normCop.dispose();
			
			return new Image(original.getDevice(),id);
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
		
		public static Image getUserImage(boolean large,boolean online,boolean active,boolean op) {
			int i = large?6:0;
			i += (op?3:0);
			i += (online? (active?0:1) :2) ;
			return USERIMAGES[i];
		}
		
		public static Image getUserImage(IUser usr,boolean large) {
			int i = large?6:0;
			i += (usr.isOp()?3:0);
			i += (usr.isOnline()? (usr.isTCPActive()?0:1) :2) ;
			return USERIMAGES[i];
			
//			if (usr.isOnline()) {
//				if (usr.getModechar() == Mode.ACTIVE) {
//					if (usr.isOp()) {
//						return OP_ACTIVE;
//					} else {
//						return NORM_ACTIVE;
//					}
//				} else {
//					if (usr.isOp()) {
//						return OP_PASSIVE;
//					} else {
//						return NORM_PASSIVE;	
//					}
//				}
//			} else {
//				if (usr.isOp()) {
//					return OP_OFFLINE;
//				} else {
//					return NORM_OFFLINE;
//				}
//			}
		}
		@Override
		public Image getImage(IUser cur) {
			return getUserImage(cur,false);
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


		@Override
		public Comparator<IUser> getComparator() {
			return new Comparator<IUser>() {
				public int compare(IUser o1, IUser o2) {
					return GH.compareTo(o1.getUs(),o2.getUs()) ;
				}
			};
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
			return hub == null? "": hub.getName();
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
