; Script generated by the HM NIS Edit Script Wizard.

; HM NIS Edit Wizard helper defines
!define PRODUCT_NAME "ThreatFactor NSIA"
!define PRODUCT_VERSION "0.8.99"
!define PRODUCT_PUBLISHER "ThreatFactor"
!define PRODUCT_WEB_SITE "http://ThreatFactor.com"

!define PRODUCT_DIR_REGKEY "Software\Microsoft\Windows\CurrentVersion\App Paths\bin\ThreatFactor NSIA.exe"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"
!define PRODUCT_UNINST_ROOT_KEY "HKLM"

; MUI 1.67 compatible ------
!include "MUI.nsh"

; Set the path where the files are located (dependent on the location of the project)
!define ROOT_PATH "../.."

; MUI Settings
!define MUI_ABORTWARNING
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\orange-install.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\orange-uninstall.ico"

; MUI Settings / Header
!define MUI_HEADERIMAGE
!define MUI_HEADERIMAGE_RIGHT
!define MUI_HEADERIMAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Header\orange-r.bmp"
!define MUI_HEADERIMAGE_UNBITMAP "${NSISDIR}\Contrib\Graphics\Header\orange-uninstall-r.bmp"

; MUI Settings / Wizard
!define MUI_WELCOMEFINISHPAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Wizard\orange.bmp"
!define MUI_UNWELCOMEFINISHPAGE_BITMAP "${NSISDIR}\Contrib\Graphics\Wizard\orange-uninstall.bmp"



; Welcome page
!insertmacro MUI_PAGE_WELCOME

; License page
!insertmacro MUI_PAGE_LICENSE "${ROOT_PATH}\doc\License.rtf"

; Directory page
!insertmacro MUI_PAGE_DIRECTORY

; Page custom DatabaseInitForm

; Finish page with option to start service
; Page custom RunNSIAForm RunNSIAFormLeave

; The dialog to get the username and login
Page custom LoginPasswordForm LoginPasswordFormLeave

; Instfiles page
!insertmacro MUI_PAGE_INSTFILES

; Finish page
;!define MUI_FINISHPAGE_RUN_TEXT " has been successfully installed on your computer.\n Click finish to close the wizard and open a browser window to the management interface."
;!define MUI_FINISHPAGE_RUN_TEXT " has been successfully installed on your computer. If you want NSIA to run when the computer starts up, start the NSIA service from services.msc\n Click finish to close the wizard."
;!define MUI_FINISHPAGE_RUN_FUNCTION "OpenBrowserToNSIA"
!define MUI_FINISHPAGE_RUN "$INSTDIR\bin\ThreatFactor NSIA.exe"
!insertmacro MUI_PAGE_FINISH

; Uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

; Language files
!insertmacro MUI_LANGUAGE "English"

; MUI end ------

Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "../../bin/ThreatFactor NSIA Setup.exe"
InstallDir "$PROGRAMFILES\ThreatFactor NSIA"
InstallDirRegKey HKLM "${PRODUCT_DIR_REGKEY}" ""
ShowInstDetails show
ShowUnInstDetails show

Var JREPATH

Var PASSWORDTEXT
Var LOGINNAMETEXT

Section "MainSection" SEC01
  ; Check for a valid JRE first and try to install it
  Call GetJRE
  Pop $JREPATH
  
  SetOverwrite try
  CreateDirectory "$INSTDIR\var"
  SetOutPath "$INSTDIR\lib"
  File "${ROOT_PATH}\lib\chardet.jar"
  File "${ROOT_PATH}\lib\commons-codec-1.3.jar"
  File "${ROOT_PATH}\lib\commons-dbcp-1.2.1.jar"
  File "${ROOT_PATH}\lib\commons-fileupload-1.2.jar"
  File "${ROOT_PATH}\lib\commons-httpclient-3.0.1.jar"
  File "${ROOT_PATH}\lib\commons-lang-2.3.jar"
  File "${ROOT_PATH}\lib\commons-logging-api.jar"
  File "${ROOT_PATH}\lib\commons-logging.jar"
  File "${ROOT_PATH}\lib\commons-pool-1.3.jar"
  File "${ROOT_PATH}\lib\derby.jar"
  File "${ROOT_PATH}\lib\derby.war"
  File "${ROOT_PATH}\lib\derbyclient.jar"
  File "${ROOT_PATH}\lib\derbyLocale_cs.jar"
  File "${ROOT_PATH}\lib\derbyLocale_de_DE.jar"
  File "${ROOT_PATH}\lib\derbyLocale_es.jar"
  File "${ROOT_PATH}\lib\derbyLocale_fr.jar"
  File "${ROOT_PATH}\lib\derbyLocale_hu.jar"
  File "${ROOT_PATH}\lib\derbyLocale_it.jar"
  File "${ROOT_PATH}\lib\derbyLocale_ja_JP.jar"
  File "${ROOT_PATH}\lib\derbyLocale_ko_KR.jar"
  File "${ROOT_PATH}\lib\derbyLocale_pl.jar"
  File "${ROOT_PATH}\lib\derbyLocale_pt_BR.jar"
  File "${ROOT_PATH}\lib\derbyLocale_ru.jar"
  File "${ROOT_PATH}\lib\derbyLocale_zh_CN.jar"
  File "${ROOT_PATH}\lib\derbyLocale_zh_TW.jar"
  File "${ROOT_PATH}\lib\derbynet.jar"
  File "${ROOT_PATH}\lib\derbyrun.jar"
  File "${ROOT_PATH}\lib\derbytools.jar"
  File "${ROOT_PATH}\lib\filterbuilder.jar"
  File "${ROOT_PATH}\lib\freemarker.jar"
  File "${ROOT_PATH}\lib\gnujaxp.jar"
  File "${ROOT_PATH}\lib\htmllexer.jar"
  File "${ROOT_PATH}\lib\htmlparser.jar"
  File "${ROOT_PATH}\lib\jasper-compiler-jdt.jar"
  File "${ROOT_PATH}\lib\jasper-compiler.jar"
  File "${ROOT_PATH}\lib\jasper-runtime.jar"
  File "${ROOT_PATH}\lib\java-diff-1.0.5.jar"
  File "${ROOT_PATH}\lib\jcommon-1.0.0.jar"
  File "${ROOT_PATH}\lib\jfreechart-1.0.1.jar"
  File "${ROOT_PATH}\lib\JSAP-2.0b.jar"
  File "${ROOT_PATH}\lib\jsdk-24.jar"
  File "${ROOT_PATH}\lib\log4j-1.2.14.jar"
  File "${ROOT_PATH}\lib\mail.jar"
  File "${ROOT_PATH}\lib\mime-util.jar"
  File "${ROOT_PATH}\lib\mina-core-1.1.5.jar"
  File "${ROOT_PATH}\lib\org.mortbay.jetty.jar"
  File "${ROOT_PATH}\lib\slf4j-api-1.4.3.jar"
  File "${ROOT_PATH}\lib\slf4j-log4j12-1.4.3.jar"
  File "${ROOT_PATH}\lib\webConsole.war"
  File "${ROOT_PATH}\lib\xmlrpc-2.0.jar"
  File "${ROOT_PATH}\lib\trilead-ssh2-build213.jar"
  File "${ROOT_PATH}\lib\swt.jar"
  SetOutPath "$INSTDIR\etc"
  File "${ROOT_PATH}\etc\mime.types"
  File "${ROOT_PATH}\etc\config.ini"
  SetOutPath "$INSTDIR\bin"
  File "${ROOT_PATH}\bin\nsia.jar"
  File "${ROOT_PATH}\bin\ThreatFactor NSIA.exe"
  File "${ROOT_PATH}\bin\ThreatFactor NSIA CLI.exe"
  File "${ROOT_PATH}\bin\ThreatFactor NSIA Service.exe"
  CreateDirectory "$SMPROGRAMS\ThreatFactor"
  CreateShortCut "$SMPROGRAMS\ThreatFactor\ThreatFactor NSIA.lnk" "$INSTDIR\bin\ThreatFactor NSIA.exe"
  CreateShortCut "$SMPROGRAMS\ThreatFactor\ThreatFactor NSIA (with interactive console).lnk" "$INSTDIR\bin\ThreatFactor NSIA CLI.exe"
  CreateShortCut "$DESKTOP\ThreatFactor NSIA.lnk" "$INSTDIR\bin\ThreatFactor NSIA.exe"
  CreateShortCut "$INSTDIR\ThreatFactor NSIA.lnk" "$INSTDIR\bin\ThreatFactor NSIA.exe"
  CreateShortCut "$INSTDIR\ThreatFactor NSIA (with interactive console).lnk" "$INSTDIR\bin\ThreatFactor NSIA CLI.exe"
  
  ; Install the service
  ; In regards to the fourth argument: 3 is for manual start, 2 is for automatic start
  nsSCM::Install "NSIA" "ThreatFactor NSIA" 16 3 "$INSTDIR\bin\ThreatFactor NSIA Service.exe" "" "Tcpip" "" ""

  ; Finalize the installer
  Call CompleteInstall
  ; nsSCM::Start "NSIA"
SectionEnd

Section -AdditionalIcons
  WriteIniStr "$INSTDIR\${PRODUCT_NAME}.url" "InternetShortcut" "URL" "${PRODUCT_WEB_SITE}"
  CreateShortCut "$SMPROGRAMS\ThreatFactor\Website.lnk" "$INSTDIR\${PRODUCT_NAME}.url"
  CreateShortCut "$SMPROGRAMS\ThreatFactor\Uninstall.lnk" "$INSTDIR\uninst.exe"
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\bin\ThreatFactor NSIA.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\bin\ThreatFactor NSIA.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "Publisher" "${PRODUCT_PUBLISHER}"
SectionEnd


Function un.onUninstSuccess
  HideWindow
  IfSilent +2
  MessageBox MB_ICONINFORMATION|MB_OK "$(^Name) was successfully removed from your computer."
FunctionEnd

Function un.onInit
  IfSilent +2
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Are you sure you want to completely remove $(^Name) and all of its components?" IDYES +2
  Abort
FunctionEnd

Section Uninstall
  ; Stop and remove the service
  nsSCM::Stop "NSIA"
  nsSCM::Remove "NSIA"

  Delete "$INSTDIR\${PRODUCT_NAME}.url"
  Delete "$INSTDIR\uninst.exe"
  Delete "$INSTDIR\etc\config.ini"
  Delete "$INSTDIR\bin\ThreatFactor NSIA Service.exe"
  Delete "$INSTDIR\bin\ThreatFactor NSIA.exe"
  Delete "$INSTDIR\bin\ThreatFactor NSIA CLI.exe"
  Delete "$INSTDIR\bin\nsia.jar"
  Delete "$INSTDIR\etc\mime.types"
  Delete "$INSTDIR\lib\swt.jar"
  Delete "$INSTDIR\lib\trilead-ssh2-build213.jar"
  Delete "$INSTDIR\lib\xmlrpc-2.0.jar"
  Delete "$INSTDIR\lib\webConsole.war"
  Delete "$INSTDIR\lib\slf4j-log4j12-1.4.3.jar"
  Delete "$INSTDIR\lib\slf4j-api-1.4.3.jar"
  Delete "$INSTDIR\lib\org.mortbay.jetty.jar"
  Delete "$INSTDIR\lib\mina-core-1.1.5.jar"
  Delete "$INSTDIR\lib\mime-util.jar"
  Delete "$INSTDIR\lib\mail.jar"
  Delete "$INSTDIR\lib\log4j-1.2.14.jar"
  Delete "$INSTDIR\lib\jsdk-24.jar"
  Delete "$INSTDIR\lib\JSAP-2.0b.jar"
  Delete "$INSTDIR\lib\jfreechart-1.0.1.jar"
  Delete "$INSTDIR\lib\jcommon-1.0.0.jar"
  Delete "$INSTDIR\lib\java-diff-1.0.5.jar"
  Delete "$INSTDIR\lib\jasper-runtime.jar"
  Delete "$INSTDIR\lib\jasper-compiler.jar"
  Delete "$INSTDIR\lib\jasper-compiler-jdt.jar"
  Delete "$INSTDIR\lib\htmlparser.jar"
  Delete "$INSTDIR\lib\htmllexer.jar"
  Delete "$INSTDIR\lib\gnujaxp.jar"
  Delete "$INSTDIR\lib\freemarker.jar"
  Delete "$INSTDIR\lib\filterbuilder.jar"
  Delete "$INSTDIR\lib\derbytools.jar"
  Delete "$INSTDIR\lib\derbynet.jar"
  Delete "$INSTDIR\lib\derbyLocale_zh_TW.jar"
  Delete "$INSTDIR\lib\derbyLocale_zh_CN.jar"
  Delete "$INSTDIR\lib\derbyLocale_ru.jar"
  Delete "$INSTDIR\lib\derbyLocale_pt_BR.jar"
  Delete "$INSTDIR\lib\derbyLocale_pl.jar"
  Delete "$INSTDIR\lib\derbyLocale_ko_KR.jar"
  Delete "$INSTDIR\lib\derbyLocale_ja_JP.jar"
  Delete "$INSTDIR\lib\derbyLocale_it.jar"
  Delete "$INSTDIR\lib\derbyLocale_hu.jar"
  Delete "$INSTDIR\lib\derbyLocale_fr.jar"
  Delete "$INSTDIR\lib\derbyLocale_es.jar"
  Delete "$INSTDIR\lib\derbyLocale_de_DE.jar"
  Delete "$INSTDIR\lib\derbyLocale_cs.jar"
  Delete "$INSTDIR\lib\derbyclient.jar"
  Delete "$INSTDIR\lib\derby.war"
  Delete "$INSTDIR\lib\derby.jar"
  Delete "$INSTDIR\lib\commons-pool-1.3.jar"
  Delete "$INSTDIR\lib\commons-logging.jar"
  Delete "$INSTDIR\lib\commons-logging-api.jar"
  Delete "$INSTDIR\lib\commons-lang-2.3.jar"
  Delete "$INSTDIR\lib\commons-httpclient-3.0.1.jar"
  Delete "$INSTDIR\lib\commons-fileupload-1.2.jar"
  Delete "$INSTDIR\lib\commons-dbcp-1.2.1.jar"
  Delete "$INSTDIR\lib\commons-codec-1.3.jar"
  Delete "$INSTDIR\lib\chardet.jar"
  
  Delete "$SMPROGRAMS\ThreatFactor\Uninstall.lnk"
  Delete "$SMPROGRAMS\ThreatFactor\Website.lnk"
  Delete "$DESKTOP\ThreatFactor NSIA.lnk"
  Delete "$SMPROGRAMS\ThreatFactor\ThreatFactor NSIA.lnk"
  Delete "$SMPROGRAMS\ThreatFactor\ThreatFactor NSIA (with interactive console).lnk"
  Delete "$INSTDIR\ThreatFactor NSIA.lnk"
  Delete "$INSTDIR\ThreatFactor NSIA (with interactive console).lnk"

  RMDir "$SMPROGRAMS\ThreatFactor"
  RMDir "$INSTDIR\lib"
  RMDir "$INSTDIR\etc"
  RMDir "$INSTDIR\bin"
  RMDir "$INSTDIR"

  DeleteRegKey ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"
  SetAutoClose true
SectionEnd


; ----------------------------------------------------
; [START]PASSWORD AND LOGIN NAME RETRIEVER[/START]
; Get the login name and password for the first account
; ----------------------------------------------------
!include nsDialogs.nsh
!include LogicLib.nsh

!define VALIDUSERNAME "abcdefghijklmnopqrstuvwxyz -._0123456789"

Var PASSWORD1
Var PASSWORD2
Var LOGINNAME
Var DEFAULTNAME

Function LoginPasswordForm
  !insertmacro MUI_HEADER_TEXT "Create Account" "Setup the default administrator account"
  nsDialogs::Create /NOUNLOAD 1018
  Pop $0

  UserInfo::GetName
  Pop $DEFAULTNAME

  ${NSD_CreateLabel} 0 0u 100% 24u "Define the username and password of the administrator account. Make sure to remember the login information for this account since you will need it to login to the web interface."
  Pop $0

  ; Login name
  ${NSD_CreateLabel} 0 30u 25% 12u "Username:"
  Pop $0

  ${NSD_CreateText} 80u 30u 75% 12u $DEFAULTNAME
  Pop $LOGINNAME

  ${NSD_SetFocus} $LOGINNAME

  ; Password
  ${NSD_CreateLabel} 0 60u 25% 12u "Password:"
  Pop $0

  ${NSD_CreatePassword} 80u 60u 100% 12u ""
  Pop $PASSWORD1

  ; Password Confirmation
  ${NSD_CreateLabel} 0 80u 25% 12u "Confirm Password:"
  Pop $0

  ${NSD_CreatePassword} 80u 80u 75% 12u ""
  Pop $PASSWORD2

  #${NSD_CreateGroupBox} 0 30u 75% 60u "Password:"
  #Pop $0

  nsDialogs::Show

FunctionEnd

Function LoginPasswordFormLeave

  Call ValidateForm

  StrLen $3 $PASSWORDTEXT

  ${If} $3 < 8
    GetDlgItem $0 $HWNDPARENT 1 ; Next button
    #SendMessage $0 ${WM_SETTEXT} 0 "STR:Retry"
    Abort ;Do not leave the page until an acceptable login name and password are set
  ${EndIf}

FunctionEnd

Function DatabaseInitForm
  !insertmacro MUI_HEADER_TEXT "Initializing System" "NSIA is being initialized"
  nsDialogs::Create /NOUNLOAD 1018
  Pop $0
  
  ${NSD_CreateLabel} 0 40u 100% 64u "Your almost done! NSIA is being initialized (creating the database, setting up your account, etc). This should only take about a minute or so at most. Once it is done, you'll be able to start NSIA."
  Pop $0
  
  nsDialogs::Show
FunctionEnd

Var RUN_SERVICE
Var RUN_GUI
Var RUN_CLI
Var RUN_NOTHING
Var INSTALLED_SERVICE

Var RUN_SELECTED

Function RunNSIAForm
  !insertmacro MUI_HEADER_TEXT "Installation Successful" "NSIA was successfully installed; select one of the options below to start it"
  nsDialogs::Create /NOUNLOAD 1018
  Pop $0
  
  ; $INSTALLED_SERVICE = 1
  IntOp $INSTALLED_SERVICE 0 + 1
  
  ${If} $INSTALLED_SERVICE = 1
        ; Run the service
        ${NSD_CreateRadioButton} 0 30u 605% 12u "Run the NSIA Windows Service"
        Pop $RUN_SERVICE
  ${EndIf}
  
  ; Run the NSIA GUI
  ${NSD_CreateRadioButton} 0 50u 60% 12u "Run the NSIA GUI"
  Pop $RUN_GUI

  ; Run the NSIA CLI
  ${NSD_CreateRadioButton} 0 70u 60% 12u "Run NSIA with the interactive console interface"
  Pop $RUN_CLI
  
  ; Run the NSIA GUI
  ${NSD_CreateRadioButton} 0 90u 60% 12u "Don't start NSIA"
  Pop $RUN_NOTHING
  
  ; Decide which option to select as default
  ${If} $INSTALLED_SERVICE = 1
       ${NSD_Check} $RUN_SERVICE
  ${Else}
       ${NSD_Check} $RUN_GUI
  ${EndIf}
  
  nsDialogs::Show

FunctionEnd

Function RunNSIAFormLeave

  ${NSD_GetState} $RUN_SERVICE $RUN_SELECTED
  
  ${If} $RUN_SELECTED == ${BST_CHECKED}
        MessageBox MB_OK "Running Service"
  ${EndIf}
  
  ${NSD_GetState} $RUN_GUI $RUN_SELECTED

  ${If} $RUN_SELECTED == ${BST_CHECKED}
        MessageBox MB_OK "Running GUI"
  ${EndIf}
  
  ${NSD_GetState} $RUN_CLI $RUN_SELECTED

  ${If} $RUN_SELECTED == ${BST_CHECKED}
        MessageBox MB_OK "Running CLI"
  ${EndIf}
  
  ${NSD_GetState} $RUN_NOTHING $RUN_SELECTED

  ${If} $RUN_SELECTED == ${BST_CHECKED}
        MessageBox MB_OK "Running nothing"
  ${EndIf}

  nsSCM::Start "NSIA"
  StrCpy $0 "http://127.0.0.1:8080"
  call openLinkNewWindow
  
FunctionEnd

Function OpenBrowserToNSIA
  StrCpy $0 "http://127.0.0.1:8080"
  call openLinkNewWindow
FunctionEnd

;Push "value to check"
;Push "comparisonlist"
Function Validate
  Push $0
  Push $1
  Push $2
  Push $3 ;value length
  Push $4 ;count 1
  Push $5 ;tmp var 1
  Push $6 ;list length
  Push $7 ;count 2
  Push $8 ;tmp var 2
  Exch 9
  Pop $1 ;list
  Exch 9
  Pop $2 ;value
  StrCpy $0 1
  StrLen $3 $2
  StrLen $6 $1
  StrCpy $4 0
  lbl_loop:
    StrCpy $5 $2 1 $4
    StrCpy $7 0
    lbl_loop2:
      StrCpy $8 $1 1 $7
      StrCmp $5 $8 lbl_loop_next 0
      IntOp $7 $7 + 1
      IntCmp $7 $6 lbl_loop2 lbl_loop2 lbl_error
  lbl_loop_next:
  IntOp $4 $4 + 1
  IntCmp $4 $3 lbl_loop lbl_loop lbl_done
  lbl_error:
  StrCpy $0 0
  lbl_done:
  Pop $6
  Pop $5
  Pop $4
  Pop $3
  Pop $2
  Pop $1
  Exch 2
  Pop $7
  Pop $8
  Exch $0
FunctionEnd

Function ValidateForm

  Pop $0 # HWND

  ;Validate the username
  System::Call user32::GetWindowText(i$LOGINNAME,t.r0,i${NSIS_MAX_STRLEN})

  Push $0
  Push "${VALIDUSERNAME}"
  Call Validate
  pop $1
  ${If} $1 == 0
    MessageBox MB_OK|MB_ICONEXCLAMATION "The username is invalid. It must at least 8 characters consisting of alpha-numeric characters, periods, underscores or spaces."
  ${EndIf}
  strcpy $LOGINNAMETEXT $0

  ;Validate the password
  System::Call user32::GetWindowText(i$PASSWORD1,t.r0,i${NSIS_MAX_STRLEN})
  System::Call user32::GetWindowText(i$PASSWORD2,t.r1,i${NSIS_MAX_STRLEN})

  strcmp $1 "$0" CheckPasswordlength
  MessageBox MB_OK|MB_ICONEXCLAMATION "The passwords do not match."
  Abort

  CheckPasswordlength:
    #MessageBox MB_OK "The passwords match."
    StrLen $3 $1

    ${If} $3 < 8
      MessageBox MB_OK|MB_ICONEXCLAMATION "The password must contain at least 8 characters."
    ${EndIf}

    strcpy $PASSWORDTEXT $0

FunctionEnd

; ----------------------------------------------------
; [END]PASSWORD AND LOGIN NAME RETRIEVER[/END]
; ----------------------------------------------------


; ----------------------------------------------------
; [START]FUNCTION TO OPEN LINK IN NEW BROWSER WINDOW[/START]
; ----------------------------------------------------
# Uses $0
Function openLinkNewWindow
  Push $3
  Push $2
  Push $1
  Push $0
  ReadRegStr $0 HKCR "http\shell\open\command" ""
# Get browser path
    DetailPrint $0
  StrCpy $2 '"'
  StrCpy $1 $0 1
  StrCmp $1 $2 +2 # if path is not enclosed in " look for space as final char
    StrCpy $2 ' '
  StrCpy $3 1
  loop:
    StrCpy $1 $0 1 $3
    DetailPrint $1
    StrCmp $1 $2 found
    StrCmp $1 "" found
    IntOp $3 $3 + 1
    Goto loop

  found:
    StrCpy $1 $0 $3
    StrCmp $2 " " +2
      StrCpy $1 '$1"'

  Pop $0
  Exec '$1 $0'
  Pop $1
  Pop $2
  Pop $3
FunctionEnd
; ----------------------------------------------------
; [END]FUNCTION TO OPEN LINK IN NEW BROWSER WINDOW[/END]
; ----------------------------------------------------


; ----------------------------------------------------
; [START]DATABASE INITIALIZER[/START]
; The section below initializes the database and installs the user
; ----------------------------------------------------

Function CompleteInstall

  ; change for your purpose (-jar etc.)
  ;${GetParameters} $1
  #StrCpy $0 '"$R0" -classpath "${CLASSPATH}" ${CLASS} $1'
  StrCpy $0 '"$JREPATH" -jar "$INSTDIR/bin/nsia.jar" --install "$LOGINNAMETEXT" "$LOGINNAMETEXT" "$PASSWORDTEXT"'
  
  #SetOutPath $EXEDIR
  ExecWait $0
FunctionEnd

; ----------------------------------------------------
; [END]DATABASE INITIALIZER[/END]
; ----------------------------------------------------





; ----------------------------------------------------
; [START]JAVA INSTALLER[/START]
; The section below is for the Java Installer
; ----------------------------------------------------
!define CLASSPATH "sample.jar"
!define CLASS "Sample"


; Definitions for Java 6.0
!define JRE_VERSION "6.0"
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=20288&/jre-6u6-windows-i586-p.exe"
;!define JRE_VERSION "5.0"
;!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=18675&/jre-1_5_0_15-windows-i586-p.exe"

; use javaw.exe to avoid dosbox.
; use java.exe to keep stdout/stderr
!define JAVAEXE "javaw.exe"

#RequestExecutionLevel user
#SilentInstall silent
#AutoCloseWindow true
#ShowInstDetails nevershow

!include "FileFunc.nsh"
!insertmacro GetFileVersion
!insertmacro GetParameters
!include "WordFunc.nsh"
!insertmacro VersionCompare

;  returns the full path of a valid java.exe
;  looks in:
;  1 - .\jre directory (JRE Installed with application)
;  2 - JAVA_HOME environment variable
;  3 - the registry
;  4 - hopes it is in current dir or PATH
Function GetJRE
    Push $R0
    Push $R1
    Push $2
  
  ; 1) Check local JRE
  CheckLocal:
    ClearErrors
    StrCpy $R0 "$EXEDIR\jre\bin\${JAVAEXE}"
    IfFileExists $R0 JreFound

  ; 2) Check for JAVA_HOME
  CheckJavaHome:
    ClearErrors
    ReadEnvStr $R0 "JAVA_HOME"
    StrCpy $R0 "$R0\bin\${JAVAEXE}"
    IfErrors CheckRegistry
    IfFileExists $R0 0 CheckRegistry
    Call CheckJREVersion
    IfErrors CheckRegistry JreFound

  ; 3) Check for registry
  CheckRegistry:
    ClearErrors
    ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
    StrCpy $R0 "$R0\bin\${JAVAEXE}"
    IfErrors DownloadJRE
    IfFileExists $R0 0 DownloadJRE
    Call CheckJREVersion
    IfErrors DownloadJRE JreFound

  DownloadJRE:
    #Call ElevateToAdmin
    MessageBox MB_ICONINFORMATION "${PRODUCT_NAME} uses Java Runtime Environment ${JRE_VERSION}, it will now be downloaded and installed."
    StrCpy $2 "$TEMP\Java Runtime Environment.exe"
    nsisdl::download /TIMEOUT=30000 ${JRE_URL} $2
    Pop $R0 ;Get the return value
    StrCmp $R0 "success" +3
      MessageBox MB_ICONSTOP "Download failed: $R0"
      Abort
    ExecWait $2
    Delete $2

    ReadRegStr $R1 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    ReadRegStr $R0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$R1" "JavaHome"
    StrCpy $R0 "$R0\bin\${JAVAEXE}"
    IfFileExists $R0 0 GoodLuck
    Call CheckJREVersion
    IfErrors GoodLuck JreFound

  ; 4) wishing you good luck
  GoodLuck:
    StrCpy $R0 "${JAVAEXE}"
    ; MessageBox MB_ICONSTOP "Cannot find appropriate Java Runtime Environment."
    ; Abort

  JreFound:
    Pop $2
    Pop $R1
    Exch $R0
FunctionEnd

; Pass the "javaw.exe" path by $R0
Function CheckJREVersion
    Push $R1

    ; Get the file version of javaw.exe
    ${GetFileVersion} $R0 $R1
    ${VersionCompare} ${JRE_VERSION} $R1 $R1

    ; Check whether $R1 != "1"
    ClearErrors
    StrCmp $R1 "1" 0 CheckDone
    SetErrors

  CheckDone:
    Pop $R1
FunctionEnd
; ----------------------------------------------------
; [END]JAVA INSTALLER[/END]
; ----------------------------------------------------