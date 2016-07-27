@echo off

cd C:\LifePics\LabCenter
del install.xml
rmdir /S /Q run

copy StartLabCenter.lnk "C:\Documents and Settings\All Users\Desktop\StartLabCenter.lnk"
cd C:\Documents and Settings\All Users\Start Menu\Programs
mkdir LifePics
copy "C:\LifePics\LabCenter\StartLabCenter.lnk" "C:\Documents and Settings\All Users\Start Menu\Programs\LifePics\StartLabCenter.lnk"

cd C:\LifePics\LabCenter

echo LabCenter Installation Complete!
call StartLabCenter.lnk
echo Starting LabCenter. Please wait...
pause
del labcenter.cmd
