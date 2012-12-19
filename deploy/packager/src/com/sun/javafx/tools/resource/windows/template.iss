;This file will be executed next to the application bundle image
;I.e. current directory will contain folder APPLICATION_NAME with application files
[Setup]
AppId={{PRODUCT_APP_IDENTIFIER}}
AppName=APPLICATION_NAME
AppVersion=APPLICATION_VERSION
AppVerName=APPLICATION_NAME APPLICATION_VERSION
AppPublisher=APPLICATION_VENDOR
AppComments=APPLICATION_COMMENTS
AppCopyright=APPLICATION_COPYRIGHT
;AppPublisherURL=http://java.com/
;AppSupportURL=http://java.com/
;AppUpdatesURL=http://java.com/
DefaultDirName=APPLICATION_INSTALL_ROOT\APPLICATION_NAME
DisableStartupPrompt=Yes
DisableDirPage=Yes
DisableProgramGroupPage=Yes
DisableReadyPage=Yes
DisableFinishedPage=Yes
DisableWelcomePage=Yes
DefaultGroupName=APPLICATION_GROUP
;Optional License
LicenseFile=APPLICATION_LICENSE_FILE
;WinXP or above
MinVersion=0,5.1 
OutputBaseFilename=APPLICATION_NAME-APPLICATION_VERSION
Compression=lzma
SolidCompression=yes
PrivilegesRequired=APPLICATION_INSTALL_PRIVILEGE
SetupIconFile=APPLICATION_NAME\APPLICATION_NAME.ico
UninstallDisplayIcon={app}\APPLICATION_NAME.ico
UninstallDisplayName=APPLICATION_NAME
WizardImageStretch=No
WizardSmallImageFile=APPLICATION_NAME-setup-icon.bmp   

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Files]
Source: "APPLICATION_NAME\APPLICATION_NAME.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "APPLICATION_NAME\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{group}\APPLICATION_NAME"; Filename: "{app}\APPLICATION_NAME.exe"; IconFilename: "{app}\APPLICATION_NAME.ico"; Check: APPLICATION_MENU_SHORTCUT()
Name: "{commondesktop}\APPLICATION_NAME"; Filename: "{app}\APPLICATION_NAME.exe";  IconFilename: "{app}\APPLICATION_NAME.ico"; Check: APPLICATION_DESKTOP_SHORTCUT()

[Run]
Filename: "{app}\APPLICATION_NAME.exe"; Description: "{cm:LaunchProgram,APPLICATION_NAME}"; Flags: nowait postinstall skipifsilent

[Code]
function returnTrue(): Boolean;
begin
  Result := True;
end;

function returnFalse(): Boolean;
begin
  Result := False;
end;

function InitializeSetup(): Boolean;
begin
// Possible future improvements:
//   if version less or same => just launch app
//   if upgrade => check if same app is running and wait for it to exit
//   Add pack200/unpack200 support? 
  Result := True;
end;  
