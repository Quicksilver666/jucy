package eu.jucy.gui.transferview;


import java.net.InetAddress;
import java.util.Comparator;

import logger.LoggerFactory;

import helpers.SizeEnum;


import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;

import org.eclipse.swt.graphics.Image;


import org.eclipse.ui.plugin.AbstractUIPlugin;

import eu.jucy.gui.Application;
import eu.jucy.gui.GuiHelpers;
import eu.jucy.gui.IImageKeys;
import eu.jucy.gui.Lang;


import uc.IUser;
import uc.files.transfer.FileTransferInformation;
import uc.files.transfer.IFileTransfer;
import uc.protocols.client.ClientProtocolStateMachine;
import uc.protocols.client.ClientProtocol;
import uihelpers.TableViewerAdministrator.ColumnDescriptor;

public abstract class TransferColumns extends ColumnDescriptor<Object> {

	private static final Logger logger = LoggerFactory.make(); 
	
	
	
	public TransferColumns(int defaultColumnSize, String columnName, int style) {
		super(defaultColumnSize, columnName, style);
	}
	

	@Override
	public String getText(Object o) {
		if (o instanceof ClientProtocol) {
			ClientProtocol cp = (ClientProtocol)o;
			IFileTransfer ft = cp.getFileTransfer();
			IUser other = cp.getUser();
			
			return getText(cp,ft,other);
		} 
		if (o instanceof ClientProtocolStateMachine ) {
			ClientProtocolStateMachine ccspm =  (ClientProtocolStateMachine)o;
			return getText(ccspm, ccspm.getUser(), ccspm.getLastDownload());
		}
		throw new IllegalStateException();
	}

	/**
	 * retrieves text for a Connection protocol item
	 * 
	 * @param cp - the Protocol itself
	 * @param ft - the FileTransfer may be null if not already started
	 * @param other - the user  never null
	 * @return a String for the Column
	 */
	protected abstract String getText(ClientProtocol cp,IFileTransfer ft, IUser other);


	protected abstract String getText(ClientProtocolStateMachine ccspm, IUser usr, FileTransferInformation last);

	public static class UserColumn extends TransferColumns {

		public static final Image 	
		
		ENC_ICON			=	AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.ENCRYPTED).createImage(),
		ENCKEYP_ICON =   AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.ENCRYPTEDKEYP).createImage(),
		
		UPLOAD_ICON		=	AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.UPLOAD_ICON).createImage(),
		UPLOAD_ENC_ICON =  GuiHelpers.addCornerIcon(UPLOAD_ICON,ENC_ICON)	,
		UPLOAD_KEYP_ICON = GuiHelpers.addCornerIcon(UPLOAD_ICON, ENCKEYP_ICON),

						
		DOWNLOAD_ICON 	=	AbstractUIPlugin.imageDescriptorFromPlugin(
				Application.PLUGIN_ID, IImageKeys.DOWNLOAD_ICON).createImage(),
		DOWNLOAD_ENC_ICON =	GuiHelpers.addCornerIcon(DOWNLOAD_ICON ,ENC_ICON),
		DOWNLOAD_KEYP_ICON = GuiHelpers.addCornerIcon(DOWNLOAD_ICON ,ENCKEYP_ICON)
		;
		
		private static final Image[] ALL = new Image[]{null,ENC_ICON,ENCKEYP_ICON,
			UPLOAD_ICON,UPLOAD_ENC_ICON,UPLOAD_KEYP_ICON,
			DOWNLOAD_ICON,DOWNLOAD_ENC_ICON,DOWNLOAD_KEYP_ICON};
		
			//AbstractUIPlugin.imageDescriptorFromPlugin(
			//	Application.PLUGIN_ID, IImageKeys.DOWNLOAD_ENC_ICON).createImage();
		
		
		
		
		public UserColumn() {
			super(120, Lang.User, SWT.LEAD);
		}

		@Override
		protected String getText(ClientProtocol cp, IFileTransfer ft, IUser other) {
			return other != null ? other.getNick(): cp.getOtherip().getHostAddress();
		}

		@Override
		public Image getImage(Object o) {
			if (o instanceof ClientProtocol) {
				ClientProtocol cp = (ClientProtocol)o;
				IFileTransfer ft = cp.getFileTransfer();
				int i = (cp.isEncrypted()?1:0)   + (cp.isFingerPrintUsed()?1:0);
				i +=  ft == null? 0: (ft.isUpload()?3:6 );
				return ALL[i];
//				
//				if (ft == null) {
//					return cp.isEncrypted()? (cp.isFingerPrintUsed()?ENCKEYP_ICON:ENC_ICON): null;
//				} else if (ft.isUpload()){
//					if (cp.isEncrypted()) {
//						return  cp.isFingerPrintUsed() ? UPLOAD_KEYP_ICON:UPLOAD_ENC_ICON ;
//					} else {
//						return UPLOAD_ICON;
//					}
//				} else {
//					if (cp.isEncrypted()) {
//						return  cp.isFingerPrintUsed() ?  DOWNLOAD_KEYP_ICON: DOWNLOAD_ENC_ICON ;
//					} else {
//						return DOWNLOAD_ICON;
//					}
//				}
			} else {
				return DOWNLOAD_ICON;
			}
		}

		@Override
		protected String getText(ClientProtocolStateMachine ccspm, IUser usr, FileTransferInformation last) {
			return usr.getNick();
		}

	}
	
	
	public static class HubColumn extends TransferColumns {

		public HubColumn() {
			super(100, Lang.Hub, SWT.LEAD);
		}

		@Override
		protected String getText(ClientProtocol cp, IFileTransfer ft, IUser other) {
			return other != null && other.getHub()!= null ? other.getHub().getName(): "";
		}

		@Override
		protected String getText(ClientProtocolStateMachine ccspm,IUser usr, FileTransferInformation last) {
			return getText(null, null, usr);
		}
		
	}
	
	/**
	 * more or less just a place holder .. -> a widget is added
	 * to each table item to draw the bar..
	 * @author Quicksilver
	 *
	 */
	public static class StatusColumn extends TransferColumns {

		public StatusColumn() {
			super(250, Lang.Status, SWT.LEAD);
		}

		@Override
		protected String getText(ClientProtocol cp, IFileTransfer ft, IUser other) {
			return ""; //nothing as the StatusString is painted..
		}

		@Override
		protected String getText(ClientProtocolStateMachine ccspm,IUser usr, FileTransferInformation last) {
			return "";//nothing as the DownloadString is painted..
		}

		@Override
		public Comparator<Object> getComparator() {
			return getComp(false);
		}

		@Override
		public Comparator<Object> getReverseComparator() {
			return getComp(true);
		}
		
		private Comparator<Object> getComp(final boolean reverse) {
			return new Comparator<Object>() {

				public int compare(Object o1, Object o2) {

					if (o1 instanceof ClientProtocol && o2 instanceof ClientProtocol) {
						IFileTransfer ft1 = ((ClientProtocol)o1).getFileTransfer();
						IFileTransfer ft2 = ((ClientProtocol)o2).getFileTransfer();
			
						if (ft1 != null && ft2 != null) {
							int comp = (reverse?-1:1) * Boolean.valueOf(ft1.isUpload()).compareTo(ft2.isUpload());
							if (comp == 0) {
								comp = ft1.getOther().getNick().compareTo(ft2.getOther().getNick()); //compare nick  so sorting is stable..
							}
							return comp ; 
						} else {
							
							return -Boolean.valueOf(ft1 != null).compareTo(ft2 != null); 
						}
					}
					if (o1 instanceof ClientProtocol) {
						logger.debug("comparing 1 Filetransfers"+o1.getClass()+" "+o2.getClass());
						return -1;
					}
					if (o2 instanceof ClientProtocol) {
						logger.debug("comparing 1 Filetransfers"+o1.getClass()+" "+o2.getClass());
						return 1;
					}
					
					logger.debug("comparing  "+o1.getClass()+" "+o2.getClass()); 
					
					return 0;
				}
			};
		}
		
	} 
	
	
	public static class TimeLeftColumn extends TransferColumns {

		public TimeLeftColumn() {
			super(60, Lang.TimeLeft, SWT.TRAIL);
		}

		@Override
		public String getText(ClientProtocol cp, IFileTransfer ft, IUser other) {
			return ft != null ? SizeEnum.timeEstimation( ft.getTimeRemaining() ): ""; 
		}

		@Override
		protected String getText(ClientProtocolStateMachine ccspm,IUser usr, FileTransferInformation last) {
			return "";
		}
		
	}
	
	public static class SpeedColumn extends TransferColumns {

		public SpeedColumn() {
			super(70, Lang.Speed, SWT.TRAIL);
		}

		@Override
		protected String getText(ClientProtocol cp, IFileTransfer ft, IUser other) {
			return ft != null ? SizeEnum.toSpeedString(1000,ft.getSpeed() ): "";
		}

		@Override
		protected String getText(ClientProtocolStateMachine ccspm,IUser usr, FileTransferInformation last) {
			return "";
		}
		
	}
	
	public static class FilenameColumn extends TransferColumns {

		public FilenameColumn() {
			super(180, Lang.Filename, SWT.LEAD);
		}

		@Override
		protected String getText(ClientProtocol cp, IFileTransfer ft, IUser other) {
			return ft != null ? ft.getNameOfTransferred(): ""; // Filename
		}

		@Override
		protected String getText(ClientProtocolStateMachine ccspm,IUser usr, FileTransferInformation last) {
			if (last != null) {
				return last.getNameOfTransferred();
			} else {
				return "";
			}
		}
		
		
	}
	
	public static class SizeColumn extends TransferColumns {

		public SizeColumn() {
			super(70, Lang.Size, SWT.TRAIL);
		}

		@Override
		protected String getText(ClientProtocol cp, IFileTransfer ft, IUser other) {
			return ft != null ? SizeEnum.getReadableSize(ft.getFileInterval().length()): ""; 
		}

		@Override
		protected String getText(ClientProtocolStateMachine ccspm,
				IUser usr, FileTransferInformation last) {
			if (last != null) {
				return SizeEnum.getReadableSize(last.getLength());
			} else {
				return "";
			}
		}
	}
	
	
	public static class IPColumn extends TransferColumns {

		public IPColumn() {
			super(100, Lang.IP, SWT.LEAD);
		}

		@Override
		protected String getText(ClientProtocol cp, IFileTransfer ft, IUser other) {
			InetAddress ia =cp.getOtherip();
			return ia != null? ia.getHostAddress(): "" ;
		}

		@Override
		protected String getText(ClientProtocolStateMachine ccspm,
				IUser usr, FileTransferInformation last) {
			InetAddress ia = usr.getIp();
			return ia != null? ia.getHostAddress(): "" ;
		}
	}
	
	
	
	
}
