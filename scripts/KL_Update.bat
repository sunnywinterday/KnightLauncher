@echo off
REM by Crowfunder
REM my gh: https://github.com/Crowfunder
REM KnightLauncher gh: https://github.com/lucasluqui/KnightLauncher
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

REM Set Constant Vars
REM I have no idea how to insert a var into cutting from string.
REM For now "download_url" is purely informational.
SET regkeys[1]="HKEY_CURRENT_USER\SOFTWARE\Grey Havens\Spiral Knights"
SET regkeys[2]="HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\Valve\Steam"
SET api_url="https://api.github.com/repos/lucasluqui/KnightLauncher/releases/latest"
SET download_url="https://github.com/lucasluqui/KnightLauncher/releases/download/"
SET filename=KnightLauncher.zip
SET javaurl="https://javadl.oracle.com/webapps/download/AutoDL?BundleId=245057_d3c52aa6bfa54d3ca74e617f18309292"
SET javafname=jre_kl.exe
SET separator=--------------------------------------------------------------------------------

REM gamepathfailsafe gets triggered once the script finds a suitable SK path.
REM Later on, the script checks if it was triggered. 
REM If it was not - user is notified and prompted to manually enter the SK folder path.
SET /A gamepathfailsafe=0

REM Check if the script is used inside the SK folder. 
REM If that's the case we skip whole game path detection.
IF EXIST rsrc IF EXIST scenes IF EXIST code\projectx-pcode.jar ( 
    SET gamepath="%cd%"
    SET /A gamepathfailsafe=1
    GOTO Install 
)

REM Check if SK could've been installed anywhere. 
REM If not, prompt the user to manually enter the SK folder path and skip detection.
REM Simply, per every "RegKey Not Found" error, the script increments the noflag counter.
REM It gets handled in manner:
REM noflag = 0   User has to choose whether to install for Steam or for standalone.
REM noflag = 1   Check which installation is present.
REM noflag = 2   Prompt user to manually enter the SK folder path and skip detection.
SET /A noflag=0
setlocal enabledelayedexpansion
FOR /L %%i in (1,1,2) do (
    REG QUERY !regkeys[%%i]! >nul 2>&1!
    IF !errorlevel! EQU 1 (
        SET /A noflag=!noflag! + 1
    )
)
:NoflagLoop
IF !noflag! EQU 0 (
    ECHO Where would you like your KnightLauncher installed? Pick a number.
    ECHO 1 Steam
    ECHO 2 Standalone
    SET /P "installchoice=Your choice: "
    IF "!installchoice!" EQU "1" (
        GOTO SteamPath
    )
    IF "!installchoice!" EQU "2" (
        rem
    )
    IF "!installchoice!" NEQ "1" IF "!installchoice!" NEQ "2" (
        ECHO Wrong choice! Choose 1 or 2.
        GOTO NoflagLoop
    )
)
IF !noflag! EQU 2 (
    GOTO Install
)

REM Check if it's standalone, if not, skip to SteamPath.
REG QUERY %regkeys[1]% >nul 2>&1
IF %errorlevel% EQU 1 (
    SETLOCAL disabledelayedexpansion
    GOTO SteamPath
) ELSE (
    FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY !regkeys[1]! /v INSTALL_DIR_REG_KEY`) DO (
        SET gamepath="%%A %%B"
        IF EXIST !gamepath! ( SET /A gamepathfailsafe=1 )
    )
    SETLOCAL disabledelayedexpansion
    GOTO Install
)


REM Getting Steam Folder Path. We check if SK is installed in default Steam folder.
REM Otherwise we parse libraryfolders.vdf file and check every other folder.
:SteamPath
FOR /F "usebackq tokens=3*" %%A IN (`REG QUERY %regkeys[2]% /v InstallPath`) DO (
    IF "%%B" EQU "" ( SET appdir=%%A) ELSE (
        SET appdir=%%A %%B
        )
    )

IF EXIST "%appdir%\steamapps\common\Spiral Knights" (
    SET gamepath="%appdir%\steamapps\common\Spiral Knights"
    SET /A gamepathfailsafe=1
    GOTO Install
    )
SET listfile="%appdir%\steamapps\libraryfolders.vdf"


REM If user has more than 9 steam library folders the script will commit die.
REM I have no idea how to handle that.
REM I will take a look :)
SET /A i=0
SETLOCAL enabledelayedexpansion
FOR /F "tokens=* skip=4 usebackq" %%a in (%listfile%) do (
    IF /I "%%a" NEQ "}" IF /I "%%a" NEQ "" ( 
        SET str="%%a"
        SET str=!str:~7,-1!
        SET /A i+=1
        SET pathlist[!i!]=!str!
      )
)

REM We check every steam dir
SET /A Filesx=!i!
FOR /L %%i in (1,1,!Filesx!) do (
    IF EXIST "!pathlist[%%i]!\steamapps\common\Spiral Knights" (
        SET gamepath="!pathlist[%%i]!\steamapps\common\Spiral Knights"
    )
) 
SETLOCAL disabledelayedexpansion

SET /A gamepathfailsafe=1

:Install
    IF %gamepathfailsafe% EQU 0 (
        ECHO Unable to find Spiral Knights' folder.
        SET /P gamepath=Run the script inside it or please enter its path:   
    ) 
    IF NOT EXIST %gamepath%\rsrc ( 
	    IF NOT EXIST %gamepath%\scenes (
		    IF NOT EXIST %gamepath%\code\projectx-pcode.jar (
    		    SET /A gamepathfailsafe=0
    	    )	
        )		
    )

    REM Simple asking for confirmation, just in case.
    %gamepath:~1,2%
    SET buff=false
    CD "%gamepath%"
    ECHO KnightLauncher will be installed/updated in this folder: %gamepath%
    SET /P opt=Would you like to proceed? (Y/N) 
    IF /I %opt% EQU y ( SET buff=true )
    IF /I %opt% EQU yes ( SET buff=true )
    IF /I %buff% NEQ true ( EXIT /b )

    REM Check if Java is properly installed. Download and install if it's not.
    SETLOCAL EnableDelayedExpansion
    javaw >nul 2>&1!
    IF "%errorlevel%" EQU "9009" (
        ECHO You need Java installed on your machine to use KnightLauncher.
	    IF NOT EXIST jre_kl.exe (
		    ECHO Downloading...
		    ECHO %separator%
		    curl.exe !javaurl! -o !javafname! -L
		    ECHO %separator%
		    ECHO Success. 
	    )
        ECHO Please proceed with the installation. The script will restart itself afterwards.
        PING -n 4 127.0.0.1>nul
        !javafname!
        START KL_Update.bat & EXIT
    )
    DEL /F /Q !javafname!
    SETLOCAL DisableDelayedExpansion

    REM Checking if other versions are installed.
    REM If there are - remove them retaining "KnightLauncher.properties".
    REM Also trying to kill the KnightLauncher process just in case.
    IF EXIST *KnightLauncher* ( 
        ECHO Detected other version installed, removing...
        TASKKILL /IM javaw.exe >nul 2>&1!
        IF EXIST KnightLauncher.properties ( REN KnightLauncher.properties move.properties)
        DEL *KnightLauncher*
        IF EXIST move.properties ( REN move.properties KnightLauncher.properties )
        ECHO Success!
    )

    REM Preparing installation info.
    ECHO Downloading...
    curl.exe -sSL %api_url% | findstr browser_download_url > temp 
    SET /P url=<temp
    DEL temp
    SET url=%url:"=%
    SET url=%url:      browser_download_url: =%

    REM Downloading, installing and running the new version.
    REM NOTE: If "tar" fails - update to Windows 10, or update your Windows 10. Build 17063 at least.
    ECHO %separator%
    curl.exe %url% -o %filename% -L
    ECHO %separator%
    tar -xf %filename% & DEL %filename% & KnightLauncher_windows.bat
    ECHO Success!

exit /b 0
