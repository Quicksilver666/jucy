package eu.jucy.gui;

import uc.DCClient;
import helpers.NLS;





public class Lang {
	
/*	private  static String getString(String key) {
		try {
			return Lang.RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			try {
				return ResourceBundle
				.getBundle(Lang.BUNDLE_NAME,Locale.ENGLISH).getString(key);
			} catch(MissingResourceException e2) {
				return '!' + key + '!';
			}
		}
	} */

//	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
//	.getBundle(Lang.BUNDLE_NAME);
	//private static final String BUNDLE_NAME = "gui"; //$NON-NLS-1$
	
	
	public static String
	
	ActiveModeSettings ,//= Lang.getString("ActiveModeSettings"), //$NON-NLS-1$
	AddedFileViaMagnet,
	AddSoundNotificationToPM ,//= Lang.getString("AddSoundNotificationToPM"), //$NON-NLS-1$
	AddSoundNotificationToChatroom ,//= Lang.getString("AddSoundNotificationToChatroom"), //$NON-NLS-1$
	AddSoundOnNickInMainchat ,//= Lang.getString("AddSoundOnNickInMainchat"), //$NON-NLS-1$
	Advanced 		,//= Lang.getString("Advanced"), //$NON-NLS-1$
	AdvancedFavHubProperties ,//=Lang.getString("AdvancedFavHubProperties"), //$NON-NLS-1$
	AreYouSureYouWantToCloseJucy,//= Lang.getString("AreYouSureYouWantToCloseJucy"), //$NON-NLS-1$
	AskBeforeShutdown,//= Lang.getString("AskBeforeShutdown"), //$NON-NLS-1$
	AverageSpeed	,//= Lang.getString("AverageSpeed"),//$NON-NLS-1$
	AutomaticallyCheckForUpdates ,//= Lang.getString("AutomaticallyCheckForUpdates"),//$NON-NLS-1$
	AutomaticallyFollowRedirects ,//= Lang.getString("AutomaticallyFollowRedirects"),//$NON-NLS-1$
	AutomaticallySearchForAlternates ,//= Lang.getString("AutomaticallySearchForAlternates"),//$NON-NLS-1$
	
	CopyMagnetLinkToClipboard,
	
	DifferentFiles	,//= Lang.getString("DifferentFiles"),//$NON-NLS-1$
	DeleteLog		,//= Lang.getString("DeleteLog"),//$NON-NLS-1$
	DownloadQueue	,//= Lang.getString("DownloadQueue"),//$NON-NLS-1$
	Duration		,//= Lang.getString("Duration"),//$NON-NLS-1$
	
	EnterSearch,
	ExcludedFiles	,//= Lang.getString("ExcludedFiles"),//$NON-NLS-1$
//	ExportAllLogs	,
	ExportLog 		,//= Lang.getString("ExportLog"),//$NON-NLS-1$
	
	
	FavouritesNicks	,//= Lang.getString("FavouritesNicks"), //$NON-NLS-1$
	FileCol			,//= Lang.getString("FileCol"),//$NON-NLS-1$
	FileInDownloadCol,
	FileInShareCol,
	FileMultiUserCol,
	FileDefaultCol,
	FilelistRefreshInterval ,//= Lang.getString("FilelistRefreshInterval"), //$NON-NLS-1$
	FilesLeft,
	Finished		,//= Lang.getString("Finished"),//$NON-NLS-1$
	FinishedDownloads ,//=Lang.getString("FinishedDownloads"), //$NON-NLS-1$
	FinishedUploads	,//= Lang.getString("FinishedUploads"), //$NON-NLS-1$
	FirstRequest	,//= Lang.getString("FirstRequest"), //$NON-NLS-1$
	FontColour ,//= Lang.getString("FontColour"),//$NON-NLS-1$
	
	IncludedFiles	,//= Lang.getString("IncludedFiles"),//$NON-NLS-1$
	ImportFromDCPPDescription,//= Lang.getString("ImportFromDCPPDescription"),//$NON-NLS-1$
	
	LastRequest		,//= Lang.getString("LastRequest"), //$NON-NLS-1$
	
	
	
	MaxHashSpeed		,//= Lang.getString("MaxHashSpeed"), //$NON-NLS-1$
	
	No				,//= Lang.getString("No"), //$NON-NLS-1$
	NormalNicks		,//= Lang.getString("NormalNicks"), //$NON-NLS-1$
	
	OwnNick			,//= Lang.getString("OwnNick"), //$NON-NLS-1$
	OperatorNicks	,//= Lang.getString("OperatorNicks"), //$NON-NLS-1$
	OverrideProtocolCodepage ,//= Lang.getString("OverrideProtocolCodepage"), //$NON-NLS-1$
	OpenPMinForeground	,//= Lang.getString("OpenPMinForeground"), //$NON-NLS-1$
	
	PopupDissappearanceTime,
	PruneAllOlderThan ,//= Lang.getString("PruneAllOlderThan"), //$NON-NLS-1$
	
	
	Question 		,//= Lang.getString("Question"), //$NON-NLS-1$
	
	Ratio			,//= Lang.getString("Ratio"),
	RequestsReceived,//= Lang.getString("RequestsReceived"), //$NON-NLS-1$
	ReceivedResults ,//= Lang.getString("ReceivedResults"), //$NON-NLS-1$
	
	Started			,//= Lang.getString("Started") ,//$NON-NLS-1$
	SlotReceived	,//= Lang.getString("SlotReceived") ,//$NON-NLS-1$
	SizeLeft,

	ShowSharesize	,//= Lang.getString("ShowSharesize"), //$NON-NLS-1$
	ShowHubs		,//= Lang.getString("ShowHubs"), //$NON-NLS-1$
	ShowSlots		,//= Lang.getString("ShowSlots"), //$NON-NLS-1$
	ShowDownTotal	,//= Lang.getString("ShowDownTotal"), //$NON-NLS-1$
	ShowUpTotal		,//= Lang.getString("ShowUpTotal"), //$NON-NLS-1$
	ShowDownSpeed	,//= Lang.getString("ShowDownSpeed"), //$NON-NLS-1$
	ShowUpSpeed		,//= Lang.getString("ShowUpSpeed"), //$NON-NLS-1$
	ShowConnStatus	,//= Lang.getString("ShowConnStatus"), //$NON-NLS-1$
	ShowPMInMainchat,
	
	ShowPOPUPonPM 	,//= Lang.getString("ShowPOPUPonPM"), //$NON-NLS-1$
	ShowPOPUPonChatroom 	,//= Lang.getString("ShowPOPUPonChatroom"), //$NON-NLS-1$
	ShowPOPUPonNickinMC 	,//= Lang.getString("ShowPOPUPonNickinMC"), //$NON-NLS-1$
	
	
	TimeStamps		,//= Lang.getString("TimeStamps"),//$NON-NLS-1$
	TLSTCPPort		,//= Lang.getString("TLSTCPPort"),//$NON-NLS-1$
	TotalFiles		,//= Lang.getString("TotalFiles"),//$NON-NLS-1$
	TotalSize		,//= Lang.getString("TotalSize"),//$NON-NLS-1$
	TotalSizeLeft,
	TotalTimeLeft,
	TotalDownloaded	,//= Lang.getString("TotalDownloaded"),//$NON-NLS-1$
	TotalElements	,//= Lang.getString("TotalElements"),//$NON-NLS-1$
	TotalMessages	,//= Lang.getString("TotalMessages"),//$NON-NLS-1$
	
	Unlimited,
	UseAlternativeTabs,//= Lang.getString("UseAlternativeTabs"),//$NON-NLS-1$
	UseTLSIfPossible,//= Lang.getString("UseTLSIfPossible"),//$NON-NLS-1$
	UploadLimit 	,//= Lang.getString("UploadLimit"),//$NON-NLS-1$
	UploadQueue		,//= Lang.getString("UploadQueue"),//$NON-NLS-1$
	UpdateAvailable ,//= Lang.getString("UpdateAvailable"),//$NON-NLS-1$
	UpdateAvailableMessage ,//= Lang.getString("UpdateAvailableMessage"),//$NON-NLS-1$
	URLColour,	//$NON-NLS-1$
	URLFont,
//	URLUnderline,
	
	WindowBackgroundColour ,//= Lang.getString("WindowBackgroundColour"),//$NON-NLS-1$
	
	
	Yes				,// ,//= Lang.getString("Yes");//$NON-NLS-1$

	
	// Start old stuff from LanguageKeys
	ActionMen, 
	Address	, 
	Added	, 
	AddFolder, 
	AddToFavorites, //hublist +gui
	AllXUsersOffline, 
	All	, 
	AllowUsingUPnP,
	AutoConnect	, 
	AutoGrantSlot, 

	
	Browse, 
	BrowseFilelist, 
	
	ChatOnly, 
	ChooseFolder, 
	CloseConnection	, 
	Command, //UserCommand pop-up
	ConfigurePublicHubLists	, //hublist
	Connect, //hublist+gui
	Connection	, 
	ConnectionSettings, //unused
	Context,
	CopyAddressToClipboard, //hublist
	CopyNickToClipboard, 
	CopyTTHToClipboard, 
	
	CSNATDetected,
	CSNoNATDetected,
	CSPassiveMode,
	CSWORKING,
	CSNOTWORKING,
	CSUNKNOWN,
	CSToolTip,  //connection status tooltip

	
	DefaultAwayMessage, 
	DefaultDownloadDirectory, 
	Description	, //TOO many uses.. may need disambiguation
	Directory	, 
	DownloadTo	, 
	Download, 
	Downloaded	, 
	Downloading	, 
	DownloadLimit, 
	

	EMail	, 
	EnterAddressOfTheHublist, //hublist
	Errors, 
	ExactSize, 
	ExternalWANIP, 
	ExecuteAfterDownload, 
	

	FavHubProperties, 
	FavoriteHubs, 
	FavoriteUsers, 
	
	FileMen	,//name of the FileMenu/main menu  
	Filename, 
	Files	, 
	FileListMenu,
	FileType, 
	Find, 
	FinishedHashingXinY, 
	ForceAttempt, 
	SelectFont, 
	
	GetFilelist	, 
	GrantExtraSlot, 
	
	HasExtraSlotUntil, 
	Help, 
	High,   //priority in DQ, 
	Highest,  //priority in DQ, 
	Hublist	,  //hublist
	Hub	, 
	HubAddedToFavorites, 
	HubMenu,
	HubExplaCommands, //hub with some explanation in UserCommandsDialog
	Hubs, //hublist
	HubIsAlreadyFavorite, 
	
	Identification, 
	IncomingConnectionSettings, //unused
	IP	, 

	

	LineSpeed, 
	LoadHublist, //hublist
	LogMainchat, 
	LogPM, 
	LogFeed, 
	Low	, //Priority in DQ  //eu.jucy
	Lowest, //Priority in DQ //eu.jucy
	
	MatchQueue, 
	MaximumSimultaneousDownloads, 
	MinimizeToTray, 
	MoveDown, 
	MoveUp, 
	MoveRename, 
	

	
	Name, 
	NameUnderWhichTheOthersSeeTheDirectory, 
	New, 
	Next, 
	Nick, 
	Normal, //Priorities
	NoUsers, 
	
	

	Online, 
	OpenFileList, 
	OpenOwnList, 
	OnlyUsersWithFreeSlots, 
	OnlyWhereIAmOp, 

							
	Password, 
	Parameter, //UserCommand dialog
	Path, 
	Paused	,//priority in DQ //eu.jucy
	PublicHubs, //TODO move to hublist
	Properties, 
	Priority, 
	PrimaryDownloadColour, 
	PrimaryUploadColour, 
	
	
	QuickConnect, 
	

	ReAddUserToFile, 
	RefreshFilelist, 
	Remove, 
	RemoveFromFavorites, 
	RemoveUserFromDQ, 
	RemoveUserFromFile, 
	Rename, 
	
	
	SearchForAlternates, 
	Search, 
	SearchMenu,
	SecondaryDownloadColour, 
	SecondaryUploadColour, 
	SendOnceForEachUser,
	Separator,
	SetPriority	, 
	SendPrivateMessage, 
	Shared, 
	SharedDirectorys, 
	ShareHiddenFiles, 
	ShowSidebar, 
	SearchFor, 
	SearchOptions, 
	Size, 
	Slots, 
	Status, 
	Speed, 
	SystemLog, 

	

	
	Tag	, 
	TCPPort, 
	TimeLastSeen, 
	TimeLeft, 
	TotalSizeNoParm, 
	TTHRoot, 
	Type,  //FileType column /ending / Type command or separator
	

	
	UDPPort, 
	UnfinishedDownloadsDirectory, 
	UploadSlots, 
	UsePassiveMode, 
	Users, //gui+hublist
	User, 
	UserCommand,
	UserCommands,
	UserMenu,
	UserOnline	, 
	UserDescription	, 
	

	
	VirtualName, 
	View, 
	

	
	XOfYUsersOnline; 

	static {
		try {
			NLS.load("gui", Lang.class);
		} catch(RuntimeException re) {
			DCClient.logger.warn(re,re);
		}
	}
	

}
