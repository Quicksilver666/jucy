package eu.jucy.language;

import helpers.NLS;





public class LanguageKeys {

	public static String

//	ActionMen, 

//	Address	, 
	AddressCouldNotBeResolved, //in eu.jucy
//	Added	, 
//	AddFolder, 
//	AddToFavorites, //hublist +gui
//	AllXUsersOffline, 
//	All	, 
//	AllowUsingUPnP,
	AtLeast, //eu.jucy
	AtMost,  //eu.jucy
//	AutoConnect	, 
//	AutoGrantSlot, 

	
//	Browse, 
//	BrowseFilelist, 
	
//	ChatOnly, 
//	ChooseFolder, 
	Closed, //eu.jucy
//	CloseConnection	, 
//	Command, //UserCommand pop-up
//	ConfigurePublicHubLists	, //hublist
//	Connect, //hublist+gui
	Connected, //eu.jucy
	Connecting, //eu.jucy
	ConnectingTo, //eu.jucy
//	Connection	, 
//	ConnectionSettings, //unused
	CDCheckForwardedPorts, //eu.jucy
	CDCheckTCP,//eu.jucy
	CDCheckUDP,//eu.jucy
	CDCheckFirewall,//eu.jucy
	CDDeterminedIPOverWeb,//eu.jucy
	CDUPnPNotWorking,//eu.jucy
	CDUPnPNotPresent,//eu.jucy
//	CSToolTip,  //connection status tooltip
//	CSNATDetected,
//	CSNoNATDetected,
//	CSPassiveMode,
//	CSWORKING,
//	CSNOTWORKING,
//	CSUNKNOWN,
	ConnectionTimeout,  //eu.jucy
//	Context,
//	CopyAddressToClipboard, //hublist
//	CopyNickToClipboard, 
//	CopyTTHToClipboard, 

	
//	DefaultAwayMessage, 
//	DefaultDownloadDirectory, 
//	Description	, //TOO many uses.. may need disambiguation
	Disconnected, //eu.jucy
//	Directories ,  //unused
//	Directory	, 
//	DirectoryForLogs, //unused
//	DownloadTo	, 
//	Download, 
//	DownloadAndOpen, //unused
//	Downloads,  //unused
//	Downloaded	, 
//	Downloading	, 
//	DownloadLimit, 
	
//	Edit, //unused
//	EditTheHublist, //unused
//	EMail	, 
//	EnterAddressOfTheHublist, //hublist
//	Errors, 
//	ExactSize, 
//	ExternalWANIP, 
//	ExecuteAfterDownload, 
	Equals, //eu.jucy
	

//	FavHubProperties, 
//	FavoriteHubs, 
//	FavoriteUsers, 
	
//	FileMen	,//name of the FileMenu/main menu  
//	Filename, 
//	Files	, 
//	File, //unused
//	FileListMenu,
//	FileType, 
//	Find, 
	FinishedFilelistRefresh, //eu.jucy
	FilelistRefreshAlreadyInProgress, //eu.jucy
//	FinishedHashingXinY, 
//	ForceAttempt, 
//	SelectFont, 
	
//	GetFilelist	, 
//	GrantExtraSlot, 
	
//	HasExtraSlotUntil, 
//	Help, 

//	Hublist	,  //hublist
//	Hub	, 
//	HubAddedToFavorites, 
//	HubMenu,
//	HubExplaCommands, //hub with some explanation in UserCommandsDialog
//	Hubs, //hublist
//	HubIsFull, //unused
//	HubIsAlreadyFavorite, 
	HubRequestedPassword, //message on $GetPass   //eu.jucy
	
//	Identification, 
//	IncomingConnectionSettings, //unused
//	IP	, 
//	IsFavoriteUser, 
	Idle	, //eu.jucy
	
//	LastChange, //unused
//	Limits, //unused
//	LineSpeed, 
	LoadHublist, //hublist

	LoginTimeout, //eu.jucy
	LoggedIn,  //eu.jucy
//	LogMainchat, 
//	LogPM, 
//	LogFeed, 
	
	MatchedXFilesWithUserY, //eu.jucy
//	MatchQueue, 
//	MaximumSimultaneousDownloads, 
//	MinimizeToTray, 
//	MoveDown, 
//	MoveUp, 
//	MoveRename, 
	

	
//	Name, 
//	NameUnderWhichTheOthersSeeTheDirectory, 
//	New, 
//	Next, 
//	Nick, 
//	No, 
//	NoUsers, 

	
	
//	OK,  //unused
//	Online, 
//	OpenFileList, 
//	OpenOwnList, 
//	OnlyUsersWithFreeSlots, 
//	OnlyWhereIAmOp, 

							
//	Password, 

//	Parameter, //UserCommand dialog
//	Path, 
//	PersonalInformation, //unused
//	PublicHubs, //TODO move to hublist
//	PublicHubsList, //unused
//	Properties, 
//	Priority, 
//	PrimaryDownloadColour, 
//	PrimaryUploadColour, 
	
	
//	QuickConnect, 
	
//	Ratio, //Unused
//	ReAddUserToFile, 
	Reconnecting,  //eu.jucy
	RedirectReceived, //eu.jucy
//	RefreshFilelist, 
//	Remove, 
//	RemoveFromFavorites, 
//	RemoveUserFromDQ, 
//	RemoveUserFromFile, 
//	Rename, 
	
	
//	SearchForAlternates, 
//	Search, 
//	SearchMenu,
//	SearchString, //unused
//	SecondaryDownloadColour, 
//	SecondaryUploadColour, 
//	SendOnceForEachUser,
	SendingPassword, //statusmessage for hub  //eu.jucy
//	Separator,
//	SetPriority	, 
//	SendPrivateMessage, 
//	Shared, 
//	SharedDirectorys, 
//	ShareHiddenFiles, 
//	Sharing, //unused
//	ShowSidebar, 
//	SearchFor, 
//	SearchOptions, 
//	Settings, //unused
//	Server,  //unused
//	Size, 
//	Slots, 
//	Status, 
	
	//ADC status errors  all eu.jucy
	STA11HubFull,
	STA12HubDisabled,
	STA21NickInvalid,
	STA22NickTaken,
	STA23InvalidPassword,
	STA24CIDtaken,
	STA25AccessToCommandDenied,
	STA26RegisteredUsersOnly,
	STA27InvalidPIDSupplied,
	STA31PermanentlyBanned,
	STA32TemporarilyBanned,
	STA41TransferProtocolUnsupported,
	STA42DirectConnectionFailed,
	STA43RequiredINFfieldMissing,
	STA43RequiredINFfieldBad,
	STA44InvalidState,
	STA45RequiredFeatureMissing,
	STA46InvalidIPSupplied,
	STA47NoHashSupportOverlapHub,
	STA51FileNotAvailable,
	STA52FilePartNotAvailable,
	STA53SlotsFull,
	STA54NoHashSupportOverlapClient,
	//ADC status errors end
	
//	Speed, 
//	SystemLog, 
	StartedRefreshingTheFilelist, //eu.jucy
	

	
//	Tag	, 
//	TCPPort, 
//	Time, //time column SearchSpy //unused
//	TimeLastSeen, 
//	TimeLeft, 
//	TotalSizeNoParm, 
	TransferFinished, //eu.jucy
//	TTHRoot, 
//	Type,  //FileType column /ending / Type command or separator
	

	
//	UDPPort, 
//	UnfinishedDownloadsDirectory, 
//	UploadSlots, 
//	UsePassiveMode, 
//	Users, //gui+hublist
//	User, 
//	UserCommand,
//	UserCommands,
	UserConnected, //eu.jucy
	UserDisconnected, //eu.jucy
	UserLeft,  //eu.jucy
//	UserMenu,
//	UserOnline	, 
	UserOffline, //gui + eu.jucy
//	UserDescription	, 
	

	
//	VirtualName, 
//	View, 
	
	WaitingForConnect //eu.jucy
	
//	XOfYUsersOnline, 
//
//	Yes	
	; 
	
	
	static {
		try {
			NLS.load("language", LanguageKeys.class);
		} catch (RuntimeException re) {
			re.printStackTrace();
		}
	}
	
	//private final String defaultTranslation;
	
/*	LanguageKeys() {
		defaultTranslation = Messages.getString(name());
	}
	
	LanguageKeys(String defaultTranslation) {
		this.defaultTranslation = defaultTranslation;
	}
	
	
	public String getTranslation() {
		return defaultTranslation;
	}
	
	public String getFTrans(Object... o) {
		return String.format( getTranslation(), o);
	}

	
	public String toString() {
		return defaultTranslation;
	} */
	
	
	
	
}
